/* eslint-disable require-jsdoc, max-len, operator-linebreak */

import {getAuth} from "firebase-admin/auth";
import {DocumentReference, DocumentSnapshot, getFirestore} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";
import * as logger from "firebase-functions/logger";
import {defineSecret} from "firebase-functions/params";
import {onRequest, Request} from "firebase-functions/v2/https";
import {randomBytes} from "node:crypto";
import OpenAI from "openai";

export const OPENAI_API_KEY_SECRET = defineSecret("OPENAI_API_KEY");
export const SCHEDULE_IMPORT_MODEL = "gpt-5.5";
// 관리자 웹(호스팅)에서 Bearer 토큰과 함께 호출한다. 권한은 requireCafeManager가 검사한다
export const SCHEDULE_IMPORT_CORS_ORIGINS = [
  "https://concafe-5f7fd.web.app",
  "https://concafe-5f7fd.firebaseapp.com",
  /^http:\/\/localhost(:\d+)?$/,
];
const SCHEDULE_IMPORTS = "scheduleImports";
const SCHEDULE_IMPORT_STORAGE_FOLDER = "scheduleImports";
const SCHEDULE_IMPORT_CREATED_VIA = "SCHEDULE_IMAGE_IMPORT";
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const MAX_APPLY_ENTRIES = 500;
const CAST_ROSTER_STATUS_INACTIVE = "INACTIVE";
const CAST_ROSTER_STATUS_ACTIVE = "ACTIVE";
const MAX_CAST_NAME_LENGTH = 40;
const FIRESTORE_BATCH_LIMIT = 450;
const DATE_WINDOW_PAST_DAYS = 45;
const DATE_WINDOW_FUTURE_DAYS = 120;
const FALLBACK_DEFAULT_START_TIME = "14:00";
const FALLBACK_DEFAULT_END_TIME = "22:00";
const SUPPORTED_IMAGE_MEDIA_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"] as const;
const WEEKDAYS = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"] as const;
const CONCEPT_ROLES = ["maid", "butler", "idol"];

export type SupportedImageMediaType = typeof SUPPORTED_IMAGE_MEDIA_TYPES[number];
type Weekday = typeof WEEKDAYS[number];
export type MatchType = "EXACT" | "PARTIAL" | "NONE";

type ExtractedShift = {
  castNames: string[];
  startTime: string;
  endTime: string;
};

type ExtractedDay = {
  date: string;
  weekday: Weekday;
  isClosed: boolean;
  shifts: ExtractedShift[];
  notes: string[];
};

type ExtractedSchedule = {
  isSchedule: boolean;
  title: string;
  days: ExtractedDay[];
  notes: string[];
};

export type CafeCast = {
  id: string;
  name: string;
  normalizedName: string;
  inactive: boolean;
};

type DraftDay = {
  date: string;
  isClosed: boolean;
  weekdayMismatch: boolean;
  notes: string[];
};

type DraftEntry = {
  entryId: string;
  date: string;
  castName: string;
  startTime: string | null;
  endTime: string | null;
  castId: string | null;
  matchedCastName: string | null;
  matchType: MatchType;
  existingStartTime: string | null;
  existingEndTime: string | null;
};

type ApplyEntryInput = {
  date: string;
  castId: string | null;
  newCastName: string | null;
  startTime: string | null;
  endTime: string | null;
};

export class ScheduleImportError extends Error {
  constructor(readonly status: number, readonly code: string) {
    super(code);
  }
}

const EXTRACTION_SCHEMA = {
  type: "object",
  properties: {
    isSchedule: {
      type: "boolean",
      description: "true only when the image is a cafe work schedule (출근표 / 出勤表).",
    },
    title: {
      type: "string",
      description: "Heading printed on the schedule, e.g. \"10월 2주차\". Empty string when none.",
    },
    days: {
      type: "array",
      items: {
        type: "object",
        properties: {
          date: {type: "string", description: "Full date as YYYY-MM-DD."},
          weekday: {type: "string", enum: [...WEEKDAYS], description: "Weekday printed on or implied by the column for this day."},
          isClosed: {type: "boolean", description: "true when the day is marked as a holiday / 휴무 / 定休日 for the whole cafe."},
          shifts: {
            type: "array",
            items: {
              type: "object",
              properties: {
                castNames: {type: "array", items: {type: "string"}},
                startTime: {type: "string", description: "HH:MM, or empty string when no time is printed."},
                endTime: {type: "string", description: "HH:MM, or empty string when no time is printed."},
              },
              required: ["castNames", "startTime", "endTime"],
              additionalProperties: false,
            },
          },
          notes: {
            type: "array",
            items: {type: "string"},
            description: "Event or theme labels attached to this day, e.g. \"코스데이\", \"레이 생탄 이벤트\".",
          },
        },
        required: ["date", "weekday", "isClosed", "shifts", "notes"],
        additionalProperties: false,
      },
    },
    notes: {
      type: "array",
      items: {type: "string"},
      description: "Notices that apply to the whole schedule rather than one day.",
    },
  },
  required: ["isSchedule", "title", "days", "notes"],
  additionalProperties: false,
};

