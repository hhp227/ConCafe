import {setGlobalOptions} from "firebase-functions";
import * as functionsV1 from "firebase-functions/v1";
import {getAuth, UserRecord} from "firebase-admin/auth";
import {onDocumentDeleted, onDocumentWritten} from "firebase-functions/v2/firestore";
import {onRequest} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {defineSecret} from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import {initializeApp} from "firebase-admin/app";
import {FieldPath, GeoPoint, getFirestore, FieldValue} from "firebase-admin/firestore";
import {getMessaging} from "firebase-admin/messaging";
import {getStorage} from "firebase-admin/storage";
import {createHash} from "node:crypto";

setGlobalOptions({ maxInstances: 10 });

initializeApp();

function db() {
  return getFirestore();
}

type DeletedUserCleanupSummary = {
  removedOwnerCafeCount: number;
  unlinkedCastCount: number;
  deletedCastClaims: number;
  deletedCafeOwnerClaims: number;
  deletedCafeRegistrationClaims: number;
  ownerQueryError: string | null;
  castQueryError: string | null;
  userQueryError: string | null;
  castClaimsCleanupError: string | null;
  cafeOwnerClaimsCleanupError: string | null;
  cafeRegistrationClaimsCleanupError: string | null;
  ownerCleanupError: string | null;
  castCleanupError: string | null;
  userTreeCleanupError: string | null;
  unexpectedError: string | null;
};

const USER_DELETION_REQUESTS = "_userDeletionRequests";

function asErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
}

function emptyDeletedUserCleanupSummary(): DeletedUserCleanupSummary {
  return {
    removedOwnerCafeCount: 0,
    unlinkedCastCount: 0,
    deletedCastClaims: 0,
    deletedCafeOwnerClaims: 0,
    deletedCafeRegistrationClaims: 0,
    ownerQueryError: null,
    castQueryError: null,
    userQueryError: null,
    castClaimsCleanupError: null,
    cafeOwnerClaimsCleanupError: null,
    cafeRegistrationClaimsCleanupError: null,
    ownerCleanupError: null,
    castCleanupError: null,
    userTreeCleanupError: null,
    unexpectedError: null,
  };
}

async function cleanupDeletedUserData(
  firestore: FirebaseFirestore.Firestore,
  userId: string
): Promise<DeletedUserCleanupSummary> {
  const summary = emptyDeletedUserCleanupSummary();
  let ownerQueryError: string | null = null;
  let castQueryError: string | null = null;
  let userQueryError: string | null = null;
  let cafesSnapshot: FirebaseFirestore.QuerySnapshot<FirebaseFirestore.DocumentData>;
  let castRefs: FirebaseFirestore.DocumentReference[] = [];
  let ownedCafeIds: string[] = [];

  try {
    cafesSnapshot = await firestore
      .collection("cafes")
      .where("ownerIds", "array-contains", userId)
      .get();
  } catch (error) {
    ownerQueryError = asErrorMessage(error, "owner query failed");
    logger.error("cleanupDeletedUserData owner cafe query failed.", {
      userId: userId,
      error: ownerQueryError,
    });
    cafesSnapshot = await firestore.collection("cafes").where(FieldPath.documentId(), "==", "__none__").get();
  }

  try {
    const castsSnapshot = await firestore
      .collectionGroup("casts")
      .where("linkedUserId", "==", userId)
      .get();
    castRefs = castsSnapshot.docs.map((doc) => doc.ref);
  } catch (error) {
    castQueryError = asErrorMessage(error, "cast query failed");
    logger.error("cleanupDeletedUserData cast query failed.", {
      userId: userId,
      error: castQueryError,
    });
  }

  try {
    const userSnapshot = await firestore
      .collection("users")
      .doc(userId)
      .get();
    ownedCafeIds = asStringArray(userSnapshot.data()?.ownedCafeIds);
  } catch (error) {
    userQueryError = asErrorMessage(error, "user query failed");
    logger.error("cleanupDeletedUserData user doc query failed.", {
      userId: userId,
      error: userQueryError,
    });
  }

  const claimCollections = ["castClaims", "cafeOwnerClaims", "cafeRegistrationClaims"];
  const claimDeleteCounts = new Map<string, number>();
  const claimCleanupErrors = new Map<string, string | null>();
  const BATCH_SIZE = 500;

  for (const collectionName of claimCollections) {
    try {
      const snapshot = await firestore
        .collection(collectionName)
        .where("userId", "==", userId)
        .get();

      if (snapshot.empty) {
        claimDeleteCounts.set(collectionName, 0);
        claimCleanupErrors.set(collectionName, null);
        continue;
      }

      let deletedCount = 0;
      for (let i = 0; i < snapshot.docs.length; i += BATCH_SIZE) {
        const batch = firestore.batch();
        const chunk = snapshot.docs.slice(i, i + BATCH_SIZE);
        chunk.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
        deletedCount += chunk.length;
      }
      claimDeleteCounts.set(collectionName, deletedCount);
      claimCleanupErrors.set(collectionName, null);
    } catch (error) {
      const cleanupError = asErrorMessage(error, `${collectionName} cleanup failed`);
      claimDeleteCounts.set(collectionName, 0);
      claimCleanupErrors.set(collectionName, cleanupError);
      logger.error("cleanupDeletedUserData claim cleanup failed.", {
        userId: userId,
        collectionName: collectionName,
        error: cleanupError,
      });
    }
  }

  const cafeRefMap = new Map<string, FirebaseFirestore.DocumentReference>();
  cafesSnapshot.docs.forEach((doc) => cafeRefMap.set(doc.id, doc.ref));
  for (const cafeId of ownedCafeIds) {
    if (cafeId && !cafeRefMap.has(cafeId)) {
      cafeRefMap.set(cafeId, firestore.collection("cafes").doc(cafeId));
    }
  }

  const cafeRefs = [...cafeRefMap.values()];
  let ownerCleanupError: string | null = null;
  let castCleanupError: string | null = null;
  let userTreeCleanupError: string | null = null;

  if (cafeRefs.length > 0) {
    try {
      for (let i = 0; i < cafeRefs.length; i += BATCH_SIZE) {
        const batch = firestore.batch();
        cafeRefs.slice(i, i + BATCH_SIZE).forEach((ref) => {
          batch.set(ref, {ownerIds: FieldValue.arrayRemove(userId)}, {merge: true});
        });
        await batch.commit();
      }
    } catch (error) {
      ownerCleanupError = asErrorMessage(error, "owner cleanup failed");
      logger.error("cleanupDeletedUserData ownerIds cleanup failed.", {
        userId: userId,
        error: ownerCleanupError,
      });
    }
  }

  if (castRefs.length > 0) {
    try {
      for (let i = 0; i < castRefs.length; i += BATCH_SIZE) {
        const batch = firestore.batch();
        castRefs.slice(i, i + BATCH_SIZE).forEach((ref) => {
          batch.set(ref, {
            linkedUserId: null,
            userId: FieldValue.delete(),
            uid: FieldValue.delete(),
          }, {merge: true});
        });
        await batch.commit();
      }
    } catch (error) {
      castCleanupError = asErrorMessage(error, "cast cleanup failed");
      logger.error("cleanupDeletedUserData cast unlink failed.", {
        userId: userId,
        error: castCleanupError,
      });
    }
  }

  try {
    await firestore.recursiveDelete(firestore.collection("users").doc(userId));
  } catch (error) {
    userTreeCleanupError = asErrorMessage(error, "user recursive delete failed");
    logger.error("cleanupDeletedUserData user tree delete failed.", {
      userId: userId,
      error: userTreeCleanupError,
    });
  }

  summary.removedOwnerCafeCount = cafeRefs.length;
  summary.unlinkedCastCount = castRefs.length;
  summary.deletedCastClaims = claimDeleteCounts.get("castClaims") ?? 0;
  summary.deletedCafeOwnerClaims = claimDeleteCounts.get("cafeOwnerClaims") ?? 0;
  summary.deletedCafeRegistrationClaims = claimDeleteCounts.get("cafeRegistrationClaims") ?? 0;
  summary.ownerQueryError = ownerQueryError;
  summary.castQueryError = castQueryError;
  summary.userQueryError = userQueryError;
  summary.castClaimsCleanupError = claimCleanupErrors.get("castClaims") ?? null;
  summary.cafeOwnerClaimsCleanupError = claimCleanupErrors.get("cafeOwnerClaims") ?? null;
  summary.cafeRegistrationClaimsCleanupError = claimCleanupErrors.get("cafeRegistrationClaims") ?? null;
  summary.ownerCleanupError = ownerCleanupError;
  summary.castCleanupError = castCleanupError;
  summary.userTreeCleanupError = userTreeCleanupError;

  return summary;
}

type ReviewLike = {
  id?: unknown;
  cafeId?: unknown;
  rating?: unknown;
  userId?: unknown;
  taggedCastIds?: unknown;
  visitVerified?: unknown;
};

type VisitLike = {
  cafeId?: unknown;
  userId?: unknown;
  verified?: unknown;
  location?: unknown;
  verificationDistanceMeters?: unknown;
  verificationFailureReason?: unknown;
  allowedRadiusMeters?: unknown;
};

type CafeFavoriteLike = {
  cafeId?: unknown;
  userId?: unknown;
};

type CastFollowLike = {
  userId?: unknown;
  castId?: unknown;
  cafeId?: unknown;
};

type StampLike = {
  userId?: unknown;
  cafeId?: unknown;
  visitId?: unknown;
};

type CastClaimLike = {
  castId?: unknown;
  castName?: unknown;
  cafeId?: unknown;
  userId?: unknown;
  status?: unknown;
  requesterNickname?: unknown;
  requesterProfileImage?: unknown;
  createdAt?: unknown;
};

type CafeRegistrationClaimLike = {
  userId?: unknown;
  cafeName?: unknown;
  status?: unknown;
  requestedAt?: unknown;
  imageUrl?: unknown;
  approvedCafeId?: unknown;
};

type CafeOwnerClaimLike = {
  userId?: unknown;
  cafeId?: unknown;
  cafeName?: unknown;
  status?: unknown;
  requestedAt?: unknown;
};

type FanAnnouncementRequestLike = {
  userId?: unknown;
  cafeId?: unknown;
  castId?: unknown;
  title?: unknown;
  body?: unknown;
  createdAt?: unknown;
};

type CastScheduleLike = {
  castId?: unknown;
  cafeId?: unknown;
  date?: unknown;
  status?: unknown;
  startTime?: unknown;
  endTime?: unknown;
};

type NoticeLike = {
  title?: unknown;
  content?: unknown;
  createdAt?: unknown;
};

type EventLike = {
  title?: unknown;
  desc?: unknown;
  relatedCastId?: unknown;
  createdAt?: unknown;
};

type HomeBannerLike = {
  title?: unknown;
  subtitle?: unknown;
  imageUrl?: unknown;
  relatedCafeId?: unknown;
  linkType?: unknown;
  linkTarget?: unknown;
  displayDays?: unknown;
  status?: unknown;
  createdAtEpochMillis?: unknown;
  activatedAtEpochMillis?: unknown;
};

type NotificationQuietHoursMode = "OFF" | "NIGHT" | "ALL_DAY";

type UserNotificationSettings = {
  isPushNotificationsEnabled: boolean;
  isShiftNotificationsEnabled: boolean;
  isBirthdayNotificationsEnabled: boolean;
  isNoticeNotificationsEnabled: boolean;
  isFollowNotificationsEnabled: boolean;
  isEventNotificationsEnabled: boolean;
  quietHoursMode: NotificationQuietHoursMode;
};

function asPlainObject(value: unknown): Record<string, unknown> | null {
  if (typeof value !== "object" || value == null || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, unknown>;
}

function asNonNegativeInt(value: unknown): number | null {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return null;
  }
  return Math.max(0, Math.floor(value));
}

function asFiniteNumber(value: unknown): number | null {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return null;
  }
  return value;
}

function asNonBlankString(value: unknown): string | null {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

function asDate(value: unknown): Date | null {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }
  if (typeof value === "object" && value != null && typeof (value as {toDate?: unknown}).toDate === "function") {
    const resolved = ((value as {toDate: () => Date}).toDate)();
    return Number.isNaN(resolved.getTime()) ? null : resolved;
  }
  if (typeof value === "string" || typeof value === "number") {
    const resolved = new Date(value);
    return Number.isNaN(resolved.getTime()) ? null : resolved;
  }
  return null;
}

function asStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .filter((item) => typeof item === "string")
    .map((item) => (item as string).trim())
    .filter((item) => item.length > 0);
}

function userCafeKey(cafeId: string, userId: string): string {
  return `${cafeId}::${userId}`;
}

function parseUserCafeKey(key: string): {cafeId: string; userId: string} {
  const splitIndex = key.indexOf("::");
  if (splitIndex < 0) {
    return {cafeId: "", userId: ""};
  }
  return {
    cafeId: key.substring(0, splitIndex),
    userId: key.substring(splitIndex + 2),
  };
}

function castUserTargetKey(cafeId: string, castId: string, userId: string): string {
  return `${cafeId}::${castId}::${userId}`;
}

function parseCastUserTargetKey(key: string): {cafeId: string; castId: string; userId: string} {
  const firstSplitIndex = key.indexOf("::");
  const secondSplitIndex = key.indexOf("::", firstSplitIndex + 2);

  if (firstSplitIndex < 0 || secondSplitIndex < 0) {
    return {cafeId: "", castId: "", userId: ""};
  }
  return {
    cafeId: key.substring(0, firstSplitIndex),
    castId: key.substring(firstSplitIndex + 2, secondSplitIndex),
    userId: key.substring(secondSplitIndex + 2),
  };
}

