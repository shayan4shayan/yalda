package com.shdarv.yalda

import com.shdarv.yalda.audit.QuizAuditSession
import com.shdarv.yalda.db.AuditAction
import com.shdarv.yalda.db.AuditEvent
import com.shdarv.yalda.db.WordEntry
import kotlin.test.*

class QuizAuditSessionTest {
    private val word = WordEntry(7, "hello", "سلام", null, 3, 1)
    private val events = mutableListOf<AuditEvent>()
    private var sequence = 0
    private fun session() = QuizAuditSession(
        profileId = 1, categoryId = null, quizType = "Spelling", categoryNames = mapOf(3L to "Basics"),
        emit = events::add, now = { 1000L }, newId = { "event-${sequence++}" }
    )

    @Test fun retriesKeepQuestionIdentityAndFirstAnswerAccuracy() {
        val session = session()
        session.showQuestion(word)
        assertTrue(session.answer(word, "helo", false))
        assertFalse(session.answer(word, "helo", false))
        assertTrue(session.answer(word, "hello", true))
        assertFalse(session.answer(word, "hello", true))
        session.close()
        session.close()
        val answers = events.filter { it.action == AuditAction.QUIZ_ANSWERED }
        assertEquals(listOf(false, true), answers.map { it.isCorrect })
        assertEquals(listOf(1, 2), answers.map { it.attemptNumber })
        assertEquals(1, answers.map { it.questionId }.distinct().size)
        assertEquals(1, events.count { it.action == AuditAction.QUIZ_STARTED })
        assertEquals(1, events.count { it.action == AuditAction.QUIZ_ENDED })
        assertEquals(0, events.count { it.action == AuditAction.QUESTION_SKIPPED })
        answers.forEach {
            assertEquals("Basics", it.categoryName)
            assertEquals("hello", it.wordText)
            assertEquals("سلام", it.meaning)
            assertEquals(3L, it.categoryId)
            assertEquals(1L, it.profileId)
        }
    }

    @Test fun repeatedWordIsANewQuestionAndUnansweredQuestionsAreSkipped() {
        val session = session()
        session.showQuestion(word)
        session.showQuestion(word)
        session.close()
        assertEquals(2, events.count { it.action == AuditAction.QUESTION_SKIPPED })
        assertEquals(2, events.filter { it.action == AuditAction.QUESTION_SHOWN }.map { it.questionId }.distinct().size)
        assertFalse(session.answer(word, "hello", true))
    }

    @Test fun emptySessionsAndBlankSubmissionsDoNotInflateStatistics() {
        session().close()
        assertTrue(events.isEmpty())
        val session = session()
        session.showQuestion(word)
        assertFalse(session.answer(word, "  ", false))
        assertEquals(0, events.count { it.action == AuditAction.QUIZ_ANSWERED })
    }

    @Test fun otherProfileOrStaleWordCannotBeAttributedToCurrentQuestion() {
        val session = session()
        assertFailsWith<IllegalArgumentException> { session.showQuestion(word.copy(profileIdFk = 2)) }
        session.showQuestion(word)
        assertFalse(session.answer(word.copy(wordId = 9), "hello", true))
    }
}
