"""Home counters and local-calendar chart queries run against the production schema."""
import os
import time
import unittest
from datetime import datetime
from zoneinfo import ZoneInfo
import test_audit_sql as audit_tests
from test_audit_sql import DB, constant

SOURCE = (DB / 'HomeStatistics.kt').read_text()


class HomeStatisticsSqlTest(unittest.TestCase):
    def setUp(self):
        audit_tests.AuditSqlTest.setUp(self)
        self.previous_tz = os.environ.get('TZ')
        os.environ['TZ'] = 'America/Toronto'
        time.tzset()

    def tearDown(self):
        audit_tests.AuditSqlTest.tearDown(self)
        if self.previous_tz is None:
            os.environ.pop('TZ', None)
        else:
            os.environ['TZ'] = self.previous_tz
        time.tzset()

    append = audit_tests.AuditSqlTest.append

    @staticmethod
    def timestamp(date):
        return int(datetime.fromisoformat(date).replace(tzinfo=ZoneInfo('America/Toronto')).timestamp() * 1000)

    def totals(self, profile=1):
        return self.db.execute(constant(SOURCE, 'HOME_STATISTICS_SQL'), dict(profileId=profile)).fetchone()

    def days(self, now='2026-03-10T12:00:00'):
        return list(self.db.execute(constant(SOURCE, 'HOME_ACTIVITY_SQL'), dict(profileId=1, nowMillis=self.timestamp(now))))

    def test_empty_history_zero_fills_seven_days_including_today(self):
        self.assertEqual((0, 0, 0, 0), tuple(self.totals()))
        rows = self.days()
        self.assertEqual(['2026-03-04', '2026-03-05', '2026-03-06', '2026-03-07',
                          '2026-03-08', '2026-03-09', '2026-03-10'], [r['localDate'] for r in rows])
        self.assertTrue(all(r['questionsAnswered'] == r['remindersSent'] == 0 for r in rows))

    def test_first_answers_only_and_reminder_outcomes_are_deduplicated(self):
        stamp = self.timestamp('2026-03-09T12:00:00')
        self.append('wrong', 'QUIZ_ANSWERED', correct=0, attempt=1, time=stamp)
        self.append('retry', 'QUIZ_ANSWERED', correct=1, attempt=2, time=stamp)
        self.append('right', 'QUIZ_ANSWERED', correct=1, attempt=1, time=stamp)
        for action in ['ALERT_POSTED', 'ALERT_DELIVERED', 'ALERT_SCHEDULED', 'ALERT_BLOCKED', 'ALERT_FAILED']:
            self.append(action, action, time=stamp)
        self.db.execute("UPDATE audit_events SET notificationId='same-reminder' WHERE action LIKE 'ALERT_%'")
        self.append('sample', 'ALERT_POSTED', sample=1, time=stamp)
        self.append('other-profile', 'QUIZ_ANSWERED', profile=2, correct=1, attempt=1, time=stamp)
        self.assertEqual((1, 2, 1, 1), tuple(self.totals()))
        row = next(r for r in self.days() if r['localDate'] == '2026-03-09')
        self.assertEqual(2, row['questionsAnswered'])
        self.assertEqual(1, row['remindersSent'])
        self.assertEqual((0, 1, 1, 0), tuple(self.totals(profile=2)))

    def test_calendar_boundaries_across_daylight_saving_and_reactive_updates(self):
        for key, date in [('before', '2026-03-03T23:59:59'), ('start', '2026-03-04T00:00:00'),
                          ('dst-before', '2026-03-08T01:59:59'), ('dst-after', '2026-03-08T03:00:00'),
                          ('late', '2026-03-09T23:59:59'), ('today', '2026-03-10T00:00:00'),
                          ('after-query-clock', '2026-03-10T12:00:01'), ('tomorrow', '2026-03-11T00:00:00')]:
            self.append(key, 'QUIZ_ANSWERED', correct=1, attempt=1, time=self.timestamp(date))
        rows = {r['localDate']: r['questionsAnswered'] for r in self.days()}
        self.assertEqual(1, rows['2026-03-04'])
        self.assertEqual(2, rows['2026-03-08'])
        self.assertEqual(1, rows['2026-03-09'])
        self.assertEqual(2, rows['2026-03-10'])
        self.assertEqual(6, sum(rows.values()))

    def test_same_reminder_observed_on_two_dates_is_counted_on_first_date_only(self):
        self.append('post', 'ALERT_POSTED', time=self.timestamp('2026-03-09T23:59:59'))
        self.append('delivery', 'ALERT_DELIVERED', time=self.timestamp('2026-03-10T00:00:00'))
        self.db.execute("UPDATE audit_events SET notificationId='one-reminder'")
        self.assertEqual(1, self.totals()['remindersSent'])
        rows = self.days()
        self.assertEqual(1, sum(r['remindersSent'] for r in rows))
        self.assertEqual(0, rows[-1]['remindersSent'])