function buildCastFollowDocumentId(userId: string, castId: string): string {
  const normalizedUserId = userId.replace(/\//g, "_");
  const normalizedCastId = castId.replace(/\//g, "_");
  return `${normalizedUserId}_${normalizedCastId}`;
}

function buildCastVisitCertificationDocumentId(cafeId: string, castId: string, userId: string): string {
  return `${cafeId.replace(/\//g, "_")}__${castId.replace(/\//g, "_")}__${userId.replace(/\//g, "_")}`;
}

function sanitizeNotificationDocumentId(value: string): string {
  return value.replace(/\//g, "_").trim();
}

const NOTIFICATION_FANOUT_CHUNK_SIZE = 100;
const NOTIFICATION_FANOUT_MAX_RECIPIENTS = 1000;

function toNotificationRecipientUserIds(
  userIds: Array<string | null | undefined>,
  excludedUserIds: string[] = []
): {targets: string[]; droppedByCap: number} {
  const excluded = new Set(excludedUserIds.filter((userId) => userId.trim().length > 0));
  const unique = new Set<string>();

  userIds.forEach((rawUserId) => {
    const userId = rawUserId == null ? "" : rawUserId.trim();

    if (userId.length == 0 || excluded.has(userId)) {
      return;
    }
    unique.add(userId);
  });

  const allTargets = Array.from(unique);
  const targets = allTargets.slice(0, NOTIFICATION_FANOUT_MAX_RECIPIENTS);

  return {
    targets: targets,
    droppedByCap: Math.max(0, allTargets.length - targets.length),
  };
}

async function processInBatches<T>(
  items: T[],
  worker: (item: T) => Promise<void>,
  chunkSize: number = NOTIFICATION_FANOUT_CHUNK_SIZE
): Promise<void> {
  for (let index = 0; index < items.length; index += chunkSize) {
    const chunk = items.slice(index, index + chunkSize);
    await Promise.all(chunk.map(async (item) => worker(item)));
  }
}

function kstNow(): Date {
  const now = new Date();
  const utcMillis = now.getTime() + (now.getTimezoneOffset() * 60 * 1000);
  return new Date(utcMillis + (9 * 60 * 60 * 1000));
}

function kstDateKey(date: Date): string {
  const year = date.getUTCFullYear();
  const month = `${date.getUTCMonth() + 1}`.padStart(2, "0");
  const day = `${date.getUTCDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function kstBirthdayKey(date: Date): string {
  const month = `${date.getUTCMonth() + 1}`.padStart(2, "0");
  const day = `${date.getUTCDate()}`.padStart(2, "0");
  return `${month}-${day}`;
}

function readNotificationSettings(data: unknown): UserNotificationSettings {
  const plain = asPlainObject(data);
  const isPushEnabled = plain?.isPushNotificationsEnabled !== false;
  const isShiftEnabled = plain?.isShiftNotificationsEnabled !== false;
  const isBirthdayEnabled = plain?.isBirthdayNotificationsEnabled !== false;
  const isNoticeEnabled = plain?.isNoticeNotificationsEnabled !== false;
  const isFollowEnabled = plain?.isFollowNotificationsEnabled !== false;
  const isEventEnabled = plain?.isEventNotificationsEnabled !== false;
  const quietHoursRaw = asNonBlankString(plain?.quietHoursMode)?.toUpperCase();
  const quietHoursMode: NotificationQuietHoursMode =
    quietHoursRaw === "NIGHT" || quietHoursRaw === "ALL_DAY" ? quietHoursRaw : "OFF";

  return {
    isPushNotificationsEnabled: isPushEnabled,
    isShiftNotificationsEnabled: isShiftEnabled,
    isBirthdayNotificationsEnabled: isBirthdayEnabled,
    isNoticeNotificationsEnabled: isNoticeEnabled,
    isFollowNotificationsEnabled: isFollowEnabled,
    isEventNotificationsEnabled: isEventEnabled,
    quietHoursMode: quietHoursMode,
  };
}

function isQuietHoursPushSuppressed(settings: UserNotificationSettings): boolean {
  if (settings.quietHoursMode === "ALL_DAY") {
    return true;
  } else if (settings.quietHoursMode === "OFF") {
    return false;
  } else {
    const hour = kstNow().getUTCHours();
    return hour >= 23 || hour < 8;
  }
}

async function loadUserNotificationSettings(userId: string): Promise<UserNotificationSettings> {
  const snapshot = await db()
    .collection("users")
    .doc(userId)
    .collection("notificationSettings")
    .doc("default")
    .get();

  if (!snapshot.exists) {
    return {
      isPushNotificationsEnabled: true,
      isShiftNotificationsEnabled: true,
      isBirthdayNotificationsEnabled: true,
      isNoticeNotificationsEnabled: true,
      isFollowNotificationsEnabled: true,
      isEventNotificationsEnabled: true,
      quietHoursMode: "OFF",
    };
  } else {
    return readNotificationSettings(snapshot.data());
  }
}

function isPendingApprovalStatus(status: string | null): boolean {
  if (status == null) {
    return true;
  }
  if (status === "PENDING" || status === "승인 대기 중") {
    return true;
  }
  return status.includes("승인 대기");
}

function isApprovedStatus(status: string | null): boolean {
  if (status == null) {
    return false;
  }
  return status === "APPROVED" || status === "승인 완료";
}

function isRejectedStatus(status: string | null): boolean {
  if (status == null) {
    return false;
  }
  if (status === "REJECTED" || status === "반려") {
    return true;
  }
  return status.includes("반려");
}

async function loadAdminUserIds(): Promise<string[]> {
  const snapshot = await db()
    .collection("users")
    .where("role", "==", "ADMIN")
    .get();

  return snapshot.docs
    .map((doc) => asNonBlankString(doc.id))
    .filter((value): value is string => value != null);
}

async function loadCafeOwnerUserIds(cafeId: string): Promise<string[]> {
  const snapshot = await db()
    .collection("users")
    .where("affiliatedCafeId", "==", cafeId)
    .where("role", "==", "CAFE_OWNER")
    .get();

  return snapshot.docs
    .map((doc) => asNonBlankString(doc.id))
    .filter((value): value is string => value != null);
}

async function hasFanAnnouncementPermission(
  requesterUserId: string,
  requesterRole: string | null,
  requesterAffiliatedCafeId: string | null,
  cafeId: string,
  castId: string
): Promise<boolean> {
  if (requesterRole === "ADMIN") {
    return true;
  } else if (requesterRole === "CAFE_OWNER") {
    const userSnapshot = await db().collection("users").doc(requesterUserId).get();
    const ownedCafeIds = asStringArray(userSnapshot.data()?.ownedCafeIds);
    const cafeSnapshot = await db().collection("cafes").doc(cafeId).get();
    const ownerIds = asStringArray(cafeSnapshot.data()?.ownerIds);
    const hasOwnerPermission = ownedCafeIds.includes(cafeId) || ownerIds.includes(requesterUserId);

    if (hasOwnerPermission) {
      return true;
    } else {
      return requesterAffiliatedCafeId === cafeId;
    }
  } else if (requesterRole === "CAST") {
    const castSnapshot = await db()
      .collection("cafes")
      .doc(cafeId)
      .collection("casts")
      .doc(castId)
      .get();
    const linkedUserId = asNonBlankString(castSnapshot.data()?.linkedUserId);
    const isLinkedCast = linkedUserId === requesterUserId;

    if (isLinkedCast) {
      return true;
    } else {
      return requesterAffiliatedCafeId === cafeId;
    }
  } else {
    return false;
  }
}

async function createApprovalRequestNotifications(
  recipientUserIds: string[],
  notificationIdPrefix: string,
  type: "CAFE_APPROVAL_REQUEST" | "CAFE_OWNER_APPROVAL_REQUEST" | "CAST_CLAIM_REQUEST",
  title: string,
  body: string,
  targetId: string,
  createdAt: string
): Promise<void> {
  if (recipientUserIds.length == 0) {
    return;
  }
  const uniqueUserIds = Array.from(new Set(recipientUserIds));
  const tasks = uniqueUserIds.map(async (userId) => {
    await createUserNotification(
      userId,
      `${notificationIdPrefix}_${userId}`,
      type,
      title,
      body,
      targetId,
      createdAt
    );
  });

  await Promise.all(tasks);
}

async function createApprovalResultNotification(
  recipientUserId: string | null,
  notificationIdPrefix: string,
  type:
    | "CAFE_APPROVED"
    | "CAFE_OWNER_APPROVED"
    | "CAST_CLAIM_APPROVED"
    | "CAFE_REJECTED"
    | "CAFE_OWNER_REJECTED"
    | "CAST_CLAIM_REJECTED",
  title: string,
  body: string,
  targetId: string,
  createdAt: string
): Promise<void> {
  if (recipientUserId == null) {
    return;
  }
  await createUserNotification(
    recipientUserId,
    `${notificationIdPrefix}_${recipientUserId}`,
    type,
    title,
    body,
    targetId,
    createdAt
  );
}

async function createUserNotification(
  userId: string,
  notificationId: string,
  type:
    | "CAST_SHIFT"
    | "BIRTHDAY"
    | "CAFE_NOTICE"
    | "CAFE_EVENT"
    | "FAN_ANNOUNCEMENT"
    | "FOLLOW_UPDATE"
    | "CAFE_APPROVAL_REQUEST"
    | "CAFE_OWNER_APPROVAL_REQUEST"
    | "CAST_CLAIM_REQUEST"
    | "CAFE_APPROVED"
    | "CAFE_OWNER_APPROVED"
    | "CAST_CLAIM_APPROVED"
    | "CAFE_REJECTED"
    | "CAFE_OWNER_REJECTED"
    | "CAST_CLAIM_REJECTED"
    | "CAFE_CHECK_IN"
    | "CAFE_TABLE_COUNT_UPDATE"
    | "COMMUNITY_COMMENT"
    | "COMMUNITY_LIKE",
  title: string,
  body: string,
  targetId: string,
  createdAt: string,
  settings: UserNotificationSettings | null = null
): Promise<void> {
  const sanitizedId = sanitizeNotificationDocumentId(notificationId);
  const userRef = db().collection("users").doc(userId);
  const notificationRef = userRef.collection("notifications").doc(sanitizedId);
  try {
    await notificationRef.create(
      {
        userId: userId,
        type: type,
        title: title,
        body: body,
        targetId: targetId,
        createdAt: createdAt,
        relativeTime: "방금 전",
        isRead: false,
        updatedAt: createdAt,
      }
    );
  } catch (error) {
    const code = (error as {code?: unknown})?.code;

    if (code === 6 || code === "already-exists" || code === "ALREADY_EXISTS") {
      return;
    }
    throw error;
  }
  await sendPushToUser(userId, title, body, type, targetId, sanitizedId, settings);
}

async function sendPushToUser(
  userId: string,
  title: string,
  body: string,
  type: string,
  targetId: string,
  notificationId: string,
  settings: UserNotificationSettings | null = null
): Promise<void> {
  const resolvedSettings = settings ?? await loadUserNotificationSettings(userId);

  if (!resolvedSettings.isPushNotificationsEnabled || isQuietHoursPushSuppressed(resolvedSettings)) {
    return;
  }
  const tokenSnapshot = await db()
    .collection("users")
    .doc(userId)
    .collection("deviceTokens")
    .where("isEnabled", "==", true)
    .select("token")
    .get();

  if (tokenSnapshot.empty) {
    return;
  }
  const tokens = tokenSnapshot.docs
    .map((doc) => asNonBlankString(doc.get("token")))
    .filter((token): token is string => token !== null);

  if (tokens.length == 0) {
    return;
  }
  const response = await getMessaging().sendEachForMulticast({
    tokens: tokens,
    notification: {
      title: title,
      body: body,
    },
    data: {
      type: type,
      targetId: targetId,
      notificationId: notificationId,
    },
    apns: {
      payload: {
        aps: {
          sound: "default",
        },
      },
    },
    android: {
      priority: "high",
      notification: {
        color: "#EF6797",
        sound: "default",
      },
    },
  });

  if (response.failureCount == 0) {
    return;
  }
  const deleteTasks = response.responses.map(async (sendResponse, index) => {
    if (sendResponse.success) {
      return;
    }
    const token = tokens[index];
    const errorCode = sendResponse.error?.code ?? "";
    const shouldDeleteToken = errorCode.includes("registration-token-not-registered")
      || errorCode.includes("invalid-argument");

    if (!shouldDeleteToken) {
      return;
    }
    const targetDocs = tokenSnapshot.docs.filter((doc) => doc.get("token") === token);
    await Promise.all(targetDocs.map(async (doc) => doc.ref.delete()));
  });

  await Promise.all(deleteTasks);
}

function asGeoPoint(value: unknown): {latitude: number; longitude: number} | null {
  if (value == null || typeof value !== "object") {
    return null;
  }
  const point = value as {
    latitude?: unknown;
    longitude?: unknown;
    _latitude?: unknown;
    _longitude?: unknown;
  };
  const latitude = asFiniteNumber(point.latitude ?? point._latitude);
  const longitude = asFiniteNumber(point.longitude ?? point._longitude);

  if (latitude == null || longitude == null) {
    return null;
  }
  return {latitude, longitude};
}

function resolveBannerActivatedAtEpochMillis(banner: HomeBannerLike | undefined): number {
  const activated = asFiniteNumber(banner?.activatedAtEpochMillis);
  const created = asFiniteNumber(banner?.createdAtEpochMillis);

  if (activated != null && activated > 0) {
    return Math.floor(activated);
  } else if (created != null && created > 0) {
    return Math.floor(created);
  } else {
    return 0;
  }
}

function resolveBannerDisplayDays(banner: HomeBannerLike | undefined): number {
  const rawDisplayDays = asNonNegativeInt(banner?.displayDays);

  if (rawDisplayDays == null || rawDisplayDays <= 0) {
    return 1;
  } else {
    return rawDisplayDays;
  }
}

function isBannerActiveNow(banner: HomeBannerLike | undefined, nowEpochMillis: number): boolean {
  const status = asNonBlankString(banner?.status)?.toUpperCase() ?? "";

  if (status !== "ACTIVE") {
    return false;
  }
  const activatedAtEpochMillis = resolveBannerActivatedAtEpochMillis(banner);

  if (activatedAtEpochMillis <= 0) {
    return false;
  }
  const displayDays = resolveBannerDisplayDays(banner);
  const expireAtEpochMillis = activatedAtEpochMillis + (displayDays * 24 * 60 * 60 * 1000);
  return nowEpochMillis < expireAtEpochMillis;
}

function resolveBannerHref(linkType: string | null, linkTarget: string | null): string {
  if (linkType == null || linkTarget == null) {
    return "";
  }
  if (linkType === "EXTERNAL_LINK") {
    return linkTarget;
  } else if (linkType === "CAFE_DETAIL") {
    return `https://concafe-5f7fd.firebaseapp.com/?cafeId=${encodeURIComponent(linkTarget)}`;
  } else if (linkType === "EVENT_DETAIL") {
    return `https://concafe-5f7fd.firebaseapp.com/?eventId=${encodeURIComponent(linkTarget)}`;
  } else if (linkType === "NOTICE") {
    return `https://concafe-5f7fd.firebaseapp.com/?noticeId=${encodeURIComponent(linkTarget)}`;
  } else {
    return "";
  }
}

function resolvePublicBannerImageUrl(rawImageUrl: string | null): string | null {
  if (rawImageUrl == null) {
    return null;
  }
  const trimmed = rawImageUrl.trim();

  if (trimmed.length == 0) {
    return null;
  }
  const decodeStoragePath = (value: string): string => {
    let decoded = value.trim();

    for (let index = 0; index < 2; index += 1) {
      try {
        const nextDecoded = decodeURIComponent(decoded);

        if (nextDecoded === decoded) {
          break;
        }
        decoded = nextDecoded;
      } catch (_error) {
        break;
      }
    }
    return decoded;
  };

  if (trimmed.startsWith("gs://")) {
    const withoutScheme = trimmed.substring("gs://".length);
    const firstSlashIndex = withoutScheme.indexOf("/");

    if (firstSlashIndex <= 0 || firstSlashIndex >= withoutScheme.length - 1) {
      return null;
    }
    const bucket = withoutScheme.substring(0, firstSlashIndex).trim();
    const objectPath = decodeStoragePath(withoutScheme.substring(firstSlashIndex + 1));

    if (bucket.length == 0 || objectPath.length == 0) {
      return null;
    }
    return `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodeURIComponent(objectPath)}?alt=media`;
  }
  if (trimmed.startsWith("http://")) {
    return `https://${trimmed.substring("http://".length)}`;
  }
  if (trimmed.startsWith("https://firebasestorage.googleapis.com/")
    || trimmed.startsWith("http://firebasestorage.googleapis.com/")) {
    try {
      const normalized = trimmed.startsWith("http://")
        ? `https://${trimmed.substring("http://".length)}`
        : trimmed;
      const parsedUrl = new URL(normalized);
      const segments = parsedUrl.pathname.split("/").filter((segment) => segment.length > 0);
      const bIndex = segments.findIndex((segment) => segment === "b");
      const oIndex = segments.findIndex((segment) => segment === "o");

      if (bIndex < 0 || oIndex < 0 || oIndex <= bIndex || oIndex >= segments.length - 1) {
        return normalized;
      }
      const bucket = segments[bIndex + 1];
      const encodedObjectPath = segments.slice(oIndex + 1).join("/");
      const objectPath = decodeStoragePath(encodedObjectPath);
      const token = parsedUrl.searchParams.get("token");
      const tokenQuery = token == null || token.trim().length == 0 ? "" : `&token=${encodeURIComponent(token.trim())}`;
      return `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodeURIComponent(objectPath)}?alt=media${tokenQuery}`;
    } catch (_error) {
      return trimmed;
    }
  }
  return trimmed;
}

async function deleteStorageFileByUrl(imageUrl: string): Promise<void> {
  const trimmed = imageUrl.trim();

  if (!trimmed.startsWith("https://firebasestorage.googleapis.com/")) {
    return;
  }
  const parsedUrl = new URL(trimmed);
  const segments = parsedUrl.pathname.split("/").filter((s) => s.length > 0);
  const bIndex = segments.findIndex((s) => s === "b");
  const oIndex = segments.findIndex((s) => s === "o");

  if (bIndex < 0 || oIndex < 0 || oIndex <= bIndex || oIndex >= segments.length - 1) {
    return;
  }
  const bucketName = segments[bIndex + 1];
  const encodedObjectPath = segments.slice(oIndex + 1).join("/");
  const objectPath = decodeURIComponent(encodedObjectPath);

  await getStorage().bucket(bucketName).file(objectPath).delete();
}

function haversineMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const earthRadius = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const normalizedLat1 = (lat1 * Math.PI) / 180;
  const normalizedLat2 = (lat2 * Math.PI) / 180;
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(normalizedLat1) * Math.cos(normalizedLat2) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadius * c;
}

async function upsertVisitStamp(visitId: string, userId: string, cafeId: string): Promise<void> {
  if (visitId.length == 0 || userId.length == 0 || cafeId.length == 0) {
    return;
  }
  await db().collection("stamps").doc(visitId).set(
    {
      userId: userId,
      cafeId: cafeId,
      visitId: visitId,
      earnedAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {merge: true}
  );
}

async function syncCastClaimRequesterSnapshot(claimId: string, claim: CastClaimLike | undefined): Promise<void> {
  const userId = asNonBlankString(claim?.userId);
  const requesterNickname = asNonBlankString(claim?.requesterNickname);
  const requesterProfileImage = asNonBlankString(claim?.requesterProfileImage);

  if (claimId.trim().length == 0 || userId == null) {
    return;
  } else if (requesterNickname != null && requesterProfileImage != null) {
    return;
  }

  const userSnapshot = await db().collection("users").doc(userId).get();
  const userNickname = asNonBlankString(userSnapshot.get("nickname"));
  const userProfileImage = asNonBlankString(userSnapshot.get("profileImage"));

  if (userNickname == null && userProfileImage == null) {
    return;
  }

  await db().collection("castClaims").doc(claimId).set(
    {
      requesterNickname: userNickname ?? null,
      requesterProfileImage: userProfileImage ?? null,
    },
    {merge: true}
  );
}

async function hasTaggedReviewForCastByUser(
  cafeId: string,
  castId: string,
  userId: string
): Promise<boolean> {
  const snapshot = await db()
    .collection("reviews")
    .where("cafeId", "==", cafeId)
    .where("userId", "==", userId)
    .where("taggedCastIds", "array-contains", castId)
    .select("userId")
    .limit(1)
    .get();
  return !snapshot.empty;
}

async function hasVerifiedVisitByUserAtCafe(cafeId: string, userId: string): Promise<boolean> {
  const snapshot = await db()
    .collection("visits")
    .where("cafeId", "==", cafeId)
    .where("userId", "==", userId)
    .where("verified", "==", true)
    .select("userId")
    .limit(1)
    .get();
  return !snapshot.empty;
}

async function syncCastVisitCertificationForUser(
  cafeId: string,
  castId: string,
  userId: string
): Promise<void> {
  if (cafeId.length == 0 || castId.length == 0 || userId.length == 0) {
    return;
  }
  const [hasTaggedReview, hasVerifiedVisit] = await Promise.all([
    hasTaggedReviewForCastByUser(cafeId, castId, userId),
    hasVerifiedVisitByUserAtCafe(cafeId, userId),
  ]);
  const nextQualified = hasTaggedReview && hasVerifiedVisit;
  const firestore = db();
  const certificationRef = firestore
    .collection("castVisitCertifications")
    .doc(buildCastVisitCertificationDocumentId(cafeId, castId, userId));
  const castRef = firestore
    .collection("cafes")
    .doc(cafeId)
    .collection("casts")
    .doc(castId);

  await firestore.runTransaction(async (transaction) => {
    const [certificationSnapshot, castSnapshot] = await Promise.all([
      transaction.get(certificationRef),
      transaction.get(castRef),
    ]);
    if (!castSnapshot.exists) {
      transaction.delete(certificationRef);
      return;
    }
    const previousQualified = certificationSnapshot.get("qualified") === true;
    if (previousQualified === nextQualified) {
      if (nextQualified) {
        transaction.set(
          certificationRef,
          {
            cafeId: cafeId,
            castId: castId,
            userId: userId,
            qualified: true,
            updatedAt: new Date().toISOString(),
          },
          {merge: true}
        );
      } else if (certificationSnapshot.exists) {
        transaction.delete(certificationRef);
      }
      return;
    }

    const currentCount = Math.max(0, Math.round(asNumber(castSnapshot.get("visitCertificationCount")) ?? 0));
    const nextCount = Math.max(0, currentCount + (nextQualified ? 1 : -1));

    transaction.set(
      castRef,
      {
        visitCertificationCount: nextCount,
      },
      {merge: true}
    );
    if (nextQualified) {
      transaction.set(
        certificationRef,
        {
          cafeId: cafeId,
          castId: castId,
          userId: userId,
          qualified: true,
          updatedAt: new Date().toISOString(),
        },
        {merge: true}
      );
    } else {
      transaction.delete(certificationRef);
    }
  });
}

async function hasVerifiedVisitAtCafe(cafeId: string, userId: string): Promise<boolean> {
  const snapshot = await db()
    .collection("visits")
    .where("cafeId", "==", cafeId)
    .where("userId", "==", userId)
    .where("verified", "==", true)
    .limit(1)
    .select("userId")
    .get();
  return !snapshot.empty;
}

async function syncSingleReviewVisitVerified(reviewId: string, review: ReviewLike | undefined): Promise<void> {
  const cafeId = asNonBlankString(review?.cafeId);
  const userId = asNonBlankString(review?.userId);

  if (cafeId == null || userId == null || reviewId.trim().length == 0) {
    return;
  }
  const verified = await hasVerifiedVisitAtCafe(cafeId, userId);
  const currentValue = review?.visitVerified === true;

  if (currentValue === verified) {
    return;
  }

  await db()
    .collection("reviews")
    .doc(reviewId)
    .set(
      {
        visitVerified: verified,
      },
      {merge: true}
    );
}

async function syncUserCafeReviewsVisitVerified(cafeId: string, userId: string): Promise<void> {
  const verified = await hasVerifiedVisitAtCafe(cafeId, userId);
  const reviewsSnapshot = await db()
    .collection("reviews")
    .where("cafeId", "==", cafeId)
    .where("userId", "==", userId)
    .select("userId")
    .get();

  if (reviewsSnapshot.empty) {
    return;
  }
  const writeBatch = db().batch();

  reviewsSnapshot.docs.forEach((doc) => {
    writeBatch.set(
      doc.ref,
      {
        visitVerified: verified,
      },
      {merge: true}
    );
  });

  await writeBatch.commit();
}

async function applyUserVisitCountDelta(userId: string, delta: number): Promise<void> {
  if (userId.length == 0 || delta == 0) {
    return;
  }
  const userRef = db().collection("users").doc(userId);

  await db().runTransaction(async (transaction) => {
    const snapshot = await transaction.get(userRef);
    const userData = snapshot.data();
    const statsRaw = asPlainObject(userData?.stats);
    const stats = statsRaw == null ? {} : {...statsRaw};
    const currentVisitCount = asNonNegativeInt(stats.visitCount)
      ?? asNonNegativeInt(userData?.visitCount)
      ?? 0;
    const nextVisitCount = Math.max(0, currentVisitCount + delta);
    const nextLevel = Math.max(1, 1 + Math.floor(nextVisitCount / 5));

    stats.visitCount = nextVisitCount;
    stats.level = nextLevel;

    transaction.set(
      userRef,
      {
        stats: stats,
        visitCount: nextVisitCount,
        level: nextLevel,
      },
      {merge: true}
    );
  });
}

async function syncUserFavoriteCountAggregate(userId: string, delta: number): Promise<void> {
  if (userId.length == 0 || delta == 0) {
    return;
  }
  const userRef = db().collection("users").doc(userId);

  await db().runTransaction(async (transaction) => {
    const snapshot = await transaction.get(userRef);
    const userData = snapshot.data();
    const statsRaw = asPlainObject(userData?.stats);
    const stats = statsRaw == null ? {} : {...statsRaw};
    const currentFavoriteCount = asNonNegativeInt(stats.favoritesCount)
      ?? asNonNegativeInt(userData?.favoritesCount)
      ?? 0;
    const nextFavoriteCount = Math.max(0, currentFavoriteCount + delta);

    stats.favoritesCount = nextFavoriteCount;

    transaction.set(
      userRef,
      {
        stats: stats,
        favoritesCount: nextFavoriteCount,
      },
      {merge: true}
    );
  });
}

async function applyUserStampCountDelta(userId: string, delta: number): Promise<void> {
  if (userId.length == 0 || delta == 0) {
    return;
  }
  const userRef = db().collection("users").doc(userId);

  await db().runTransaction(async (transaction) => {
    const snapshot = await transaction.get(userRef);
    const userData = snapshot.data();
    const statsRaw = asPlainObject(userData?.stats);
    const stats = statsRaw == null ? {} : {...statsRaw};
    const currentStampCount = asNonNegativeInt(stats.stampCount)
      ?? asNonNegativeInt(userData?.stampCount)
      ?? 0;
    const nextStampCount = Math.max(0, currentStampCount + delta);

    stats.stampCount = nextStampCount;

    transaction.set(
      userRef,
      {
        stats: stats,
        stampCount: nextStampCount,
      },
      {merge: true}
    );
  });
}

function collectCastUserTargetsFromReviewPayload(review: ReviewLike | undefined): Set<string> {
  const targets = new Set<string>();
  const cafeId = asNonBlankString(review?.cafeId);
  const userId = asNonBlankString(review?.userId);

  if (cafeId == null || userId == null) {
    return targets;
  }

  asStringArray(review?.taggedCastIds).forEach((castId) => {
    targets.add(castUserTargetKey(cafeId, castId, userId));
  });
  return targets;
}

async function loadTaggedCastIdsByCafeAndUser(cafeId: string, userId: string): Promise<Set<string>> {
  const snapshot = await db()
    .collection("reviews")
    .where("cafeId", "==", cafeId)
    .where("userId", "==", userId)
    .select("taggedCastIds")
    .get();
  const castIds = new Set<string>();

  snapshot.forEach((doc) => {
    asStringArray(doc.get("taggedCastIds")).forEach((castId) => {
      castIds.add(castId);
    });
  });

  return castIds;
}

async function applyCafeReviewAggregateDelta(
  cafeId: string,
  reviewCountDelta: number,
  ratingTotalDelta: number
): Promise<void> {
  if (reviewCountDelta == 0 && ratingTotalDelta == 0) {
    return;
  }

  const cafeRef = db().collection("cafes").doc(cafeId);
  await db().runTransaction(async (transaction) => {
    const cafeSnapshot = await transaction.get(cafeRef);
    const currentData = cafeSnapshot.data() ?? {};
    const currentReviewCount = Math.max(0, Math.round(asNumber(currentData.reviewCount) ?? 0));
    const currentRatingAvg = asNumber(currentData.ratingAvg) ?? 0;
    const currentRatingTotal = currentReviewCount == 0 ? 0 : currentRatingAvg * currentReviewCount;
    const nextReviewCount = Math.max(0, currentReviewCount + reviewCountDelta);
    const nextRatingTotal = nextReviewCount == 0 ?
      0 :
      Math.max(0, currentRatingTotal + ratingTotalDelta);
    const nextRatingAvg = nextReviewCount == 0 ?
      0 :
      Number((nextRatingTotal / nextReviewCount).toFixed(2));

    transaction.set(
      cafeRef,
      {
        reviewCount: nextReviewCount,
        ratingAvg: nextRatingAvg,
      },
      {merge: true}
    );
  });
}

function buildReviewAggregateContribution(review: ReviewLike | undefined): {
  cafeId: string;
  reviewCount: number;
  ratingTotal: number;
} | null {
  const cafeId = asNonBlankString(review?.cafeId);
  const rating = asNumber(review?.rating);

  if (cafeId == null || rating == null) {
    return null;
  }
  return {
    cafeId: cafeId,
    reviewCount: 1,
    ratingTotal: rating,
  };
}

async function syncCastFollowNotifications(
  followId: string,
  beforeData: unknown,
  afterData: unknown
): Promise<void> {
  const beforeFollow = beforeData as CastFollowLike | undefined;
  const afterFollow = afterData as CastFollowLike | undefined;
  const beforeUserId = asNonBlankString(beforeFollow?.userId);
  const afterUserId = asNonBlankString(afterFollow?.userId);
  const castId = asNonBlankString(afterFollow?.castId);
  const cafeId = asNonBlankString(afterFollow?.cafeId);

  if (beforeData != null) {
    return;
  } else if (afterData == null) {
    return;
  } else if (beforeUserId != null) {
    return;
  } else if (afterUserId == null || castId == null || cafeId == null) {
    return;
  }
  const castSnapshot = await db()
    .collection("cafes")
    .doc(cafeId)
    .collection("casts")
    .doc(castId)
    .get();
  const recipientUserId = asNonBlankString(castSnapshot.data()?.linkedUserId);

  if (recipientUserId == null || recipientUserId === afterUserId) {
    return;
  }
  const settings = await loadUserNotificationSettings(recipientUserId);

  if (!settings.isFollowNotificationsEnabled) {
    return;
  }
  const castName = asNonBlankString(castSnapshot.data()?.name) ?? "내 캐스트";
  const followerSnapshot = await db().collection("users").doc(afterUserId).get();
  const followerName = asNonBlankString(followerSnapshot.data()?.nickname) ?? "새 팬";
  const createdAt = new Date().toISOString();

  await createUserNotification(
    recipientUserId,
    `follow_update_${followId}_${recipientUserId}`,
    "FOLLOW_UPDATE",
    "새 팔로워 알림",
    `${followerName}님이 ${castName}님을 팔로우했어요.`,
    castId,
    createdAt,
    settings
  );
}

async function syncCastScheduleNotifications(
  scheduleId: string,
  beforeData: CastScheduleLike | undefined,
  afterData: CastScheduleLike | undefined
): Promise<void> {
  const beforeStatus = asNonBlankString(beforeData?.status);
  const afterStatus = asNonBlankString(afterData?.status);
  const castId = asNonBlankString(afterData?.castId);
  const cafeId = asNonBlankString(afterData?.cafeId);
  const scheduleDate = asNonBlankString(afterData?.date);
  const todayDate = kstDateKey(kstNow());

  if (afterStatus !== "WORK") {
    return;
  } else if (beforeStatus === "WORK") {
    return;
  } else if (castId == null || cafeId == null || scheduleDate == null) {
    return;
  } else if (scheduleDate !== todayDate) {
    return;
  }

  const castSnapshot = await db()
    .collection("cafes")
    .doc(cafeId)
    .collection("casts")
    .doc(castId)
    .get();
  const castName = asNonBlankString(castSnapshot.data()?.name) ?? "팔로우한 캐스트";
  const followers = await db()
    .collection("castFollows")
    .where("castId", "==", castId)
    .select("userId")
    .get();

  if (followers.empty) {
    return;
  }
  const {targets: recipientUserIds, droppedByCap} = toNotificationRecipientUserIds(
    followers.docs.map((followerDoc) => asNonBlankString(followerDoc.get("userId")))
  );

  if (recipientUserIds.length == 0) {
    return;
  }
  const startTime = asNonBlankString(afterData?.startTime) ?? "";
  const endTime = asNonBlankString(afterData?.endTime) ?? "";
  const timeLabel = startTime.length > 0 && endTime.length > 0
    ? `${startTime} - ${endTime}`
    : "오늘";
  const createdAt = new Date().toISOString();
  let sentCount = 0;
  let skippedBySettingsCount = 0;

  await processInBatches(recipientUserIds, async (userId) => {
    const settings = await loadUserNotificationSettings(userId);

    if (!settings.isPushNotificationsEnabled || !settings.isShiftNotificationsEnabled) {
      skippedBySettingsCount += 1;
      return;
    }
    await createUserNotification(
      userId,
      `cast_shift_${scheduleId}_${userId}`,
      "CAST_SHIFT",
      "팔로우 캐스트 출근 알림",
      `${castName}님이 ${timeLabel} 출근 예정이에요.`,
      castId,
      createdAt,
      settings
    );
    sentCount += 1;
  });
  logger.info("Processed cast schedule notification fanout.", {
    scheduleId: scheduleId,
    castId: castId,
    targetCount: recipientUserIds.length,
    sentCount: sentCount,
    skippedBySettingsCount: skippedBySettingsCount,
    droppedByCap: droppedByCap,
  });
}

async function syncFavoriteCafeNoticeNotifications(
  cafeId: string,
  noticeId: string,
  beforeData: NoticeLike | undefined,
  afterData: NoticeLike | undefined
): Promise<void> {
  if (afterData == null || beforeData != null) {
    return;
  }
  const title = asNonBlankString(afterData.title) ?? "새 공지";
  const content = asNonBlankString(afterData.content) ?? "즐겨찾기 카페에 새 공지가 등록되었어요.";
  const cafeSnapshot = await db().collection("cafes").doc(cafeId).get();
  const cafeName = asNonBlankString(cafeSnapshot.data()?.name) ?? "즐겨찾기 카페";
  const favorites = await db()
    .collection("cafeFavorites")
    .where("cafeId", "==", cafeId)
    .select("userId")
    .get();

  if (favorites.empty) {
    return;
  }
  const {targets: recipientUserIds, droppedByCap} = toNotificationRecipientUserIds(
    favorites.docs.map((favoriteDoc) => asNonBlankString(favoriteDoc.get("userId")))
  );

  if (recipientUserIds.length == 0) {
    return;
  }
  const createdAt = asNonBlankString(afterData.createdAt) ?? new Date().toISOString();
  let sentCount = 0;
  let skippedBySettingsCount = 0;

  await processInBatches(recipientUserIds, async (userId) => {
    const settings = await loadUserNotificationSettings(userId);

    if (!settings.isPushNotificationsEnabled || !settings.isNoticeNotificationsEnabled) {
      skippedBySettingsCount += 1;
      return;
    }
    await createUserNotification(
      userId,
      `cafe_notice_${noticeId}_${userId}`,
      "CAFE_NOTICE",
      `${cafeName} 공지 업데이트`,
      title.length > 0 ? title : content,
      cafeId,
      createdAt,
      settings
    );
    sentCount += 1;
  });
  logger.info("Processed cafe notice notification fanout.", {
    cafeId: cafeId,
    noticeId: noticeId,
    targetCount: recipientUserIds.length,
    sentCount: sentCount,
    skippedBySettingsCount: skippedBySettingsCount,
    droppedByCap: droppedByCap,
  });
}

async function syncCafeEventNotifications(
  cafeId: string,
  eventId: string,
  beforeData: EventLike | undefined,
  afterData: EventLike | undefined
): Promise<void> {
  if (afterData == null || beforeData != null) {
    return;
  }
  const cafeSnapshot = await db().collection("cafes").doc(cafeId).get();
  const cafeName = asNonBlankString(cafeSnapshot.data()?.name) ?? "카페";
  const title = asNonBlankString(afterData.title) ?? "새 이벤트";
  const desc = asNonBlankString(afterData.desc) ?? "새 이벤트가 등록되었어요.";
  const relatedCastId = asNonBlankString(afterData.relatedCastId);
  const favorites = await db()
    .collection("cafeFavorites")
    .where("cafeId", "==", cafeId)
    .select("userId")
    .get();
  const rawRecipientUserIds: Array<string | null> = [];

  favorites.docs.forEach((favoriteDoc) => {
    const userId = asNonBlankString(favoriteDoc.get("userId"));

    if (userId != null) {
      rawRecipientUserIds.push(userId);
    }
  });
  if (relatedCastId != null) {
    const followers = await db()
      .collection("castFollows")
      .where("castId", "==", relatedCastId)
      .select("userId")
      .get();

    followers.docs.forEach((followerDoc) => {
      const userId = asNonBlankString(followerDoc.get("userId"));

      if (userId != null) {
        rawRecipientUserIds.push(userId);
      }
    });
  }
  const {targets: recipientUserIds, droppedByCap} = toNotificationRecipientUserIds(rawRecipientUserIds);

  if (recipientUserIds.length == 0) {
    return;
  }
  const createdAt = asNonBlankString(afterData.createdAt) ?? new Date().toISOString();
  let sentCount = 0;
  let skippedBySettingsCount = 0;

  await processInBatches(recipientUserIds, async (userId) => {
    const settings = await loadUserNotificationSettings(userId);

    if (!settings.isEventNotificationsEnabled) {
      skippedBySettingsCount += 1;
      return;
    }
    await createUserNotification(
      userId,
      `cafe_event_${eventId}_${userId}`,
      "CAFE_EVENT",
      `${cafeName} 이벤트 업데이트`,
      title.length > 0 ? title : desc,
      cafeId,
      createdAt,
      settings
    );
    sentCount += 1;
  });
  logger.info("Processed cafe event notification fanout.", {
    cafeId: cafeId,
    eventId: eventId,
    targetCount: recipientUserIds.length,
    sentCount: sentCount,
    skippedBySettingsCount: skippedBySettingsCount,
    droppedByCap: droppedByCap,
  });
}

async function syncBirthdayNotifications(): Promise<void> {
  const today = kstNow();
  const birthdayKey = kstBirthdayKey(today);
  const castSnapshot = await db()
    .collectionGroup("casts")
    .where("birthdayKey", "==", birthdayKey)
    .select("name")
    .get();

  if (castSnapshot.empty) {
    return;
  }
  const settingsCache = new Map<string, UserNotificationSettings>();
  const loadCachedSettings = async (userId: string): Promise<UserNotificationSettings> => {
    const cached = settingsCache.get(userId);

    if (cached != null) {
      return cached;
    }
    const loaded = await loadUserNotificationSettings(userId);
    settingsCache.set(userId, loaded);
    return loaded;
  };
  const createdAt = new Date().toISOString();
  await processInBatches(castSnapshot.docs, async (castDoc) => {
    const castId = castDoc.id;
    const castName = asNonBlankString(castDoc.get("name")) ?? "팔로우한 캐스트";
    const followers = await db()
      .collection("castFollows")
      .where("castId", "==", castId)
      .select("userId")
      .get();

    if (followers.empty) {
      return;
    }
    const {targets: recipientUserIds, droppedByCap} = toNotificationRecipientUserIds(
      followers.docs.map((followerDoc) => asNonBlankString(followerDoc.get("userId")))
    );

    if (recipientUserIds.length == 0) {
      return;
    }
    let sentCount = 0;
    let skippedBySettingsCount = 0;

    await processInBatches(recipientUserIds, async (userId) => {
      const settings = await loadCachedSettings(userId);

      if (!settings.isPushNotificationsEnabled || !settings.isBirthdayNotificationsEnabled) {
        skippedBySettingsCount += 1;
        return;
      }
      await createUserNotification(
        userId,
        `birthday_${castId}_${birthdayKey}_${userId}`,
        "BIRTHDAY",
        "팔로우 캐스트 생일 알림",
        `오늘은 ${castName}님의 생일이에요. 축하 메시지를 남겨보세요.`,
        castId,
        createdAt,
        settings
      );
      sentCount += 1;
    });
    logger.info("Processed birthday notification fanout.", {
      castId: castId,
      birthdayKey: birthdayKey,
      targetCount: recipientUserIds.length,
      sentCount: sentCount,
      skippedBySettingsCount: skippedBySettingsCount,
      droppedByCap: droppedByCap,
    });
  });
}

type RankingScope = {
  country: string | null;
  city: string | null;
  token: string;
};

type CafeRankingSource = {
  id: string;
  name: string;
  subtitle: string;
  country: string;
  city: string;
  score: number;
  imageUrl: string | null;
};

type CastRankingSource = {
  id: string;
  name: string;
  subtitle: string;
  country: string;
  city: string;
  score: number;
  imageUrl: string | null;
};

type RankingSnapshotEntry = {
  id: string;
  name: string;
  subtitle: string;
  score: number;
  rank: number;
  prevRank: number | null;
  change: string;
  imageUrl: string | null;
};

const RANKING_SYNC_DOC_PATH = "rankingSync/state";
const RANKING_MAX_COUNT = 50;
const RANKING_SOURCE_QUERY_LIMIT = RANKING_MAX_COUNT * 3;
const RANKING_PERIODS = ["WEEKLY", "MONTHLY"] as const;
const RANKING_SCOPES: RankingScope[] = [
  {country: null, city: null, token: "all_all"},
  {country: "KR", city: "Seoul", token: "kr_seoul"},
  {country: "JP", city: "Tokyo", token: "jp_tokyo"},
  {country: "JP", city: "Osaka", token: "jp_osaka"},
];

function asNumber(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  return null;
}

function normalizeCountry(value: unknown): string {
  const raw = asNonBlankString(value);
  if (raw == null) {
    return "";
  }
  return raw.toUpperCase();
}

function normalizeCity(value: unknown): string {
  const raw = asNonBlankString(value);
  if (raw == null) {
    return "";
  }
  return raw;
}

function resolveCafeSubtitle(address: string, city: string): string {
  const fromGu = address.split("구")[0]?.trim() ?? "";
  const fromRoad = fromGu.split("로")[0]?.trim() ?? "";
  if (fromRoad.length > 0) {
    return fromRoad;
  } else {
    return city;
  }
}

function matchesScope(country: string, city: string, scope: RankingScope): boolean {
  if (scope.country == null || scope.city == null) {
    return true;
  }
  const countryMatches = country === scope.country;
  const cityMatches = city === scope.city;
  return countryMatches && cityMatches;
}

function rankingDocumentId(kind: string, period: string, scope: RankingScope): string {
  const kindValue = kind.trim().toLowerCase();
  const periodValue = period.trim().toLowerCase();
  return `${kindValue}_${periodValue}_${scope.token}`;
}

function buildRankingChange(prevRank: number | null, currentRank: number): string {
  if (prevRank == null) {
    return "NEW";
  }
  const delta = prevRank - currentRank;

  if (delta > 0) {
    return `+${delta}`;
  } else if (delta < 0) {
    return `${delta}`;
  }
  return "0";
}

function parsePreviousRankingMap(rawEntries: unknown): Map<string, number> {
  const rankById = new Map<string, number>();

  if (!Array.isArray(rawEntries)) {
    return rankById;
  }
  rawEntries.forEach((entry) => {
    const entryObject = asPlainObject(entry);
    const id = asNonBlankString(entryObject?.id);
    const rank = asNumber(entryObject?.rank);

    if (id == null || rank == null) {
      return;
    }
    rankById.set(id, Math.max(1, Math.floor(rank)));
  });
  return rankById;
}

function parseRankingSnapshotEntries(rawEntries: unknown): RankingSnapshotEntry[] {
  if (!Array.isArray(rawEntries)) {
    return [];
  }
  return rawEntries
    .map((entry) => {
      const object = asPlainObject(entry);
      const id = asNonBlankString(object?.id);
      const rank = asNumber(object?.rank);
      const prevRankRaw = asNumber(object?.prevRank);

      if (id == null || rank == null) {
        return null;
      }
      return {
        id: id,
        name: asNonBlankString(object?.name) ?? "",
        subtitle: asNonBlankString(object?.subtitle) ?? "",
        score: Math.max(0, Math.floor(asNumber(object?.score) ?? 0)),
        rank: Math.max(1, Math.floor(rank)),
        prevRank: prevRankRaw == null ? null : Math.max(1, Math.floor(prevRankRaw)),
        change: asNonBlankString(object?.change) ?? "0",
        imageUrl: asNonBlankString(object?.imageUrl),
      } as RankingSnapshotEntry;
    })
    .filter((entry): entry is RankingSnapshotEntry => entry !== null);
}

function buildRankingSnapshotEntries<T extends CafeRankingSource | CastRankingSource>(
  sources: T[],
  previousRankById: Map<string, number>
): RankingSnapshotEntry[] {
  return sources
    .slice(0, RANKING_MAX_COUNT)
    .map((source, index) => {
      const rank = index + 1;
      const prevRank = previousRankById.get(source.id) ?? null;
      const change = buildRankingChange(prevRank, rank);
      return {
        id: source.id,
        name: source.name,
        subtitle: source.subtitle,
        score: source.score,
        rank: rank,
        prevRank: prevRank,
        change: change,
        imageUrl: source.imageUrl,
      };
    });
}

function areRankingEntriesEqual(left: RankingSnapshotEntry[], right: RankingSnapshotEntry[]): boolean {
  if (left.length !== right.length) {
    return false;
  }
  for (let index = 0; index < left.length; index += 1) {
    const lhs = left[index];
    const rhs = right[index];

    if (
      lhs.id !== rhs.id ||
      lhs.rank !== rhs.rank ||
      lhs.prevRank !== rhs.prevRank ||
      lhs.score !== rhs.score ||
      lhs.name !== rhs.name ||
      lhs.subtitle !== rhs.subtitle ||
      lhs.change !== rhs.change ||
      lhs.imageUrl !== rhs.imageUrl
    ) {
      return false;
    }
  }
  return true;
}

function parseCafeRankingSourceFromDoc(
  doc: FirebaseFirestore.QueryDocumentSnapshot<FirebaseFirestore.DocumentData>
): CafeRankingSource | null {
  const data = doc.data();
  const approved = data.approved !== false;

  if (!approved) {
    return null;
  }
  const region = asPlainObject(data.region);
  const country = normalizeCountry(region?.country);
  const city = normalizeCity(region?.city);
  const address = asNonBlankString(region?.address) ?? asNonBlankString(data.address) ?? city;
  const subtitle = resolveCafeSubtitle(address, city);
  const score = Math.max(0, Math.floor((asNumber(data.ratingAvg) ?? 0) * 100));

  return {
    id: doc.id,
    name: asNonBlankString(data.name) ?? "",
    subtitle: subtitle,
    country: country,
    city: city,
    score: score,
    imageUrl: asNonBlankString(data.thumbnailImage),
  } as CafeRankingSource;
}

async function loadCafeRankingSourcesByScope(scope: RankingScope): Promise<CafeRankingSource[]> {
  let query: FirebaseFirestore.Query<FirebaseFirestore.DocumentData> = db()
    .collection("cafes")
    .where("approved", "==", true)
    .orderBy("ratingAvg", "desc")
    .select("name", "region", "address", "ratingAvg", "thumbnailImage", "approved")
    .limit(RANKING_SOURCE_QUERY_LIMIT);

  if (scope.country != null && scope.city != null) {
    query = db()
      .collection("cafes")
      .where("approved", "==", true)
      .where("region.country", "==", scope.country)
      .where("region.city", "==", scope.city)
      .orderBy("ratingAvg", "desc")
      .select("name", "region", "address", "ratingAvg", "thumbnailImage", "approved")
      .limit(RANKING_SOURCE_QUERY_LIMIT);
  }
  const snapshot = await query.get();

  return snapshot.docs
    .map((doc) => parseCafeRankingSourceFromDoc(doc))
    .filter((item): item is CafeRankingSource => item != null)
    .sort((left, right) => right.score - left.score || left.id.localeCompare(right.id));
}

async function loadCastRankingSources(cafeById: Map<string, CafeRankingSource>): Promise<CastRankingSource[]> {
  const snapshot = await db()
    .collectionGroup("casts")
    .select("name", "followerCount", "profileImage")
    .get();

  return snapshot.docs
    .map((doc) => {
      const cafeRef = doc.ref.parent.parent;
      const cafeId = cafeRef?.id ?? "";
      const cafe = cafeById.get(cafeId);

      if (cafe == null) {
        return null;
      }
      const data = doc.data();
      const score = Math.max(0, Math.floor(asNumber(data.followerCount) ?? 0));

      return {
        id: doc.id,
        name: asNonBlankString(data.name) ?? "",
        subtitle: cafe.name,
        country: cafe.country,
        city: cafe.city,
        score: score,
        imageUrl: asNonBlankString(data.profileImage),
      } as CastRankingSource;
    })
    .filter((item): item is CastRankingSource => item !== null)
    .sort((left, right) => right.score - left.score || left.id.localeCompare(right.id));
}

async function syncRankingSnapshotDocument(
  kind: "cast" | "cafe",
  period: typeof RANKING_PERIODS[number],
  scope: RankingScope,
  sourceEntries: Array<CafeRankingSource | CastRankingSource>
): Promise<void> {
  const rankingId = rankingDocumentId(kind, period, scope);
  const rankingRef = db().collection("rankings").doc(rankingId);
  const previousSnapshot = await rankingRef.get();
  const previousData = previousSnapshot.data();
  const previousRankById = parsePreviousRankingMap(previousData?.entries);
  const nextEntries = buildRankingSnapshotEntries(sourceEntries, previousRankById);
  const previousEntries = parseRankingSnapshotEntries(previousData?.entries);

  if (areRankingEntriesEqual(previousEntries, nextEntries)) {
    return;
  }
  await rankingRef.set(
    {
      kind: kind.toUpperCase(),
      period: period,
      country: scope.country,
      city: scope.city,
      updatedAt: new Date().toISOString(),
      entries: nextEntries,
    },
    {merge: true}
  );
}

type RankingDirtyState = {
  shouldSyncCafe: boolean;
  shouldSyncCast: boolean;
};

async function syncAllRankingSnapshots(dirtyState: RankingDirtyState): Promise<void> {
  const scopedCafeSourcesByToken = new Map<string, CafeRankingSource[]>();
  let castSources: CastRankingSource[] = [];

  if (dirtyState.shouldSyncCafe) {
    const scopedCafeResults = await Promise.all(RANKING_SCOPES.map(async (scope) => {
      const sources = await loadCafeRankingSourcesByScope(scope);

      return {
        token: scope.token,
        sources: sources,
      };
    }));

    scopedCafeResults.forEach((result) => {
      scopedCafeSourcesByToken.set(result.token, result.sources);
    });
  }
  if (dirtyState.shouldSyncCast) {
    const globalCafeSources = await loadCafeRankingSourcesByScope({
      country: null,
      city: null,
      token: "all_all",
    });
    const cafeById = new Map<string, CafeRankingSource>();

    globalCafeSources.forEach((cafe) => {
      cafeById.set(cafe.id, cafe);
    });
    castSources = await loadCastRankingSources(cafeById);
  }
  const tasks: Promise<void>[] = [];

  RANKING_PERIODS.forEach((period) => {
    RANKING_SCOPES.forEach((scope) => {
      if (dirtyState.shouldSyncCafe) {
        const scopedCafes = scopedCafeSourcesByToken.get(scope.token) ?? [];

        tasks.push(syncRankingSnapshotDocument("cafe", period, scope, scopedCafes));
      }
      if (dirtyState.shouldSyncCast) {
        const scopedCasts = castSources.filter((entry) => matchesScope(entry.country, entry.city, scope));

        tasks.push(syncRankingSnapshotDocument("cast", period, scope, scopedCasts));
      }
    });
  });
  await Promise.all(tasks);
}

async function markRankingSyncDirty(
  reason: string,
  payload: Record<string, unknown>,
  targets: {cafe?: boolean; cast?: boolean} = {cafe: true, cast: true}
): Promise<void> {
  const nextCafeDirty = targets.cafe === true;
  const nextCastDirty = targets.cast === true;

  if (!nextCafeDirty && !nextCastDirty) {
    return;
  }

  await db().doc(RANKING_SYNC_DOC_PATH).set(
    {
      dirty: true,
      dirtyCafe: nextCafeDirty,
      dirtyCast: nextCastDirty,
      updatedAt: new Date().toISOString(),
      reason: reason,
      payload: payload,
    },
    {merge: true}
  );
}

async function resolveRankingDirtyState(): Promise<RankingDirtyState | null> {
  const snapshot = await db().doc(RANKING_SYNC_DOC_PATH).get();

  if (!snapshot.exists) {
    return {
      shouldSyncCafe: true,
      shouldSyncCast: true,
    };
  }
  const dirty = snapshot.get("dirty") === true;

  if (!dirty) {
    return null;
  }
  const dirtyCafe = snapshot.get("dirtyCafe");
  const dirtyCast = snapshot.get("dirtyCast");
  const shouldSyncCafe = dirtyCafe === true || (dirtyCafe !== false && dirtyCast !== true);
  const shouldSyncCast = dirtyCast === true || (dirtyCast !== false && dirtyCafe !== true);

  if (!shouldSyncCafe && !shouldSyncCast) {
    return null;
  }
  return {
    shouldSyncCafe: shouldSyncCafe,
    shouldSyncCast: shouldSyncCast,
  };
}

async function completeRankingSync(): Promise<void> {
  await db().doc(RANKING_SYNC_DOC_PATH).set(
    {
      dirty: false,
      dirtyCafe: false,
      dirtyCast: false,
      syncedAt: new Date().toISOString(),
    },
    {merge: true}
  );
}


export const onReviewWrittenSyncCafeAggregate = onDocumentWritten(
  "reviews/{reviewId}",
  async (event) => {
    const beforeData = event.data?.before.data() as ReviewLike | undefined;
    const afterData = event.data?.after.data() as ReviewLike | undefined;
    const beforeContribution = buildReviewAggregateContribution(beforeData);
    const afterContribution = buildReviewAggregateContribution(afterData);
    const deltas = new Map<string, {reviewCountDelta: number; ratingTotalDelta: number}>();

    const appendDelta = (
      cafeId: string,
      reviewCountDelta: number,
      ratingTotalDelta: number
    ) => {
      const current = deltas.get(cafeId) ?? {reviewCountDelta: 0, ratingTotalDelta: 0};
      deltas.set(cafeId, {
        reviewCountDelta: current.reviewCountDelta + reviewCountDelta,
        ratingTotalDelta: current.ratingTotalDelta + ratingTotalDelta,
      });
    };

    if (beforeContribution != null) {
      appendDelta(
        beforeContribution.cafeId,
        -beforeContribution.reviewCount,
        -beforeContribution.ratingTotal
      );
    }
    if (afterContribution != null) {
      appendDelta(
        afterContribution.cafeId,
        afterContribution.reviewCount,
        afterContribution.ratingTotal
      );
    }
    if (deltas.size == 0) {
      return;
    }

    await Promise.all(
      Array.from(deltas.entries()).map(async ([cafeId, delta]) => {
        await applyCafeReviewAggregateDelta(
          cafeId,
          delta.reviewCountDelta,
          delta.ratingTotalDelta
        );
      })
    );
    await markRankingSyncDirty("review_written", {
      reviewId: event.params.reviewId,
      cafeIds: Array.from(deltas.keys()),
    }, {cafe: true, cast: false});
    logger.info("Synced cafe review aggregate.", {
      cafeIds: Array.from(deltas.keys()),
      reviewId: event.params.reviewId,
    });
  }
);

export const onCastFollowWrittenSyncFollowerCount = onDocumentWritten(
  "castFollows/{followId}",
  async (event) => {
    const followId = event.params.followId;
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();

    logger.info("FOLLOW EVENT TRIGGERED", {
      followId,
      beforeData,
      afterData,
    });

    const isCreate = !beforeData && afterData;
    const isDelete = beforeData && !afterData;

    if (!isCreate && !isDelete) {
      return;
    }

    const target = isCreate ? afterData : beforeData;

    const castId = target?.castId;
    const cafeId = target?.cafeId;

    if (
      typeof castId !== "string" ||
      typeof cafeId !== "string" ||
      castId.length === 0 ||
      cafeId.length === 0
    ) {
      logger.warn("Invalid follow data", { followId, target });
      return;
    }
    const delta = isCreate ? 1 : -1;

    const castRef = db()
      .collection("cafes")
      .doc(cafeId)
      .collection("casts")
      .doc(castId);

    try {
      await castRef.set(
        {
          followerCount: FieldValue.increment(delta),
        },
        { merge: true }
      );

      logger.info("Follower count updated", {
        followId,
        castId,
        cafeId,
        delta,
      });

      await markRankingSyncDirty("cast_follow_written", {
        followId,
        castId,
        cafeId,
        delta,
      }, {cafe: false, cast: true});

    } catch (error) {
      logger.error("Failed to update follower count", {
        followId,
        castId,
        cafeId,
        error: error instanceof Error ? error.message : String(error),
      });
    }
    await syncCastFollowNotifications(followId, beforeData, afterData);
  }
);

export const onCastScheduleWrittenCreateShiftNotifications = onDocumentWritten(
  "castSchedules/{scheduleId}",
  async (event) => {
    const scheduleId = asNonBlankString(event.params.scheduleId);
    const beforeData = event.data?.before.data() as CastScheduleLike | undefined;
    const afterData = event.data?.after.data() as CastScheduleLike | undefined;

    if (scheduleId == null) {
      return;
    }
    await syncCastScheduleNotifications(scheduleId, beforeData, afterData);
    logger.info("Synced cast schedule notifications.", {
      scheduleId: scheduleId,
      status: asNonBlankString(afterData?.status),
      date: asNonBlankString(afterData?.date),
      castId: asNonBlankString(afterData?.castId),
    });
  }
);

export const onFanAnnouncementRequestWrittenSendPush = onDocumentWritten(
  "fanAnnouncementRequests/{requestId}",
  async (event) => {
    const requestId = asNonBlankString(event.params.requestId);
    const beforeData = event.data?.before.data() as FanAnnouncementRequestLike | undefined;
    const afterData = event.data?.after.data() as FanAnnouncementRequestLike | undefined;

    if (requestId == null || afterData == null || beforeData != null) {
      return;
    }
    try {
      const requesterUserId = asNonBlankString(afterData.userId);
      const cafeId = asNonBlankString(afterData.cafeId);
      const castId = asNonBlankString(afterData.castId);
      const rawTitle = asNonBlankString(afterData.title);
      const rawBody = asNonBlankString(afterData.body);

      if (requesterUserId == null || cafeId == null || castId == null || rawTitle == null || rawBody == null) {
        return;
      }
      const title = rawTitle.substring(0, 50);
      const body = rawBody.substring(0, 300);
      const requesterSnapshot = await db().collection("users").doc(requesterUserId).get();
      const requesterRole = asNonBlankString(requesterSnapshot.data()?.role);
      const requesterAffiliatedCafeId = asNonBlankString(requesterSnapshot.data()?.affiliatedCafeId);
      const hasAnnouncementPermission = await hasFanAnnouncementPermission(
        requesterUserId,
        requesterRole,
        requesterAffiliatedCafeId,
        cafeId,
        castId
      );

      if (!hasAnnouncementPermission) {
        logger.warn("Fan announcement request rejected due to permission.", {
          requestId: requestId,
          requesterUserId: requesterUserId,
          requesterRole: requesterRole,
          requesterAffiliatedCafeId: requesterAffiliatedCafeId,
          cafeId: cafeId,
        });
        return;
      }
      const followers = await db()
        .collection("castFollows")
        .where("castId", "==", castId)
        .select("userId")
        .get();

      if (followers.empty) {
        logger.info("Fan announcement has no follower target.", {
          requestId: requestId,
          requesterUserId: requesterUserId,
          castId: castId,
        });
        return;
      }
      const createdAt = asNonBlankString(afterData.createdAt) ?? new Date().toISOString();
      const {targets: recipientUserIds, droppedByCap} = toNotificationRecipientUserIds(
        followers.docs.map((followerDoc) => asNonBlankString(followerDoc.get("userId"))),
        [requesterUserId]
      );

      if (recipientUserIds.length == 0) {
        logger.info("Fan announcement target is empty after filtering.", {
          requestId: requestId,
          castId: castId,
        });
        return;
      }
      let sentCount = 0;
      let skippedBySettingsCount = 0;
      let failedCount = 0;

      await processInBatches(recipientUserIds, async (userId) => {
        try {
          const settings = await loadUserNotificationSettings(userId);

          if (!settings.isPushNotificationsEnabled) {
            skippedBySettingsCount += 1;
            return;
          }
          if (!settings.isFollowNotificationsEnabled) {
            skippedBySettingsCount += 1;
            return;
          }
          if (isQuietHoursPushSuppressed(settings)) {
            skippedBySettingsCount += 1;
            return;
          }
          await createUserNotification(
            userId,
            sanitizeNotificationDocumentId(`fan_announcement_${requestId}_${userId}`),
            "FAN_ANNOUNCEMENT",
            title,
            body,
            castId,
            createdAt,
            settings
          );
          sentCount += 1;
        } catch (error) {
          failedCount += 1;
        }
      });
      logger.info("Processed fan announcement notification fanout.", {
        requestId: requestId,
        castId: castId,
        targetCount: recipientUserIds.length,
        sentCount: sentCount,
        skippedBySettingsCount: skippedBySettingsCount,
        failedCount: failedCount,
        droppedByCap: droppedByCap,
      });
    } finally {
      await event.data?.after.ref.delete();
    }
  }
);

export const onCafeNoticeWrittenCreateFavoriteNotifications = onDocumentWritten(
  "cafes/{cafeId}/notices/{noticeId}",
  async (event) => {
    const cafeId = asNonBlankString(event.params.cafeId);
    const noticeId = asNonBlankString(event.params.noticeId);
    const beforeData = event.data?.before.data() as NoticeLike | undefined;
    const afterData = event.data?.after.data() as NoticeLike | undefined;

    if (cafeId == null || noticeId == null) {
      return;
    }
    await syncFavoriteCafeNoticeNotifications(cafeId, noticeId, beforeData, afterData);
    logger.info("Synced favorite cafe notice notifications.", {
      cafeId: cafeId,
      noticeId: noticeId,
      created: beforeData == null && afterData != null,
    });
  }
);

export const onCafeEventWrittenCreateNotifications = onDocumentWritten(
  "cafes/{cafeId}/events/{eventId}",
  async (event) => {
    const cafeId = asNonBlankString(event.params.cafeId);
    const eventId = asNonBlankString(event.params.eventId);
    const beforeData = event.data?.before.data() as EventLike | undefined;
    const afterData = event.data?.after.data() as EventLike | undefined;

    if (cafeId == null || eventId == null) {
      return;
    }
    await syncCafeEventNotifications(cafeId, eventId, beforeData, afterData);
    logger.info("Synced cafe event notifications.", {
      cafeId: cafeId,
      eventId: eventId,
      created: beforeData == null && afterData != null,
    });
  }
);

export const onCastClaimWrittenSyncRequesterSnapshot = onDocumentWritten(
  "castClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const afterData = event.data?.after.data() as CastClaimLike | undefined;

    if (claimId == null || afterData == null) {
      return;
    }

    await syncCastClaimRequesterSnapshot(claimId, afterData);
    logger.info("Synced cast claim requester snapshot.", {
      claimId: claimId,
    });
  }
);

export const onCafeRegistrationClaimWrittenCreateAdminNotifications = onDocumentWritten(
  "cafeRegistrationClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CafeRegistrationClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeRegistrationClaimLike | undefined;

    if (claimId == null || afterData == null || beforeData != null) {
      return;
    }
    const status = asNonBlankString(afterData.status);

    if (!isPendingApprovalStatus(status)) {
      return;
    }
    const requesterUserId = asNonBlankString(afterData.userId);
    const cafeName = asNonBlankString(afterData.cafeName) ?? "새 카페";
    const createdAt = new Date().toISOString();
    const adminUserIds = await loadAdminUserIds();
    const recipientUserIds = adminUserIds.filter((userId) => userId != requesterUserId);

    await createApprovalRequestNotifications(
      recipientUserIds,
      `approval_cafe_registration_${claimId}`,
      "CAFE_APPROVAL_REQUEST",
      "카페 등록 승인 요청",
      `${cafeName} 등록 승인 요청이 접수되었어요.`,
      claimId,
      createdAt
    );
    logger.info("Created admin notifications for cafe registration claim.", {
      claimId: claimId,
      requesterUserId: requesterUserId,
      recipientCount: recipientUserIds.length,
    });
  }
);