const EXTRACTION_SYSTEM_PROMPT = `You read work schedules (출근표) that concept cafes and maid cafes post on Instagram, and transcribe them into structured data for the cafe's staff-scheduling system. The cafe owner reviews your output before anything is saved, so transcribe what is printed rather than guessing.

How these schedules are usually laid out:
- A weekly grid (one box per day, e.g. "MON (05)") or a monthly calendar (weekday header row, day numbers, cast names listed under each day).
- A time range followed by the names of every cast working that range, e.g. "15-22" then "나코 린 카루". Names are separated by spaces or commas; every name gets the time range printed directly above it.
- A list of names per day where only exceptions carry a time: plain names work the full business hours, "1시~) 아코" starts at that hour and works until closing, "~6시) 마메" works from opening until that hour, "4시 반~)" means 16:30.
- Some schedules print no times at all, only names under each day.
- Days marked 휴무 / 定休日 / CLOSED with no names are closed days for the whole cafe.
- Decorative labels such as "BUNNY", the cafe logo, and stickers are not cast names. Event or theme labels under a day (e.g. "코스데이", "교복데이", "메루생일") are day notes, not casts.

Rules:
- Times: write HH:MM in 24-hour form. A bare hour like "15" is 15:00. Korean schedules often write afternoon hours in 12-hour form without 오전/오후 ("1시~", "6시"); read them as the hour that falls inside the cafe's business hours (1시 = 13:00, 9시 = 21:00, 12시 = 12:00).
- Business hours: when the schedule prints business hours (e.g. "평일 2시~9시 / 주말 및 공휴일 12시~9시"), use them to complete partial ranges: a name without a time gets that day's full opening-closing range, an open-ended start gets that day's closing time, an open-ended end gets that day's opening time. Use the weekend/holiday hours on Saturdays, Sundays and Korean public holidays. When no business hours are printed, leave the unknown side as an empty string. Write midnight as 24:00. A shift that ends after midnight keeps the small hour: "15-02" is 15:00 to 02:00.
- Dates: write full YYYY-MM-DD dates. Use the reference date to infer the year and month. Calendars often begin with trailing days of the previous month (e.g. 27-30 before 1), so roll the month accordingly. Set weekday from the column or label the day sits under.
- Cast names: copy each name exactly as printed. The registered cast roster is given only so you can tell where one name ends and the next begins; do not replace a printed name with a roster name.
- Include every day that appears on the schedule, including closed days and days with no shifts.
- If the image is not a work schedule, set isSchedule to false and return an empty days array.`;

function db() {
  return getFirestore();
}

export function asNonBlankString(value: unknown): string | null {
  if (typeof value !== "string") {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function asStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.map((item) => asNonBlankString(item)).filter((item): item is string => item != null);
}

function pad2(value: number): string {
  return value.toString().padStart(2, "0");
}

export function kstToday(): Date {
  const now = new Date(Date.now() + 9 * 60 * 60 * 1000);
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
}

export function toDateKey(date: Date): string {
  return `${date.getUTCFullYear()}-${pad2(date.getUTCMonth() + 1)}-${pad2(date.getUTCDate())}`;
}

export function parseDateKey(value: string): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  if (match == null) {
    return null;
  }
  const date = new Date(Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3])));
  return toDateKey(date) === value.trim() ? date : null;
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getTime() + days * 24 * 60 * 60 * 1000);
}

function normalizeCastName(value: string): string {
  return value.normalize("NFKC").toLowerCase().replace(/[\s\p{P}\p{S}]/gu, "");
}

// Normalizes to the app's HH:MM convention, where midnight is "24:00" and post-midnight hours wrap to 01:00, 02:00...
function normalizeScheduleTime(value: string | null | undefined, isEndTime: boolean): string | null {
  const raw = value?.trim() ?? "";
  const match = /^(\d{1,2})(?::(\d{2}))?$/.exec(raw);
  if (match == null) {
    return null;
  }
  let hour = Number(match[1]);
  const minute = Number(match[2] ?? "0");
  if (hour > 30 || minute > 59) {
    return null;
  }
  if (hour > 24 || (hour === 24 && minute > 0)) {
    hour -= 24;
  }
  if (isEndTime && hour === 0 && minute === 0) {
    hour = 24;
  }
  return `${pad2(hour)}:${pad2(minute)}`;
}

