"""Execute the production migration, triggers and query fragments using real SQLite."""
import json
import re
import sqlite3
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB = ROOT / 'composeApp/src/commonMain/kotlin/com/shdarv/yalda/db'
MIGRATION = (DB / 'AuditMigration.kt').read_text()
DAO = (DB / 'AuditDao.kt').read_text()


def constant(source, name):
    return re.search(r'const val ' + name + r' = """(.*?)"""', source, re.S)[1]


class AuditSqlTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(':memory:')
        self.db.row_factory = sqlite3.Row
        self.db.execute('PRAGMA foreign_keys = ON')
        # Use the actual exported v2 schema, populated before migration.
        schema = json.loads((ROOT / 'composeApp/schemas/com.shdarv.yalda.db.AppDatabase/2.json').read_text())
        for entity in schema['database']['entities']:
            self.db.execute(entity['createSql'].replace('${TABLE_NAME}', entity['tableName']))
        self.db.execute("INSERT INTO profiles VALUES (1, 'English', 'en', 'fa')")
        self.db.execute("INSERT INTO profiles VALUES (2, 'Other', 'en', 'de')")
        self.db.execute("INSERT INTO categories VALUES (3, 'Basics', 1, NULL, NULL, NULL, 1)")
        self.db.execute("INSERT INTO word_entries VALUES (7, 'hello', 'سلام', NULL, 3, 1, NULL, NULL, NULL, 1)")
        self.db.commit()
        self.db.execute(constant(MIGRATION, 'CREATE_AUDIT_TABLE'))
        for sql in re.findall(r'connection.execSQL\("(CREATE (?:UNIQUE )?INDEX.*?)"\)', MIGRATION):
            self.db.execute(sql)
        for trigger in re.findall(r'"""\s*(CREATE TRIGGER.*?)\s*"""', MIGRATION, re.S):
            self.db.executescript(trigger)

    def tearDown(self):
        self.db.close()

    def append(self, key, action, profile=1, category=3, word=7, time=1000, correct=None, attempt=None, sample=0):
        self.db.execute('''INSERT OR IGNORE INTO audit_events
            (eventKey, action, occurredAt, profileId, categoryId, wordId, sessionId,
             questionId, isCorrect, attemptNumber, isSample)
            VALUES (?, ?, ?, ?, ?, ?, 'quiz-1', 'question-1', ?, ?, ?)''',
            (key, action, time, profile, category, word, correct, attempt, sample))

    def stats(self, **kwargs):
        params = dict(profileId=None, categoryId=None, wordId=None, fromInclusive=0, untilExclusive=2**63-1)
        params.update(kwargs)
        return self.db.execute('SELECT ' + constant(DAO, 'AUDIT_TOTALS') +
            ' FROM audit_events WHERE ' + constant(DAO, 'AUDIT_SCOPE'), params).fetchone()

    def test_migration_preserves_data_and_does_not_invent_history(self):
        self.assertEqual('سلام', self.db.execute('SELECT meaning FROM word_entries WHERE wordId=7').fetchone()[0])
        self.assertEqual(0, self.stats()['totalEvents'])
        self.assertEqual(0, self.stats()['correctAnswers'])
        self.assertIsNone(self.stats()['lastActivityAt'])
        self.assertEqual([], list(self.db.execute('PRAGMA foreign_key_check')))

    def test_scopes_dates_retries_and_samples(self):
        self.append('a', 'QUIZ_ANSWERED', correct=0, attempt=1)
        self.append('b', 'QUIZ_ANSWERED', correct=1, attempt=2)
        self.append('c', 'QUIZ_ANSWERED', profile=2, category=4, word=8, correct=1, attempt=1)
        self.append('d', 'ALERT_SCHEDULED')
        self.append('e', 'ALERT_POSTED')
        self.append('sample', 'ALERT_POSTED', sample=1)
        self.append('delivery', 'ALERT_DELIVERED')
        self.append('delivery', 'ALERT_DELIVERED')  # Reconciliation is idempotent.
        row = self.stats(profileId=1, categoryId=3, wordId=7)
        self.assertEqual(2, row['answerAttempts'])
        self.assertEqual(1, row['correctAnswers'])
        self.assertEqual(1, row['incorrectAnswers'])
        self.assertEqual(1, row['firstAnswers'])
        self.assertEqual(0, row['firstCorrectAnswers'])
        self.assertEqual(1, row['alertsPosted'])
        self.assertEqual(1, row['alertsDelivered'])
        self.assertEqual(1, row['alertsScheduled'])
        self.assertEqual(3, self.stats()['answerAttempts'])
        self.assertEqual(0, self.stats(untilExclusive=1000)['totalEvents'])
        self.assertEqual(row['totalEvents'], self.stats(profileId=1, fromInclusive=1000)['totalEvents'])
        self.assertEqual(0, self.stats(profileId=2, wordId=7)['totalEvents'])

    def test_mutations_are_atomic_and_history_survives_cascade_deletion(self):
        self.append('answer', 'QUIZ_ANSWERED', correct=1, attempt=1)
        self.db.commit()
        self.db.execute("UPDATE word_entries SET meaning='درود' WHERE wordId=7")
        row = self.db.execute("SELECT * FROM audit_events WHERE action='WORD_UPDATED'").fetchone()
        details = json.loads(row['detailsJson'])
        self.assertEqual('سلام', details['before']['meaning'])
        self.assertEqual('درود', details['after']['meaning'])
        self.db.rollback()
        self.assertEqual(0, self.db.execute("SELECT COUNT(*) FROM audit_events WHERE action='WORD_UPDATED'").fetchone()[0])
        self.db.execute('DELETE FROM profiles WHERE id=1')
        self.assertEqual(0, self.db.execute('SELECT COUNT(*) FROM word_entries').fetchone()[0])
        self.assertEqual(1, self.stats(profileId=1, wordId=7)['correctAnswers'])
        actions = [r[0] for r in self.db.execute('SELECT action FROM audit_events')]
        for action in ['WORD_DELETED', 'CATEGORY_DELETED', 'PROFILE_DELETED']:
            self.assertIn(action, actions)
        self.assertEqual([], list(self.db.execute('PRAGMA foreign_key_check')))

    def test_daily_statistics_and_scoped_history(self):
        self.append('first', 'QUIZ_ANSWERED', time=1000, correct=0, attempt=1)
        self.append('retry', 'QUIZ_ANSWERED', time=2000, correct=1, attempt=2)
        self.append('next-day', 'ALERT_POSTED', time=86_400_000)
        self.append('sample', 'ALERT_POSTED', time=86_400_000, sample=1)
        self.append('other-profile', 'QUIZ_ANSWERED', profile=2, category=4, word=8, correct=1, attempt=1)
        daily = re.search(r'@Query\("""(.*?)""" \+ AUDIT_SCOPE \+ " GROUP BY utcDay ORDER BY utcDay"\)', DAO, re.S)[1]
        params = dict(profileId=1, categoryId=None, wordId=None, fromInclusive=0, untilExclusive=2**63-1)
        rows = list(self.db.execute(daily + constant(DAO, 'AUDIT_SCOPE') + ' GROUP BY utcDay ORDER BY utcDay', params))
        self.assertEqual(['1970-01-01', '1970-01-02'], [r['utcDay'] for r in rows])
        self.assertEqual(2, rows[0]['answerAttempts'])
        self.assertEqual(1, rows[0]['correctAnswers'])
        self.assertEqual(1, rows[1]['alertsPosted'])
        query = ('SELECT * FROM audit_events WHERE ' + constant(DAO, 'AUDIT_SCOPE') +
                 ' AND (:action IS NULL OR action = :action) ORDER BY occurredAt DESC, id DESC LIMIT :limit OFFSET :offset')
        rows = list(self.db.execute(query, dict(params, action='QUIZ_ANSWERED', limit=1, offset=0)))
        self.assertEqual('retry', rows[0]['eventKey'])
        rows = list(self.db.execute(query, dict(params, action='QUIZ_ANSWERED', limit=1, offset=1)))
        self.assertEqual('first', rows[0]['eventKey'])

    def test_migration_matches_room_export(self):
        schema = json.loads((ROOT / 'composeApp/schemas/com.shdarv.yalda.db.AppDatabase/3.json').read_text())
        entity = next(e for e in schema['database']['entities'] if e['tableName'] == 'audit_events')
        other = sqlite3.connect(':memory:')
        other.execute(entity['createSql'].replace('${TABLE_NAME}', 'audit_events'))
        actual = [tuple(r) for r in self.db.execute('PRAGMA table_info(audit_events)')]
        expected = list(other.execute('PRAGMA table_info(audit_events)'))
        self.assertEqual(expected, actual)
        self.assertEqual({i['name'] for i in entity['indices']},
                         {r[1] for r in self.db.execute('PRAGMA index_list(audit_events)')})
        other.close()


if __name__ == '__main__':
    unittest.main()