export const onCafeOwnerClaimWrittenCreateAdminNotifications = onDocumentWritten(
  "cafeOwnerClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CafeOwnerClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeOwnerClaimLike | undefined;

    if (claimId == null || afterData == null || beforeData != null) {
      return;
    }
    const status = asNonBlankString(afterData.status);

    if (!isPendingApprovalStatus(status)) {
      return;
    }
    const requesterUserId = asNonBlankString(afterData.userId);
    const cafeName = asNonBlankString(afterData.cafeName) ?? "카페";
    const createdAt = new Date().toISOString();
    const adminUserIds = await loadAdminUserIds();
    const recipientUserIds = adminUserIds.filter((userId) => userId != requesterUserId);

    await createApprovalRequestNotifications(
      recipientUserIds,
      `approval_cafe_owner_${claimId}`,
      "CAFE_OWNER_APPROVAL_REQUEST",
      "카페 운영 권한 승인 요청",
      `${cafeName} 운영 권한 요청이 접수되었어요.`,
      claimId,
      createdAt
    );
    logger.info("Created admin notifications for cafe owner claim.", {
      claimId: claimId,
      requesterUserId: requesterUserId,
      recipientCount: recipientUserIds.length,
    });
  }
);

export const onCastClaimWrittenCreateCafeApprovalNotifications = onDocumentWritten(
  "castClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CastClaimLike | undefined;
    const afterData = event.data?.after.data() as CastClaimLike | undefined;

    if (claimId == null || afterData == null || beforeData != null) {
      return;
    }
    const status = asNonBlankString(afterData.status);

    if (!isPendingApprovalStatus(status)) {
      return;
    }
    const cafeId = asNonBlankString(afterData.cafeId);
    const requesterUserId = asNonBlankString(afterData.userId);
    const castId = asNonBlankString(afterData.castId) ?? claimId;

    if (cafeId == null) {
      return;
    }
    const castName = asNonBlankString(afterData.castName) ?? "캐스트";
    const requesterNickname = asNonBlankString(afterData.requesterNickname) ?? "사용자";
    const ownerUserIds = await loadCafeOwnerUserIds(cafeId);
    const adminUserIds = await loadAdminUserIds();
    const recipientUserIds = Array.from(new Set([...ownerUserIds, ...adminUserIds]))
      .filter((userId) => userId != requesterUserId);
    const createdAt = asNonBlankString(afterData.createdAt) ?? new Date().toISOString();

    await createApprovalRequestNotifications(
      recipientUserIds,
      `approval_cast_claim_${claimId}`,
      "CAST_CLAIM_REQUEST",
      "캐스트 프로필 연결 승인 요청",
      `${requesterNickname}님이 ${castName} 프로필 연결 승인을 요청했어요.`,
      castId,
      createdAt
    );
    logger.info("Created cafe approval notifications for cast claim.", {
      claimId: claimId,
      cafeId: cafeId,
      requesterUserId: requesterUserId,
      recipientCount: recipientUserIds.length,
    });
  }
);