function scheduleTimeToMinutes(value: string): number {
  const [hour, minute] = value.split(":").map((part) => Number(part));
  return hour * 60 + minute;
}

function isValidScheduleRange(startTime: string, endTime: string): boolean {
  const duration = scheduleTimeToMinutes(endTime) - scheduleTimeToMinutes(startTime);
  return (duration < 0 ? duration + 24 * 60 : duration) > 0;
}

function resolveDefaultTimes(businessHours: unknown): {startTime: string; endTime: string} {
  const raw = typeof businessHours === "string" ? businessHours : "";
  const match = /(\d{1,2}):(\d{2})\s*[-~〜]\s*(\d{1,2}):(\d{2})/.exec(raw);
  const startTime = match == null ? null : normalizeScheduleTime(`${match[1]}:${match[2]}`, false);
  const endTime = match == null ? null : normalizeScheduleTime(`${match[3]}:${match[4]}`, true);
  if (startTime == null || endTime == null || !isValidScheduleRange(startTime, endTime)) {
    return {startTime: FALLBACK_DEFAULT_START_TIME, endTime: FALLBACK_DEFAULT_END_TIME};
  }
  return {startTime, endTime};
}

function readBearerToken(request: Request): string {
  const authorization = request.header("Authorization") ?? request.header("authorization") ?? "";
  return authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : "";
}

export async function requireCafeManager(request: Request, cafeId: string): Promise<{uid: string; cafeSnapshot: DocumentSnapshot}> {
  const idToken = readBearerToken(request);
  if (!idToken) {
    throw new ScheduleImportError(401, "missing_auth");
  }
  let uid: string;
  try {
    uid = (await getAuth().verifyIdToken(idToken)).uid;
  } catch (error) {
    logger.warn("scheduleImport verifyIdToken failed.", error);
    throw new ScheduleImportError(401, "invalid_auth");
  }
  const [userSnapshot, cafeSnapshot] = await Promise.all([
    db().collection("users").doc(uid).get(),
    db().collection("cafes").doc(cafeId).get(),
  ]);
  if (!cafeSnapshot.exists) {
    throw new ScheduleImportError(404, "cafe_not_found");
  }
  const isAdmin = asNonBlankString(userSnapshot.get("role")) === "ADMIN";
  const isOwner = asStringArray(cafeSnapshot.get("ownerIds")).includes(uid) ||
    asStringArray(userSnapshot.get("ownedCafeIds")).includes(cafeId);
  if (!isAdmin && !isOwner) {
    throw new ScheduleImportError(403, "forbidden");
  }
  return {uid, cafeSnapshot};
}

function resolveStorageObjectPath(imageUrl: string, bucketName: string, objectPrefix: string): string | null {
  let url: URL;
  try {
    url = new URL(imageUrl);
  } catch {
    return null;
  }
  const match = /^\/v0\/b\/([^/]+)\/o\/(.+)$/.exec(url.pathname);
  if (url.protocol !== "https:" || url.hostname !== "firebasestorage.googleapis.com" || match == null || match[1] !== bucketName) {
    return null;
  }
  const objectPath = decodeURIComponent(match[2]);
  return objectPath.startsWith(objectPrefix) && !objectPath.includes("..") ? objectPath : null;
}

// objectPrefix 아래(예: "scheduleImports/{cafeId}/")에 올라온 이미지만 내려받아 다른 경로의 파일을 읽지 못하게 한다
export async function downloadStorageImage(imageUrl: string, objectPrefix: string): Promise<{objectPath: string; mediaType: SupportedImageMediaType; base64: string}> {
  const bucket = getStorage().bucket();
  const objectPath = resolveStorageObjectPath(imageUrl, bucket.name, objectPrefix);
  if (objectPath == null) {
    throw new ScheduleImportError(400, "invalid_image_url");
  }
  const file = bucket.file(objectPath);
  const [metadata] = await file.getMetadata().catch(() => {
    throw new ScheduleImportError(404, "image_not_found");
  });
  const mediaType = SUPPORTED_IMAGE_MEDIA_TYPES.find((type) => type === metadata.contentType);
  if (mediaType == null) {
    throw new ScheduleImportError(400, "unsupported_image_type");
  }
  if (Number(metadata.size ?? 0) > MAX_IMAGE_BYTES) {
    throw new ScheduleImportError(400, "image_too_large");
  }
  const [bytes] = await file.download();
  return {objectPath, mediaType, base64: bytes.toString("base64")};
}

