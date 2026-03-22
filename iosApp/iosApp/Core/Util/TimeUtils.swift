//
//  TimeUtils.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/18.
//

import Foundation

final class TimeUtils {
    private static let weekdayLabels = ["월", "화", "수", "목", "금", "토", "일"]

    static func weekdayLabel(fromIsoDate date: String) -> String? {
        guard let dayOfWeekIndex = dayOfWeekIndex(fromIsoDate: date),
              weekdayLabels.indices.contains(dayOfWeekIndex) else {
            return nil
        }
        return weekdayLabels[dayOfWeekIndex]
    }

    static func epochDay(fromIsoDate value: String) -> Int? {
        let parts = value.split(separator: "-")
        guard parts.count == 3,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              let day = Int(parts[2]),
              (1...12).contains(month),
              (1...31).contains(day) else {
            return nil
        }

        let adjustedYear = year - (month <= 2 ? 1 : 0)
        let era = adjustedYear >= 0 ? adjustedYear / 400 : (adjustedYear - 399) / 400
        let yearOfEra = adjustedYear - era * 400
        let adjustedMonth = month + (month > 2 ? -3 : 9)
        let dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
        let dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146097 + dayOfEra - 719468
    }

    static func relativeVisitedLabel(
        visitedAt: String,
        visitedLabel: String,
        referenceDate: String
    ) -> String {
        guard let referenceEpochDay = epochDay(fromIsoDate: referenceDate),
              let visitedEpochDay = epochDay(fromIsoDate: String(visitedAt.prefix(10))) else {
            return visitedLabel
        }

        let daysAgo = referenceEpochDay - visitedEpochDay
        switch daysAgo {
        case ..<0:
            return visitedLabel
        case 0:
            return "오늘"
        case 1:
            return "어제"
        default:
            return "\(daysAgo)일 전"
        }
    }

    static func makeVisitedAtString(
        date: Date,
        time: Date,
        fallback: String = "2026-03-09T15:00:00Z"
    ) -> String {
        let calendar = Calendar.current
        let dateComponents = calendar.dateComponents([.year, .month, .day], from: date)
        let timeComponents = calendar.dateComponents([.hour, .minute], from: time)

        guard let year = dateComponents.year,
              let month = dateComponents.month,
              let day = dateComponents.day,
              let hour = timeComponents.hour,
              let minute = timeComponents.minute else {
            return fallback
        }

        return String(format: "%04d-%02d-%02dT%02d:%02d:00Z", year, month, day, hour, minute)
    }

    static func formatHourMinute(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar.current
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }

    static func formatHourMinute(hour: Int, minute: Int) -> String {
        String(format: "%02d:%02d", hour, minute)
    }

    static func parseHourMinute(
        _ value: String,
        defaultHour: Int = 10,
        defaultMinute: Int = 0
    ) -> Date {
        let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines)
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "H:mm"
        if let parsed = formatter.date(from: normalized) {
            return parsed
        }

        var components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        components.hour = defaultHour
        components.minute = defaultMinute
        components.second = 0
        return Calendar.current.date(from: components) ?? Date()
    }

    static func extractNormalizedHourMinuteList(from value: String) -> [String] {
        let pattern = #"(\d{1,2}):(\d{2})"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        let range = NSRange(location: 0, length: value.utf16.count)
        let matches = regex.matches(in: value, range: range)
        return matches.compactMap { match in
            guard
                let hourRange = Range(match.range(at: 1), in: value),
                let minuteRange = Range(match.range(at: 2), in: value),
                let hour = Int(value[hourRange]),
                let minute = Int(value[minuteRange]),
                (0...23).contains(hour),
                (0...59).contains(minute)
            else {
                return nil
            }
            return formatHourMinute(hour: hour, minute: minute)
        }
    }

    static func defaultHalfHourTimeOptions(startHour: Int = 8, endHour: Int = 23) -> [String] {
        var options: [String] = []

        for hour in startHour...endHour {
            options.append(String(format: "%02d:00", hour))
            if hour != endHour {
                options.append(String(format: "%02d:30", hour))
            }
        }
        return options
    }

    static func computeDurationMinutes(start: String, end: String) -> Int {
        max(parseTimeMinutes(end) - parseTimeMinutes(start), 0)
    }

    private static func dayOfWeekIndex(fromIsoDate date: String) -> Int? {
        let parts = date.split(separator: "-")
        guard parts.count == 3,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              let day = Int(parts[2]) else {
            return nil
        }
        return dayOfWeekIndex(year: year, month: month, day: day)
    }

    private static func dayOfWeekIndex(year: Int, month: Int, day: Int) -> Int {
        var adjustedYear = year
        var adjustedMonth = month
        if adjustedMonth < 3 {
            adjustedMonth += 12
            adjustedYear -= 1
        }
        let k = adjustedYear % 100
        let j = adjustedYear / 100
        let h = (day + (13 * (adjustedMonth + 1)) / 5 + k + (k / 4) + (j / 4) + (5 * j)) % 7
        switch h {
        case 2: return 0
        case 3: return 1
        case 4: return 2
        case 5: return 3
        case 6: return 4
        case 0: return 5
        default: return 6
        }
    }

    private static func parseTimeMinutes(_ time: String) -> Int {
        let parts = time.split(separator: ":")
        let hour = Int(parts.first ?? "0") ?? 0
        let minute = Int(parts.dropFirst().first ?? "0") ?? 0
        return hour * 60 + minute
    }
}