export const onCafeRegistrationClaimWrittenRequesterApprovedNotification = onDocumentWritten(
  "cafeRegistrationClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CafeRegistrationClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeRegistrationClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (claimId == null || beforeData == null || afterData == null) {
      return;
    }
    if (!isApprovedStatus(afterStatus) || isApprovedStatus(beforeStatus)) {
      return;
    }
    const requesterUserId = asNonBlankString(afterData.userId);
    const cafeName = asNonBlankString(afterData.cafeName) ?? "카페";
    const cafeId = asNonBlankString(afterData.approvedCafeId) ?? claimId;
    const createdAt = new Date().toISOString();

    await createApprovalResultNotification(
      requesterUserId,
      `approved_cafe_registration_${claimId}`,
      "CAFE_APPROVED",
      "카페 등록 승인 완료",
      `${cafeName} 등록 요청이 승인되었어요.`,
      cafeId,
      createdAt
    );
    logger.info("Created requester approved notification for cafe registration claim.", {
      claimId: claimId,
      requesterUserId: requesterUserId,
    });
  }
);

export const onCafeOwnerClaimWrittenCreateRequesterApprovedNotification = onDocumentWritten(
  "cafeOwnerClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CafeOwnerClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeOwnerClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (claimId == null || beforeData == null || afterData == null) {
      return;
    }
    if (!isApprovedStatus(afterStatus) || isApprovedStatus(beforeStatus)) {
      return;
    }
    const requesterUserId = asNonBlankString(afterData.userId);
    const cafeName = asNonBlankString(afterData.cafeName) ?? "카페";
    const cafeId = asNonBlankString(afterData.cafeId) ?? claimId;
    const createdAt = new Date().toISOString();

    await createApprovalResultNotification(
      requesterUserId,
      `approved_cafe_owner_${claimId}`,
      "CAFE_OWNER_APPROVED",
      "카페 운영 권한 승인 완료",
      `${cafeName} 운영 권한 요청이 승인되었어요.`,
      cafeId,
      createdAt
    );
    logger.info("Created requester approved notification for cafe owner claim.", {
      claimId: claimId,
      requesterUserId: requesterUserId,
    });
  }
);

export const onCastClaimWrittenCreateRequesterApprovedNotification = onDocumentWritten(
  "castClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CastClaimLike | undefined;
    const afterData = event.data?.after.data() as CastClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (claimId == null || beforeData == null || afterData == null) {
      return;
    }
    if (!isApprovedStatus(afterStatus) || isApprovedStatus(beforeStatus)) {
      return;
    }
    const requesterUserId = asNonBlankString(afterData.userId);
    const castName = asNonBlankString(afterData.castName) ?? "캐스트";
    const castId = asNonBlankString(afterData.castId) ?? claimId;
    const createdAt = new Date().toISOString();

    await createApprovalResultNotification(
      requesterUserId,
      `approved_cast_claim_${claimId}`,
      "CAST_CLAIM_APPROVED",
      "캐스트 프로필 연결 승인 완료",
      `${castName} 프로필 연결 요청이 승인되었어요.`,
      castId,
      createdAt
    );
    logger.info("Created requester approved notification for cast claim.", {
      claimId: claimId,
      requesterUserId: requesterUserId,
    });
  }
);

export const onCafeRegistrationClaimWrittenRequesterRejectedNotification = onDocumentWritten(
  "cafeRegistrationClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CafeRegistrationClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeRegistrationClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (claimId == null || beforeData == null || afterData == null) {
      return;
    }
    if (!isRejectedStatus(afterStatus) || isRejectedStatus(beforeStatus)) {
      return;
    }
    const requesterUserId = asNonBlankString(afterData.userId);
    const cafeName = asNonBlankString(afterData.cafeName) ?? "카페";
    const createdAt = new Date().toISOString();

    await createApprovalResultNotification(
      requesterUserId,
      `rejected_cafe_registration_${claimId}`,
      "CAFE_REJECTED",
      "카페 등록 반려 안내",
      `${cafeName} 등록 요청이 반려되었어요.`,
      claimId,
      createdAt
    );
    logger.info("Created requester rejected notification for cafe registration claim.", {
      claimId: claimId,
      requesterUserId: requesterUserId,
    });
  }
);

export const onCafeRegistrationClaimRejectedDeleteImage = onDocumentWritten(
  "cafeRegistrationClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CafeRegistrationClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeRegistrationClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (!isRejectedStatus(afterStatus) || isRejectedStatus(beforeStatus)) {
      return;
    }
    const imageUrl = asNonBlankString(afterData?.imageUrl);

    if (!imageUrl) {
      return;
    }
    try {
      await deleteStorageFileByUrl(imageUrl);
      logger.info("Deleted image for rejected cafe registration claim.", {
        claimId: claimId,
        imageUrl: imageUrl,
      });
    } catch (error) {
      logger.warn("Failed to delete image for rejected cafe registration claim.", {
        claimId: claimId,
        imageUrl: imageUrl,
        error: error,
      });
    }
  }
);

export const onCafeOwnerClaimWrittenCreateRequesterRejectedNotification = onDocumentWritten(
  "cafeOwnerClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CafeOwnerClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeOwnerClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (claimId == null || beforeData == null || afterData == null) {
      return;
    }
    if (!isRejectedStatus(afterStatus) || isRejectedStatus(beforeStatus)) {
      return;
    }
    const requesterUserId = asNonBlankString(afterData.userId);
    const cafeName = asNonBlankString(afterData.cafeName) ?? "카페";
    const cafeId = asNonBlankString(afterData.cafeId) ?? claimId;
    const createdAt = new Date().toISOString();

    await createApprovalResultNotification(
      requesterUserId,
      `rejected_cafe_owner_${claimId}`,
      "CAFE_OWNER_REJECTED",
      "카페 운영 권한 반려 안내",
      `${cafeName} 운영 권한 요청이 반려되었어요.`,
      cafeId,
      createdAt
    );
    logger.info("Created requester rejected notification for cafe owner claim.", {
      claimId: claimId,
      requesterUserId: requesterUserId,
    });
  }
);

export const onCastClaimWrittenCreateRequesterRejectedNotification = onDocumentWritten(
  "castClaims/{claimId}",
  async (event) => {
    const claimId = asNonBlankString(event.params.claimId);
    const beforeData = event.data?.before.data() as CastClaimLike | undefined;
    const afterData = event.data?.after.data() as CastClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (claimId == null || beforeData == null || afterData == null) {
      return;
    }
    if (!isRejectedStatus(afterStatus) || isRejectedStatus(beforeStatus)) {
      return;
    }
    const requesterUserId = asNonBlankString(afterData.userId);
    const castName = asNonBlankString(afterData.castName) ?? "캐스트";
    const castId = asNonBlankString(afterData.castId) ?? claimId;
    const createdAt = new Date().toISOString();

    await createApprovalResultNotification(
      requesterUserId,
      `rejected_cast_claim_${claimId}`,
      "CAST_CLAIM_REJECTED",
      "캐스트 프로필 연결 반려 안내",
      `${castName} 프로필 연결 요청이 반려되었어요.`,
      castId,
      createdAt
    );
    logger.info("Created requester rejected notification for cast claim.", {
      claimId: claimId,
      requesterUserId: requesterUserId,
    });
  }
);

export const onCastClaimWrittenCleanupSelfFollow = onDocumentWritten(
  "castClaims/{claimId}",
  async (event) => {
    const claimId = event.params.claimId;
    const beforeData = event.data?.before.data() as CastClaimLike | undefined;
    const afterData = event.data?.after.data() as CastClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);
    const castId = asNonBlankString(afterData?.castId);
    const cafeId = asNonBlankString(afterData?.cafeId);
    const userId = asNonBlankString(afterData?.userId);

    if (!isApprovedStatus(afterStatus)) {
      return;
    } else if (isApprovedStatus(beforeStatus)) {
      return;
    } else if (castId == null || cafeId == null || userId == null) {
      return;
    }

    const followId = buildCastFollowDocumentId(userId, castId);
    const firestore = db();
    const castRef = firestore.collection("cafes").doc(cafeId).collection("casts").doc(castId);
    const userRef = firestore.collection("users").doc(userId);
    const followRef = firestore.collection("castFollows").doc(followId);

    await firestore.runTransaction(async (transaction) => {
      const castSnapshot = await transaction.get(castRef);
      const currentLinkedUserId = asNonBlankString(castSnapshot.data()?.linkedUserId);
      const followSnapshot = await transaction.get(followRef);
      const followCastId = asNonBlankString(followSnapshot.data()?.castId);
      const followUserId = asNonBlankString(followSnapshot.data()?.userId);

      if (!castSnapshot.exists) {
        logger.error("Cast claim approval side effects skipped: cast not found.", {
          claimId: claimId,
          cafeId: cafeId,
          castId: castId,
          userId: userId,
        });
        return;
      } else if (currentLinkedUserId != null && currentLinkedUserId !== userId) {
        logger.error("Cast claim approval side effects skipped: cast already linked.", {
          claimId: claimId,
          cafeId: cafeId,
          castId: castId,
          userId: userId,
          currentLinkedUserId: currentLinkedUserId,
        });
        return;
      }

      transaction.set(castRef, {linkedUserId: userId}, {merge: true});
      transaction.set(userRef, {affiliatedCafeId: cafeId}, {merge: true});

      if (followSnapshot.exists && followCastId === castId && followUserId === userId) {
        transaction.delete(followRef);
      }
    });

    logger.info("Applied cast claim approval side effects.", {
      claimId: claimId,
      cafeId: cafeId,
      castId: castId,
      userId: userId,
      followId: followId,
    });
  }
);

export const onReviewWrittenSyncCastVisitCertificationCount = onDocumentWritten(
  "reviews/{reviewId}",
  async (event) => {
    const beforeData = event.data?.before.data() as ReviewLike | undefined;
    const afterData = event.data?.after.data() as ReviewLike | undefined;
    const targetKeys = new Set<string>();

    collectCastUserTargetsFromReviewPayload(beforeData).forEach((key) => targetKeys.add(key));
    collectCastUserTargetsFromReviewPayload(afterData).forEach((key) => targetKeys.add(key));

    if (targetKeys.size == 0) {
      return;
    }

    await Promise.all(
      Array.from(targetKeys).map(async (key) => {
        const {cafeId, castId, userId} = parseCastUserTargetKey(key);

        if (cafeId.length == 0 || castId.length == 0 || userId.length == 0) {
          return;
        }
        await syncCastVisitCertificationForUser(cafeId, castId, userId);
      })
    );

    logger.info("Synced cast visit certification aggregate from review write.", {
      reviewId: event.params.reviewId,
      targets: Array.from(targetKeys),
    });
  }
);

export const onReviewWrittenSyncReviewVisitVerified = onDocumentWritten(
  "reviews/{reviewId}",
  async (event) => {
    const reviewId = asNonBlankString(event.params.reviewId);
    const afterData = event.data?.after.data() as ReviewLike | undefined;

    if (reviewId == null || afterData == null) {
      return;
    }
    await syncSingleReviewVisitVerified(reviewId, afterData);
    logger.info("Synced review visitVerified from review write.", {
      reviewId: reviewId,
    });
  }
);

export const onVisitWrittenSyncCastVisitCertificationCount = onDocumentWritten(
  "visits/{visitId}",
  async (event) => {
    const beforeData = event.data?.before.data() as VisitLike | undefined;
    const afterData = event.data?.after.data() as VisitLike | undefined;
    const beforeCafeId = asNonBlankString(beforeData?.cafeId);
    const beforeUserId = asNonBlankString(beforeData?.userId);
    const beforeVerified = beforeData?.verified === true;
    const afterCafeId = asNonBlankString(afterData?.cafeId);
    const afterUserId = asNonBlankString(afterData?.userId);
    const afterVerified = afterData?.verified === true;

    if (
      beforeCafeId === afterCafeId &&
      beforeUserId === afterUserId &&
      beforeVerified === afterVerified
    ) {
      return;
    }

    const targetPairs = new Set<string>();
    const sourcePairs = new Set<string>();
    const visitSources = [beforeData, afterData];

    visitSources.forEach((visit) => {
      const cafeId = asNonBlankString(visit?.cafeId);
      const userId = asNonBlankString(visit?.userId);

      if (cafeId == null || userId == null) {
        return;
      }
      sourcePairs.add(userCafeKey(cafeId, userId));
    });

    await Promise.all(Array.from(sourcePairs).map(async (sourcePair) => {
      const parsedSource = parseUserCafeKey(sourcePair);
      const cafeId = parsedSource.cafeId;
      const userId = parsedSource.userId;

      if (cafeId.length == 0 || userId.length == 0) {
        return;
      }
      const castIds = await loadTaggedCastIdsByCafeAndUser(cafeId, userId);

      castIds.forEach((castId) => {
        targetPairs.add(castUserTargetKey(cafeId, castId, userId));
      });
    }));

    if (targetPairs.size == 0) {
      return;
    }

    await Promise.all(
      Array.from(targetPairs).map(async (key) => {
        const {cafeId, castId, userId} = parseCastUserTargetKey(key);

        if (cafeId.length == 0 || castId.length == 0 || userId.length == 0) {
          return;
        }
        await syncCastVisitCertificationForUser(cafeId, castId, userId);
      })
    );

    logger.info("Synced cast visit certification aggregate from visit write.", {
      visitId: event.params.visitId,
      targets: Array.from(targetPairs),
    });
  }
);

export const onVisitWrittenSyncReviewVisitVerified = onDocumentWritten(
  "visits/{visitId}",
  async (event) => {
    const beforeData = event.data?.before.data() as VisitLike | undefined;
    const afterData = event.data?.after.data() as VisitLike | undefined;
    const sourcePairs = new Set<string>();
    const visitSources = [beforeData, afterData];

    visitSources.forEach((visit) => {
      const cafeId = asNonBlankString(visit?.cafeId);
      const userId = asNonBlankString(visit?.userId);

      if (cafeId == null || userId == null) {
        return;
      }
      sourcePairs.add(userCafeKey(cafeId, userId));
    });

    if (sourcePairs.size == 0) {
      return;
    }
    await Promise.all(Array.from(sourcePairs).map(async (sourcePair) => {
      const parsed = parseUserCafeKey(sourcePair);

      if (parsed.cafeId.length == 0 || parsed.userId.length == 0) {
        return;
      }
      await syncUserCafeReviewsVisitVerified(parsed.cafeId, parsed.userId);
    }));

    logger.info("Synced review visitVerified from visit write.", {
      visitId: event.params.visitId,
      targets: Array.from(sourcePairs),
    });
  }
);