export async function loadCafeCasts(cafeId: string): Promise<CafeCast[]> {
  const snapshot = await db().collection("cafes").doc(cafeId).collection("casts").select("name", "rosterStatus").get();
  return snapshot.docs
    .map((doc) => {
      const name = asNonBlankString(doc.get("name"));
      const inactive = doc.get("rosterStatus") === CAST_ROSTER_STATUS_INACTIVE;
      return name == null ? null : {id: doc.id, name, normalizedName: normalizeCastName(name), inactive};
    })
    .filter((cast): cast is CafeCast => cast != null);
}

export function matchCast(castName: string, casts: CafeCast[]): {cast: CafeCast | null; matchType: MatchType} {
  const normalized = normalizeCastName(castName);
  if (normalized.length === 0) {
    return {cast: null, matchType: "NONE"};
  }
  const exact = casts.find((cast) => cast.normalizedName === normalized);
  if (exact != null) {
    return {cast: exact, matchType: "EXACT"};
  }
  const partials = casts.filter((cast) =>
    Math.min(cast.normalizedName.length, normalized.length) >= 2 &&
    (cast.normalizedName.includes(normalized) || normalized.includes(cast.normalizedName))
  );
  return partials.length === 1 ? {cast: partials[0], matchType: "PARTIAL"} : {cast: null, matchType: "NONE"};
}

async function getAllInChunks(refs: DocumentReference[]): Promise<DocumentSnapshot[]> {
  const snapshots: DocumentSnapshot[] = [];
  for (let index = 0; index < refs.length; index += 100) {
    const chunk = refs.slice(index, index + 100);
    if (chunk.length > 0) {
      snapshots.push(...await db().getAll(...chunk));
    }
  }
  return snapshots;
}

function scheduleDocId(castId: string, date: string): string {
  return `${castId}_${date}`;
}

async function callModelForSchedule(
  image: {mediaType: SupportedImageMediaType; base64: string},
  cafeName: string,
  castNames: string[],
  referenceDate: Date
): Promise<ExtractedSchedule> {
  // 함수 타임아웃(300초) 안에 응답하도록 재시도 없이 240초로 제한한다
  const client = new OpenAI({apiKey: OPENAI_API_KEY_SECRET.value(), timeout: 240_000, maxRetries: 0});
  const referenceDateKey = toDateKey(referenceDate);
  const rosterLine = castNames.length > 0 ? castNames.join(", ") : "(none registered yet)";
  const response = await client.responses.create({
    model: SCHEDULE_IMPORT_MODEL,
    max_output_tokens: 32000,
    reasoning: {effort: "high"},
    instructions: EXTRACTION_SYSTEM_PROMPT,
    text: {
      format: {type: "json_schema", name: "cafe_schedule", schema: EXTRACTION_SCHEMA, strict: true},
    },
    input: [
      {
        role: "user",
        content: [
          {type: "input_image", detail: "high", image_url: `data:${image.mediaType};base64,${image.base64}`},
          {
            type: "input_text",
            text: `Cafe: ${cafeName}\nReference date (the day this schedule was uploaded, KST): ${referenceDateKey} (${WEEKDAYS[referenceDate.getUTCDay()]})\nRegistered cast roster: ${rosterLine}\n\nTranscribe this schedule.`,
          },
        ],
      },
    ],
  });

  const refusal = response.output
    .flatMap((item) => item.type === "message" ? item.content : [])
    .find((content) => content.type === "refusal");
  if (refusal != null) {
    logger.warn("scheduleImport extraction refused.", {refusal: refusal.refusal});
    throw new ScheduleImportError(422, "extraction_refused");
  }
  if (response.status === "incomplete") {
    logger.warn("scheduleImport extraction incomplete.", {reason: response.incomplete_details?.reason});
    throw new ScheduleImportError(422, response.incomplete_details?.reason === "content_filter" ? "extraction_refused" : "extraction_truncated");
  }
  const text = response.output_text;
  try {
    return JSON.parse(text) as ExtractedSchedule;
  } catch (error) {
    logger.error("scheduleImport extraction returned invalid JSON.", {error, text: text.slice(0, 500)});
    throw new ScheduleImportError(502, "extraction_invalid");
  }
}

