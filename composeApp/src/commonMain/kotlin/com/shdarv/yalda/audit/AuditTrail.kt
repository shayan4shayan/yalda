package com.shdarv.yalda.audit

import com.shdarv.yalda.db.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-lifetime ordered writer; navigation does not cancel pending history writes. */
object AuditTrail {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queue = Channel<AuditEvent>(Channel.UNLIMITED)
    private val _writeFailed = MutableStateFlow(false)
    val writeFailed = _writeFailed.asStateFlow()

    init {
        scope.launch {
            for (event in queue) {
                // Retain the event and retry transient storage errors using the same key.
                var delayMillis = 250L
                while (isActive) {
                    try {
                        database.get().auditDao().append(event)
                        _writeFailed.value = false
                        break
                    } catch (canceled: CancellationException) {
                        throw canceled
                    } catch (error: Exception) {
                        _writeFailed.value = true
                        println("Audit write failed: ${error.message}")
                        delay(delayMillis)
                        delayMillis = (delayMillis * 2).coerceAtMost(30_000)
                    }
                }
            }
        }
    }

    fun record(event: AuditEvent) {
        check(queue.trySend(event).isSuccess)
    }
}

/** One practice session per profile/category/mode. Each displayed question has its own ID. */
class QuizAuditSession(
    private val profileId: Long,
    private val categoryId: Long?,
    private val quizType: String,
    private val categoryNames: Map<Long, String>,
    private val emit: (AuditEvent) -> Unit = AuditTrail::record,
    private val now: () -> Long = ::auditNow,
    private val newId: () -> String = ::newAuditId
) {
    val sessionId = newId()
    private var started = false
    private var closed = false
    private var current: WordEntry? = null
    private var questionId: String? = null
    private var attemptNumber = 0
    private var lastAnswer: String? = null
    private var solved = false
    val isSolved: Boolean get() = solved

    fun showQuestion(word: WordEntry) {
        if (closed) return
        require(word.profileIdFk == profileId)
        if (!started) {
            started = true
            emit(sessionEvent(AuditAction.QUIZ_STARTED))
        }
        skipUnanswered()
        current = word
        questionId = newId()
        attemptNumber = 0
        lastAnswer = null
        solved = false
        emit(wordEvent(AuditAction.QUESTION_SHOWN))
    }

    /** Consecutive identical submissions and submissions after success aren't new attempts. */
    fun answer(word: WordEntry, answer: String, correct: Boolean): Boolean {
        if (closed || current?.wordId != word.wordId || solved) return false
        val normalized = answer.trim()
        if (normalized.isEmpty() || normalized == lastAnswer) return false
        lastAnswer = normalized
        attemptNumber++
        solved = correct
        emit(wordEvent(AuditAction.QUIZ_ANSWERED).copy(
            answer = answer, isCorrect = correct, attemptNumber = attemptNumber
        ))
        return true
    }

    fun close() {
        if (closed) return
        closed = true
        if (started) {
            skipUnanswered()
            emit(sessionEvent(AuditAction.QUIZ_ENDED))
        }
    }

    private fun skipUnanswered() {
        if (current != null && attemptNumber == 0) emit(wordEvent(AuditAction.QUESTION_SKIPPED))
    }

    private fun sessionEvent(action: String) = AuditEvent(
        eventKey = newId(), action = action, occurredAt = now(), profileId = profileId,
        categoryId = categoryId, categoryName = categoryNames[categoryId],
        sessionId = sessionId, quizType = quizType
    )

    private fun wordEvent(action: String): AuditEvent {
        val word = checkNotNull(current)
        return sessionEvent(action).copy(
            categoryId = word.categoryIdFk, categoryName = categoryNames[word.categoryIdFk],
            wordId = word.wordId, wordText = word.word, meaning = word.meaning,
            questionId = questionId
        )
    }
}

suspend fun wordAlertEvent(
    action: String, word: WordEntry, notificationId: String,
    isSample: Boolean = false, scheduledAt: Long? = null
): AuditEvent = AuditEvent(
    eventKey = "$notificationId:$action", action = action,
    profileId = word.profileIdFk, categoryId = word.categoryIdFk, wordId = word.wordId,
    categoryName = database.get().categoryDao().getCategoryById(word.categoryIdFk)?.name,
    wordText = word.word, meaning = word.meaning, notificationId = notificationId,
    isSample = isSample, scheduledAt = scheduledAt
)