export const onVisitWrittenValidateDistance = onDocumentWritten(
  "visits/{visitId}",
  async (event) => {
    const visitId = asNonBlankString(event.params.visitId) ?? "";
    const afterData = event.data?.after.data() as VisitLike | undefined;

    if (visitId.length == 0 || afterData == null) {
      return;
    }
    const visitRef = db().collection("visits").doc(visitId);
    const cafeId = asNonBlankString(afterData.cafeId);
    const userId = asNonBlankString(afterData.userId);
    const userLocation = asGeoPoint(afterData.location);
    const checkInMethod = asNonBlankString((afterData as Record<string, unknown>).checkInMethod)?.toUpperCase() ?? "LOCATION";
    const isQrCheckIn = checkInMethod === "QR";
    const allowedRadiusMeters = 200;
    const currentVerified = afterData.verified === true;
    const currentDistance = asFiniteNumber(afterData.verificationDistanceMeters);
    const currentFailureReason = asNonBlankString(afterData.verificationFailureReason);

    if (cafeId == null || userId == null) {
      await visitRef.delete();
      logger.warn("Deleted invalid visit payload (missing cafeId or userId).", {
        visitId: visitId,
        hasCafeId: cafeId != null,
        hasUserId: userId != null,
      });
      return;
    }

    if (isQrCheckIn) {
      if (currentVerified && currentFailureReason == null) {
        return;
      }
      await visitRef.set(
        {
          verified: true,
          verificationFailureReason: FieldValue.delete(),
          verifiedAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
        {merge: true}
      );
      await upsertVisitStamp(visitId, userId, cafeId);
      logger.info("Auto-verified QR check-in visit.", {visitId, cafeId, userId});
      return;
    }

    if (userLocation == null) {
      await visitRef.delete();
      logger.warn("Deleted location-based visit with missing location.", {
        visitId: visitId,
        hasCafeId: cafeId != null,
        hasUserId: userId != null,
      });
      return;
    }
    const cafeSnapshot = await db().collection("cafes").doc(cafeId).get();
    const cafeData = cafeSnapshot.data();
    const region = asPlainObject(cafeData?.region);
    const cafeLocation = asGeoPoint(region?.location);

    if (cafeLocation == null) {
      await visitRef.delete();
      logger.warn("Deleted visit because cafe location is missing.", {
        visitId: visitId,
        cafeId: cafeId,
      });
      return;
    }
    const distanceMeters = haversineMeters(
      cafeLocation.latitude,
      cafeLocation.longitude,
      userLocation.latitude,
      userLocation.longitude
    );
    const shouldVerify = distanceMeters <= allowedRadiusMeters;

    if (!shouldVerify) {
      const failureReason = "OUT_OF_RADIUS";
      const isDistanceSynced = currentDistance != null &&
        Math.abs(currentDistance - distanceMeters) < 0.1;
      const isAlreadyRejected = !currentVerified &&
        currentFailureReason === failureReason &&
        isDistanceSynced;

      if (!isAlreadyRejected) {
        await visitRef.set(
          {
            verified: false,
            verificationDistanceMeters: distanceMeters,
            allowedRadiusMeters: allowedRadiusMeters,
            verificationFailureReason: failureReason,
            updatedAt: new Date().toISOString(),
          },
          {merge: true}
        );
      }
      await db().collection("stamps").doc(visitId).delete();
      logger.info("Rejected visit outside allowed radius without deletion.", {
        visitId: visitId,
        cafeId: cafeId,
        userId: userId,
        distanceMeters: distanceMeters,
        allowedRadiusMeters: allowedRadiusMeters,
      });
      return;
    }
    const isDistanceSynced = currentDistance != null &&
      Math.abs(currentDistance - distanceMeters) < 0.1;

    if (currentVerified && isDistanceSynced && currentFailureReason == null) {
      return;
    }
    await visitRef.set(
      {
        verified: true,
        verificationDistanceMeters: distanceMeters,
        allowedRadiusMeters: allowedRadiusMeters,
        verificationFailureReason: FieldValue.delete(),
        verifiedAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      {merge: true}
    );
    await upsertVisitStamp(visitId, userId, cafeId);

    logger.info("Validated visit distance and marked as verified.", {
      visitId: visitId,
      cafeId: cafeId,
      userId: userId,
      distanceMeters: distanceMeters,
      allowedRadiusMeters: allowedRadiusMeters,
    });
  }
);

export const onVisitWrittenSyncUserVisitStats = onDocumentWritten(
  "visits/{visitId}",
  async (event) => {
    const beforeData = event.data?.before.data() as VisitLike | undefined;
    const afterData = event.data?.after.data() as VisitLike | undefined;
    const beforeUserId = asNonBlankString(beforeData?.userId);
    const beforeVerified = beforeData?.verified === true;
    const afterUserId = asNonBlankString(afterData?.userId);
    const afterVerified = afterData?.verified === true;
    const deltaByUserId = new Map<string, number>();

    const appendDelta = (userId: string | null, delta: number) => {
      if (userId == null || delta == 0) {
        return;
      }
      const current = deltaByUserId.get(userId) ?? 0;
      deltaByUserId.set(userId, current + delta);
    };

    if (beforeVerified) {
      appendDelta(beforeUserId, -1);
    }
    if (afterVerified) {
      appendDelta(afterUserId, 1);
    }
    if (deltaByUserId.size == 0) {
      return;
    }

    await Promise.all(Array.from(deltaByUserId.entries()).map(async ([userId, delta]) => {
      await applyUserVisitCountDelta(userId, delta);
    }));

    logger.info("Synced user visitCount aggregate from visit write.", {
      visitId: event.params.visitId,
      targets: Array.from(deltaByUserId.entries()).map(([userId, delta]) => ({userId, delta})),
    });
  }
);

export const onCafeFavoriteWrittenSyncUserFavoriteStats = onDocumentWritten(
  "cafeFavorites/{favoriteId}",
  async (event) => {
    const beforeData = event.data?.before.data() as CafeFavoriteLike | undefined;
    const afterData = event.data?.after.data() as CafeFavoriteLike | undefined;
    const beforeUserId = asNonBlankString(beforeData?.userId);
    const afterUserId = asNonBlankString(afterData?.userId);
    const deltaByUserId = new Map<string, number>();

    if (beforeUserId != null) {
      deltaByUserId.set(beforeUserId, (deltaByUserId.get(beforeUserId) ?? 0) - 1);
    }
    if (afterUserId != null) {
      deltaByUserId.set(afterUserId, (deltaByUserId.get(afterUserId) ?? 0) + 1);
    }

    const targetEntries = Array.from(deltaByUserId.entries())
      .filter(([userId, delta]) => userId.length > 0 && delta != 0);
    if (targetEntries.length == 0) {
      return;
    }

    await Promise.all(targetEntries.map(async ([userId, delta]) => {
      await syncUserFavoriteCountAggregate(userId, delta);
    }));

    logger.info("Synced user favoritesCount aggregate from favorite write.", {
      favoriteId: event.params.favoriteId,
      targets: targetEntries.map(([userId, delta]) => ({userId, delta})),
    });
  }
);

export const onVisitWrittenIssueStamp = onDocumentWritten(
  "visits/{visitId}",
  async (event) => {
    const visitId = asNonBlankString(event.params.visitId) ?? "";
    const beforeData = event.data?.before.data() as VisitLike | undefined;
    const afterData = event.data?.after.data() as VisitLike | undefined;
    const beforeUserId = asNonBlankString(beforeData?.userId);
    const beforeCafeId = asNonBlankString(beforeData?.cafeId);
    const afterUserId = asNonBlankString(afterData?.userId);
    const afterCafeId = asNonBlankString(afterData?.cafeId);
    const afterVerified = afterData?.verified === true;

    if (visitId.length == 0) {
      return;
    }
    const stampRef = db().collection("stamps").doc(visitId);
    const shouldDelete = afterUserId == null || afterCafeId == null || !afterVerified;
    const hasBefore = beforeUserId != null && beforeCafeId != null;
    const hasAfter = afterUserId != null && afterCafeId != null;
    const isSourceChanged = hasBefore
      && hasAfter
      && (beforeUserId !== afterUserId || beforeCafeId !== afterCafeId);
    const shouldUpsert = !shouldDelete && !isSourceChanged;

    if (shouldDelete || isSourceChanged) {
      await stampRef.delete();
    }
    if (shouldUpsert) {
      await upsertVisitStamp(visitId, afterUserId, afterCafeId);
    }

    logger.info("Synced stamp from visit write.", {
      visitId: visitId,
      beforeUserId: beforeUserId,
      afterUserId: afterUserId,
      beforeCafeId: beforeCafeId,
      afterCafeId: afterCafeId,
      afterVerified: afterVerified,
      deleted: shouldDelete || isSourceChanged,
      upserted: shouldUpsert,
    });
  }
);

export const onVisitWrittenNotifyCafeOwner = onDocumentWritten(
  "visits/{visitId}",
  async (event) => {
    const visitId = asNonBlankString(event.params.visitId) ?? "";
    const beforeData = event.data?.before.data() as VisitLike | undefined;
    const afterData = event.data?.after.data() as VisitLike | undefined;

    const beforeVerified = beforeData?.verified === true;
    const afterVerified = afterData?.verified === true;

    // verified 상태가 false→true 로 전환될 때만 처리 (중복 알림 방지)
    if (!(!beforeVerified && afterVerified)) {
      return;
    }

    const cafeId = asNonBlankString(afterData?.cafeId);
    const userId = asNonBlankString(afterData?.userId);

    if (cafeId == null || userId == null || visitId.length == 0) {
      return;
    }

    const [cafeSnapshot, userSnapshot] = await Promise.all([
      db().collection("cafes").doc(cafeId).get(),
      db().collection("users").doc(userId).get(),
    ]);

    const cafeName = asNonBlankString(cafeSnapshot.data()?.name) ?? "카페";
    const ownerIds = asStringArray(cafeSnapshot.data()?.ownerIds);
    const visitorNickname = asNonBlankString(userSnapshot.data()?.nickname) ?? "방문자";
    const checkInMethod = asNonBlankString((afterData as Record<string, unknown>)?.checkInMethod)?.toUpperCase() ?? "LOCATION";
    const methodLabel = checkInMethod === "QR" ? "QR 체크인" : "위치 체크인";
    const now = new Date().toISOString();
    const notificationIdPrefix = `visit_checkin_${visitId}`;

    if (ownerIds.length == 0) {
      return;
    }

    const tasks = ownerIds.map(async (ownerId) => {
      if (ownerId === userId) return; // 본인 체크인은 알림 제외
      await createUserNotification(
        ownerId,
        `${notificationIdPrefix}_${ownerId}`,
        "CAFE_CHECK_IN",
        `${cafeName} 새 체크인`,
        `${visitorNickname}님이 ${methodLabel}으로 체크인했어요.`,
        cafeId,
        now
      );
    });

    await Promise.all(tasks);

    logger.info("Sent check-in notification to cafe owners.", {
      visitId,
      cafeId,
      userId,
      ownerCount: ownerIds.length,
    });
  }
);

export const onStampWrittenSyncUserStampStats = onDocumentWritten(
  "stamps/{stampId}",
  async (event) => {
    const beforeData = event.data?.before.data() as StampLike | undefined;
    const afterData = event.data?.after.data() as StampLike | undefined;
    const beforeUserId = asNonBlankString(beforeData?.userId);
    const afterUserId = asNonBlankString(afterData?.userId);
    const deltaByUserId = new Map<string, number>();

    if (beforeUserId != null) {
      deltaByUserId.set(beforeUserId, (deltaByUserId.get(beforeUserId) ?? 0) - 1);
    }
    if (afterUserId != null) {
      deltaByUserId.set(afterUserId, (deltaByUserId.get(afterUserId) ?? 0) + 1);
    }

    const targetEntries = Array.from(deltaByUserId.entries())
      .filter(([userId, delta]) => userId.length > 0 && delta != 0);

    if (targetEntries.length == 0) {
      return;
    }

    await Promise.all(targetEntries.map(async ([userId, delta]) => {
      await applyUserStampCountDelta(userId, delta);
    }));

    logger.info("Synced user stampCount aggregate from stamp write.", {
      stampId: event.params.stampId,
      targets: targetEntries.map(([userId, delta]) => ({userId, delta})),
    });
  }
);

function hasCafeRankingRelevantChange(
  beforeData: Record<string, unknown> | undefined,
  afterData: Record<string, unknown> | undefined
): {cafe: boolean; cast: boolean} {
  if (beforeData == null && afterData == null) {
    return {cafe: false, cast: false};
  }
  if (beforeData == null || afterData == null) {
    return {cafe: true, cast: true};
  }
  const beforeRegion = asPlainObject(beforeData.region);
  const afterRegion = asPlainObject(afterData.region);
  const beforeCountry = normalizeCountry(beforeRegion?.country);
  const afterCountry = normalizeCountry(afterRegion?.country);
  const beforeCity = normalizeCity(beforeRegion?.city);
  const afterCity = normalizeCity(afterRegion?.city);
  const beforeAddress = asNonBlankString(beforeRegion?.address) ?? asNonBlankString(beforeData.address) ?? "";
  const afterAddress = asNonBlankString(afterRegion?.address) ?? asNonBlankString(afterData.address) ?? "";
  const beforeName = asNonBlankString(beforeData.name) ?? "";
  const afterName = asNonBlankString(afterData.name) ?? "";
  const beforeRating = asNumber(beforeData.ratingAvg) ?? 0;
  const afterRating = asNumber(afterData.ratingAvg) ?? 0;
  const beforeApproved = beforeData.approved !== false;
  const afterApproved = afterData.approved !== false;
  const beforeImageUrl = asNonBlankString(beforeData.thumbnailImage) ?? "";
  const afterImageUrl = asNonBlankString(afterData.thumbnailImage) ?? "";

  const cafeRankingChanged =
    beforeName !== afterName ||
    beforeCountry !== afterCountry ||
    beforeCity !== afterCity ||
    beforeAddress !== afterAddress ||
    beforeRating !== afterRating ||
    beforeApproved !== afterApproved ||
    beforeImageUrl !== afterImageUrl;
  const castRankingDependencyChanged =
    beforeName !== afterName ||
    beforeCountry !== afterCountry ||
    beforeCity !== afterCity ||
    beforeApproved !== afterApproved;

  return {
    cafe: cafeRankingChanged,
    cast: castRankingDependencyChanged,
  };
}

export const onCafeWrittenMarkRankingDirty = onDocumentWritten(
  "cafes/{cafeId}",
  async (event) => {
    const beforeData = event.data?.before.data() as Record<string, unknown> | undefined;
    const afterData = event.data?.after.data() as Record<string, unknown> | undefined;
    const targets = hasCafeRankingRelevantChange(beforeData, afterData);

    if (!targets.cafe && !targets.cast) {
      return;
    }
    await markRankingSyncDirty("cafe_written", {
      cafeId: event.params.cafeId,
      targets: targets,
    }, targets);
  }
);

export const onCastWrittenMarkRankingDirty = onDocumentWritten(
  "cafes/{cafeId}/casts/{castId}",
  async (event) => {
    await markRankingSyncDirty("cast_written", {
      cafeId: event.params.cafeId,
      castId: event.params.castId,
    }, {cafe: false, cast: true});
  }
);

export const onCafeCastWrittenSyncCastDirectory = onDocumentWritten(
  "cafes/{cafeId}/casts/{castId}",
  async (event) => {
    const cafeId = asNonBlankString(event.params.cafeId);
    const castId = asNonBlankString(event.params.castId);

    if (cafeId == null || castId == null) {
      return;
    }
    const castDirectoryRef = db().collection("castDirectory").doc(castId);
    const afterData = event.data?.after.data();

    if (afterData == null) {
      await castDirectoryRef.delete();
      logger.info("Deleted castDirectory entry from cast delete.", {
        cafeId: cafeId,
        castId: castId,
      });
      return;
    }
    await castDirectoryRef.set(
      {
        castId: castId,
        cafeId: cafeId,
        updatedAt: new Date().toISOString(),
      },
      {merge: true}
    );
    logger.info("Upserted castDirectory entry from cast write.", {
      cafeId: cafeId,
      castId: castId,
    });
  }
);

export const onCastDeletedCleanupImages = onDocumentDeleted(
  "cafes/{cafeId}/casts/{castId}",
  async (event) => {
    const castId = asNonBlankString(event.params.castId) ?? "unknown_cast";
    const beforeData = event.data?.data() as Record<string, unknown> | undefined;
    const profileImageUrl = asNonBlankString(beforeData?.profileImage);
    const galleryImageUrls = asStringArray(beforeData?.galleryImages);
    const imageUrls = [
      ...galleryImageUrls,
      ...(profileImageUrl == null ? [] : [profileImageUrl]),
    ]
      .map((imageUrl) => imageUrl.trim())
      .filter((imageUrl) => imageUrl.length > 0);
    const uniqueImageUrls = Array.from(new Set(imageUrls));

    if (uniqueImageUrls.length == 0) {
      return;
    }
    const results = await Promise.allSettled(
      uniqueImageUrls.map((imageUrl) => deleteStorageFileByUrl(imageUrl))
    );
    const failedImageUrls = results
      .map((result, index) => ({result, imageUrl: uniqueImageUrls[index]}))
      .filter((entry) => entry.result.status === "rejected")
      .map((entry) => entry.imageUrl);

    if (failedImageUrls.length > 0) {
      logger.warn("Failed to cleanup some cast images after cast deletion.", {
        castId: castId,
        failedCount: failedImageUrls.length,
        failedImageUrls: failedImageUrls,
      });
    } else {
      logger.info("Cleaned up cast images after cast deletion.", {
        castId: castId,
        deletedImageCount: uniqueImageUrls.length,
      });
    }
  }
);

export const onScheduleSyncRankingSnapshots = onSchedule(
  {
    schedule: "every 30 minutes",
    timeZone: "Asia/Seoul",
  },
  async () => {
    const dirtyState = await resolveRankingDirtyState();

    if (dirtyState == null) {
      return;
    }
    await syncAllRankingSnapshots(dirtyState);
    await completeRankingSync();
    logger.info("Synced ranking snapshots.", {
      periods: RANKING_PERIODS,
      scopeCount: RANKING_SCOPES.length,
      syncedCafe: dirtyState.shouldSyncCafe,
      syncedCast: dirtyState.shouldSyncCast,
    });
  }
);

export const onScheduleCreateBirthdayNotifications = onSchedule(
  {
    schedule: "every day 10:00",
    timeZone: "Asia/Seoul",
  },
  async () => {
    await syncBirthdayNotifications();
    logger.info("Synced birthday notifications.", {
      birthdayKey: kstBirthdayKey(kstNow()),
    });
  }
);

export const onScheduleDeleteExpiredCastSchedules = onSchedule(
  {
    schedule: "every monday 03:00",
    timeZone: "Asia/Seoul",
  },
  async () => {
    const firestore = db();
    const now = kstNow();
    // 지난 주 일요일(현재 주 시작 - 1일) 이전 데이터를 삭제한다
    const cutoffDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    const cutoffDateKey = kstDateKey(cutoffDate);

    const snapshot = await firestore
      .collection("castSchedules")
      .where("date", "<", cutoffDateKey)
      .get();

    if (snapshot.empty) {
      logger.info("onScheduleDeleteExpiredCastSchedules: no expired schedules found.");
      return;
    }

    const BATCH_SIZE = 500;
    let deletedCount = 0;

    for (let i = 0; i < snapshot.docs.length; i += BATCH_SIZE) {
      const batch = firestore.batch();
      const chunk = snapshot.docs.slice(i, i + BATCH_SIZE);
      chunk.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
      deletedCount += chunk.length;
    }

    logger.info("onScheduleDeleteExpiredCastSchedules completed.", {
      cutoffDate: cutoffDateKey,
      deletedCount,
    });
  }
);

export const getPublicHomeBanners = onRequest(
  {
    region: "us-central1",
  },
  async (request, response) => {
    response.set("Access-Control-Allow-Origin", "*");
    response.set("Access-Control-Allow-Methods", "GET, OPTIONS");
    response.set("Access-Control-Allow-Headers", "Content-Type");

    if (request.method === "OPTIONS") {
      response.status(204).send("");
      return;
    } else if (request.method !== "GET") {
      response.status(405).json({
        error: "method_not_allowed",
      });
      return;
    }
    try {
      const limitRaw = asNonNegativeInt(Number(request.query.limit));
      const limit = Math.min(Math.max(limitRaw ?? 5, 1), 10);
      const nowEpochMillis = Date.now();
      const snapshot = await db()
        .collection("homeBanners")
        .get();
      const items = snapshot.docs
        .map((doc) => {
          const data = doc.data() as HomeBannerLike;
          const linkType = asNonBlankString(data.linkType)?.toUpperCase() ?? null;
          const linkTarget = asNonBlankString(data.linkTarget);
          const href = resolveBannerHref(linkType, linkTarget);
          const activatedAtEpochMillis = resolveBannerActivatedAtEpochMillis(data);
          const displayDays = resolveBannerDisplayDays(data);
          const isActive = isBannerActiveNow(data, nowEpochMillis);

          if (!isActive) {
            return null;
          }
          return {
            id: doc.id,
            title: asNonBlankString(data.title) ?? "콘카 소식",
            subtitle: asNonBlankString(data.subtitle) ?? "",
            imageUrl: resolvePublicBannerImageUrl(asNonBlankString(data.imageUrl)),
            linkType: linkType,
            linkTarget: linkTarget,
            href: href,
            displayDays: displayDays,
            activatedAtEpochMillis: activatedAtEpochMillis,
          };
        })
        .filter((item): item is NonNullable<typeof item> => item != null)
        .sort((a, b) => b.activatedAtEpochMillis - a.activatedAtEpochMillis)
        .slice(0, limit);

      response.status(200).json({
        items: items,
        serverTimeEpochMillis: nowEpochMillis,
      });
    } catch (error) {
      logger.error("getPublicHomeBanners failed.", error);
      response.status(500).json({
        error: "internal",
      });
    }
  }
);

type MockCafeSeedConfig = {
  cafeCount: number;
  castsPerCafe: number;
  menusPerCafe: number;
  noticesPerCafe: number;
  reviewsPerCafe: number;
};

const DEFAULT_MOCK_CAFE_SEED_CONFIG: MockCafeSeedConfig = {
  cafeCount: 3,
  castsPerCafe: 30,
  menusPerCafe: 15,
  noticesPerCafe: 20,
  reviewsPerCafe: 40,
};

const MOCK_CAFE_PRESETS = [
  {
    id: "mock_cafe_001",
    name: "콘카페 서울 가든",
    city: "Seoul",
    address: "Mapo-gu Hongik-ro 10",
    latitude: 37.5569,
    longitude: 126.9245,
    country: "KR",
    desc: "홍대 인근의 메이드 컨셉 카페로, 무대 이벤트와 시즌 음료가 활발한 매장입니다.",
    thumbnailImage: "https://picsum.photos/seed/concafe_seoul/960/540",
  },
  {
    id: "mock_cafe_002",
    name: "콘카페 도쿄 스테이지",
    city: "Tokyo",
    address: "Chiyoda-ku Akihabara 2-5",
    latitude: 35.6984,
    longitude: 139.773,
    country: "JP",
    desc: "아키하바라 감성의 컨셉 카페로, 아이돌 퍼포먼스와 팬 소통존이 특징입니다.",
    thumbnailImage: "https://picsum.photos/seed/concafe_tokyo/960/540",
  },
  {
    id: "mock_cafe_003",
    name: "콘카페 오사카 하버",
    city: "Osaka",
    address: "Kita-ku Umeda 1-8",
    latitude: 34.7024,
    longitude: 135.4959,
    country: "JP",
    desc: "우메다 지역의 따뜻한 분위기 카페로, 디저트와 포토존 중심의 구성을 갖췄습니다.",
    thumbnailImage: "https://picsum.photos/seed/concafe_osaka/960/540",
  },
];

const MOCK_HOME_BANNERS = [
  {
    id: "mock_home_banner_001",
    title: "주말 한정 스테이지 이벤트",
    subtitle: "서울 가든에서 이번 주말 특별 공연 진행",
    startColorHex: "FFD1DC",
    endColorHex: "F58FB2",
    imageUrl: "https://picsum.photos/seed/mock_home_banner_001/1280/720",
    linkType: "CAFE",
    linkTargetCafeId: "mock_cafe_001",
    displayDays: 7,
  },
  {
    id: "mock_home_banner_002",
    title: "시즌 디저트 신메뉴 오픈",
    subtitle: "도쿄 스테이지, 오사카 하버 동시 출시",
    startColorHex: "FDEBC8",
    endColorHex: "F7C58D",
    imageUrl: "https://picsum.photos/seed/mock_home_banner_002/1280/720",
    linkType: "CAFE",
    linkTargetCafeId: "mock_cafe_002",
    displayDays: 10,
  },
];

function pad3(value: number): string {
  return `${value}`.padStart(3, "0");
}

const MOCK_USER_NICKNAME_POOL = [
  "딸기라떼", "하늘토끼", "별빛고양이", "달콤푸딩", "유자소다", "벚꽃우유", "라임쿠키", "눈송이", "은하수", "단팥붕어",
  "바닐라구름", "복숭아티", "모찌러버", "밀크티덕후", "크림소다", "체리무스", "마카롱냥", "허니토스트", "초코크림", "말차라떼",
  "시나몬롤", "캔디스타", "무지개푸딩", "포근담요", "달빛산책", "소금빵러버", "카페산책", "설탕비", "도토리", "해질녘",
  "하트스푼", "구름사탕", "몽글몽글", "노을빛", "봄날기록", "오렌지피즈", "밤하늘", "디저트픽", "카페메모", "핫초코",
  "수플레", "메론소다", "포토존러", "주말나들이", "리본쿠키", "디저트탐험", "우유푸딩", "체크인러", "라떼한잔", "달님",
];

const MOCK_CAST_NAME_POOL = [
  "하은", "서아", "지안", "나연", "유리", "수아", "시온", "채린", "가은", "예린",
  "도아", "해린", "소윤", "아린", "지우", "라희", "민서", "다은", "서윤", "유나",
  "채아", "은별", "세아", "보민", "하린", "주아", "나래", "예나", "라온", "다온",
  "소희", "가빈", "은채", "지유", "하연", "미소", "연우", "선아", "지민", "태린",
];

const MOCK_REVIEW_OPENERS = [
  "응대가 꼼꼼하고 친절해서 첫 방문도 편했습니다.",
  "포토존 분위기가 좋아서 사진 찍기 좋았습니다.",
  "캐스트와의 대화가 자연스럽고 부담이 없었습니다.",
  "무대 진행 타이밍이 깔끔해서 몰입감이 좋았습니다.",
  "디저트 퀄리티가 기대 이상이었습니다.",
  "대기 줄이 생각보다 빨리 빠졌습니다.",
];

const MOCK_REVIEW_CLOSERS = [
  "다음에는 친구들이랑 다시 방문할 예정입니다.",
  "처음 가보는 분들에게도 추천할 만합니다.",
  "개인적으로 재방문 의사가 높은 매장입니다.",
  "다음에는 시즌 메뉴도 꼭 먹어보려고 합니다.",
  "추천해준 메뉴 선택이 정말 좋았습니다.",
  "전체적으로 만족도가 높은 방문이었습니다.",
];

const MOCK_MEMO_POOL = [
  "퇴근 후 짧게 들러 체크인했습니다.",
  "근처 공연 보고 이동해서 방문했습니다.",
  "추천받은 시즌 음료를 마셨습니다.",
  "생일 테마 이벤트 날에 맞춰 방문했습니다.",
  "첫 방문인데 응대가 정말 친절했습니다.",
  "주말 라인업 분위기가 활기찼습니다.",
];

const MOCK_MENU_TEMPLATES: Record<string, string[]> = {
  drink: [
    "딸기 우유", "유자 스파클링", "바닐라 라떼", "복숭아 티", "흑임자 라떼",
    "말차 플로트", "크림 소다", "체리 에이드", "허니 레몬 티", "하우스 블렌드 커피",
  ],
  dessert: [
    "베리 팬케이크", "크림 파르페", "초코 와플", "모찌 플레이트", "카라멜 푸딩",
    "딸기 쇼트케이크", "수플레 치즈케이크", "몽블랑", "허니 토스트", "티라미수 컵",
  ],
  goods: [
    "아크릴 키링", "포토카드 세트", "스티커 팩", "미니 타월", "배지 세트",
    "캐릭터 컵", "이벤트 포스터", "데스크 캘린더", "폴라로이드 슬리브", "팬 키트",
  ],
};

function seededInt(seed: number): number {
  return (((seed * 1103515245) + 12345) >>> 0);
}

function pickSeeded<T>(items: T[], seed: number): T {
  const index = seededInt(seed) % items.length;
  return items[index];
}

function buildMockCastDisplayName(cafeIndex: number, castIndex: number): string {
  const base = pickSeeded(MOCK_CAST_NAME_POOL, (cafeIndex * 1000) + castIndex);
  return base;
}

function buildMockReviewContent(cafeName: string, reviewIndex: number): string {
  const opener = pickSeeded(MOCK_REVIEW_OPENERS, reviewIndex * 31);
  const closer = pickSeeded(MOCK_REVIEW_CLOSERS, reviewIndex * 43);
  return `${opener} ${cafeName}는 이번 방문에서도 전반적인 퀄리티가 안정적이었습니다. ${closer}`;
}

function buildMockNoticeTitle(cafeName: string, noticeIndex: number): string {
  const prefixCycle = ["이벤트", "업데이트", "안내", "스케줄", "캠페인"];
  const prefix = prefixCycle[noticeIndex % prefixCycle.length];
  return `${prefix} ${pad3(noticeIndex)} - ${cafeName}`;
}

function buildMockNoticeContent(cafeName: string, noticeIndex: number): string {
  const templateCycle = [
    "라인업 및 좌석 안내가 업데이트되었습니다. 방문 전 오픈 시간을 확인해 주세요.",
    "재료 수급 상황에 따라 일부 한정 메뉴 제공 시간이 변경되었습니다.",
    "혼잡 완화를 위해 포토존 운영 시간이 조정되었습니다.",
    "다음 주 이벤트 사전 예약 창이 오픈되었습니다.",
    "매장 운영 공지: 대기열 및 좌석 운영 정책이 변경되었습니다.",
  ];
  const body = templateCycle[noticeIndex % templateCycle.length];
  return `${cafeName}: ${body}`;
}

function resolveSeedConfig(request: {query: Record<string, unknown>}): MockCafeSeedConfig {
  const toPositiveInt = (raw: unknown, fallback: number, max: number): number => {
    const parsed = asNonNegativeInt(Number(raw));
    const normalized = parsed ?? fallback;
    return Math.max(1, Math.min(normalized, max));
  };
  return {
    cafeCount: toPositiveInt(request.query.cafeCount, DEFAULT_MOCK_CAFE_SEED_CONFIG.cafeCount, 3),
    castsPerCafe: toPositiveInt(request.query.castsPerCafe, DEFAULT_MOCK_CAFE_SEED_CONFIG.castsPerCafe, 100),
    menusPerCafe: toPositiveInt(request.query.menusPerCafe, DEFAULT_MOCK_CAFE_SEED_CONFIG.menusPerCafe, 100),
    noticesPerCafe: toPositiveInt(request.query.noticesPerCafe, DEFAULT_MOCK_CAFE_SEED_CONFIG.noticesPerCafe, 100),
    reviewsPerCafe: toPositiveInt(request.query.reviewsPerCafe, DEFAULT_MOCK_CAFE_SEED_CONFIG.reviewsPerCafe, 200),
  };
}

async function deleteDocumentsWithPrefix(collectionPath: string, prefix: string): Promise<number> {
  const firestore = db();
  const documentSnapshot = await firestore.collection(collectionPath)
    .where(FieldPath.documentId(), ">=", prefix)
    .where(FieldPath.documentId(), "<=", `${prefix}\uf8ff`)
    .get();
  const docs = documentSnapshot.docs;
  let batch = firestore.batch();
  let pendingDeleteCount = 0;
  let deletedCount = 0;

  for (let i = 0; i < docs.length; i += 1) {
    batch.delete(docs[i].ref);
    pendingDeleteCount += 1;
    deletedCount += 1;

    if (pendingDeleteCount >= 450) {
      await batch.commit();
      batch = firestore.batch();
      pendingDeleteCount = 0;
    }
  }

  if (pendingDeleteCount > 0) {
    await batch.commit();
  }
  return deletedCount;
}

type JpShopListResponse = {
  count?: unknown;
  shops?: unknown;
};

type JpShopSource = {
  key: "tokyo" | "osaka" | "yokohama";
  defaultCity: "Tokyo" | "Osaka" | "Yokohama";
  baseUrl: string;
};

type JpCrawlSyncSummary = {
  shopListPages: number;
  shopsDiscovered: number;
  shopDetailsFetched: number;
  castDetailsFetched: number;
  cafesUpserted: number;
  cafesSkipped: number;
  cafesDeleted: number;
  castsUpserted: number;
  castsSkipped: number;
  translationRequests: number;
  translationFailures: number;
  apiErrors: number;
  dryRun: boolean;
};

type JpCrawlRunOptions = {
  dryRun: boolean;
  sourceKeys: Set<"tokyo" | "osaka" | "yokohama">;
  maxPages: number;
  maxShops: number;
};

const JP_SHOP_IMAGE_BASE_URL = "https://img.con-cafe.jp/upload/";
const GOOGLE_TRANSLATE_API_KEY_SECRET = defineSecret("GOOGLE_TRANSLATE_API_KEY");
const JP_CRAWL_SOURCES: JpShopSource[] = [
  {
    key: "tokyo",
    defaultCity: "Tokyo",
    baseUrl: "https://con-cafe.jp/api/shop?displayed=1&request=1&front_displayed=1&region=area02&sort=priority&order=desc",
  },
  {
    key: "osaka",
    defaultCity: "Osaka",
    baseUrl: "https://con-cafe.jp/api/shop?displayed=1&request=1&front_displayed=1&region=area06&prefecture=pre25&sort=priority&order=desc",
  },
  {
    key: "yokohama",
    defaultCity: "Yokohama",
    baseUrl: "https://con-cafe.jp/api/shop?displayed=1&request=1&front_displayed=1&region=area02&prefecture=pre09&area=sub063&sort=priority&order=desc",
  },
];

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function toFiniteNumber(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return null;
}

function toInt(value: unknown): number | null {
  const parsed = toFiniteNumber(value);
  if (parsed == null) {
    return null;
  }
  return Math.trunc(parsed);
}

function decodeBasicHtmlEntities(value: string): string {
  return value
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, "\"")
    .replace(/&#39;/g, "'");
}

function withPage(baseUrl: string, page: number): string {
  return `${baseUrl}&page=${Math.max(1, page)}`;
}

function normalizeImageUrl(raw: unknown): string | null {
  const value = asNonBlankString(raw);
  if (value == null) {
    return null;
  }
  if (value.startsWith("http://") || value.startsWith("https://")) {
    return value;
  }
  const normalizedPath = value.startsWith("/") ? value.substring(1) : value;
  return `${JP_SHOP_IMAGE_BASE_URL}${normalizedPath}`;
}

function uniqueNonBlankStrings(values: Array<string | null | undefined>): string[] {
  return Array.from(new Set(values.filter((value): value is string => value != null && value.trim().length > 0)));
}

function toBooleanQuery(value: unknown): boolean {
  if (typeof value !== "string") {
    return false;
  }
  const normalized = value.trim().toLowerCase();
  return normalized === "1" || normalized === "true" || normalized === "yes";
}

function parsePositiveIntQuery(value: unknown, fallback: number, max: number): number {
  const asString = typeof value === "string" ? value : "";
  const parsed = Number(asString);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback;
  }
  return Math.min(Math.floor(parsed), max);
}

function parseSourceKeys(value: unknown): Set<"tokyo" | "osaka" | "yokohama"> {
  const allowed = new Set<"tokyo" | "osaka" | "yokohama">(["tokyo", "osaka", "yokohama"]);
  if (typeof value !== "string") {
    return new Set(allowed);
  }
  const tokens = value
    .split(",")
    .map((token) => token.trim().toLowerCase())
    .filter((token) => token.length > 0);
  if (tokens.length == 0 || tokens.includes("all")) {
    return new Set(allowed);
  }
  const selected = tokens.filter((token): token is "tokyo" | "osaka" | "yokohama" =>
    allowed.has(token as "tokyo" | "osaka" | "yokohama"));
  if (selected.length == 0) {
    return new Set(allowed);
  }
  return new Set(selected);
}

function sanitizeSocialAccount(raw: string): string | null {
  const normalized = raw.trim().replace(/^[\s@]+/, "").replace(/\/+$/, "").toLowerCase();
  if (normalized.length == 0) {
    return null;
  }
  if (normalized.includes(" ")) {
    return null;
  }
  return normalized;
}

function tryParseUrl(raw: string): URL | null {
  try {
    if (raw.startsWith("http://") || raw.startsWith("https://")) {
      return new URL(raw);
    }
    return new URL(`https://${raw}`);
  } catch (_error) {
    return null;
  }
}

function extractSocialAccountId(rawValue: unknown, platform: "twitter" | "instagram" | "tiktok" | "youtube"): string | null {
  const raw = asNonBlankString(rawValue);
  if (raw == null) {
    return null;
  }
  const url = tryParseUrl(raw);

  if (url == null) {
    return sanitizeSocialAccount(raw);
  }
  const host = url.hostname.toLowerCase();
  const segments = url.pathname.split("/").filter((segment) => segment.length > 0);
  const first = segments[0] ?? "";

  if (platform === "twitter") {
    if (!host.includes("x.com") && !host.includes("twitter.com")) {
      return null;
    }
    return sanitizeSocialAccount(first);
  }
  if (platform === "instagram") {
    if (!host.includes("instagram.com")) {
      return null;
    }
    return sanitizeSocialAccount(first);
  }
  if (platform === "tiktok") {
    if (!host.includes("tiktok.com")) {
      return null;
    }
    return sanitizeSocialAccount(first.replace(/^@/, ""));
  }
  if (!host.includes("youtube.com")) {
    return null;
  }
  if (first === "@" || first.length == 0) {
    return null;
  }
  if (first.startsWith("@")) {
    return sanitizeSocialAccount(first.substring(1));
  }
  if (first === "channel") {
    return sanitizeSocialAccount(segments[1] ?? "");
  }
  return sanitizeSocialAccount(first);
}

function mapJpCity(
  shopDetail: Record<string, unknown>,
  fallback: "Tokyo" | "Osaka" | "Yokohama"
): "Tokyo" | "Osaka" | "Yokohama" {
  const prefecture = asPlainObject(shopDetail.prefecture);
  const area = asPlainObject(shopDetail.area);
  const areaPrefecture = asPlainObject(area?.prefecture);
  const prefectureSlug = asNonBlankString(prefecture?.slug) ?? asNonBlankString(areaPrefecture?.slug);
  const prefectureName = asNonBlankString(prefecture?.name) ?? asNonBlankString(areaPrefecture?.name);

  if (prefectureSlug === "pre08" || prefectureName?.includes("東京") === true) {
    return "Tokyo";
  }
  if (prefectureSlug === "pre25" || prefectureName?.includes("大阪") === true) {
    return "Osaka";
  }
  if (
    prefectureSlug === "pre09" ||
    prefectureName?.includes("神奈川") === true ||
    prefectureName?.includes("横浜") === true
  ) {
    return "Yokohama";
  }
  return fallback;
}

function inferConceptType(shopDetail: Record<string, unknown>): "MAID" | "BUTLER" | "IDOL" {
  const concepts = Array.isArray(shopDetail.concepts) ? shopDetail.concepts : [];
  const conceptNames = concepts
    .map((item) => asPlainObject(item))
    .map((item) => asPlainObject(item?.concept))
    .map((concept) => asNonBlankString(concept?.name) ?? "")
    .join(" ");
  const text = [
    asNonBlankString(shopDetail.name) ?? "",
    asNonBlankString(shopDetail.description) ?? "",
    conceptNames,
  ].join(" ").toLowerCase();

  if (text.includes("執事") || text.includes("butler")) {
    return "BUTLER";
  }
  if (text.includes("アイドル") || text.includes("idol")) {
    return "IDOL";
  }
  return "MAID";
}

function toBirthdayIso(birthMonth: unknown, birthDay: unknown): string | null {
  const month = toInt(birthMonth);
  const day = toInt(birthDay);
  if (month == null || day == null || month < 1 || month > 12 || day < 1 || day > 31) {
    return null;
  }
  return `2000-${month.toString().padStart(2, "0")}-${day.toString().padStart(2, "0")}`;
}

function toBirthdayKey(birthdayIso: string | null): string | null {
  if (birthdayIso == null) {
    return null;
  }
  const parts = birthdayIso.split("-");
  if (parts.length !== 3) {
    return null;
  }
  return `${parts[1]}-${parts[2]}`;
}

const DAY_TYPE_KO: Record<string, string> = {
  MONDAY: "월",
  TUESDAY: "화",
  WEDNESDAY: "수",
  THURSDAY: "목",
  FRIDAY: "금",
  SATURDAY: "토",
  SUNDAY: "일",
};

function buildBusinessHours(newBusinessHours: unknown, businessHours: unknown): string | null {
  if (Array.isArray(newBusinessHours) && newBusinessHours.length > 0) {
    const lines = newBusinessHours
      .map((item) => asPlainObject(item))
      .map((item) => {
        const day = asNonBlankString(item?.day_type);
        const start = asNonBlankString(item?.start_time);
        const end = asNonBlankString(item?.end_time);
        if (day == null || start == null || end == null) {
          return null;
        }
        const dayKo = DAY_TYPE_KO[day.toUpperCase()] ?? day;
        return `${dayKo} ${start}-${end}`;
      })
      .filter((line): line is string => line != null);
    if (lines.length > 0) {
      return lines.join("\n");
    }
  }
  return asNonBlankString(businessHours);
}

function toSourceHash(payload: unknown): string {
  return createHash("sha256").update(JSON.stringify(payload)).digest("hex");
}

async function fetchJsonWithRetry(url: string, maxAttempts: number): Promise<unknown> {
  let attempt = 0;
  let lastError: unknown = null;

  while (attempt < maxAttempts) {
    attempt += 1;
    try {
      const response = await fetch(url, {
        method: "GET",
        headers: {
          "Accept": "application/json",
          "User-Agent": "ConCafeCrawler/1.0",
        },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      lastError = error;
      if (attempt >= maxAttempts) {
        break;
      }
      await sleep(300 * attempt);
    }
  }
  throw lastError instanceof Error ? lastError : new Error("request failed");
}

async function translateJaToKo(
  rawText: string,
  cache: Map<string, string>,
  summary: JpCrawlSyncSummary
): Promise<{text: string; status: "OK" | "FAILED" | "SKIPPED_NO_KEY"}> {
  const text = rawText.trim();
  if (text.length == 0) {
    return {text: "", status: "SKIPPED_NO_KEY"};
  }
  const cached = cache.get(text);
  if (cached != null) {
    return {text: cached, status: "OK"};
  }
  let apiKey = "";
  try {
    apiKey = GOOGLE_TRANSLATE_API_KEY_SECRET.value().trim();
  } catch (_error) {
    apiKey = "";
  }
  if (apiKey == null || apiKey.length == 0) {
    cache.set(text, text);
    return {text: text, status: "SKIPPED_NO_KEY"};
  }

  summary.translationRequests += 1;
  try {
    const response = await fetch(`https://translation.googleapis.com/language/translate/v2?key=${encodeURIComponent(apiKey)}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        q: text,
        source: "ja",
        target: "ko",
        format: "text",
      }),
    });
    if (!response.ok) {
      throw new Error(`translation_http_${response.status}`);
    }
    const payload = asPlainObject(await response.json());
    const data = asPlainObject(payload?.data);
    const translations = Array.isArray(data?.translations) ? data?.translations : [];
    const first = asPlainObject(translations[0]);
    const translated = asNonBlankString(first?.translatedText);
    if (translated == null) {
      throw new Error("translation_empty");
    }
    const decoded = decodeBasicHtmlEntities(translated);
    cache.set(text, decoded);
    return {text: decoded, status: "OK"};
  } catch (error) {
    summary.translationFailures += 1;
    logger.warn("translateJaToKo failed. fallback to source text.", {
      error: error instanceof Error ? error.message : "unknown",
    });
    cache.set(text, text);
    return {text: text, status: "FAILED"};
  }
}

async function runJpCrawledDataSync(options: JpCrawlRunOptions): Promise<JpCrawlSyncSummary> {
  const firestore = db();
  const summary: JpCrawlSyncSummary = {
    shopListPages: 0,
    shopsDiscovered: 0,
    shopDetailsFetched: 0,
    castDetailsFetched: 0,
    cafesUpserted: 0,
    cafesSkipped: 0,
    cafesDeleted: 0,
    castsUpserted: 0,
    castsSkipped: 0,
    translationRequests: 0,
    translationFailures: 0,
    apiErrors: 0,
    dryRun: options.dryRun,
  };
  const translationCache = new Map<string, string>();
  const seenShopIds = new Set<number>();

  for (const source of JP_CRAWL_SOURCES) {
    if (!options.sourceKeys.has(source.key)) {
      continue;
    }
    let page = 1;
    while (page <= options.maxPages && seenShopIds.size < options.maxShops) {
      const pageUrl = withPage(source.baseUrl, page);
      let payload: JpShopListResponse;
      try {
        payload = await fetchJsonWithRetry(pageUrl, 3) as JpShopListResponse;
      } catch (error) {
        summary.apiErrors += 1;
        logger.error("runJpCrawledDataSync list fetch failed.", {
          source: source.key,
          page: page,
          error: error instanceof Error ? error.message : "unknown",
        });
        break;
      }
      summary.shopListPages += 1;
      const shops = Array.isArray(payload.shops) ? payload.shops : [];
      if (shops.length == 0) {
        break;
      }
      for (const shopRaw of shops) {
        if (seenShopIds.size >= options.maxShops) {
          break;
        }
        const shop = asPlainObject(shopRaw);
        const shopId = toInt(shop?.id);
        if (shopId == null || shopId <= 0 || seenShopIds.has(shopId)) {
          continue;
        }
        seenShopIds.add(shopId);
        summary.shopsDiscovered += 1;

        let shopDetail = asPlainObject(shop);
        try {
          const detailPayload = await fetchJsonWithRetry(`https://con-cafe.jp/api/shop/${shopId}`, 3);
          const detail = asPlainObject(detailPayload);
          if (detail != null) {
            shopDetail = detail;
            summary.shopDetailsFetched += 1;
          }
        } catch (error) {
          summary.apiErrors += 1;
          logger.warn("runJpCrawledDataSync shop detail fetch failed.", {
            source: source.key,
            shopId: shopId,
            error: error instanceof Error ? error.message : "unknown",
          });
        }
        if (shopDetail == null) {
          continue;
        }
        // 진단 로그: 실제 API 응답의 casts 필드 구조 확인
        logger.info("runJpCrawledDataSync shop casts diagnostic.", {
          source: source.key,
          shopId: shopId,
          castsType: typeof shopDetail.casts,
          castsIsArray: Array.isArray(shopDetail.casts),
          castsLength: Array.isArray(shopDetail.casts) ? shopDetail.casts.length : null,
          castsPreview: Array.isArray(shopDetail.casts)
            ? shopDetail.casts.slice(0, 2).map((c: unknown) => {
              const obj = asPlainObject(c);
              return obj ? {id: obj.id, name: obj.name, front_displayed: obj.front_displayed, displayed: obj.displayed} : c;
            })
            : shopDetail.casts,
        });
        // casts 필드가 없거나 빈 배열인 카페는 즉시 건너뜀
        if (!Array.isArray(shopDetail.casts) || shopDetail.casts.length === 0) {
          summary.cafesSkipped += 1;
          logger.info("runJpCrawledDataSync skipped cafe: casts field is absent or empty.", {
            source: source.key,
            shopId: shopId,
          });
          continue;
        }
        const casts = shopDetail.casts;
        const validCastItems = casts
          .map((castRaw: unknown) => asPlainObject(castRaw))
          .filter((castItem) => {
            if (castItem == null) return false;
            const castIdNumber = toInt(castItem.id);
            if (castIdNumber == null || castIdNumber <= 0) return false;
            // 이름이 없는 캐스트는 비활성/퇴직으로 간주
            if (asNonBlankString(castItem.name) == null) return false;
            // 노출 여부 체크: boolean(true/false) 및 number(1/0) 모두 처리
            const frontDisplayed = castItem.front_displayed;
            if (frontDisplayed !== undefined) {
              return frontDisplayed === 1 || frontDisplayed === true;
            }
            const displayed = castItem.displayed;
            if (displayed !== undefined) {
              return displayed === 1 || displayed === true;
            }
            return true;
          });
        if (validCastItems.length === 0) {
          summary.cafesSkipped += 1;
          logger.info("runJpCrawledDataSync skipped cafe: no active casts after filtering.", {
            source: source.key,
            shopId: shopId,
            totalCasts: casts.length,
          });
          continue;
        }

        const cafeId = `jp_shop_${shopId}`;
        const shopNameJa = asNonBlankString(shopDetail.name) ?? `JP Shop ${shopId}`;
        const shopDescJa = asNonBlankString(shopDetail.description) ?? asNonBlankString(shopDetail.subtitle) ?? "";
        const addressJa = asNonBlankString(shopDetail.address) ?? "";
        const translatedName = await translateJaToKo(shopNameJa, translationCache, summary);
        const translatedDesc = await translateJaToKo(shopDescJa, translationCache, summary);
        const translatedAddress = await translateJaToKo(addressJa, translationCache, summary);
        const city = mapJpCity(shopDetail, source.defaultCity);
        const lat = toFiniteNumber(shopDetail.lat) ?? 0;
        const lng = toFiniteNumber(shopDetail.lng) ?? 0;
        const galleryImages = uniqueNonBlankStrings([
          normalizeImageUrl(shopDetail.background_filename),
          ...(Array.isArray(shopDetail.shop_covers) ?
            shopDetail.shop_covers
              .map((coverRaw) => asPlainObject(coverRaw))
              .map((cover) => normalizeImageUrl(cover?.upload_filename)) :
            []),
          ...(Array.isArray(shopDetail.shop_gallaries) ?
            shopDetail.shop_gallaries
              .map((galleryRaw) => asPlainObject(galleryRaw))
              .map((gallery) => normalizeImageUrl(gallery?.upload_filename)) :
            []),
        ]);
        const socialMedia: Record<string, string> = {};
        const twitterId = extractSocialAccountId(shopDetail.twitter, "twitter");
        const instagramId = extractSocialAccountId(shopDetail.instagram, "instagram");
        const tiktokId = extractSocialAccountId(shopDetail.tiktok, "tiktok");
        const youtubeId = extractSocialAccountId(shopDetail.youtube, "youtube");
        if (twitterId != null) socialMedia.twitter = twitterId;
        if (instagramId != null) socialMedia.instagram = instagramId;
        if (tiktokId != null) socialMedia.tiktok = tiktokId;
        if (youtubeId != null) socialMedia.youtube = youtubeId;
        const reservationUrl = asNonBlankString(shopDetail.reservation);
        const sourcePayload = {
          shopId: shopId,
          updatedAt: asNonBlankString(shopDetail.updated_at),
          nameJa: shopNameJa,
          descJa: shopDescJa,
          addressJa: addressJa,
        };
        const sourceHash = toSourceHash(sourcePayload);
        const cafeDoc: Record<string, unknown> = {
          name: translatedName.text,
          desc: translatedDesc.text,
          region: {
            country: "JP",
            city: city,
            address: translatedAddress.text,
            location: new GeoPoint(lat, lng),
          },
          thumbnailImage: normalizeImageUrl(shopDetail.logo_filename),
          galleryImages: galleryImages,
          approved: true,
          ratingAvg: 0,
          reviewCount: 0,
          conceptType: inferConceptType(shopDetail),
          ownerIds: [],
          socialMedia: socialMedia,
          businessHours: buildBusinessHours(shopDetail.new_business_hours, shopDetail.business_hours),
          source: {
            provider: "con-cafe.jp",
            ...sourcePayload,
          },
          sourceHash: sourceHash,
          translationStatus: translatedName.status === "FAILED" || translatedDesc.status === "FAILED" || translatedAddress.status === "FAILED" ?
            "FAILED_FALLBACK" :
            "OK",
          translatedAt: new Date().toISOString(),
        };
        if (reservationUrl != null) {
          cafeDoc.reservationUrl = reservationUrl;
        }

        const cafeRef = firestore.collection("cafes").doc(cafeId);
        const existingCafeSnapshot = await cafeRef.get();
        const existingCafeSourceHash = asNonBlankString(existingCafeSnapshot.get("sourceHash"));
        if (existingCafeSourceHash === sourceHash) {
          summary.cafesSkipped += 1;
        } else if (!options.dryRun) {
          await cafeRef.set(cafeDoc, {merge: true});
          summary.cafesUpserted += 1;
        } else {
          summary.cafesUpserted += 1;
        }

        for (const castItem of validCastItems) {
          const castIdNumber = toInt(castItem?.id);
          if (castIdNumber == null || castIdNumber <= 0) {
            continue;
          }
          let castDetail = castItem;
          try {
            const castPayload = await fetchJsonWithRetry(`https://con-cafe.jp/api/cast/${castIdNumber}`, 3);
            const castFetched = asPlainObject(castPayload);
            if (castFetched != null) {
              castDetail = castFetched;
              summary.castDetailsFetched += 1;
            }
          } catch (error) {
            summary.apiErrors += 1;
            logger.warn("runJpCrawledDataSync cast detail fetch failed.", {
              source: source.key,
              shopId: shopId,
              castId: castIdNumber,
              error: error instanceof Error ? error.message : "unknown",
            });
          }
          if (castDetail == null) {
            continue;
          }
          const castNameJa = asNonBlankString(castDetail.name) ?? "";
          // 이름이 없는 캐스트는 저장 건너뜀 (detail API가 비활성 캐스트를 반환한 경우)
          if (castNameJa === "") {
            logger.info("runJpCrawledDataSync skipped cast with no name.", {
              source: source.key,
              shopId: shopId,
              castId: castIdNumber,
            });
            continue;
          }
          const castId = `jp_cast_${castIdNumber}`;
          const castDescJa = asNonBlankString(castDetail.comment) ?? "";
          const translatedCastName = await translateJaToKo(castNameJa, translationCache, summary);
          const translatedCastDesc = await translateJaToKo(castDescJa, translationCache, summary);
          const resolvedCastDesc = asNonBlankString(translatedCastDesc.text) ?? "캐스트 소개가 비어있습니다.";
          const birthday = toBirthdayIso(castDetail.birth_month, castDetail.birth_day);
          const castSourcePayload = {
            castId: castIdNumber,
            updatedAt: asNonBlankString(castDetail.updated_at),
            nameJa: castNameJa,
            descJa: castDescJa,
          };
          const castSourceHash = toSourceHash(castSourcePayload);
          const castGalleryImages = uniqueNonBlankStrings([
            ...(Array.isArray(castDetail.cast_gallaries) ?
              castDetail.cast_gallaries
                .map((galleryRaw) => asPlainObject(galleryRaw))
                .map((gallery) => normalizeImageUrl(gallery?.upload_filename)) :
              []),
          ]);
          const castDoc: Record<string, unknown> = {
            name: translatedCastName.text,
            linkedUserId: null,
            profileImage: normalizeImageUrl(castDetail.profile_filename),
            desc: resolvedCastDesc,
            birthday: birthday,
            birthdayKey: toBirthdayKey(birthday),
            conceptRole: "maid",
            followerCount: 0,
            rating: 0,
            visitCertificationCount: 0,
            galleryImages: castGalleryImages,
            source: {
              provider: "con-cafe.jp",
              ...castSourcePayload,
            },
            sourceHash: castSourceHash,
            translationStatus: translatedCastName.status === "FAILED" || translatedCastDesc.status === "FAILED" ?
              "FAILED_FALLBACK" :
              "OK",
            translatedAt: new Date().toISOString(),
          };

          const castRef = firestore.collection("cafes").doc(cafeId).collection("casts").doc(castId);
          const existingCastSnapshot = await castRef.get();
          const existingCastSourceHash = asNonBlankString(existingCastSnapshot.get("sourceHash"));
          if (existingCastSourceHash === castSourceHash) {
            summary.castsSkipped += 1;
          } else if (!options.dryRun) {
            await castRef.set(castDoc, {merge: true});
            summary.castsUpserted += 1;
          } else {
            summary.castsUpserted += 1;
          }
          await sleep(20);
        }

        await sleep(30);
      }
      if (shops.length < 20) {
        break;
      }
      page += 1;
    }
    if (seenShopIds.size >= options.maxShops) {
      break;
    }
  }

  // Cleanup: casts 서브컬렉션이 비어있는 JP 카페 삭제 (이전 크롤에서 저장된 캐스트 없는 카페 제거)
  {
    const jpCafesSnapshot = await firestore
      .collection("cafes")
      .where("source.provider", "==", "con-cafe.jp")
      .get();

    for (const cafeDoc of jpCafesSnapshot.docs) {
      const castsSnapshot = await cafeDoc.ref.collection("casts").limit(1).get();
      if (castsSnapshot.empty) {
        if (!options.dryRun) {
          await cafeDoc.ref.delete();
        }
        summary.cafesDeleted += 1;
        logger.info("runJpCrawledDataSync deleted JP cafe with no casts.", {
          cafeId: cafeDoc.id,
          dryRun: options.dryRun,
        });
      }
    }
  }

  return summary;
}

export const syncJpCrawledConCafeData = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 540,
    memory: "1GiB",
    secrets: [GOOGLE_TRANSLATE_API_KEY_SECRET],
  },
  async (request, response) => {
    response.set("Access-Control-Allow-Origin", "*");
    response.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    response.set("Access-Control-Allow-Headers", "Content-Type, X-Seed-Token");

    if (request.method === "OPTIONS") {
      response.status(204).send("");
      return;
    } else if (request.method !== "POST") {
      response.status(405).json({
        error: "method_not_allowed",
      });
      return;
    }

    const expectedToken = process.env.JP_CRAWL_TOKEN?.trim() ?? process.env.MOCK_SEED_TOKEN?.trim();
    const providedToken = request.get("x-seed-token")?.trim();
    if (expectedToken != null && expectedToken.length > 0 && providedToken !== expectedToken) {
      response.status(401).json({
        error: "unauthorized",
      });
      return;
    }

    const dryRun = toBooleanQuery(request.query.dryRun);
    const sourceKeys = parseSourceKeys(request.query.source ?? request.query.region);
    const maxPages = parsePositiveIntQuery(request.query.maxPages, 500, 500);
    const maxShops = parsePositiveIntQuery(request.query.maxShops, 5000, 10000);
    try {
      const summary = await runJpCrawledDataSync({
        dryRun: dryRun,
        sourceKeys: sourceKeys,
        maxPages: maxPages,
        maxShops: maxShops,
      });
      logger.info("syncJpCrawledConCafeData completed.", summary);
      response.status(200).json({
        ok: true,
        summary: summary,
      });
    } catch (error) {
      logger.error("syncJpCrawledConCafeData failed.", error);
      response.status(500).json({
        error: "internal",
      });
    }
  }
);

