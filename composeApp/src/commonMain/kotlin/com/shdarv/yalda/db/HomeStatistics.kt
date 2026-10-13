package com.shdarv.yalda.db

/** First submissions count questions once; reminders are deduplicated across observed outcomes. */
data class HomeStatistics(
    val remindersSent: Long,
    val questionsAnswered: Long,
    val correctAnswers: Long,
    val incorrectAnswers: Long
) {
    val correctFraction: Float?
        get() = if (questionsAnswered == 0L) null else correctAnswers.toFloat() / questionsAnswered
}

data class HomeActivityDay(
    val localDate: String,
    val remindersSent: Long,
    val questionsAnswered: Long
)

internal const val HOME_STATISTICS_SQL = """
SELECT
    COUNT(DISTINCT CASE WHEN action IN ('ALERT_POSTED', 'ALERT_DELIVERED') AND isSample = 0
        THEN COALESCE(notificationId, eventKey) END) AS remindersSent,
    COALESCE(SUM(action = 'QUIZ_ANSWERED' AND attemptNumber = 1), 0) AS questionsAnswered,
    COALESCE(SUM(action = 'QUIZ_ANSWERED' AND attemptNumber = 1 AND isCorrect = 1), 0) AS correctAnswers,
    COALESCE(SUM(action = 'QUIZ_ANSWERED' AND attemptNumber = 1 AND isCorrect = 0), 0) AS incorrectAnswers
FROM audit_events WHERE profileId = :profileId
"""

/** SQLite uses the device time zone, including the offset on each event's date. */
internal const val HOME_ACTIVITY_SQL = """
WITH RECURSIVE days(localDate, dayIndex) AS (
    SELECT date(:nowMillis / 1000, 'unixepoch', 'localtime', '-6 days'), 0
    UNION ALL
    SELECT date(localDate, '+1 day'), dayIndex + 1 FROM days WHERE dayIndex < 6
), events AS (
    SELECT occurredAt, 'question' AS kind FROM audit_events
    WHERE profileId = :profileId AND action = 'QUIZ_ANSWERED' AND attemptNumber = 1
    UNION ALL
    SELECT MIN(occurredAt), 'reminder' AS kind FROM audit_events
    WHERE profileId = :profileId AND action IN ('ALERT_POSTED', 'ALERT_DELIVERED') AND isSample = 0
    GROUP BY COALESCE(notificationId, eventKey)
), activity AS (
    SELECT date(occurredAt / 1000, 'unixepoch', 'localtime') AS localDate,
        SUM(kind = 'reminder') AS remindersSent,
        SUM(kind = 'question') AS questionsAnswered
    FROM events
    WHERE occurredAt >= CAST(strftime('%s', date(:nowMillis / 1000, 'unixepoch', 'localtime', '-6 days'), 'utc') AS INTEGER) * 1000
        AND occurredAt < CAST(strftime('%s', date(:nowMillis / 1000, 'unixepoch', 'localtime', '+1 day'), 'utc') AS INTEGER) * 1000
    GROUP BY localDate
)
SELECT days.localDate, COALESCE(activity.remindersSent, 0) AS remindersSent,
    COALESCE(activity.questionsAnswered, 0) AS questionsAnswered
FROM days LEFT JOIN activity USING (localDate) ORDER BY days.localDate
"""
