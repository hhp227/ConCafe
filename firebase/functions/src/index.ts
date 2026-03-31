import {setGlobalOptions} from "firebase-functions";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import {onRequest} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import {initializeApp} from "firebase-admin/app";
import {FieldPath, getFirestore, FieldValue} from "firebase-admin/firestore";
import {getMessaging} from "firebase-admin/messaging";
import {getStorage} from "firebase-admin/storage";

setGlobalOptions({ maxInstances: 10 });

initializeApp();

function db() {
  return getFirestore();
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
  imageUrl?: unknown;
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
      });

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
      const followerTasks = followers.docs.map(async (followerDoc) => {
        const userId = asNonBlankString(followerDoc.get("userId"));

        if (userId == null || userId === requesterUserId) {
          return;
        }
        try {
          const settings = await loadUserNotificationSettings(userId);

          if (!settings.isPushNotificationsEnabled) {
            logger.info("Skipped fan announcement push because push is disabled.", {
              requestId: requestId,
              targetUserId: userId,
              castId: castId,
            });
            return;
          }
          if (!settings.isFollowNotificationsEnabled) {
            logger.info("Skipped fan announcement push because follow notification is disabled.", {
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
          logger.info("Sent fan announcement push.", {
            requestId: requestId,
            targetUserId: userId,
            castId: castId,
            createdAt: createdAt,
          });
        } catch (error) {
          logger.error("Failed to send fan announcement push.", {
            requestId: requestId,
            targetUserId: userId,
            castId: castId,
            error: error instanceof Error ? error.message : String(error),
          });
        }
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