export const onScheduleSyncJpCrawledConCafeData = onSchedule(
  {
    schedule: "0 4 1 */2 *",
    timeZone: "Asia/Seoul",
    timeoutSeconds: 540,
    memory: "1GiB",
    secrets: [GOOGLE_TRANSLATE_API_KEY_SECRET],
  },
  async () => {
    const summary = await runJpCrawledDataSync({
      dryRun: false,
      sourceKeys: new Set(["tokyo"]),
      maxPages: 500,
      maxShops: 10000,
    });
    logger.info("onScheduleSyncJpCrawledConCafeData completed (tokyo).", summary);
  }
);

export const onScheduleSyncJpCrawledConCafeDataOsaka = onSchedule(
  {
    schedule: "0 4 15 */2 *",
    timeZone: "Asia/Seoul",
    timeoutSeconds: 540,
    memory: "1GiB",
    secrets: [GOOGLE_TRANSLATE_API_KEY_SECRET],
  },
  async () => {
    const summary = await runJpCrawledDataSync({
      dryRun: false,
      sourceKeys: new Set(["osaka"]),
      maxPages: 500,
      maxShops: 10000,
    });
    logger.info("onScheduleSyncJpCrawledConCafeDataOsaka completed (osaka).", summary);
  }
);

export const onScheduleSyncJpCrawledConCafeDataYokohama = onSchedule(
  {
    schedule: "0 4 22 */2 *",
    timeZone: "Asia/Seoul",
    timeoutSeconds: 540,
    memory: "1GiB",
    secrets: [GOOGLE_TRANSLATE_API_KEY_SECRET],
  },
  async () => {
    const summary = await runJpCrawledDataSync({
      dryRun: false,
      sourceKeys: new Set(["yokohama"]),
      maxPages: 500,
      maxShops: 10000,
    });
    logger.info("onScheduleSyncJpCrawledConCafeDataYokohama completed (yokohama).", summary);
  }
);

