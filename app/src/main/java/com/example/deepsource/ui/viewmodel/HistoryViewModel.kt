package com.example.deepsource.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.deepsource.AppState
import com.example.deepsource.domain.TimeEntry
import com.example.deepsource.domain.inMonth
import java.time.YearMonth

internal class HistoryViewModel(private val app: AppState) : ViewModel() {
    val month get() = app.month
    val projects get() = app.projects

    fun selectMonth(month: YearMonth) {
        app.month = month
    }

    fun filterEntries(search: String, projectId: String?): List<TimeEntry> {
        val matchingProjects = projects.filter { it.name.contains(search, ignoreCase = true) }.map { it.id }.toSet()
        return app.records.inMonth(month).filter { entry ->
            (projectId == null || entry.projectId == projectId) &&
                (entry.task.contains(search, ignoreCase = true) || entry.projectId in matchingProjects)
        }
    }

    fun openEntry(entry: TimeEntry? = null) = app.openEntry(entry)
}
