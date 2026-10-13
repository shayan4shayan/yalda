package com.shdarv.yalda.db

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal const val CREATE_AUDIT_TABLE = """
CREATE TABLE IF NOT EXISTS `audit_events` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `eventKey` TEXT NOT NULL,
    `action` TEXT NOT NULL,
    `occurredAt` INTEGER NOT NULL,
    `profileId` INTEGER,
    `categoryId` INTEGER,
    `wordId` INTEGER,
    `categoryName` TEXT,
    `wordText` TEXT,
    `meaning` TEXT,
    `sessionId` TEXT,
    `questionId` TEXT,
    `quizType` TEXT,
    `answer` TEXT,
    `isCorrect` INTEGER,
    `attemptNumber` INTEGER,
    `notificationId` TEXT,
    `scheduledAt` INTEGER,
    `isSample` INTEGER NOT NULL,
    `detailsJson` TEXT
)
"""

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_AUDIT_TABLE)
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_audit_events_eventKey ON audit_events(eventKey)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_profileId_occurredAt ON audit_events(profileId, occurredAt)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_profileId_categoryId_occurredAt ON audit_events(profileId, categoryId, occurredAt)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_profileId_wordId_occurredAt ON audit_events(profileId, wordId, occurredAt)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_sessionId_questionId ON audit_events(sessionId, questionId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_action_occurredAt ON audit_events(action, occurredAt)")
        installAuditTriggers(connection)
    }
}

val AUDIT_CALLBACK = object : RoomDatabase.Callback() {
    override fun onOpen(connection: SQLiteConnection) {
        installAuditTriggers(connection)
    }
}

/** Triggers cover every write path, including imports, cascades and bulk operations. */
internal fun installAuditTriggers(connection: SQLiteConnection) {
    AUDIT_TRIGGERS.forEach(connection::execSQL)
}

internal val AUDIT_TRIGGERS = listOf(
    """
CREATE TRIGGER IF NOT EXISTS audit_profiles_insert
AFTER INSERT ON profiles
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'PROFILE_CREATED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        NEW.id, NULL, NULL, NULL, NULL, NULL, 0, json_object('before', NULL, 'after', json_object('name', NEW.name, 'sourceLanguage', NEW.sourceLanguage, 'targetLanguage', NEW.targetLanguage)));
END
    """,
    """
CREATE TRIGGER IF NOT EXISTS audit_profiles_update
AFTER UPDATE ON profiles
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'PROFILE_UPDATED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        NEW.id, NULL, NULL, NULL, NULL, NULL, 0, json_object('before', json_object('name', OLD.name, 'sourceLanguage', OLD.sourceLanguage, 'targetLanguage', OLD.targetLanguage), 'after', json_object('name', NEW.name, 'sourceLanguage', NEW.sourceLanguage, 'targetLanguage', NEW.targetLanguage)));
END
    """,
    """
CREATE TRIGGER IF NOT EXISTS audit_profiles_delete
AFTER DELETE ON profiles
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'PROFILE_DELETED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        OLD.id, NULL, NULL, NULL, NULL, NULL, 0, json_object('before', json_object('name', OLD.name, 'sourceLanguage', OLD.sourceLanguage, 'targetLanguage', OLD.targetLanguage), 'after', NULL));
END
    """,
    """
CREATE TRIGGER IF NOT EXISTS audit_categories_insert
AFTER INSERT ON categories
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'CATEGORY_CREATED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        NEW.profileIdFk, NEW.id, NULL, NEW.name, NULL, NULL, 0, json_object('before', NULL, 'after', json_object('name', NEW.name, 'profileIdFk', NEW.profileIdFk, 'datasetPackId', NEW.datasetPackId, 'isUserModified', NEW.isUserModified)));
END
    """,
    """
CREATE TRIGGER IF NOT EXISTS audit_categories_update
AFTER UPDATE ON categories
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'CATEGORY_UPDATED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        NEW.profileIdFk, NEW.id, NULL, NEW.name, NULL, NULL, 0, json_object('before', json_object('name', OLD.name, 'profileIdFk', OLD.profileIdFk, 'datasetPackId', OLD.datasetPackId, 'isUserModified', OLD.isUserModified), 'after', json_object('name', NEW.name, 'profileIdFk', NEW.profileIdFk, 'datasetPackId', NEW.datasetPackId, 'isUserModified', NEW.isUserModified)));
END
    """,
    """
CREATE TRIGGER IF NOT EXISTS audit_categories_delete
AFTER DELETE ON categories
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'CATEGORY_DELETED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        OLD.profileIdFk, OLD.id, NULL, OLD.name, NULL, NULL, 0, json_object('before', json_object('name', OLD.name, 'profileIdFk', OLD.profileIdFk, 'datasetPackId', OLD.datasetPackId, 'isUserModified', OLD.isUserModified), 'after', NULL));
END
    """,
    """
CREATE TRIGGER IF NOT EXISTS audit_word_entries_insert
AFTER INSERT ON word_entries
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'WORD_CREATED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        NEW.profileIdFk, NEW.categoryIdFk, NEW.wordId, (SELECT name FROM categories WHERE id = NEW.categoryIdFk), NEW.word, NEW.meaning, 0, json_object('before', NULL, 'after', json_object('word', NEW.word, 'meaning', NEW.meaning, 'description', NEW.description, 'categoryIdFk', NEW.categoryIdFk, 'profileIdFk', NEW.profileIdFk, 'datasetPackId', NEW.datasetPackId, 'isUserModified', NEW.isUserModified)));
END
    """,
    """
CREATE TRIGGER IF NOT EXISTS audit_word_entries_update
AFTER UPDATE ON word_entries
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'WORD_UPDATED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        NEW.profileIdFk, NEW.categoryIdFk, NEW.wordId, (SELECT name FROM categories WHERE id = NEW.categoryIdFk), NEW.word, NEW.meaning, 0, json_object('before', json_object('word', OLD.word, 'meaning', OLD.meaning, 'description', OLD.description, 'categoryIdFk', OLD.categoryIdFk, 'profileIdFk', OLD.profileIdFk, 'datasetPackId', OLD.datasetPackId, 'isUserModified', OLD.isUserModified), 'after', json_object('word', NEW.word, 'meaning', NEW.meaning, 'description', NEW.description, 'categoryIdFk', NEW.categoryIdFk, 'profileIdFk', NEW.profileIdFk, 'datasetPackId', NEW.datasetPackId, 'isUserModified', NEW.isUserModified)));
END
    """,
    """
CREATE TRIGGER IF NOT EXISTS audit_word_entries_delete
AFTER DELETE ON word_entries
BEGIN
    INSERT INTO audit_events (eventKey, action, occurredAt, profileId, categoryId, wordId,
        categoryName, wordText, meaning, isSample, detailsJson)
    VALUES (lower(hex(randomblob(16))), 'WORD_DELETED',
        CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER),
        OLD.profileIdFk, OLD.categoryIdFk, OLD.wordId, (SELECT name FROM categories WHERE id = OLD.categoryIdFk), OLD.word, OLD.meaning, 0, json_object('before', json_object('word', OLD.word, 'meaning', OLD.meaning, 'description', OLD.description, 'categoryIdFk', OLD.categoryIdFk, 'profileIdFk', OLD.profileIdFk, 'datasetPackId', OLD.datasetPackId, 'isUserModified', OLD.isUserModified), 'after', NULL));
END
    """
)