export function sendError(response: {status: (code: number) => {json: (body: unknown) => void}}, error: unknown, label: string): void {
  if (error instanceof ScheduleImportError) {
    response.status(error.status).json({error: error.code});
    return;
  }
  if (error instanceof OpenAI.RateLimitError) {
    logger.warn(`${label} rate limited by OpenAI.`, {code: error.code, message: error.message});
    response.status(503).json({error: "extraction_busy"});
    return;
  }
  if (error instanceof OpenAI.APIConnectionTimeoutError) {
    logger.warn(`${label} OpenAI request timed out.`);
    response.status(504).json({error: "extraction_timeout"});
    return;
  }
  if (error instanceof OpenAI.APIError) {
    logger.error(`${label} OpenAI API error.`, {status: error.status, code: error.code, message: error.message});
    response.status(502).json({error: "extraction_failed"});
    return;
  }
  logger.error(`${label} failed.`, error);
  response.status(500).json({error: "internal"});
}

export const extractScheduleImage = onRequest(
  {
    region: "us-central1",
    cors: SCHEDULE_IMPORT_CORS_ORIGINS,
    timeoutSeconds: 300,
    memory: "512MiB",
    secrets: [OPENAI_API_KEY_SECRET],
  },
  async (request, response) => {
    if (request.method !== "POST") {
      response.status(405).json({error: "method_not_allowed"});
      return;
    }
    try {
      const cafeId = asNonBlankString(request.body?.cafeId);
      const imageUrl = asNonBlankString(request.body?.imageUrl);
      if (cafeId == null || imageUrl == null) {
        throw new ScheduleImportError(400, "invalid_request");
      }
      const {uid, cafeSnapshot} = await requireCafeManager(request, cafeId);
      const [image, casts] = await Promise.all([
        downloadStorageImage(imageUrl, `${SCHEDULE_IMPORT_STORAGE_FOLDER}/${cafeId}/`),
        loadCafeCasts(cafeId),
      ]);
      const referenceDate = kstToday();
      const cafeName = asNonBlankString(cafeSnapshot.get("name")) ?? "";
      const extracted = await callModelForSchedule(image, cafeName, casts.map((cast) => cast.name), referenceDate);
      if (!extracted.isSchedule) {
        throw new ScheduleImportError(422, "not_schedule_image");
      }

      const warnings: string[] = [];
      const defaults = resolveDefaultTimes(cafeSnapshot.get("businessHours"));
      const windowStart = addDays(referenceDate, -DATE_WINDOW_PAST_DAYS);
      const windowEnd = addDays(referenceDate, DATE_WINDOW_FUTURE_DAYS);
      const dayByDate = new Map<string, DraftDay>();
      const entries: DraftEntry[] = [];
      const entryKeys = new Set<string>();

      for (const day of extracted.days) {
        const date = parseDateKey(day.date);
        if (date == null || date < windowStart || date > windowEnd) {
          warnings.push(`date_out_of_range:${day.date}`);
          continue;
        }
        const dateKey = toDateKey(date);
        const weekdayMismatch = WEEKDAYS[date.getUTCDay()] !== day.weekday;
        if (weekdayMismatch) {
          warnings.push(`weekday_mismatch:${dateKey}`);
        }
        const draftDay = dayByDate.get(dateKey) ?? {date: dateKey, isClosed: day.isClosed, weekdayMismatch, notes: []};
        draftDay.notes.push(...day.notes.map((note) => note.trim()).filter((note) => note.length > 0));
        dayByDate.set(dateKey, draftDay);

        for (const shift of day.shifts) {
          let startTime = normalizeScheduleTime(shift.startTime, false);
          let endTime = normalizeScheduleTime(shift.endTime, true);
          // "1시~", "~6시"처럼 한쪽만 적힌 시간은 카페 기본 영업시간으로 나머지를 채운다
          if ((startTime == null) !== (endTime == null)) {
            startTime = startTime ?? defaults.startTime;
            endTime = endTime ?? defaults.endTime;
          }
          if (startTime == null || endTime == null || !isValidScheduleRange(startTime, endTime)) {
            if (shift.startTime.trim().length > 0 || shift.endTime.trim().length > 0) {
              warnings.push(`invalid_time:${dateKey}:${shift.startTime}-${shift.endTime}`);
            }
            startTime = null;
            endTime = null;
          }
          for (const rawName of shift.castNames) {
            const castName = rawName.trim();
            const entryKey = `${dateKey}|${normalizeCastName(castName)}`;
            if (castName.length === 0 || entryKeys.has(entryKey)) {
              continue;
            }
            entryKeys.add(entryKey);
            const {cast, matchType} = matchCast(castName, casts);
            entries.push({
              entryId: `${dateKey}_${entries.length}`,
              date: dateKey,
              castName,
              startTime,
              endTime,
              castId: cast?.id ?? null,
              matchedCastName: cast?.name ?? null,
              matchType,
              existingStartTime: null,
              existingEndTime: null,
            });
          }
        }
      }

      const days = [...dayByDate.values()].sort((left, right) => left.date.localeCompare(right.date));
      if (days.length === 0) {
        throw new ScheduleImportError(422, "no_schedule_days");
      }

      const matchedEntries = entries.filter((entry) => entry.castId != null);
      const existingSnapshots = await getAllInChunks(
        matchedEntries.map((entry) => db().collection("castSchedules").doc(scheduleDocId(entry.castId as string, entry.date)))
      );
      existingSnapshots.forEach((snapshot, index) => {
        if (snapshot.exists && snapshot.get("status") === "WORK") {
          matchedEntries[index].existingStartTime = asNonBlankString(snapshot.get("startTime"));
          matchedEntries[index].existingEndTime = asNonBlankString(snapshot.get("endTime"));
        }
      });

      const unmatchedNames = [...new Set(entries.filter((entry) => entry.matchType !== "EXACT").map((entry) => entry.castName))];
      const importId = `import-${Date.now()}-${randomBytes(3).toString("hex")}`;
      const title = extracted.title.trim();
      const notes = extracted.notes.map((note) => note.trim()).filter((note) => note.length > 0);
      const periodStart = days[0].date;
      const periodEnd = days[days.length - 1].date;

      await db().collection(SCHEDULE_IMPORTS).doc(importId).set({
        cafeId,
        createdBy: uid,
        imageUrl,
        imageObjectPath: image.objectPath,
        status: "DRAFT",
        model: SCHEDULE_IMPORT_MODEL,
        title,
        periodStart,
        periodEnd,
        coveredDates: days.map((day) => day.date),
        entryCount: entries.length,
        extracted,
        warnings,
        createdAt: new Date().toISOString(),
      });
      logger.info("scheduleImport draft created.", {importId, cafeId, dayCount: days.length, entryCount: entries.length, unmatchedCount: unmatchedNames.length});

      response.status(200).json({
        importId,
        cafeId,
        imageUrl,
        title,
        periodStart,
        periodEnd,
        days,
        entries,
        unmatchedNames,
        casts: casts.map((cast) => ({id: cast.id, name: cast.name, inactive: cast.inactive})),
        notes,
        warnings,
        defaultStartTime: defaults.startTime,
        defaultEndTime: defaults.endTime,
      });
    } catch (error) {
      sendError(response, error, "extractScheduleImage");
    }
  }
);

