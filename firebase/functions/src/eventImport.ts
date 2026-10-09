/* eslint-disable require-jsdoc, max-len, operator-linebreak */

import * as logger from "firebase-functions/logger";
import {onRequest} from "firebase-functions/v2/https";
import OpenAI from "openai";
import {
  OPENAI_API_KEY_SECRET,
  SCHEDULE_IMPORT_CORS_ORIGINS,
  SCHEDULE_IMPORT_MODEL,
  ScheduleImportError,
  SupportedImageMediaType,
  addDays,
  asNonBlankString,
  downloadStorageImage,
  kstToday,
  loadCafeCasts,
  matchCast,
  parseDateKey,
  requireCafeManager,
  sendError,
  toDateKey,
} from "./scheduleImport.js";

const EVENT_IMAGE_STORAGE_FOLDER = "events";
const MAX_EVENT_TITLE_LENGTH = 100;
const MAX_EVENT_CONTENT_LENGTH = 2000;
const EVENT_DATE_WINDOW_PAST_DAYS = 60;
const EVENT_DATE_WINDOW_FUTURE_DAYS = 365;

type ExtractedEvent = {
  isEvent: boolean;
  title: string;
  content: string;
  startDate: string;
  endDate: string;
  participantNames: string[];
  hasLivePerformance: boolean;
};

const EVENT_EXTRACTION_SCHEMA = {
  type: "object",
  properties: {
    isEvent: {
      type: "boolean",
      description: "true when the image announces a cafe event, collaboration, birthday, anniversary, guest day, theme day or similar promotion.",
    },
    title: {
      type: "string",
      description: "Short Korean event title, at most 40 characters. Empty string when isEvent is false.",
    },
    content: {
      type: "string",
      description: "Korean announcement text for the app's event detail page, plain text with line breaks, at most 1200 characters.",
    },
    startDate: {type: "string", description: "First day of the event as YYYY-MM-DD, or empty string when no date is printed."},
    endDate: {type: "string", description: "Last day of the event as YYYY-MM-DD, or empty string when no date is printed."},
    participantNames: {
      type: "array",
      items: {type: "string"},
      description: "Names of the cafe's casts (staff) who host or take part in the event, copied exactly as printed.",
    },
    hasLivePerformance: {
      type: "boolean",
      description: "true only when the image says there is a live show, concert, stage or singing performance (라이브, 공연, ライブ, LIVE).",
    },
  },
  required: ["isEvent", "title", "content", "startDate", "endDate", "participantNames", "hasLivePerformance"],
  additionalProperties: false,
};

const EVENT_EXTRACTION_PROMPT = `You read event announcement images that concept cafes and maid cafes post on Instagram, and turn them into an event post for ConCafe, a Korean app where fans follow these cafes. A cafe admin reviews and edits your draft before it is published.

Write the title and content in Korean, even when the image is in Japanese or English. Keep cast names, cafe names, menu names and hashtags in their original spelling.

Title: a short headline that says what the event is, e.g. "레이 생탄 이벤트", "할로윈 코스튬 위크", "모찌 특별 게스트 출근".

Content: a friendly, faithful announcement that a fan can act on. Include every concrete detail printed on the image: dates and times, who appears, special menus or goods with prices, entry conditions, reservation or ticket instructions, benefits such as cheki or bonuses, and cautions. Use short paragraphs or "-" bullet lines; no markdown headings, bold or tables. Do not add facts that are not on the image, and do not invent prices, times or rules.

Dates: write YYYY-MM-DD. When the year is not printed, pick the year that puts the event closest to the reference date. A one-day event has the same startDate and endDate. Leave both empty when no date is printed.

Participants: list the names of cast members who host or appear in the event. The registered cast roster is given so you can recognise names; copy names as printed rather than replacing them with roster names. Do not list the cafe name, guests from other shops' logos, or decorative words.

If the image is not an event announcement, set isEvent to false and leave the other fields empty. A work schedule (출근표 / 出勤表: a grid or list of days with cast names and shift times) is not an event announcement even when it has a memo box mentioning events, and neither is a regular menu board or an unrelated photo.`;

