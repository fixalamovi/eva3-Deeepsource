package com.example.deepsource

import com.example.deepsource.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TimerTasksTest {
    @Test fun oneHourThirtyThreeMinutesAndThirtySecondsSavesAsOneHourThirtyFourMinutes() {
        assertEquals(5640L, roundedTimerSeconds(5_610_000))
        assertEquals("01:33", minuteClockText(5_610_000))
        assertEquals("01:34", minuteClockText(5_610_000, roundUp = true))
    }

    @Test fun exactMinuteDoesNotReceiveAnExtraMinute() {
        assertEquals(60L, roundedTimerSeconds(60_000))
        assertEquals(3600L, roundedTimerSeconds(3_600_000))
    }

    @Test fun anyPositiveRemainderRoundsUpIncludingBriefSessions() {
        assertEquals(60L, roundedTimerSeconds(1))
        assertEquals(60L, roundedTimerSeconds(30_000))
        assertEquals(120L, roundedTimerSeconds(60_001))
    }

    @Test fun roundingCarriesIntoTheNextHour() {
        assertEquals(3600L, roundedTimerSeconds(3_599_999))
        assertEquals("01:00", minuteClockText(3_599_999, roundUp = true))
    }

    @Test fun zeroNegativeAndMoreThanADayCannotBeSaved() {
        assertTrue(runCatching { roundedTimerSeconds(0) }.isFailure)
        assertTrue(runCatching { roundedTimerSeconds(-1) }.isFailure)
        assertTrue(runCatching { roundedTimerSeconds(86_400_001) }.isFailure)
        assertEquals(86400L, roundedTimerSeconds(86_400_000))
    }

    @Test fun pauseAndResumeRoundTheCombinedActiveTimeOnlyOnce() {
        val first = RunningTimer("p", "Desarrollo", 20_000, 100_000, 5_000, 1)
        val firstPart = first.elapsed(110_000, 15_000, 1)
        val paused = first.copy(accumulatedMs = firstPart, startedWall = 0, startedElapsed = 0)
        assertEquals(30_000L, paused.elapsed(500_000, 60_000, 1))
        val resumed = paused.copy(startedWall = 500_000, startedElapsed = 60_000)
        val total = resumed.elapsed(510_000, 70_000, 1)
        assertEquals(40_000L, total)
        assertEquals(60L, roundedTimerSeconds(total))
    }

    @Test fun severalEntriesRemainLinkedToTheSameTaskAndTheirTimesAddUp() {
        val task = ProjectTask("t", "p", "Desarrollo")
        val day = LocalDate.of(2026, 11, 25)
        val entries = listOf(
            TimeEntry(1, "u", "Usuario", "p", day, 600, task.name, taskId = task.id),
            TimeEntry(2, "u", "Usuario", "p", day.minusDays(1), 1200, task.name, taskId = task.id),
            TimeEntry(3, "u", "Usuario", "p", day, 300, "Pruebas", taskId = "otra")
        )
        assertEquals(listOf(1L, 2L), entries.forTask(task).map { it.id })
        assertEquals(1800L, entries.forTask(task).totalSeconds())
    }

    @Test fun taskSummaryDoesNotMixAnotherProjectOrAnotherTaskIdWithTheSameName() {
        val task = ProjectTask("t", "p", "Desarrollo")
        val day = LocalDate.now()
        val entries = listOf(
            TimeEntry(1, "u", "Usuario", "otro", day, 600, task.name, taskId = task.id),
            TimeEntry(2, "u", "Usuario", "p", day, 600, task.name, taskId = "otro-id")
        )
        assertTrue(entries.forTask(task).isEmpty())
    }

    @Test fun legacyTaskNamesAreRecognizedOnlyWithinTheirOwnProject() {
        val task = ProjectTask("t", "p", "Desarrollo")
        val day = LocalDate.now()
        val entries = listOf(
            TimeEntry(1, "u", "Usuario", "p", day, 600, " desarrollo "),
            TimeEntry(2, "u", "Usuario", "otro", day, 300, "Desarrollo")
        )
        assertEquals(listOf(1L), entries.forTask(task).map { it.id })
    }
}