function parseInactiveCastIds(value: unknown): string[] {
  if (value == null) {
    return [];
  }
  if (!Array.isArray(value) || value.length > MAX_APPLY_ENTRIES) {
    throw new ScheduleImportError(400, "invalid_inactive_casts");
  }
  const castIds = value.map((item) => asNonBlankString(item));
  if (castIds.some((castId) => castId == null)) {
    throw new ScheduleImportError(400, "invalid_inactive_casts");
  }
  return [...new Set(castIds as string[])];
}

function parseApplyEntries(value: unknown): ApplyEntryInput[] {
  if (!Array.isArray(value) || value.length === 0 || value.length > MAX_APPLY_ENTRIES) {
    throw new ScheduleImportError(400, "invalid_entries");
  }
  return value.map((item) => {
    const date = asNonBlankString(item?.date);
    const castId = asNonBlankString(item?.castId);
    const newCastName = asNonBlankString(item?.newCastName);
    if (date == null || (castId == null) === (newCastName == null) || (newCastName?.length ?? 0) > MAX_CAST_NAME_LENGTH) {
      throw new ScheduleImportError(400, "invalid_entries");
    }
    return {
      date,
      castId,
      newCastName,
      startTime: asNonBlankString(item?.startTime),
      endTime: asNonBlankString(item?.endTime),
    };
  });
}