export const seedMockConCafeData = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 540,
    memory: "1GiB",
  },
  async (request, response) => {
    response.set("Access-Control-Allow-Origin", "*");
    response.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    response.set("Access-Control-Allow-Headers", "Content-Type, X-Seed-Token");

    if (request.method === "OPTIONS") {
      response.status(204).send("");
      return;
    } else if (request.method !== "POST") {
      response.status(405).json({
        error: "method_not_allowed",
      });
      return;
    }

    const expectedSeedToken = process.env.MOCK_SEED_TOKEN?.trim();
    const providedSeedToken = request.get("x-seed-token")?.trim();
    if (expectedSeedToken != null && expectedSeedToken.length > 0 && providedSeedToken !== expectedSeedToken) {
      response.status(401).json({
        error: "unauthorized",
      });
      return;
    }

    const config = resolveSeedConfig(request);
    const targetCafes = MOCK_CAFE_PRESETS.slice(0, config.cafeCount);
    const firestore = db();
    const now = Date.now();
    const categoryCycle = ["drink", "dessert", "goods"];
    const createdUserIds = new Set<string>();
    let batch = firestore.batch();
    let pendingWriteCount = 0;
    let committedBatchCount = 0;
    const summary = {
      cafes: 0,
      casts: 0,
      menus: 0,
      notices: 0,
      reviews: 0,
      users: 0,
      visits: 0,
      homeBanners: 0,
      committedBatches: 0,
    };

    const setWithMerge = async (
      pathSegments: string[],
      data: Record<string, unknown>
    ): Promise<void> => {
      let docRef = firestore.collection(pathSegments[0]).doc(pathSegments[1]);
      for (let i = 2; i < pathSegments.length; i += 2) {
        docRef = docRef.collection(pathSegments[i]).doc(pathSegments[i + 1]);
      }
      batch.set(docRef, data, {merge: true});
      pendingWriteCount += 1;
      if (pendingWriteCount >= 450) {
        await batch.commit();
        committedBatchCount += 1;
        batch = firestore.batch();
        pendingWriteCount = 0;
      }
    };

    try {
      for (let cafeIndex = 0; cafeIndex < targetCafes.length; cafeIndex += 1) {
        const cafePreset = targetCafes[cafeIndex];
        let cafeRatingTotal = 0;
        const cafeReviewCount = config.reviewsPerCafe;
        const cafeCastFollowerTotals = new Map<string, number>();
        const cafeCastVisitTotals = new Map<string, number>();

        for (let reviewIndex = 1; reviewIndex <= config.reviewsPerCafe; reviewIndex += 1) {
          const reviewId = `mock_review_${cafePreset.id}_${pad3(reviewIndex)}`;
          const visitorSlot = seededInt((cafeIndex + 1) * 10000 + reviewIndex) % 90;
          const reviewUserId = `mock_user_${pad3(visitorSlot + 1)}`;
          const castTagIndexA = ((reviewIndex - 1) % config.castsPerCafe) + 1;
          const castTagIndexB = ((reviewIndex + 7) % config.castsPerCafe) + 1;
          const taggedCastIds = castTagIndexA === castTagIndexB ?
            [`mock_cast_${cafePreset.id}_${pad3(castTagIndexA)}`] :
            [
              `mock_cast_${cafePreset.id}_${pad3(castTagIndexA)}`,
              `mock_cast_${cafePreset.id}_${pad3(castTagIndexB)}`,
            ];
          const ratingPattern = [3.0, 3.5, 4.0, 4.5, 5.0, 4.0, 4.5, 5.0];
          const ratingValue = ratingPattern[reviewIndex % ratingPattern.length];
          const dayOffset = seededInt(reviewIndex * 17 + cafeIndex * 71) % 120;
          const hourOffset = seededInt(reviewIndex * 23 + cafeIndex * 19) % 14;
          const createdAtMillis = now - (((dayOffset * 24) + hourOffset) * 60 * 60 * 1000);
          const createdAt = new Date(createdAtMillis).toISOString();
          const visitId = `mock_visit_${cafePreset.id}_${pad3(reviewIndex)}`;
          const reviewImageUrl = reviewIndex % 4 === 0 ?
            `https://picsum.photos/seed/${reviewId}/720/720` :
            null;
          const reviewContent = buildMockReviewContent(cafePreset.name, reviewIndex);
          const userNicknameBase = pickSeeded(MOCK_USER_NICKNAME_POOL, visitorSlot + reviewIndex);
          const userNickname = `${userNicknameBase}${(visitorSlot % 30) + 1}번님`;
          const verificationDistanceMeters = (seededInt(reviewIndex * 97) % 120) + 15;

          cafeRatingTotal += ratingValue;
          taggedCastIds.forEach((castId) => {
            const followerCount = (cafeCastFollowerTotals.get(castId) ?? 0) + 1;
            const visitCount = (cafeCastVisitTotals.get(castId) ?? 0) + 1;
            cafeCastFollowerTotals.set(castId, followerCount);
            cafeCastVisitTotals.set(castId, visitCount);
          });

          if (!createdUserIds.has(reviewUserId)) {
            await setWithMerge(
              ["users", reviewUserId],
              {
                email: `${reviewUserId}@mock.concafe.app`,
                nickname: userNickname,
                role: "VISITOR",
                banned: false,
                createdAt: createdAt,
                profileImage: `https://picsum.photos/seed/${reviewUserId}/256/256`,
              },
            );
            createdUserIds.add(reviewUserId);
            summary.users += 1;
          }

          await setWithMerge(
            ["reviews", reviewId],
            {
              userId: reviewUserId,
              userNickname: userNickname,
              cafeId: cafePreset.id,
              visitId: visitId,
              rating: ratingValue,
              content: reviewContent,
              imageUrls: reviewImageUrl == null ? [] : [reviewImageUrl],
              taggedCastIds: taggedCastIds,
              likeCount: seededInt(reviewIndex * 41 + cafeIndex * 13) % 80,
              createdAt: createdAt,
              visitVerified: true,
            },
          );
          summary.reviews += 1;

          await setWithMerge(
            ["visits", visitId],
            {
              userId: reviewUserId,
              cafeId: cafePreset.id,
              visitedAt: createdAt,
              memo: pickSeeded(MOCK_MEMO_POOL, reviewIndex * 29),
              verified: true,
              verifiedAt: createdAt,
              verificationDistanceMeters: verificationDistanceMeters,
            },
          );
          summary.visits += 1;
        }

        const ratingAvg = Number((cafeRatingTotal / cafeReviewCount).toFixed(2));
        await setWithMerge(
          ["cafes", cafePreset.id],
          {
            name: cafePreset.name,
            desc: cafePreset.desc,
            region: {
              country: cafePreset.country,
              city: cafePreset.city,
              address: cafePreset.address,
              location: {
                latitude: cafePreset.latitude,
                longitude: cafePreset.longitude,
              },
            },
            thumbnailImage: cafePreset.thumbnailImage,
            ratingAvg: ratingAvg,
            reviewCount: cafeReviewCount,
            approved: true,
            conceptType: "MAID",
          },
        );
        summary.cafes += 1;

        for (let castIndex = 1; castIndex <= config.castsPerCafe; castIndex += 1) {
          const castId = `mock_cast_${cafePreset.id}_${pad3(castIndex)}`;
          const profileSeed = `${castId}_${cafePreset.city}`;
          const followerBase = (seededInt(castIndex * 37 + cafeIndex * 61) % 220) + 40;
          const followerBoost = cafeCastFollowerTotals.get(castId) ?? 0;
          const visitCertificationCount = (cafeCastVisitTotals.get(castId) ?? 0) + (castIndex % 7);
          const castRating = Number((3.4 + ((castIndex % 8) * 0.2)).toFixed(2));

          await setWithMerge(
            ["cafes", cafePreset.id, "casts", castId],
            {
              name: buildMockCastDisplayName(cafeIndex + 1, castIndex),
              linkedUserId: null,
              profileImage: `https://picsum.photos/seed/${profileSeed}/512/512`,
              desc: `${cafePreset.city} 지점에서 근무 중이며 이벤트 진행 경험이 많은 캐스트입니다.`,
              birthday: `199${castIndex % 10}-${`${(castIndex % 12) + 1}`.padStart(2, "0")}-${`${(castIndex % 27) + 1}`.padStart(2, "0")}`,
              conceptRole: "maid",
              followerCount: followerBase + followerBoost,
              rating: castRating,
              visitCertificationCount: visitCertificationCount,
            },
          );
          summary.casts += 1;
        }

        for (let menuIndex = 1; menuIndex <= config.menusPerCafe; menuIndex += 1) {
          const menuId = `mock_menu_${cafePreset.id}_${pad3(menuIndex)}`;
          const category = categoryCycle[(menuIndex - 1) % categoryCycle.length];
          const categoryTemplates = MOCK_MENU_TEMPLATES[category] ?? MOCK_MENU_TEMPLATES.drink;
          const menuName = categoryTemplates[(menuIndex - 1) % categoryTemplates.length];
          const menuPriceBase = category === "goods" ? 9800 : 5200;
          const menuPriceStep = category === "goods" ? 700 : 350;

          await setWithMerge(
            ["cafes", cafePreset.id, "menus", menuId],
            {
              name: menuName,
              price: menuPriceBase + (menuIndex * menuPriceStep),
              desc: `${cafePreset.city} 지점에서 판매 중인 ${category} 카테고리 메뉴입니다.`,
              image: `https://picsum.photos/seed/${menuId}/640/480`,
              category: category,
              isAvailable: menuIndex % 11 !== 0,
            },
          );
          summary.menus += 1;
        }

        for (let noticeIndex = 1; noticeIndex <= config.noticesPerCafe; noticeIndex += 1) {
          const noticeId = `mock_notice_${cafePreset.id}_${pad3(noticeIndex)}`;
          const createdAt = new Date(now - (noticeIndex * 24 * 60 * 60 * 1000)).toISOString();
          await setWithMerge(
            ["cafes", cafePreset.id, "notices", noticeId],
            {
              title: buildMockNoticeTitle(cafePreset.name, noticeIndex),
              content: buildMockNoticeContent(cafePreset.name, noticeIndex),
              createdAt: createdAt,
            },
          );
          summary.notices += 1;
        }
      }

      for (let bannerIndex = 0; bannerIndex < MOCK_HOME_BANNERS.length; bannerIndex += 1) {
        const banner = MOCK_HOME_BANNERS[bannerIndex];
        const activatedAtEpochMillis = now - (bannerIndex * 6 * 60 * 60 * 1000);
        await setWithMerge(
          ["homeBanners", banner.id],
          {
            title: banner.title,
            subtitle: banner.subtitle,
            startColorHex: banner.startColorHex,
            endColorHex: banner.endColorHex,
            relatedCafeId: banner.linkTargetCafeId,
            imageUrl: banner.imageUrl,
            linkType: banner.linkType,
            linkTarget: banner.linkTargetCafeId,
            displayDays: banner.displayDays,
            status: "ACTIVE",
            createdAtEpochMillis: activatedAtEpochMillis,
            activatedAtEpochMillis: activatedAtEpochMillis,
          },
        );
        summary.homeBanners += 1;
      }

      if (pendingWriteCount > 0) {
        await batch.commit();
        committedBatchCount += 1;
      }
      summary.committedBatches = committedBatchCount;
      logger.info("seedMockConCafeData completed.", {
        config: config,
        summary: summary,
      });
      response.status(200).json({
        ok: true,
        config: config,
        summary: summary,
      });
    } catch (error) {
      logger.error("seedMockConCafeData failed.", error);
      response.status(500).json({
        error: "internal",
      });
    }
  }
);

export const clearMockConCafeData = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 540,
    memory: "1GiB",
  },
  async (request, response) => {
    response.set("Access-Control-Allow-Origin", "*");
    response.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    response.set("Access-Control-Allow-Headers", "Content-Type, X-Seed-Token");

    if (request.method === "OPTIONS") {
      response.status(204).send("");
      return;
    } else if (request.method !== "POST") {
      response.status(405).json({
        error: "method_not_allowed",
      });
      return;
    }

    const expectedSeedToken = process.env.MOCK_SEED_TOKEN?.trim();
    const providedSeedToken = request.get("x-seed-token")?.trim();

    if (expectedSeedToken != null && expectedSeedToken.length > 0 && providedSeedToken !== expectedSeedToken) {
      response.status(401).json({
        error: "unauthorized",
      });
      return;
    }

    const firestore = db();
    const summary = {
      cafes: 0,
      users: 0,
      reviews: 0,
      visits: 0,
      homeBanners: 0,
    };

    try {
      const mockCafeSnapshot = await firestore.collection("cafes")
        .where(FieldPath.documentId(), ">=", "mock_cafe_")
        .where(FieldPath.documentId(), "<=", "mock_cafe_\uf8ff")
        .get();
      const mockCafes = mockCafeSnapshot.docs;

      for (let i = 0; i < mockCafes.length; i += 1) {
        await firestore.recursiveDelete(mockCafes[i].ref);
        summary.cafes += 1;
      }

      const mockUserSnapshot = await firestore.collection("users")
        .where(FieldPath.documentId(), ">=", "mock_user_")
        .where(FieldPath.documentId(), "<=", "mock_user_\uf8ff")
        .get();
      const mockUsers = mockUserSnapshot.docs;

      for (let i = 0; i < mockUsers.length; i += 1) {
        await firestore.recursiveDelete(mockUsers[i].ref);
        summary.users += 1;
      }

      summary.reviews = await deleteDocumentsWithPrefix("reviews", "mock_review_");
      summary.visits = await deleteDocumentsWithPrefix("visits", "mock_visit_");
      summary.homeBanners = await deleteDocumentsWithPrefix("homeBanners", "mock_home_banner_");

      logger.info("clearMockConCafeData completed.", {
        summary: summary,
      });
      response.status(200).json({
        ok: true,
        summary: summary,
      });
    } catch (error) {
      logger.error("clearMockConCafeData failed.", error);
      response.status(500).json({
        error: "internal",
      });
    }
  }
);

