package com.hhp227.concafe.domain.model

enum class CastAttendanceStatus {
    UPCOMING,   // 출근 예정 - 오늘 근무일, 근무 시작 전
    ON_SHIFT,   // 출근 중 - 근무 시간 중
    COMPLETED,  // 근무 완료 - 오늘 근무일, 근무 종료 후
    OFF         // 비근무 - 오늘 휴무 또는 휴가
}
