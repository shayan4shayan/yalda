package com.shdarv.yalda.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Time windows are UTC epoch milliseconds, inclusive start and exclusive end. */
internal const val AUDIT_SCOPE = """
(:profileId IS NULL OR profileId = :profileId)
    AND (:categoryId IS NULL OR categoryId = :categoryId)
    AND (:wordId IS NULL OR wordId = :wordId)
    AND occurredAt >= :fromInclusive AND occurredAt < :untilExclusive
"""
internal const val AUDIT_TOTALS = """
COUNT(*) AS totalEvents,
    COALESCE(SUM(action = 'QUIZ_STARTED'), 0) AS quizzesStarted,
    COALESCE(SUM(action = 'QUIZ_ENDED'), 0) AS quizzesEnded,
    COUNT(DISTINCT CASE WHEN action = 'QUIZ_ANSWERED' THEN sessionId END) AS quizzesPracticed,
    COALESCE(SUM(action = 'QUESTION_SHOWN'), 0) AS questionsShown,
    COALESCE(SUM(action = 'QUESTION_SKIPPED'), 0) AS questionsSkipped,
    COALESCE(SUM(action = 'QUIZ_ANSWERED'), 0) AS answerAttempts,
    COALESCE(SUM(action = 'QUIZ_ANSWERED' AND isCorrect = 1), 0) AS correctAnswers,
    COALESCE(SUM(action = 'QUIZ_ANSWERED' AND isCorrect = 0), 0) AS incorrectAnswers,
    COALESCE(SUM(action = 'QUIZ_ANSWERED' AND attemptNumber = 1), 0) AS firstAnswers,
    COALESCE(SUM(action = 'QUIZ_ANSWERED' AND attemptNumber = 1 AND isCorrect = 1), 0) AS firstCorrectAnswers,
    COALESCE(SUM(action = 'ALERT_SCHEDULED' AND isSample = 0), 0) AS alertsScheduled,
    COALESCE(SUM(action = 'ALERT_POSTED' AND isSample = 0), 0) AS alertsPosted,
    COALESCE(SUM(action = 'ALERT_DELIVERED' AND isSample = 0), 0) AS alertsDelivered,
    COALESCE(SUM(action = 'ALERT_BLOCKED' AND isSample = 0), 0) AS alertsBlocked,
    COALESCE(SUM(action = 'ALERT_FAILED' AND isSample = 0), 0) AS alertsFailed,
    MAX(occurredAt) AS lastActivityAt
"""

data class AuditStatistics(
    val totalEvents: Long,
    val quizzesStarted: Long,
    val quizzesEnded: Long,
    val quizzesPracticed: Long,
    val questionsShown: Long,
    val questionsSkipped: Long,
    val answerAttempts: Long,
    val correctAnswers: Long,
    val incorrectAnswers: Long,
    val firstAnswers: Long,
    val firstCorrectAnswers: Long,
    val alertsScheduled: Long,
    val alertsPosted: Long,
    val alertsDelivered: Long,
    val alertsBlocked: Long,
    val alertsFailed: Long,
    val lastActivityAt: Long?
) {
    val accuracy: Double? get() = if (answerAttempts == 0L) null else correctAnswers.toDouble() / answerAttempts
    val firstAnswerAccuracy: Double? get() = if (firstAnswers == 0L) null else firstCorrectAnswers.toDouble() / firstAnswers
}

data class AuditDailyStatistics(
    val utcDay: String,
    val answerAttempts: Long,
    val correctAnswers: Long,
    val alertsPosted: Long,
    val alertsDelivered: Long
)

@Dao
interface AuditDao {
    @Query(HOME_STATISTICS_SQL)
    fun homeStatistics(profileId: Long): Flow<HomeStatistics>

    @Query(HOME_ACTIVITY_SQL)
    fun homeActivity(profileId: Long, nowMillis: Long): Flow<List<HomeActivityDay>>

    // Deterministic event keys make delivery reconciliation and retries idempotent.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun append(event: AuditEvent): Long

    @Query("SELECT * FROM audit_events WHERE eventKey = :key LIMIT 1")
    suspend fun eventByKey(key: String): AuditEvent?

    @Query("SELECT " + AUDIT_TOTALS + " FROM audit_events WHERE " + AUDIT_SCOPE)
    fun statistics(
        profileId: Long? = null, categoryId: Long? = null, wordId: Long? = null,
        fromInclusive: Long = 0, untilExclusive: Long = Long.MAX_VALUE
    ): Flow<AuditStatistics>

    // Explicit scopes let SQLite use the composite profile/category/word indexes.
    @Query("SELECT " + AUDIT_TOTALS + """ FROM audit_events WHERE profileId = :profileId
        AND occurredAt >= :fromInclusive AND occurredAt < :untilExclusive""")
    fun generalStatistics(
        profileId: Long, fromInclusive: Long = 0, untilExclusive: Long = Long.MAX_VALUE
    ): Flow<AuditStatistics>

    @Query("SELECT " + AUDIT_TOTALS + """ FROM audit_events WHERE profileId = :profileId AND categoryId = :categoryId
        AND occurredAt >= :fromInclusive AND occurredAt < :untilExclusive""")
    fun categoryStatistics(
        profileId: Long, categoryId: Long, fromInclusive: Long = 0, untilExclusive: Long = Long.MAX_VALUE
    ): Flow<AuditStatistics>

    @Query("SELECT " + AUDIT_TOTALS + """ FROM audit_events WHERE profileId = :profileId AND wordId = :wordId
        AND occurredAt >= :fromInclusive AND occurredAt < :untilExclusive""")
    fun wordStatistics(
        profileId: Long, wordId: Long, fromInclusive: Long = 0, untilExclusive: Long = Long.MAX_VALUE
    ): Flow<AuditStatistics>

    @Query("SELECT * FROM audit_events WHERE " + AUDIT_SCOPE +
        " AND (:action IS NULL OR action = :action) ORDER BY occurredAt DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun history(
        profileId: Long? = null, categoryId: Long? = null, wordId: Long? = null,
        fromInclusive: Long = 0, untilExclusive: Long = Long.MAX_VALUE,
        action: String? = null, limit: Int = 100, offset: Int = 0
    ): List<AuditEvent>

    @Query("SELECT * FROM audit_events WHERE sessionId = :sessionId ORDER BY occurredAt, id")
    suspend fun quizHistory(sessionId: String): List<AuditEvent>

    @Query("""
        SELECT strftime('%Y-%m-%d', occurredAt / 1000, 'unixepoch') AS utcDay,
            SUM(action = 'QUIZ_ANSWERED') AS answerAttempts,
            SUM(action = 'QUIZ_ANSWERED' AND isCorrect = 1) AS correctAnswers,
            SUM(action = 'ALERT_POSTED' AND isSample = 0) AS alertsPosted,
            SUM(action = 'ALERT_DELIVERED' AND isSample = 0) AS alertsDelivered
        FROM audit_events WHERE """ + AUDIT_SCOPE + " GROUP BY utcDay ORDER BY utcDay")
    fun dailyStatistics(
        profileId: Long? = null, categoryId: Long? = null, wordId: Long? = null,
        fromInclusive: Long = 0, untilExclusive: Long = Long.MAX_VALUE
    ): Flow<List<AuditDailyStatistics>>
}