async function callModelForEvent(
  image: {mediaType: SupportedImageMediaType; base64: string},
  cafeName: string,
  castNames: string[],
  referenceDate: Date
): Promise<ExtractedEvent> {
  // 함수 타임아웃(300초) 안에 응답하도록 재시도 없이 240초로 제한한다
  const client = new OpenAI({apiKey: OPENAI_API_KEY_SECRET.value(), timeout: 240_000, maxRetries: 0});
  const rosterLine = castNames.length > 0 ? castNames.join(", ") : "(none registered yet)";
  const response = await client.responses.create({
    model: SCHEDULE_IMPORT_MODEL,
    max_output_tokens: 16000,
    reasoning: {effort: "medium"},
    instructions: EVENT_EXTRACTION_PROMPT,
    text: {
      format: {type: "json_schema", name: "cafe_event", schema: EVENT_EXTRACTION_SCHEMA, strict: true},
    },
    input: [
      {
        role: "user",
        content: [
          {type: "input_image", detail: "high", image_url: `data:${image.mediaType};base64,${image.base64}`},
          {
            type: "input_text",
            text: `Cafe: ${cafeName}\nReference date (the day this image was uploaded, KST): ${toDateKey(referenceDate)}\nRegistered cast roster: ${rosterLine}\n\nWrite the event post for this image.`,
          },
        ],
      },
    ],
  });

  const refusal = response.output
    .flatMap((item) => item.type === "message" ? item.content : [])
    .find((content) => content.type === "refusal");
  if (refusal != null) {
    logger.warn("eventImport extraction refused.", {refusal: refusal.refusal});
    throw new ScheduleImportError(422, "extraction_refused");
  }
  if (response.status === "incomplete") {
    logger.warn("eventImport extraction incomplete.", {reason: response.incomplete_details?.reason});
    throw new ScheduleImportError(422, response.incomplete_details?.reason === "content_filter" ? "extraction_refused" : "extraction_truncated");
  }
  const text = response.output_text;
  try {
    return JSON.parse(text) as ExtractedEvent;
  } catch (error) {
    logger.error("eventImport extraction returned invalid JSON.", {error, text: text.slice(0, 500)});
    throw new ScheduleImportError(502, "extraction_invalid");
  }
}

// 앱의 이벤트 기간 표기(YYYY.MM.DD)로 바꾸고, 읽을 수 없거나 범위를 벗어난 날짜는 비운다
function toEventDate(value: string, referenceDate: Date, warnings: string[]): string {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return "";
  }
  const date = parseDateKey(trimmed);
  if (date == null || date < addDays(referenceDate, -EVENT_DATE_WINDOW_PAST_DAYS) || date > addDays(referenceDate, EVENT_DATE_WINDOW_FUTURE_DAYS)) {
    warnings.push(`date_out_of_range:${trimmed}`);
    return "";
  }
  return toDateKey(date).replace(/-/g, ".");
}

export const extractCafeEventImage = onRequest(
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
      const {cafeSnapshot} = await requireCafeManager(request, cafeId);
      const [image, casts] = await Promise.all([
        downloadStorageImage(imageUrl, `${EVENT_IMAGE_STORAGE_FOLDER}/${cafeId}/`),
        loadCafeCasts(cafeId),
      ]);
      const referenceDate = kstToday();
      const cafeName = asNonBlankString(cafeSnapshot.get("name")) ?? "";
      const extracted = await callModelForEvent(image, cafeName, casts.map((cast) => cast.name), referenceDate);
      if (!extracted.isEvent) {
        throw new ScheduleImportError(422, "not_event_image");
      }

      const warnings: string[] = [];
      let startDate = toEventDate(extracted.startDate, referenceDate, warnings);
      let endDate = toEventDate(extracted.endDate, referenceDate, warnings);
      if (startDate.length === 0 && endDate.length > 0) {
        startDate = endDate;
      }
      if (endDate.length === 0 && startDate.length > 0) {
        endDate = startDate;
      }
      if (startDate > endDate) {
        warnings.push(`reversed_period:${startDate}-${endDate}`);
        [startDate, endDate] = [endDate, startDate];
      }

      const seenNames = new Set<string>();
      const participants = extracted.participantNames
        .map((name) => name.trim())
        .filter((name) => {
          if (name.length === 0 || seenNames.has(name)) {
            return false;
          }
          seenNames.add(name);
          return true;
        })
        .map((name) => {
          const {cast, matchType} = matchCast(name, casts);
          return {name, castId: cast?.id ?? null, matchedCastName: cast?.name ?? null, matchType};
        });

      logger.info("eventImport draft created.", {cafeId, participantCount: participants.length, hasDates: startDate.length > 0});
      response.status(200).json({
        cafeId,
        imageUrl,
        title: extracted.title.trim().slice(0, MAX_EVENT_TITLE_LENGTH),
        content: extracted.content.trim().slice(0, MAX_EVENT_CONTENT_LENGTH),
        startDate,
        endDate,
        participants,
        hasLivePerformance: extracted.hasLivePerformance,
        casts: casts.map((cast) => ({id: cast.id, name: cast.name, inactive: cast.inactive})),
        warnings,
      });
    } catch (error) {
      sendError(response, error, "extractCafeEventImage");
    }
  }
);
