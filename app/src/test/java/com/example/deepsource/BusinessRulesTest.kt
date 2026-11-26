package com.example.deepsource

import com.example.deepsource.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BusinessRulesTest {
    private val day = LocalDate.of(2026, 11, 20)
    private fun entry(project: String = "p1", task: String = "Pruebas", id: String? = "t1", date: LocalDate = day) =
        TimeEntry(1, "u1", "Persona Prueba", project, date, 120, task, taskId = id)

    @Test fun oneMillisecondRoundsUpToOneMinute() { assertEquals(60L, roundedTimerSeconds(1)) }
    @Test fun exactMinuteDoesNotRoundUpAgain() { assertEquals(60L, roundedTimerSeconds(60_000)) }
    @Test fun oneHourThirtyThreeMinutesThirtySecondsBecomesOneHourThirtyFourMinutes() {
        assertEquals(94 * 60L, roundedTimerSeconds((3600 + 33 * 60 + 30) * 1000L))
    }
    @Test fun exactMaximumIsAcceptedAndAnyExtraTimeIsRejected() {
        assertEquals(86400L, roundedTimerSeconds(86_400_000))
        assertTrue(runCatching { roundedTimerSeconds(86_400_001) }.isFailure)
    }
    @Test fun zeroAndNegativeTimerTimeCannotBeSaved() {
        assertTrue(runCatching { roundedTimerSeconds(0) }.isFailure)
        assertTrue(runCatching { roundedTimerSeconds(-1) }.isFailure)
    }
    @Test fun minuteDisplayUsesFloorWhileRunningAndCeilingForThePausedPreview() {
        assertEquals("01:33", minuteClockText(5_610_000))
        assertEquals("01:34", minuteClockText(5_610_000, true))
        assertEquals("00:00", minuteClockText(-1, true))
        assertEquals("01:00", minuteClockText(3_600_000, true))
    }
    @Test fun sameTaskCanHaveSeveralIndependentRecords() {
        val task = ProjectTask("t1", "p1", "Pruebas")
        val records = listOf(entry(), entry().copy(id = 2, seconds = 240))
        assertEquals(2, records.forTask(task).size)
        assertEquals(360L, records.forTask(task).totalSeconds())
    }
    @Test fun legacyTaskMatchesItsNameOnlyWithinItsOwnProject() {
        val task = ProjectTask("t1", "p1", "Pruebas")
        val records = listOf(entry(id = null, task = " pruebas "), entry("p2", id = null), entry(id = "t2"))
        assertEquals(listOf(records.first()), records.forTask(task))
    }
    @Test fun taskIdKeepsHistoryWhenTheTaskNameChanges() {
        assertEquals(1, listOf(entry(task = "Nombre anterior")).forTask(ProjectTask("t1", "p1", "Nombre nuevo")).size)
    }
    @Test fun monthFilterDoesNotMixTheSameMonthAcrossYears() {
        val records = listOf(entry(), entry(date = day.minusYears(1)), entry(date = day.minusMonths(1)))
        assertEquals(listOf(records.first()), records.inMonth(YearMonth.of(2026, 11)))
    }
    @Test fun emptyCollectionsAndMissingProjectHaveZeroHours() {
        assertEquals(0L, emptyList<TimeEntry>().totalSeconds())
        assertEquals(0L, listOf(entry()).totalSeconds("missing"))
    }
    @Test fun invalidNumericInputCannotCreateARecord() {
        for (hours in listOf("", "abc", "-1", "25")) assertFalse(validateEntry("p", hours, "0", "Pruebas", day, day).valid)
        for (minutes in listOf("", "abc", "-1", "60")) assertFalse(validateEntry("p", "1", minutes, "Pruebas", day, day).valid)
        assertFalse(validateEntry("p", "1", "0", "Pruebas", day, day, seconds = 60).valid)
    }
    @Test fun projectAndTaskTextLimitsAreCheckedAfterTrimming() {
        assertNull(validateName("  App  "))
        assertNotNull(validateName("a".repeat(81)))
        assertNull(validateTask("a".repeat(160)))
        assertNotNull(validateTask("a".repeat(161)))
        assertFalse(validateProject("Portal", "C", 1).valid)
        assertFalse(validateProject("Portal", "Cliente", null).valid)
        assertFalse(validateProject("Portal", "Cliente", 10001).valid)
    }
    @Test fun csvEscapesQuotesSeparatorsAndFormulaPrefixes() {
        assertEquals("\"Una \"\"prueba\"\";con separador\"", csvCell("Una \"prueba\";con separador"))
        for (value in listOf("=1+1", "+SUM(A1)", "-3+4", "@SUM(A1)", "  =2")) {
            assertTrue(csvCell(value).startsWith("\"'"))
        }
    }
    @Test fun exportIncludesHeaderUtf8MarkerAndRecordIdentity() {
        val project = Project("p1", "Portal", "Cliente", 60, 60, 0)
        val csv = exportHoursCsv(listOf(entry()), listOf(project), "persona@example.com")
        assertTrue(csv.startsWith("\uFEFFFecha;Proyecto;"))
        assertTrue(csv.contains("\"2026-11-20\";\"Portal\";\"Pruebas\""))
        assertTrue(csv.contains("\"120\";\"Persona Prueba\";\"persona@example.com\";\"Manual\""))
        assertTrue(csv.endsWith("\r\n"))
    }
    @Test fun exportRetainsRecordsWhoseProjectIsMissing() {
        assertTrue(exportHoursCsv(listOf(entry()), emptyList(), "persona@example.com").contains("\"2026-11-20\";\"\";\"Pruebas\""))
    }
    @Test fun sessionInitialsAndDemoWorkspaceStayIndependentFromAccounts() {
        val session = Session("u1", "  Persona   Prueba Más  ", "persona@example.com")
        assertEquals("PP", session.initials)
        assertEquals("u1", session.workspace)
        assertEquals("demo", session.copy(demo = true).workspace)
    }
    @Test fun invalidPasswordLengthIsRejectedBeforeHashing() {
        assertNotNull(Passwords.validate("a1"))
        assertNotNull(Passwords.validate("A1".repeat(65)))
        assertTrue(runCatching { Passwords.create("weak") }.isFailure)
    }
}