export const clearJpCrawledConCafeData = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 540,
    memory: "1GiB",
  },
  async (request, response) => {
    response.set("Access-Control-Allow-Origin", "*");
    response.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    response.set("Access-Control-Allow-Headers", "Content-Type, X-Seed-Token");

    if (request.method === "OPTIONS") {
      response.status(204).send("");
      return;
    } else if (request.method !== "POST") {
      response.status(405).json({
        error: "method_not_allowed",
      });
      return;
    }

    const expectedClearToken = process.env.CRAWLED_DATA_CLEAR_TOKEN?.trim()
      ?? process.env.MOCK_SEED_TOKEN?.trim();
    const providedSeedToken = request.get("x-seed-token")?.trim();

    if (expectedClearToken != null && expectedClearToken.length > 0 && providedSeedToken !== expectedClearToken) {
      response.status(401).json({
        error: "unauthorized",
      });
      return;
    }

    const firestore = db();
    const summary = {
      cafes: 0,
      castDirectory: 0,
    };

    try {
      const jpCafeSnapshot = await firestore.collection("cafes")
        .where(FieldPath.documentId(), ">=", "jp_shop_")
        .where(FieldPath.documentId(), "<=", "jp_shop_\uf8ff")
        .get();
      const jpCafes = jpCafeSnapshot.docs;

      for (let i = 0; i < jpCafes.length; i += 1) {
        await firestore.recursiveDelete(jpCafes[i].ref);
        summary.cafes += 1;
      }

      summary.castDirectory = await deleteDocumentsWithPrefix("castDirectory", "jp_cast_");

      logger.info("clearJpCrawledConCafeData completed.", {
        summary: summary,
      });
      response.status(200).json({
        ok: true,
        summary: summary,
      });
    } catch (error) {
      logger.error("clearJpCrawledConCafeData failed.", error);
      response.status(500).json({
        error: "internal",
      });
    }
  }
);

export const onUserDeletedCleanupOwnership =
  functionsV1.auth.user().onDelete(async (user: UserRecord) => {
    const userId = user.uid;

    if (!userId) return;
    const firestore = db();
    const markerRef = firestore.collection(USER_DELETION_REQUESTS).doc(userId);
    const cleanupSummary = emptyDeletedUserCleanupSummary();
    try {
      Object.assign(cleanupSummary, await cleanupDeletedUserData(firestore, userId));
    } catch (error) {
      cleanupSummary.unexpectedError = asErrorMessage(error, "cleanupDeletedUserData failed unexpectedly");
      logger.error("onUserDeletedCleanupOwnership failed unexpectedly.", {
        userId: userId,
        error: cleanupSummary.unexpectedError,
      });
    } finally {
      await markerRef.delete().catch((error: unknown) => {
        logger.error("onUserDeletedCleanupOwnership marker delete failed.", {
          userId: userId,
          error: asErrorMessage(error, "marker delete failed"),
        });
      });
    }
    logger.info("onUserDeletedCleanupOwnership completed.", {
      userId: userId,
      ...cleanupSummary,
    });
  });

export const deleteCurrentUserCascade = functionsV1.https.onRequest(async (request, response) => {
  if (request.method !== "POST") {
    response.status(405).json({error: "method_not_allowed"});
    return;
  }

  const authorization = request.header("Authorization") ?? request.header("authorization") ?? "";
  const idToken = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : "";

  if (!idToken) {
    response.status(401).json({error: "missing_auth"});
    return;
  }

  let uid = "";
  try {
    const decodedToken = await getAuth().verifyIdToken(idToken);
    uid = decodedToken.uid;
  } catch (error) {
    logger.warn("deleteCurrentUserCascade verifyIdToken failed.", error);
    response.status(401).json({error: "invalid_auth"});
    return;
  }

  try {
    await db().collection(USER_DELETION_REQUESTS).doc(uid).set({
      userId: uid,
      requestedAt: new Date().toISOString(),
      source: "deleteCurrentUserCascade",
    });
    await getAuth().deleteUser(uid);
    logger.info("deleteCurrentUserCascade auth deletion requested.", {
      userId: uid,
    });
    response.status(200).json({ok: true});
  } catch (error) {
    logger.error("deleteCurrentUserCascade failed.", error);
    response.status(500).json({error: "internal"});
  }
});

async function requireAdminUserIdFromRequest(request: functionsV1.https.Request): Promise<string> {
  const authorization = request.header("Authorization") ?? request.header("authorization") ?? "";
  const idToken = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : "";

  if (!idToken) {
    throw new Error("missing_auth");
  }

  const decodedToken = await getAuth().verifyIdToken(idToken);
  const userSnapshot = await db().collection("users").doc(decodedToken.uid).get();
  const role = asNonBlankString(userSnapshot.data()?.role);

  if (role !== "ADMIN") {
    throw new Error("forbidden");
  }

  return decodedToken.uid;
}

export const normalizeCastLinkedUserFields = functionsV1.https.onRequest(async (request, response) => {
  if (request.method !== "POST") {
    response.status(405).json({error: "method_not_allowed"});
    return;
  }

  try {
    const adminUserId = await requireAdminUserIdFromRequest(request);
    const firestore = db();
    const snapshot = await firestore.collectionGroup("casts").get();
    const BATCH_SIZE = 250;
    const batchOperations: Array<{
      ref: FirebaseFirestore.DocumentReference;
      cafeId: string;
      linkedUserId: string;
      shouldNormalizeCastFields: boolean;
    }> = [];
    let normalizedCount = 0;
    let affiliatedUserCount = 0;
    const conflicts: string[] = [];
    const operationKeys = new Set<string>();
    const addBatchOperation = (
      ref: FirebaseFirestore.DocumentReference,
      cafeId: string,
      linkedUserId: string,
      shouldNormalizeCastFields: boolean
    ) => {
      const operationKey = `${ref.path}::${linkedUserId}`;
      if (operationKeys.has(operationKey)) {
        return;
      }
      operationKeys.add(operationKey);
      batchOperations.push({
        ref: ref,
        cafeId: cafeId,
        linkedUserId: linkedUserId,
        shouldNormalizeCastFields: shouldNormalizeCastFields,
      });
    };

    for (const doc of snapshot.docs) {
      const data = doc.data();
      const cafeId = doc.ref.parent.parent?.id ?? "";
      const linkedUserId = asNonBlankString(data?.linkedUserId);
      const legacyUserId = asNonBlankString(data?.userId);
      const legacyUid = asNonBlankString(data?.uid);
      const legacyCandidates = [legacyUserId, legacyUid].filter((value): value is string => value != null);
      const uniqueLegacyCandidates = [...new Set(legacyCandidates)];
      const normalizedLinkedUserId = linkedUserId ?? uniqueLegacyCandidates[0] ?? null;

      if (!cafeId || normalizedLinkedUserId == null) {
        if (linkedUserId == null && uniqueLegacyCandidates.length === 0) {
          continue;
        }
        conflicts.push(doc.ref.path);
        logger.error("normalizeCastLinkedUserFields invalid cast path or linked user.", {
          castPath: doc.ref.path,
          cafeId: cafeId,
          linkedUserId: linkedUserId,
          legacyUserId: legacyUserId,
          legacyUid: legacyUid,
        });
        continue;
      }

      if (linkedUserId == null && uniqueLegacyCandidates.length === 0) {
        continue;
      } else if (linkedUserId != null && uniqueLegacyCandidates.length === 0) {
        addBatchOperation(doc.ref, cafeId, linkedUserId, false);
      } else if (linkedUserId == null && uniqueLegacyCandidates.length === 1) {
        addBatchOperation(doc.ref, cafeId, normalizedLinkedUserId, true);
      } else if (linkedUserId != null && uniqueLegacyCandidates.every((value) => value === linkedUserId)) {
        addBatchOperation(doc.ref, cafeId, linkedUserId, true);
      } else {
        conflicts.push(doc.ref.path);
        logger.error("normalizeCastLinkedUserFields conflict detected.", {
          castPath: doc.ref.path,
          linkedUserId: linkedUserId,
          legacyUserId: legacyUserId,
          legacyUid: legacyUid,
        });
      }
    }

    const approvedClaimSnapshot = await firestore
      .collection("castClaims")
      .where("status", "in", ["APPROVED", "승인 완료"])
      .get();

    for (const claimDoc of approvedClaimSnapshot.docs) {
      const claimData = claimDoc.data() as CastClaimLike;
      const claimCafeId = asNonBlankString(claimData.cafeId);
      const claimCastId = asNonBlankString(claimData.castId);
      const claimUserId = asNonBlankString(claimData.userId);

      if (claimCafeId == null || claimCastId == null || claimUserId == null) {
        continue;
      }

      const castRef = firestore.collection("cafes").doc(claimCafeId).collection("casts").doc(claimCastId);
      const castSnapshot = await castRef.get();
      const currentLinkedUserId = asNonBlankString(castSnapshot.data()?.linkedUserId);

      if (!castSnapshot.exists) {
        conflicts.push(castRef.path);
        logger.error("normalizeCastLinkedUserFields approved claim cast not found.", {
          claimId: claimDoc.id,
          cafeId: claimCafeId,
          castId: claimCastId,
          userId: claimUserId,
        });
      } else if (currentLinkedUserId != null && currentLinkedUserId !== claimUserId) {
        conflicts.push(castRef.path);
        logger.error("normalizeCastLinkedUserFields approved claim conflict detected.", {
          claimId: claimDoc.id,
          cafeId: claimCafeId,
          castId: claimCastId,
          userId: claimUserId,
          currentLinkedUserId: currentLinkedUserId,
        });
      } else {
        addBatchOperation(castRef, claimCafeId, claimUserId, currentLinkedUserId == null);
      }
    }

    for (let i = 0; i < batchOperations.length; i += BATCH_SIZE) {
      const batch = firestore.batch();
      const chunk = batchOperations.slice(i, i + BATCH_SIZE);
      chunk.forEach((operation) => {
        if (operation.shouldNormalizeCastFields) {
          batch.set(operation.ref, {
            linkedUserId: operation.linkedUserId,
            userId: FieldValue.delete(),
            uid: FieldValue.delete(),
          }, {merge: true});
          normalizedCount += 1;
        }
        batch.set(firestore.collection("users").doc(operation.linkedUserId), {
          affiliatedCafeId: operation.cafeId,
        }, {merge: true});
        affiliatedUserCount += 1;
      });
      await batch.commit();
    }

    logger.info("normalizeCastLinkedUserFields completed.", {
      adminUserId: adminUserId,
      scannedCount: snapshot.docs.length,
      normalizedCount: normalizedCount,
      affiliatedUserCount: affiliatedUserCount,
      conflictCount: conflicts.length,
    });
    response.status(200).json({
      ok: true,
      scannedCount: snapshot.docs.length,
      normalizedCount: normalizedCount,
      affiliatedUserCount: affiliatedUserCount,
      conflictCount: conflicts.length,
      conflicts: conflicts,
    });
  } catch (error) {
    const errorMessage = asErrorMessage(error, "normalizeCastLinkedUserFields failed");
    const statusCode = errorMessage === "missing_auth" ? 401 : errorMessage === "forbidden" ? 403 : 500;
    logger.error("normalizeCastLinkedUserFields failed.", {
      error: errorMessage,
    });
    response.status(statusCode).json({error: errorMessage});
  }
});

export const rebuildCastDirectoryIndex = functionsV1
  .runWith({
    timeoutSeconds: 540,
    memory: "1GB",
  })
  .https
  .onRequest(async (request, response) => {
    if (request.method !== "POST") {
      response.status(405).json({error: "method_not_allowed"});
      return;
    }
    try {
      const adminUserId = await requireAdminUserIdFromRequest(request);
      const firestore = db();
      const snapshot = await firestore.collectionGroup("casts").get();
      const BATCH_SIZE = 400;
      let syncedCount = 0;
      let skippedCount = 0;

      for (let i = 0; i < snapshot.docs.length; i += BATCH_SIZE) {
        const batch = firestore.batch();
        const chunk = snapshot.docs.slice(i, i + BATCH_SIZE);

        chunk.forEach((castDoc) => {
          const castId = castDoc.id;
          const cafeId = castDoc.ref.parent.parent?.id ?? "";

          if (castId.length == 0 || cafeId.length == 0) {
            skippedCount += 1;
            return;
          }
          const directoryRef = firestore.collection("castDirectory").doc(castId);

          batch.set(directoryRef, {
            castId: castId,
            cafeId: cafeId,
            updatedAt: new Date().toISOString(),
          }, {merge: true});
          syncedCount += 1;
        });
        await batch.commit();
      }

      logger.info("rebuildCastDirectoryIndex completed.", {
        adminUserId: adminUserId,
        scannedCount: snapshot.docs.length,
        syncedCount: syncedCount,
        skippedCount: skippedCount,
      });
      response.status(200).json({
        ok: true,
        scannedCount: snapshot.docs.length,
        syncedCount: syncedCount,
        skippedCount: skippedCount,
      });
    } catch (error) {
      const errorMessage = asErrorMessage(error, "rebuildCastDirectoryIndex failed");
      const statusCode = errorMessage === "missing_auth" ? 401 : errorMessage === "forbidden" ? 403 : 500;

      logger.error("rebuildCastDirectoryIndex failed.", {
        error: errorMessage,
      });
      response.status(statusCode).json({error: errorMessage});
    }
  });

const CLAIM_APPROVED_TTL_MS = 10 * 60 * 1000; // 10분
const CLAIM_REJECTED_TTL_MS = 60 * 60 * 1000; // 1시간

function resolveClaimExpiresAt(status: string | null): Date | null {
  const now = new Date();
  if (isApprovedStatus(status)) {
    return new Date(now.getTime() + CLAIM_APPROVED_TTL_MS);
  } else if (isRejectedStatus(status)) {
    return new Date(now.getTime() + CLAIM_REJECTED_TTL_MS);
  }
  return null;
}

function isExpiredReviewedClaim(data: Record<string, unknown> | undefined, now: Date): boolean {
  const status = asNonBlankString(data?.status);
  const expiresAt = asDate(data?.expiresAt);

  if (expiresAt != null) {
    return expiresAt.getTime() <= now.getTime();
  }

  const reviewedAt = asDate(data?.reviewedAt);
  if (reviewedAt == null) {
    return false;
  }
  if (isApprovedStatus(status)) {
    return reviewedAt.getTime() + CLAIM_APPROVED_TTL_MS <= now.getTime();
  }
  if (isRejectedStatus(status)) {
    return reviewedAt.getTime() + CLAIM_REJECTED_TTL_MS <= now.getTime();
  }
  return false;
}

export const onCastClaimWrittenSetExpiry = onDocumentWritten(
  "castClaims/{claimId}",
  async (event) => {
    const beforeData = event.data?.before.data() as CastClaimLike | undefined;
    const afterData = event.data?.after.data() as CastClaimLike | undefined;

    if (!afterData) return;

    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (beforeStatus === afterStatus) return;

    const expiresAt = resolveClaimExpiresAt(afterStatus);

    if (expiresAt == null) return;

    await event.data!.after.ref.update({expiresAt: expiresAt});
  }
);

export const onCafeRegistrationClaimWrittenSetExpiry = onDocumentWritten(
  "cafeRegistrationClaims/{claimId}",
  async (event) => {
    const beforeData = event.data?.before.data() as CafeRegistrationClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeRegistrationClaimLike | undefined;

    if (!afterData) return;

    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (beforeStatus === afterStatus) return;

    const expiresAt = resolveClaimExpiresAt(afterStatus);

    if (expiresAt == null) return;

    await event.data!.after.ref.update({expiresAt: expiresAt});
  }
);

export const onCafeOwnerClaimWrittenSetExpiry = onDocumentWritten(
  "cafeOwnerClaims/{claimId}",
  async (event) => {
    const beforeData = event.data?.before.data() as CafeOwnerClaimLike | undefined;
    const afterData = event.data?.after.data() as CafeOwnerClaimLike | undefined;

    if (!afterData) return;

    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);

    if (beforeStatus === afterStatus) return;

    const expiresAt = resolveClaimExpiresAt(afterStatus);

    if (expiresAt == null) return;

    await event.data!.after.ref.update({expiresAt: expiresAt});
  }
);

export const onScheduleDeleteExpiredClaims = onSchedule(
  {
    schedule: "every 5 minutes",
    timeZone: "Asia/Seoul",
  },
  async () => {
    const firestore = db();
    const now = new Date();
    const BATCH_SIZE = 500;
    const claimCollections = ["castClaims", "cafeRegistrationClaims", "cafeOwnerClaims"];
    let totalDeleted = 0;

    for (const collectionName of claimCollections) {
      const snapshot = await firestore
        .collection(collectionName)
        .get();

      if (snapshot.empty) continue;

      const expiredDocs = snapshot.docs.filter((doc) =>
        isExpiredReviewedClaim(doc.data() as Record<string, unknown> | undefined, now)
      );

      if (expiredDocs.length === 0) continue;

      for (let i = 0; i < expiredDocs.length; i += BATCH_SIZE) {
        const batch = firestore.batch();
        const chunk = expiredDocs.slice(i, i + BATCH_SIZE);
        chunk.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
        totalDeleted += chunk.length;
      }
    }

    logger.info("onScheduleDeleteExpiredClaims completed.", {totalDeleted});
  }
);

async function syncTableCountUpdateNotifications(
  cafeId: string,
  beforeData: Record<string, unknown> | undefined,
  afterData: Record<string, unknown> | undefined
): Promise<void> {
  if (afterData == null || beforeData == null) {
    return;
  }
  const beforeCurrent = asNonNegativeInt((asPlainObject(beforeData.tableCounts) ?? {})["current"]);
  const afterCurrent = asNonNegativeInt((asPlainObject(afterData.tableCounts) ?? {})["current"]);

  if (beforeCurrent === afterCurrent) {
    return;
  }
  const cafeName = asNonBlankString(afterData.name) ?? "즐겨찾기 카페";
  const favorites = await db()
    .collection("cafeFavorites")
    .where("cafeId", "==", cafeId)
    .select("userId")
    .get();

  if (favorites.empty) {
    return;
  }
  const {targets: recipientUserIds, droppedByCap} = toNotificationRecipientUserIds(
    favorites.docs.map((favoriteDoc) => asNonBlankString(favoriteDoc.get("userId")))
  );

  if (recipientUserIds.length == 0) {
    return;
  }
  const createdAt = new Date().toISOString();
  const current = afterCurrent ?? 0;
  let sentCount = 0;
  let skippedBySettingsCount = 0;

  await processInBatches(recipientUserIds, async (userId) => {
    const settings = await loadUserNotificationSettings(userId);

    if (!settings.isPushNotificationsEnabled || !settings.isNoticeNotificationsEnabled) {
      skippedBySettingsCount += 1;
      return;
    }
    await createUserNotification(
      userId,
      `cafe_table_count_${cafeId}_${createdAt}_${userId}`,
      "CAFE_TABLE_COUNT_UPDATE",
      `${cafeName} 테이블 현황 업데이트`,
      `현재 이용 가능한 테이블: ${current}`,
      cafeId,
      createdAt,
      settings
    );
    sentCount += 1;
  });
  logger.info("Processed cafe table count update notification fanout.", {
    cafeId: cafeId,
    beforeCurrent: beforeCurrent,
    afterCurrent: afterCurrent,
    targetCount: recipientUserIds.length,
    sentCount: sentCount,
    skippedBySettingsCount: skippedBySettingsCount,
    droppedByCap: droppedByCap,
  });
}

export const onCafeWrittenSyncTableCountNotifications = onDocumentWritten(
  "cafes/{cafeId}",
  async (event) => {
    const cafeId = asNonBlankString(event.params.cafeId);

    if (cafeId == null) {
      return;
    }
    const beforeData = event.data?.before.data() as Record<string, unknown> | undefined;
    const afterData = event.data?.after.data() as Record<string, unknown> | undefined;

    await syncTableCountUpdateNotifications(cafeId, beforeData, afterData);
  }
);

export const onCommunityPostDeletedCleanupChildren = onDocumentDeleted(
  "communityPosts/{postId}",
  async (event) => {
    const postId = asNonBlankString(event.params.postId);

    if (postId == null) {
      return;
    }

    const postRef = db().collection("communityPosts").doc(postId);

    await Promise.all([
      db().recursiveDelete(postRef.collection("comments")),
      db().recursiveDelete(postRef.collection("likes")),
    ]);

    logger.info("Deleted community post child collections.", {
      postId: postId,
      collections: ["comments", "likes"],
    });
  }
);

export const onCommunityPostLikeWrittenSendPushToAuthor = onDocumentWritten(
  "communityPosts/{postId}/likes/{userId}",
  async (event) => {
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();

    if (beforeData != null || afterData == null) {
      return;
    }
    const postId = asNonBlankString(event.params.postId);
    const likerUserId = asNonBlankString(event.params.userId)
      ?? asNonBlankString(afterData.userId);

    if (postId == null || likerUserId == null) {
      return;
    }
    const postDoc = await db().collection("communityPosts").doc(postId).get();

    if (!postDoc.exists) {
      return;
    }
    const postAuthorId = asNonBlankString(postDoc.data()?.userId);

    if (postAuthorId == null || postAuthorId === likerUserId) {
      return;
    }
    const settings = await loadUserNotificationSettings(postAuthorId);

    if (!settings.isPushNotificationsEnabled) {
      return;
    }
    const likerDoc = await db().collection("users").doc(likerUserId).get();
    const likerNickname = asNonBlankString(likerDoc.data()?.nickname) ?? "누군가";
    const createdAt = new Date().toISOString();

    await createUserNotification(
      postAuthorId,
      `post_like_${postId}_${likerUserId}_${postAuthorId}`,
      "COMMUNITY_LIKE",
      "새 좋아요 알림",
      `${likerNickname}님이 회원님의 게시글을 좋아해요.`,
      postId,
      createdAt,
      settings
    );
    logger.info("Sent community post like push notification.", {
      postId: postId,
      likerUserId: likerUserId,
      authorId: postAuthorId,
    });
  }
);

export const onCommunityCommentWrittenSendPushToAuthor = onDocumentWritten(
  "communityPosts/{postId}/comments/{commentId}",
  async (event) => {
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();
    // Only on create
    if (beforeData != null || afterData == null) {
      return;
    }
    const postId = asNonBlankString(event.params.postId);
    const commentId = asNonBlankString(event.params.commentId);
    const commenterUserId = asNonBlankString(afterData.userId);
    const commenterNickname = asNonBlankString(afterData.userNickname) ?? "누군가";

    if (postId == null || commentId == null || commenterUserId == null) {
      return;
    }
    const postDoc = await db().collection("communityPosts").doc(postId).get();

    if (!postDoc.exists) {
      return;
    }
    const postAuthorId = asNonBlankString(postDoc.data()?.userId);

    if (postAuthorId == null || postAuthorId === commenterUserId) {
      return;
    }
    const settings = await loadUserNotificationSettings(postAuthorId);

    if (!settings.isPushNotificationsEnabled) {
      return;
    }
    const createdAt = new Date().toISOString();

    await createUserNotification(
      postAuthorId,
      `post_comment_${commentId}_${postAuthorId}`,
      "COMMUNITY_COMMENT",
      "새 댓글 알림",
      `${commenterNickname}님이 회원님의 게시글에 댓글을 남겼어요.`,
      postId,
      createdAt,
      settings
    );
    logger.info("Sent community comment push notification.", {
      postId: postId,
      commentId: commentId,
      authorId: postAuthorId,
    });
  }
);
