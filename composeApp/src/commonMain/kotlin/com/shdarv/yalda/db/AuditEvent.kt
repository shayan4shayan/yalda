package com.shdarv.yalda.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalTime::class)
fun auditNow(): Long = Clock.System.now().toEpochMilliseconds()

@OptIn(ExperimentalUuidApi::class)
fun newAuditId(): String = Uuid.random().toString()

/** Immutable snapshots deliberately have no cascading foreign keys. */
@Entity(tableName = "audit_events", indices = [
    Index(value = ["eventKey"], unique = true),
    Index(value = ["profileId", "occurredAt"], unique = false),
    Index(value = ["profileId", "categoryId", "occurredAt"], unique = false),
    Index(value = ["profileId", "wordId", "occurredAt"], unique = false),
    Index(value = ["sessionId", "questionId"], unique = false),
    Index(value = ["action", "occurredAt"], unique = false)
])
data class AuditEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventKey: String = newAuditId(),
    val action: String,
    val occurredAt: Long = auditNow(),
    val profileId: Long? = null,
    val categoryId: Long? = null,
    val wordId: Long? = null,
    val categoryName: String? = null,
    val wordText: String? = null,
    val meaning: String? = null,
    val sessionId: String? = null,
    val questionId: String? = null,
    val quizType: String? = null,
    val answer: String? = null,
    val isCorrect: Boolean? = null,
    val attemptNumber: Int? = null,
    val notificationId: String? = null,
    val scheduledAt: Long? = null,
    val isSample: Boolean = false,
    val detailsJson: String? = null
)

object AuditAction {
    const val QUIZ_STARTED = "QUIZ_STARTED"
    const val QUIZ_ENDED = "QUIZ_ENDED"
    const val QUESTION_SHOWN = "QUESTION_SHOWN"
    const val QUESTION_SKIPPED = "QUESTION_SKIPPED"
    const val QUIZ_ANSWERED = "QUIZ_ANSWERED"
    const val ALERT_ATTEMPTED = "ALERT_ATTEMPTED"
    const val ALERT_SCHEDULED = "ALERT_SCHEDULED"
    const val ALERT_POSTED = "ALERT_POSTED"
    const val ALERT_DELIVERED = "ALERT_DELIVERED"
    const val ALERT_OPENED = "ALERT_OPENED"
    const val ALERT_BLOCKED = "ALERT_BLOCKED"
    const val ALERT_FAILED = "ALERT_FAILED"
    const val ALERT_CANCELED = "ALERT_CANCELED"
}
