import {setGlobalOptions} from "firebase-functions";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import {onSchedule} from "firebase-functions/v2/scheduler";
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
  location?: unknown;
  verificationDistanceMeters?: unknown;
};

type CafeFavoriteLike = {
  cafeId?: unknown;
  userId?: unknown;
};

type StampLike = {
  userId?: unknown;
  cafeId?: unknown;
  visitId?: unknown;
};

type CastClaimLike = {
  castId?: unknown;
  cafeId?: unknown;
  userId?: unknown;
  status?: unknown;
  requesterNickname?: unknown;
  requesterProfileImage?: unknown;
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

    if (visitId.length == 0) {
      return;
    }
    const stampRef = db().collection("stamps").doc(visitId);
    const shouldDelete = afterUserId == null || afterCafeId == null;
    const hasBefore = beforeUserId != null && beforeCafeId != null;
    const hasAfter = afterUserId != null && afterCafeId != null;
    const isSourceChanged = hasBefore
      && hasAfter
      && (beforeUserId !== afterUserId || beforeCafeId !== afterCafeId);
    const shouldUpsert = false;

    if (shouldDelete || isSourceChanged) {
      await stampRef.delete();
    }

    logger.info("Synced stamp from visit write.", {
      visitId: visitId,
      beforeUserId: beforeUserId,
      afterUserId: afterUserId,
      beforeCafeId: beforeCafeId,
      afterCafeId: afterCafeId,
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