export const applyScheduleImport = onRequest(
  {
    region: "us-central1",
    cors: SCHEDULE_IMPORT_CORS_ORIGINS,
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (request, response) => {
    if (request.method !== "POST") {
      response.status(405).json({error: "method_not_allowed"});
      return;
    }
    try {
      const cafeId = asNonBlankString(request.body?.cafeId);
      const importId = asNonBlankString(request.body?.importId);
      if (cafeId == null || importId == null) {
        throw new ScheduleImportError(400, "invalid_request");
      }
      const replaceExisting = request.body?.replaceExisting === true;
      const defaultStartTime = normalizeScheduleTime(asNonBlankString(request.body?.defaultStartTime), false);
      const defaultEndTime = normalizeScheduleTime(asNonBlankString(request.body?.defaultEndTime), true);
      if (defaultStartTime == null || defaultEndTime == null || !isValidScheduleRange(defaultStartTime, defaultEndTime)) {
        throw new ScheduleImportError(400, "invalid_default_time");
      }
      const entries = parseApplyEntries(request.body?.entries);
      const inactiveCastIds = parseInactiveCastIds(request.body?.inactiveCastIds);
      const {uid, cafeSnapshot} = await requireCafeManager(request, cafeId);

      const importRef = db().collection(SCHEDULE_IMPORTS).doc(importId);
      const importSnapshot = await importRef.get();
      if (!importSnapshot.exists || importSnapshot.get("cafeId") !== cafeId) {
        throw new ScheduleImportError(404, "import_not_found");
      }
      if (importSnapshot.get("status") === "APPLIED") {
        throw new ScheduleImportError(409, "import_already_applied");
      }
      const coveredDates = new Set(asStringArray(importSnapshot.get("coveredDates")));
      if (entries.some((entry) => !coveredDates.has(entry.date))) {
        throw new ScheduleImportError(400, "entry_date_out_of_import");
      }

      const casts = await loadCafeCasts(cafeId);
      const castById = new Map(casts.map((cast) => [cast.id, cast]));
      const castByNormalizedName = new Map(casts.map((cast) => [cast.normalizedName, cast]));
      const conceptType = asNonBlankString(cafeSnapshot.get("conceptType"))?.toLowerCase() ?? "";
      const conceptRole = CONCEPT_ROLES.includes(conceptType) ? conceptType : "maid";
      const now = new Date();
      const nowIso = now.toISOString();
      const newCasts = new Map<string, {id: string; name: string}>();

      const resolvedEntries = entries.map((entry) => {
        if (entry.castId != null) {
          if (!castById.has(entry.castId)) {
            throw new ScheduleImportError(400, "cast_not_found");
          }
          return {...entry, castId: entry.castId};
        }
        const newCastName = entry.newCastName as string;
        const normalizedName = normalizeCastName(newCastName);
        if (normalizedName.length === 0) {
          throw new ScheduleImportError(400, "invalid_entries");
        }
        const existingCast = castByNormalizedName.get(normalizedName);
        if (existingCast != null) {
          return {...entry, castId: existingCast.id};
        }
        let newCast = newCasts.get(normalizedName);
        if (newCast == null) {
          newCast = {id: `cast-${now.getTime() + newCasts.size}`, name: newCastName};
          newCasts.set(normalizedName, newCast);
        }
        return {...entry, castId: newCast.id};
      });

      const entryByDocId = new Map(resolvedEntries.map((entry) => [scheduleDocId(entry.castId, entry.date), entry]));
      const scheduledCastIds = new Set(resolvedEntries.map((entry) => entry.castId));
      for (const castId of inactiveCastIds) {
        if (!castById.has(castId)) {
          throw new ScheduleImportError(400, "cast_not_found");
        }
        if (scheduledCastIds.has(castId)) {
          throw new ScheduleImportError(400, "inactive_cast_in_schedule");
        }
      }
      // 현재 없는 캐스트로 분리됐던 캐스트가 새 출근표에 다시 나오면 복귀 처리한다
      const reactivatedCastIds = casts.filter((cast) => cast.inactive && scheduledCastIds.has(cast.id)).map((cast) => cast.id);
      const inactivatedCastIds = inactiveCastIds.filter((castId) => castById.get(castId)?.inactive !== true);
      const replaceCandidateIds = replaceExisting ?
        casts.flatMap((cast) => [...coveredDates].map((date) => scheduleDocId(cast.id, date))).filter((docId) => !entryByDocId.has(docId)) :
        [];
      const docIds = [...entryByDocId.keys(), ...replaceCandidateIds];
      const snapshots = await getAllInChunks(docIds.map((docId) => db().collection("castSchedules").doc(docId)));
      const snapshotById = new Map(snapshots.map((snapshot) => [snapshot.id, snapshot]));
      const requestBatchId = `import_${importId}`;

      const writes: Array<{ref: DocumentReference; data: Record<string, unknown>; merge?: boolean}> = [];
      const castRef = (castId: string) => db().collection("cafes").doc(cafeId).collection("casts").doc(castId);
      for (const castId of inactivatedCastIds) {
        writes.push({
          ref: castRef(castId),
          merge: true,
          data: {
            rosterStatus: CAST_ROSTER_STATUS_INACTIVE,
            inactiveAt: nowIso,
            inactiveBy: uid,
            inactiveReason: "NOT_IN_SCHEDULE_IMAGE",
            inactiveImportId: importId,
            updatedAt: nowIso,
          },
        });
      }
      for (const castId of reactivatedCastIds) {
        writes.push({
          ref: castRef(castId),
          merge: true,
          data: {
            rosterStatus: CAST_ROSTER_STATUS_ACTIVE,
            reactivatedAt: nowIso,
            reactivatedImportId: importId,
            updatedAt: nowIso,
          },
        });
      }
      for (const newCast of newCasts.values()) {
        writes.push({
          ref: db().collection("cafes").doc(cafeId).collection("casts").doc(newCast.id),
          data: {
            name: newCast.name,
            linkedUserId: null,
            profileImage: null,
            desc: "캐스트 소개가 비어있습니다.",
            birthday: null,
            birthdayKey: null,
            conceptRole,
            followerCount: 0,
            rating: 0,
            visitCertificationCount: 0,
            galleryImages: [],
            createdVia: SCHEDULE_IMPORT_CREATED_VIA,
            importId,
            createdAt: nowIso,
            updatedAt: nowIso,
          },
        });
      }

      for (const [docId, entry] of entryByDocId) {
        const existing = snapshotById.get(docId);
        const existingIsWork = existing?.exists === true && existing.get("status") === "WORK";
        const entryStartTime = normalizeScheduleTime(entry.startTime, false);
        const entryEndTime = normalizeScheduleTime(entry.endTime, true);
        const hasEntryTimes = entryStartTime != null && entryEndTime != null && isValidScheduleRange(entryStartTime, entryEndTime);
        const existingStartTime = existingIsWork ? asNonBlankString(existing?.get("startTime")) : null;
        const existingEndTime = existingIsWork ? asNonBlankString(existing?.get("endTime")) : null;
        const startTime = hasEntryTimes ? entryStartTime : existingStartTime ?? defaultStartTime;
        const endTime = hasEntryTimes ? entryEndTime : existingEndTime ?? defaultEndTime;
        writes.push({
          ref: db().collection("castSchedules").doc(docId),
          data: {
            castId: entry.castId,
            cafeId,
            date: entry.date,
            status: "WORK",
            startTime,
            endTime,
            createdBy: uid,
            requestBatchId,
            createdVia: SCHEDULE_IMPORT_CREATED_VIA,
            importId,
            createdAt: asNonBlankString(existing?.get("createdAt")) ?? nowIso,
            updatedAt: nowIso,
          },
        });
      }

      let clearedScheduleCount = 0;
      for (const docId of replaceCandidateIds) {
        const existing = snapshotById.get(docId);
        if (existing?.exists !== true || existing.get("status") !== "WORK") {
          continue;
        }
        clearedScheduleCount += 1;
        writes.push({
          ref: existing.ref,
          data: {
            castId: existing.get("castId"),
            cafeId,
            date: existing.get("date"),
            status: "OFF",
            startTime: null,
            endTime: null,
            createdBy: uid,
            requestBatchId,
            createdVia: SCHEDULE_IMPORT_CREATED_VIA,
            importId,
            createdAt: asNonBlankString(existing.get("createdAt")) ?? nowIso,
            updatedAt: nowIso,
          },
        });
      }

      for (let index = 0; index < writes.length; index += FIRESTORE_BATCH_LIMIT) {
        const batch = db().batch();
        for (const write of writes.slice(index, index + FIRESTORE_BATCH_LIMIT)) {
          batch.set(write.ref, write.data, {merge: write.merge === true});
        }
        await batch.commit();
      }

      const createdCasts = [...newCasts.values()];
      await importRef.update({
        status: "APPLIED",
        appliedBy: uid,
        appliedAt: nowIso,
        replaceExisting,
        createdCastIds: createdCasts.map((cast) => cast.id),
        upsertedScheduleCount: entryByDocId.size,
        clearedScheduleCount,
        inactivatedCastIds,
        reactivatedCastIds,
      });
      logger.info("scheduleImport applied.", {importId, cafeId, createdCastCount: createdCasts.length, upsertedScheduleCount: entryByDocId.size, clearedScheduleCount, inactivatedCastCount: inactivatedCastIds.length, reactivatedCastCount: reactivatedCastIds.length});

      response.status(200).json({
        createdCasts,
        upsertedScheduleCount: entryByDocId.size,
        clearedScheduleCount,
        inactivatedCastCount: inactivatedCastIds.length,
        reactivatedCastCount: reactivatedCastIds.length,
      });
    } catch (error) {
      sendError(response, error, "applyScheduleImport");
    }
  }
);
