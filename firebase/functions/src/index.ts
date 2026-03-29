import {setGlobalOptions} from "firebase-functions";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import {onSchedule} from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import {getApps, initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {getMessaging} from "firebase-admin/messaging";

setGlobalOptions({ maxInstances: 10 });

let firestoreDbInstance: ReturnType<typeof getFirestore> | null = null;

function db() {
  if (firestoreDbInstance != null) {
    return firestoreDbInstance;
  }
  if (getApps().length == 0) {
    initializeApp();
  }
  firestoreDbInstance = getFirestore();
  return firestoreDbInstance;
}

type ReviewLike = {
  id?: unknown;
  cafeId?: unknown;
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

function asStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .filter((item) => typeof item === "string")
    .map((item) => (item as string).trim())
    .filter((item) => item.length > 0);
}

function castTargetKey(cafeId: string, castId: string): string {
  return `${cafeId}::${castId}`;
}

function parseCastTargetKey(key: string): {cafeId: string; castId: string} {
  const splitIndex = key.indexOf("::");
  if (splitIndex < 0) {
    return {cafeId: "", castId: ""};
  }
  return {
    cafeId: key.substring(0, splitIndex),
    castId: key.substring(splitIndex + 2),
  };
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

function buildCastFollowDocumentId(userId: string, castId: string): string {
  const normalizedUserId = userId.replace(/\//g, "_");
  const normalizedCastId = castId.replace(/\//g, "_");
  return `${normalizedUserId}_${normalizedCastId}`;
}

function sanitizeNotificationDocumentId(value: string): string {
  return value.replace(/\//g, "_").trim();
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
  const isNoticeEnabled = plain?.isNoticeNotificationsEnabled === true;
  const isFollowEnabled = plain?.isFollowNotificationsEnabled !== false;
  const isEventEnabled = plain?.isEventNotificationsEnabled !== false;
  const quietHoursRaw = asNonBlankString(plain?.quietHoursMode)?.toUpperCase();
  const quietHoursMode: NotificationQuietHoursMode =
    quietHoursRaw === "OFF" || quietHoursRaw === "ALL_DAY" ? quietHoursRaw : "NIGHT";

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
      isNoticeNotificationsEnabled: false,
      isFollowNotificationsEnabled: true,
      isEventNotificationsEnabled: true,
      quietHoursMode: "NIGHT",
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
  if (status === "APPROVED" || status === "승인 완료") {
    return true;
  }
  return status.includes("승인");
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
    const settings = await loadUserNotificationSettings(userId);

    if (!settings.isPushNotificationsEnabled) {
      return;
    }
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
  const settings = await loadUserNotificationSettings(recipientUserId);

  if (!settings.isPushNotificationsEnabled) {
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
    | "FOLLOW_UPDATE"
    | "CAFE_APPROVAL_REQUEST"
    | "CAFE_OWNER_APPROVAL_REQUEST"
    | "CAST_CLAIM_REQUEST"
    | "CAFE_APPROVED"
    | "CAFE_OWNER_APPROVED"
    | "CAST_CLAIM_APPROVED"
    | "CAFE_REJECTED"
    | "CAFE_OWNER_REJECTED"
    | "CAST_CLAIM_REJECTED",
  title: string,
  body: string,
  targetId: string,
  createdAt: string,
  settings: UserNotificationSettings | null = null
): Promise<void> {
  const sanitizedId = sanitizeNotificationDocumentId(notificationId);
  const userRef = db().collection("users").doc(userId);
  const notificationRef = userRef.collection("notifications").doc(sanitizedId);
  const existing = await notificationRef.get();

  if (existing.exists) {
    return;
  }

  await notificationRef.set(
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
    },
    {merge: false}
  );
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

async function syncCastVisitCertificationAggregate(cafeId: string, castId: string): Promise<void> {
  const taggedReviewSnapshot = await db()
    .collection("reviews")
    .where("cafeId", "==", cafeId)
    .where("taggedCastIds", "array-contains", castId)
    .select("userId")
    .get();

  if (taggedReviewSnapshot.empty) {
    await db()
      .collection("cafes")
      .doc(cafeId)
      .collection("casts")
      .doc(castId)
      .set(
        {
          visitCertificationCount: 0,
        },
        {merge: true}
      );
    return;
  }

  const taggedReviewUserIds = taggedReviewSnapshot.docs
    .map((doc) => asNonBlankString(doc.get("userId")))
    .filter((userId): userId is string => userId !== null);
  const taggedReviewUserIdSet = new Set<string>(taggedReviewUserIds);

  const verifiedVisitSnapshot = await db()
    .collection("visits")
    .where("cafeId", "==", cafeId)
    .where("verified", "==", true)
    .select("userId")
    .get();
  const verifiedUserIds = new Set<string>(
    verifiedVisitSnapshot.docs
      .map((doc) => asNonBlankString(doc.get("userId")))
      .filter((userId): userId is string => userId !== null)
  );
  const visitCertificationCount = Array.from(taggedReviewUserIdSet)
    .filter((userId) => verifiedUserIds.has(userId))
    .length;

  await db()
    .collection("cafes")
    .doc(cafeId)
    .collection("casts")
    .doc(castId)
    .set(
      {
        visitCertificationCount: visitCertificationCount,
      },
      {merge: true}
    );
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

async function syncUserVisitCountAggregate(userId: string): Promise<void> {
  if (userId.length == 0) {
    return;
  }
  const userRef = db().collection("users").doc(userId);
  const verifiedVisitSnapshot = await db()
    .collection("visits")
    .where("userId", "==", userId)
    .where("verified", "==", true)
    .select("userId")
    .get();
  const nextVisitCount = verifiedVisitSnapshot.size;
  const nextLevel = Math.max(1, 1 + Math.floor(nextVisitCount / 5));

  await db().runTransaction(async (transaction) => {
    const snapshot = await transaction.get(userRef);
    const userData = snapshot.data();
    const statsRaw = asPlainObject(userData?.stats);
    const stats = statsRaw == null ? {} : {...statsRaw};

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

async function syncUserStampCountAggregate(userId: string): Promise<void> {
  if (userId.length == 0) {
    return;
  }
  const userRef = db().collection("users").doc(userId);
  const stampSnapshot = await db()
    .collection("stamps")
    .where("userId", "==", userId)
    .select("userId")
    .get();
  const stampCount = stampSnapshot.size;

  await db().runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    const userData = userSnapshot.data();
    const statsRaw = asPlainObject(userData?.stats);
    const stats = statsRaw == null ? {} : {...statsRaw};

    stats.stampCount = stampCount;

    transaction.set(
      userRef,
      {
        stats: stats,
        stampCount: stampCount,
      },
      {merge: true}
    );
  });
}

function collectCastTargetsFromReviewPayload(review: ReviewLike | undefined): Set<string> {
  const targets = new Set<string>();
  const cafeId = asNonBlankString(review?.cafeId);

  if (cafeId == null) {
    return targets;
  }

  asStringArray(review?.taggedCastIds).forEach((castId) => {
    targets.add(castTargetKey(cafeId, castId));
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

async function syncCafeReviewAggregate(cafeId: string): Promise<void> {
  const snapshot = await db()
    .collection("reviews")
    .where("cafeId", "==", cafeId)
    .select("rating")
    .get();
  let reviewCount = 0;
  let ratingTotal = 0;

  snapshot.forEach((doc) => {
    const rating = doc.get("rating");

    if (typeof rating === "number") {
      reviewCount += 1;
      ratingTotal += rating;
    }
  });

  const ratingAvg = reviewCount == 0 ? 0 : ratingTotal / reviewCount;

  await db().collection("cafes").doc(cafeId).set(
    {
      reviewCount: reviewCount,
      ratingAvg: ratingAvg,
    },
    {merge: true}
  );
}

async function syncCastFollowerAggregate(cafeId: string, castId: string): Promise<void> {
  const snapshot = await db()
    .collection("castFollows")
    .where("castId", "==", castId)
    .select("userId")
    .get();
  const followerCount = snapshot.size;

  await db()
    .collection("cafes")
    .doc(cafeId)
    .collection("casts")
    .doc(castId)
    .set(
      {
        followerCount: followerCount,
      },
      {merge: true}
    );
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
  const startTime = asNonBlankString(afterData?.startTime) ?? "";
  const endTime = asNonBlankString(afterData?.endTime) ?? "";
  const timeLabel = startTime.length > 0 && endTime.length > 0
    ? `${startTime} - ${endTime}`
    : "오늘";
  const createdAt = new Date().toISOString();
  const tasks = followers.docs.map(async (followerDoc) => {
    const userId = asNonBlankString(followerDoc.get("userId"));

    if (userId == null) {
      return;
    }
    const settings = await loadUserNotificationSettings(userId);

    if (!settings.isPushNotificationsEnabled || !settings.isShiftNotificationsEnabled) {
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
  });

  await Promise.all(tasks);
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
  const createdAt = asNonBlankString(afterData.createdAt) ?? new Date().toISOString();
  const tasks = favorites.docs.map(async (favoriteDoc) => {
    const userId = asNonBlankString(favoriteDoc.get("userId"));

    if (userId == null) {
      return;
    }
    const settings = await loadUserNotificationSettings(userId);

    if (!settings.isPushNotificationsEnabled || !settings.isNoticeNotificationsEnabled) {
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
  });

  await Promise.all(tasks);
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
  const recipientUserIds = new Set<string>();

  favorites.docs.forEach((favoriteDoc) => {
    const userId = asNonBlankString(favoriteDoc.get("userId"));

    if (userId != null) {
      recipientUserIds.add(userId);
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
        recipientUserIds.add(userId);
      }
    });
  }
  if (recipientUserIds.size == 0) {
    return;
  }
  const createdAt = asNonBlankString(afterData.createdAt) ?? new Date().toISOString();
  const tasks = Array.from(recipientUserIds).map(async (userId) => {
    const settings = await loadUserNotificationSettings(userId);

    if (!settings.isEventNotificationsEnabled) {
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
  });

  await Promise.all(tasks);
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
  const createdAt = new Date().toISOString();
  const tasks = castSnapshot.docs.map(async (castDoc) => {
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
    const followerTasks = followers.docs.map(async (followerDoc) => {
      const userId = asNonBlankString(followerDoc.get("userId"));

      if (userId == null) {
        return;
      }
      const settings = await loadUserNotificationSettings(userId);

      if (!settings.isPushNotificationsEnabled || !settings.isBirthdayNotificationsEnabled) {
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
    });

    await Promise.all(followerTasks);
  });

  await Promise.all(tasks);
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

async function loadCafeRankingSources(): Promise<CafeRankingSource[]> {
  const snapshot = await db()
    .collection("cafes")
    .select("name", "region", "address", "ratingAvg", "thumbnailImage", "approved")
    .get();

  return snapshot.docs
    .map((doc) => {
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
    })
    .filter((item): item is CafeRankingSource => item !== null)
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

async function syncAllRankingSnapshots(): Promise<void> {
  const cafeSources = await loadCafeRankingSources();
  const cafeById = new Map<string, CafeRankingSource>();

  cafeSources.forEach((cafe) => {
    cafeById.set(cafe.id, cafe);
  });
  const castSources = await loadCastRankingSources(cafeById);
  const tasks: Promise<void>[] = [];

  RANKING_PERIODS.forEach((period) => {
    RANKING_SCOPES.forEach((scope) => {
      const scopedCafes = cafeSources.filter((entry) => matchesScope(entry.country, entry.city, scope));
      const scopedCasts = castSources.filter((entry) => matchesScope(entry.country, entry.city, scope));

      tasks.push(syncRankingSnapshotDocument("cafe", period, scope, scopedCafes));
      tasks.push(syncRankingSnapshotDocument("cast", period, scope, scopedCasts));
    });
  });
  await Promise.all(tasks);
}

async function markRankingSyncDirty(reason: string, payload: Record<string, unknown>): Promise<void> {
  await db().doc(RANKING_SYNC_DOC_PATH).set(
    {
      dirty: true,
      updatedAt: new Date().toISOString(),
      reason: reason,
      payload: payload,
    },
    {merge: true}
  );
}

async function shouldSyncRankingSnapshots(): Promise<boolean> {
  const snapshot = await db().doc(RANKING_SYNC_DOC_PATH).get();

  if (!snapshot.exists) {
    return true;
  }
  return snapshot.get("dirty") === true;
}

async function completeRankingSync(): Promise<void> {
  await db().doc(RANKING_SYNC_DOC_PATH).set(
    {
      dirty: false,
      syncedAt: new Date().toISOString(),
    },
    {merge: true}
  );
}


export const onReviewWrittenSyncCafeAggregate = onDocumentWritten(
  "reviews/{reviewId}",
  async (event) => {
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();
    const targetCafeIds = new Set<string>();

    if (typeof beforeData?.cafeId === "string" && beforeData.cafeId.length > 0) {
      targetCafeIds.add(beforeData.cafeId);
    }
    if (typeof afterData?.cafeId === "string" && afterData.cafeId.length > 0) {
      targetCafeIds.add(afterData.cafeId);
    }
    if (targetCafeIds.size == 0) {
      return;
    }

    await Promise.all(
      Array.from(targetCafeIds).map(async (cafeId) => {
        await syncCafeReviewAggregate(cafeId);
      })
    );
    await markRankingSyncDirty("review_written", {
      reviewId: event.params.reviewId,
      cafeIds: Array.from(targetCafeIds),
    });
    logger.info("Synced cafe review aggregate.", {
      cafeIds: Array.from(targetCafeIds),
      reviewId: event.params.reviewId,
    });
  }
);

export const onCastFollowWrittenSyncFollowerCount = onDocumentWritten(
  "castFollows/{followId}",
  async (event) => {
    const followId = asNonBlankString(event.params.followId) ?? "";
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();
    const followTargets = new Map<string, string>();

    if (
      typeof beforeData?.cafeId === "string" &&
      typeof beforeData?.castId === "string" &&
      beforeData.cafeId.length > 0 &&
      beforeData.castId.length > 0
    ) {
      followTargets.set(beforeData.castId, beforeData.cafeId);
    }
    if (
      typeof afterData?.cafeId === "string" &&
      typeof afterData?.castId === "string" &&
      afterData.cafeId.length > 0 &&
      afterData.castId.length > 0
    ) {
      followTargets.set(afterData.castId, afterData.cafeId);
    }
    if (followTargets.size == 0) {
      return;
    }

    await Promise.all(
      Array.from(followTargets.entries()).map(async ([castId, cafeId]) => {
        await syncCastFollowerAggregate(cafeId, castId);
      })
    );
    await markRankingSyncDirty("cast_follow_written", {
      followId: event.params.followId,
      targets: Array.from(followTargets.entries()).map(([castId, cafeId]) => ({
        castId: castId,
        cafeId: cafeId,
      })),
    });
    logger.info("Synced cast follower aggregate.", {
      followId: event.params.followId,
      targets: Array.from(followTargets.entries()).map(([castId, cafeId]) => ({
        castId: castId,
        cafeId: cafeId,
      })),
    });
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
      const followerTasks = followers.docs.map(async (followerDoc) => {
        const userId = asNonBlankString(followerDoc.get("userId"));

        if (userId == null || userId === requesterUserId) {
          return;
        }
        const settings = await loadUserNotificationSettings(userId);

        if (!settings.isPushNotificationsEnabled) {
          logger.info("Skipped fan announcement push because push is disabled.", {
            requestId: requestId,
            targetUserId: userId,
            castId: castId,
          });
          return;
        }
        if (isQuietHoursPushSuppressed(settings)) {
          logger.info("Skipped fan announcement push due to quiet hours.", {
            requestId: requestId,
            targetUserId: userId,
            castId: castId,
            quietHoursMode: settings.quietHoursMode,
          });
          return;
        }
        await sendPushToUser(
          userId,
          title,
          body,
          "FAN_ANNOUNCEMENT",
          castId,
          sanitizeNotificationDocumentId(`fan_announcement_${requestId}_${userId}`),
          settings
        );
        logger.info("Sent fan announcement push.", {
          requestId: requestId,
          targetUserId: userId,
          castId: castId,
          createdAt: createdAt,
        });
      });

      await Promise.all(followerTasks);
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
    const createdAt = new Date().toISOString();

    await createApprovalResultNotification(
      requesterUserId,
      `approved_cafe_registration_${claimId}`,
      "CAFE_APPROVED",
      "카페 등록 승인 완료",
      `${cafeName} 등록 요청이 승인되었어요.`,
      claimId,
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
    const beforeData = event.data?.before.data() as CastClaimLike | undefined;
    const afterData = event.data?.after.data() as CastClaimLike | undefined;
    const beforeStatus = asNonBlankString(beforeData?.status);
    const afterStatus = asNonBlankString(afterData?.status);
    const castId = asNonBlankString(afterData?.castId);
    const cafeId = asNonBlankString(afterData?.cafeId);
    const userId = asNonBlankString(afterData?.userId);

    if (afterStatus !== "APPROVED") {
      return;
    } else if (beforeStatus === "APPROVED") {
      return;
    } else if (castId == null || cafeId == null || userId == null) {
      return;
    }

    const followId = buildCastFollowDocumentId(userId, castId);
    const followRef = db().collection("castFollows").doc(followId);
    const followSnapshot = await followRef.get();
    const followCastId = asNonBlankString(followSnapshot.data()?.castId);
    const followUserId = asNonBlankString(followSnapshot.data()?.userId);

    if (followSnapshot.exists && followCastId === castId && followUserId === userId) {
      await followRef.delete();
      logger.info("Removed self-follow after cast claim approval.", {
        claimId: event.params.claimId,
        cafeId: cafeId,
        castId: castId,
        userId: userId,
        followId: followId,
      });
    }
  }
);

export const onReviewWrittenSyncCastVisitCertificationCount = onDocumentWritten(
  "reviews/{reviewId}",
  async (event) => {
    const beforeData = event.data?.before.data() as ReviewLike | undefined;
    const afterData = event.data?.after.data() as ReviewLike | undefined;
    const targetKeys = new Set<string>();

    collectCastTargetsFromReviewPayload(beforeData).forEach((key) => targetKeys.add(key));
    collectCastTargetsFromReviewPayload(afterData).forEach((key) => targetKeys.add(key));

    if (targetKeys.size == 0) {
      return;
    }

    await Promise.all(
      Array.from(targetKeys).map(async (key) => {
        const {cafeId, castId} = parseCastTargetKey(key);

        if (cafeId.length == 0 || castId.length == 0) {
          return;
        }
        await syncCastVisitCertificationAggregate(cafeId, castId);
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
        targetPairs.add(castTargetKey(cafeId, castId));
      });
    }));

    if (targetPairs.size == 0) {
      return;
    }

    await Promise.all(
      Array.from(targetPairs).map(async (key) => {
        const {cafeId, castId} = parseCastTargetKey(key);

        if (cafeId.length == 0 || castId.length == 0) {
          return;
        }
        await syncCastVisitCertificationAggregate(cafeId, castId);
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
    const allowedRadiusMeters = 100;

    if (cafeId == null || userId == null || userLocation == null) {
      await visitRef.delete();
      logger.warn("Deleted invalid visit payload.", {
        visitId: visitId,
        hasCafeId: cafeId != null,
        hasUserId: userId != null,
        hasLocation: userLocation != null,
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
      await visitRef.delete();
      logger.info("Deleted visit outside allowed radius.", {
        visitId: visitId,
        cafeId: cafeId,
        userId: userId,
        distanceMeters: distanceMeters,
        allowedRadiusMeters: allowedRadiusMeters,
      });
      return;
    }
    const currentVerified = afterData.verified === true;
    const currentDistance = asFiniteNumber(afterData.verificationDistanceMeters);
    const isDistanceSynced = currentDistance != null &&
      Math.abs(currentDistance - distanceMeters) < 0.1;

    if (currentVerified && isDistanceSynced) {
      return;
    }
    await visitRef.set(
      {
        verified: true,
        verificationDistanceMeters: distanceMeters,
        allowedRadiusMeters: allowedRadiusMeters,
        verifiedAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      {merge: true}
    );

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
    const afterUserId = asNonBlankString(afterData?.userId);
    const userIds = new Set<string>();

    if (beforeUserId != null) {
      userIds.add(beforeUserId);
    }
    if (afterUserId != null) {
      userIds.add(afterUserId);
    }
    if (userIds.size == 0) {
      return;
    }

    await Promise.all(Array.from(userIds).map(async (userId) => {
      await syncUserVisitCountAggregate(userId);
    }));

    logger.info("Synced user visitCount aggregate from visit write.", {
      visitId: event.params.visitId,
      targets: Array.from(userIds),
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
      await stampRef.set(
        {
          userId: afterUserId,
          cafeId: afterCafeId,
          visitId: visitId,
          earnedAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
        {merge: true}
      );
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

export const onStampWrittenSyncUserStampStats = onDocumentWritten(
  "stamps/{stampId}",
  async (event) => {
    const beforeData = event.data?.before.data() as StampLike | undefined;
    const afterData = event.data?.after.data() as StampLike | undefined;
    const userIds = new Set<string>();
    const beforeUserId = asNonBlankString(beforeData?.userId);
    const afterUserId = asNonBlankString(afterData?.userId);

    if (beforeUserId != null) {
      userIds.add(beforeUserId);
    }
    if (afterUserId != null) {
      userIds.add(afterUserId);
    }
    if (userIds.size == 0) {
      return;
    }

    await Promise.all(
      Array.from(userIds).map(async (userId) => {
        await syncUserStampCountAggregate(userId);
      })
    );

    logger.info("Synced user stampCount aggregate from stamp write.", {
      stampId: event.params.stampId,
      targets: Array.from(userIds),
    });
  }
);

export const onCafeWrittenMarkRankingDirty = onDocumentWritten(
  "cafes/{cafeId}",
  async (event) => {
    await markRankingSyncDirty("cafe_written", {
      cafeId: event.params.cafeId,
    });
  }
);

export const onCastWrittenMarkRankingDirty = onDocumentWritten(
  "cafes/{cafeId}/casts/{castId}",
  async (event) => {
    await markRankingSyncDirty("cast_written", {
      cafeId: event.params.cafeId,
      castId: event.params.castId,
    });
  }
);

export const onScheduleSyncRankingSnapshots = onSchedule(
  {
    schedule: "every 30 minutes",
    timeZone: "Asia/Seoul",
  },
  async () => {
    const shouldSync = await shouldSyncRankingSnapshots();

    if (!shouldSync) {
      return;
    }
    await syncAllRankingSnapshots();
    await completeRankingSync();
    logger.info("Synced ranking snapshots.", {
      periods: RANKING_PERIODS,
      scopeCount: RANKING_SCOPES.length,
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
