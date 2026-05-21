/* eslint-disable require-jsdoc, max-len, operator-linebreak */

import {onRequest} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {defineSecret} from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import {FieldPath, GeoPoint, getFirestore} from "firebase-admin/firestore";
import {createHash} from "node:crypto";

function db() {
  return getFirestore();
}

function asPlainObject(value: unknown): Record<string, unknown> | null {
  if (typeof value !== "object" || value == null || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, unknown>;
}

function asNonBlankString(value: unknown): string | null {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
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

type KrCafeSource = {
  cafeName: string;
  cafeId: string;
  supabaseCafeId: string;
};

type KrCrawlSyncSummary = {
  cafeSourcesFetched: number;
  castRowsDiscovered: number;
  castsUpserted: number;
  castsSkipped: number;
  schedulesUpserted: number;
  schedulesSkipped: number;
  apiErrors: number;
  dryRun: boolean;
};

type KrCrawlRunOptions = {
  dryRun: boolean;
};

const JP_SHOP_IMAGE_BASE_URL = "https://img.con-cafe.jp/upload/";
const GOOGLE_TRANSLATE_API_KEY_SECRET = defineSecret("GOOGLE_TRANSLATE_API_KEY");
const KR_CRAWL_SUPABASE_API_KEY_SECRET = defineSecret("KR_CRAWL_SUPABASE_API_KEY");
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
const KR_SUPABASE_BASE_URL = "https://uoizizsvfmsshmgimjzn.supabase.co/rest/v1/cafe_maids";
const KR_CRAWL_PROVIDER = "kr_supabase_cafe_maids";
const KR_CRAWL_SOURCES: KrCafeSource[] = [
  {
    cafeName: "메로하우스",
    cafeId: "cafe-1776851868943",
    supabaseCafeId: "8dd6614e-f1a8-44d8-91ba-b45fb01b0550",
  },
  {
    cafeName: "로제린",
    cafeId: "cafe-1775462556233",
    supabaseCafeId: "dfff91fc-9230-4e2e-9cec-54dffa791c3e",
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

function toStableFiveDigitId(raw: string, usedIds: Set<string>): string {
  const hash = createHash("sha256").update(raw).digest();
  let value = hash.readUInt32BE(0) % 100000;

  for (let i = 0; i < 100000; i += 1) {
    const candidate = `kr_cast_${value.toString().padStart(5, "0")}`;
    if (!usedIds.has(candidate)) {
      usedIds.add(candidate);
      return candidate;
    }
    value = (value + 1) % 100000;
  }
  throw new Error("kr_cast_id_space_exhausted");
}

function normalizeAbsoluteImageUrl(raw: unknown): string | null {
  const value = asNonBlankString(raw);
  if (value == null) {
    return null;
  }
  if (value.startsWith("http://") || value.startsWith("https://")) {
    return value;
  }
  return null;
}

function firstNonBlankString(...values: unknown[]): string | null {
  for (const value of values) {
    const normalized = asNonBlankString(value);
    if (normalized != null) {
      return normalized;
    }
  }
  return null;
}

function nestedPlainObject(source: Record<string, unknown>, ...keys: string[]): Record<string, unknown> | null {
  for (const key of keys) {
    const value = asPlainObject(source[key]);
    if (value != null) {
      return value;
    }
  }
  return null;
}

function firstArray(source: Record<string, unknown>, ...keys: string[]): unknown[] {
  for (const key of keys) {
    const value = source[key];
    if (Array.isArray(value)) {
      return value;
    }
  }
  return [];
}

function toIsoDateKey(raw: unknown): string | null {
  const value = asNonBlankString(raw);
  if (value == null) {
    return null;
  }
  const dateMatch = value.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})/);
  if (dateMatch == null) {
    return null;
  }
  const year = Number(dateMatch[1]);
  const month = Number(dateMatch[2]);
  const day = Number(dateMatch[3]);
  if (
    !Number.isInteger(year) ||
    !Number.isInteger(month) ||
    !Number.isInteger(day) ||
    month < 1 ||
    month > 12 ||
    day < 1 ||
    day > 31
  ) {
    return null;
  }
  return `${year.toString().padStart(4, "0")}-${month.toString().padStart(2, "0")}-${day.toString().padStart(2, "0")}`;
}

function normalizeTime(raw: unknown): string | null {
  const value = asNonBlankString(raw);
  if (value == null) {
    return null;
  }
  const match = value.match(/(\d{1,2}):(\d{2})/);
  if (match == null) {
    return null;
  }
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (!Number.isInteger(hour) || !Number.isInteger(minute) || hour < 0 || hour > 29 || minute < 0 || minute > 59) {
    return null;
  }
  return `${hour.toString().padStart(2, "0")}:${minute.toString().padStart(2, "0")}`;
}

function buildKrSupabaseUrl(source: KrCafeSource): string {
  const params = new URLSearchParams({
    select: "role,maids(*)",
    cafe_id: `eq.${source.supabaseCafeId}`,
  });
  return `${KR_SUPABASE_BASE_URL}?${params.toString()}`;
}

function krSupabaseApiKey(): string {
  try {
    const secretValue = KR_CRAWL_SUPABASE_API_KEY_SECRET.value().trim();
    if (secretValue.length > 0) {
      return secretValue;
    }
  } catch (_error) {
    // Local emulator and tests can provide the same value through process.env.
  }
  return process.env.KR_CRAWL_SUPABASE_API_KEY?.trim() ?? "";
}

async function fetchJsonWithRetryAndHeaders(
  url: string,
  maxAttempts: number,
  headers: Record<string, string>
): Promise<unknown> {
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
          ...headers,
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

function collectKrScheduleItems(maid: Record<string, unknown>): Record<string, unknown>[] {
  const directScheduleArrays = [
    ...firstArray(maid, "schedules", "schedule", "maid_schedules", "maidSchedules", "work_schedules", "workSchedules"),
  ];
  if (directScheduleArrays.length > 0) {
    return directScheduleArrays
      .map((item) => asPlainObject(item))
      .filter((item): item is Record<string, unknown> => item != null);
  }

  const date = toIsoDateKey(firstNonBlankString(
    maid.schedule_date,
    maid.work_date,
    maid.date,
    maid.start_date,
    maid.started_at,
    maid.scheduleDate
  ));
  const info = firstNonBlankString(
    maid.schedule_info,
    maid.schedule_text,
    maid.work_info,
    maid.memo,
    maid.note,
    maid.info
  );
  if (date == null && info == null) {
    return [];
  }
  return [{
    date: date,
    info: info,
    start_time: firstNonBlankString(maid.start_time, maid.startTime, maid.started_at),
    end_time: firstNonBlankString(maid.end_time, maid.endTime, maid.ended_at),
  }];
}

function toKrScheduleDocument(
  cafeId: string,
  castId: string,
  castName: string,
  item: Record<string, unknown>
): {id: string; doc: Record<string, unknown>; sourceHash: string} | null {
  const date = toIsoDateKey(firstNonBlankString(
    item.date,
    item.schedule_date,
    item.work_date,
    item.start_date,
    item.started_at,
    item.scheduleDate
  ));
  if (date == null) {
    return null;
  }

  const startTime = normalizeTime(firstNonBlankString(item.start_time, item.startTime, item.started_at, item.from));
  const endTime = normalizeTime(firstNonBlankString(item.end_time, item.endTime, item.ended_at, item.to));
  const info = firstNonBlankString(
    item.info,
    item.memo,
    item.note,
    item.description,
    item.schedule_info,
    item.schedule_text,
    item.work_info
  );
  const sourcePayload = {
    cafeId: cafeId,
    castId: castId,
    date: date,
    startTime: startTime,
    endTime: endTime,
    info: info,
  };
  const sourceHash = toSourceHash(sourcePayload);
  const doc: Record<string, unknown> = {
    cafeId: cafeId,
    castId: castId,
    castName: castName,
    date: date,
    status: "WORK",
    startTime: startTime,
    endTime: endTime,
    memo: info,
    createdVia: "KR_CRAWLING",
    source: {
      provider: KR_CRAWL_PROVIDER,
      ...sourcePayload,
    },
    sourceHash: sourceHash,
    updatedAt: new Date().toISOString(),
  };
  return {
    id: `${castId}_${date}`,
    doc: doc,
    sourceHash: sourceHash,
  };
}

async function runKrCrawledDataSync(options: KrCrawlRunOptions): Promise<KrCrawlSyncSummary> {
  const firestore = db();
  const summary: KrCrawlSyncSummary = {
    cafeSourcesFetched: 0,
    castRowsDiscovered: 0,
    castsUpserted: 0,
    castsSkipped: 0,
    schedulesUpserted: 0,
    schedulesSkipped: 0,
    apiErrors: 0,
    dryRun: options.dryRun,
  };
  const apiKey = krSupabaseApiKey();
  if (apiKey.length === 0) {
    throw new Error("KR_CRAWL_SUPABASE_API_KEY is not configured");
  }
  const usedCastIds = new Set<string>();

  for (const source of KR_CRAWL_SOURCES) {
    let payload: unknown;
    try {
      payload = await fetchJsonWithRetryAndHeaders(buildKrSupabaseUrl(source), 3, {
        "apikey": apiKey,
        "Authorization": `Bearer ${apiKey}`,
      });
      summary.cafeSourcesFetched += 1;
    } catch (error) {
      summary.apiErrors += 1;
      logger.error("runKrCrawledDataSync cafe_maids fetch failed.", {
        cafeId: source.cafeId,
        supabaseCafeId: source.supabaseCafeId,
        error: error instanceof Error ? error.message : "unknown",
      });
      continue;
    }

    const rows = Array.isArray(payload) ? payload : [];
    for (let index = 0; index < rows.length; index += 1) {
      const row = asPlainObject(rows[index]);
      const maid = row == null ? null : nestedPlainObject(row, "maids", "maid");
      if (row == null || maid == null) {
        continue;
      }

      const name = firstNonBlankString(maid.name, maid.nickname, maid.display_name, maid.maid_name);
      if (name == null) {
        continue;
      }
      summary.castRowsDiscovered += 1;

      const sourceMaidId = firstNonBlankString(maid.id, maid.maid_id, row.maid_id) ?? `${source.cafeId}:${name}:${index}`;
      const castId = toStableFiveDigitId(`${source.cafeId}:${sourceMaidId}`, usedCastIds);
      const desc = firstNonBlankString(
        maid.introduction,
        maid.introduce,
        maid.description,
        maid.comment,
        maid.profile,
        maid.bio
      ) ?? "캐스트 소개가 비어있습니다.";
      const profileImage = normalizeAbsoluteImageUrl(firstNonBlankString(
        maid.profile_image,
        maid.profileImage,
        maid.profile_image_url,
        maid.image_url,
        maid.avatar_url,
        maid.photo_url
      ));
      const role = firstNonBlankString(row.role, maid.role, maid.position) ?? "maid";
      const castSourcePayload = {
        cafeId: source.cafeId,
        supabaseCafeId: source.supabaseCafeId,
        sourceMaidId: sourceMaidId,
        role: role,
        name: name,
        desc: desc,
        profileImage: profileImage,
        updatedAt: firstNonBlankString(maid.updated_at, row.updated_at),
      };
      const castSourceHash = toSourceHash(castSourcePayload);
      const castDoc: Record<string, unknown> = {
        name: name,
        linkedUserId: null,
        profileImage: profileImage,
        desc: desc,
        birthday: null,
        birthdayKey: null,
        conceptRole: role,
        followerCount: 0,
        rating: 0,
        visitCertificationCount: 0,
        galleryImages: [],
        createdVia: "KR_CRAWLING",
        source: {
          provider: KR_CRAWL_PROVIDER,
          ...castSourcePayload,
        },
        sourceHash: castSourceHash,
        updatedAt: new Date().toISOString(),
      };

      const castRef = firestore.collection("cafes").doc(source.cafeId).collection("casts").doc(castId);
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

      const scheduleItems = collectKrScheduleItems(maid);
      for (const scheduleItem of scheduleItems) {
        const scheduleDocument = toKrScheduleDocument(source.cafeId, castId, name, scheduleItem);
        if (scheduleDocument == null) {
          continue;
        }
        const scheduleRef = firestore.collection("castSchedules").doc(scheduleDocument.id);
        const existingScheduleSnapshot = await scheduleRef.get();
        const existingScheduleSourceHash = asNonBlankString(existingScheduleSnapshot.get("sourceHash"));
        if (existingScheduleSourceHash === scheduleDocument.sourceHash) {
          summary.schedulesSkipped += 1;
        } else if (!options.dryRun) {
          const createdAt = asNonBlankString(existingScheduleSnapshot.get("createdAt")) ?? new Date().toISOString();
          await scheduleRef.set({
            ...scheduleDocument.doc,
            createdAt: createdAt,
          }, {merge: false});
          summary.schedulesUpserted += 1;
        } else {
          summary.schedulesUpserted += 1;
        }
      }
      await sleep(20);
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

export const syncKrCrawledConCafeData = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 540,
    memory: "1GiB",
    secrets: [KR_CRAWL_SUPABASE_API_KEY_SECRET],
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

    const expectedToken = process.env.KR_CRAWL_TOKEN?.trim() ?? process.env.MOCK_SEED_TOKEN?.trim();
    const providedToken = request.get("x-seed-token")?.trim();
    if (expectedToken != null && expectedToken.length > 0 && providedToken !== expectedToken) {
      response.status(401).json({
        error: "unauthorized",
      });
      return;
    }

    const dryRun = toBooleanQuery(request.query.dryRun);
    try {
      const summary = await runKrCrawledDataSync({
        dryRun: dryRun,
      });
      logger.info("syncKrCrawledConCafeData completed.", summary);
      response.status(200).json({
        ok: true,
        summary: summary,
      });
    } catch (error) {
      logger.error("syncKrCrawledConCafeData failed.", error);
      response.status(500).json({
        error: "internal",
      });
    }
  }
);

export const clearKrCrawledConCafeData = onRequest(
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
      casts: 0,
      schedules: 0,
      castDirectory: 0,
    };

    try {
      for (const source of KR_CRAWL_SOURCES) {
        const castSnapshot = await firestore
          .collection("cafes")
          .doc(source.cafeId)
          .collection("casts")
          .where(FieldPath.documentId(), ">=", "kr_cast_")
          .where(FieldPath.documentId(), "<=", "kr_cast_\uf8ff")
          .get();

        for (let i = 0; i < castSnapshot.docs.length; i += 1) {
          await firestore.recursiveDelete(castSnapshot.docs[i].ref);
          summary.casts += 1;
        }
      }

      summary.schedules = await deleteDocumentsWithPrefix("castSchedules", "kr_cast_");
      summary.castDirectory = await deleteDocumentsWithPrefix("castDirectory", "kr_cast_");

      logger.info("clearKrCrawledConCafeData completed.", {
        summary: summary,
      });
      response.status(200).json({
        ok: true,
        summary: summary,
      });
    } catch (error) {
      logger.error("clearKrCrawledConCafeData failed.", error);
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
