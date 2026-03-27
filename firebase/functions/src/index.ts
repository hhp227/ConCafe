import {setGlobalOptions} from "firebase-functions";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";
import {getApps, initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";

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
};

type CafeFavoriteLike = {
  cafeId?: unknown;
  userId?: unknown;
};

type CastClaimLike = {
  castId?: unknown;
  cafeId?: unknown;
  userId?: unknown;
  status?: unknown;
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

async function syncUserVisitCountAggregate(userId: string, delta: number): Promise<void> {
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
    logger.info("Synced cafe review aggregate.", {
      cafeIds: Array.from(targetCafeIds),
      reviewId: event.params.reviewId,
    });
  }
);

export const onCastFollowWrittenSyncFollowerCount = onDocumentWritten(
  "castFollows/{followId}",
  async (event) => {
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
    logger.info("Synced cast follower aggregate.", {
      followId: event.params.followId,
      targets: Array.from(followTargets.entries()).map(([castId, cafeId]) => ({
        castId: castId,
        cafeId: cafeId,
      })),
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

export const onVisitWrittenSyncUserVisitStats = onDocumentWritten(
  "visits/{visitId}",
  async (event) => {
    const beforeData = event.data?.before.data() as VisitLike | undefined;
    const afterData = event.data?.after.data() as VisitLike | undefined;
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
      await syncUserVisitCountAggregate(userId, delta);
    }));

    logger.info("Synced user visitCount aggregate from visit write.", {
      visitId: event.params.visitId,
      targets: targetEntries.map(([userId, delta]) => ({userId, delta})),
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
