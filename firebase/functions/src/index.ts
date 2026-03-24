import {setGlobalOptions} from "firebase-functions";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";
import {initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";

setGlobalOptions({ maxInstances: 10 });

initializeApp();

const db = getFirestore();

async function syncCafeReviewAggregate(cafeId: string): Promise<void> {
  const snapshot = await db
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

  await db.collection("cafes").doc(cafeId).set(
    {
      reviewCount: reviewCount,
      ratingAvg: ratingAvg,
    },
    {merge: true}
  );
}

async function syncCastFollowerAggregate(cafeId: string, castId: string): Promise<void> {
  const snapshot = await db
    .collection("castFollows")
    .where("castId", "==", castId)
    .select("userId")
    .get();
  const followerCount = snapshot.size;

  await db
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
