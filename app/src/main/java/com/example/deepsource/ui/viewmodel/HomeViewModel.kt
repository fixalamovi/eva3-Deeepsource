package com.example.deepsource.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsource.AppState
import com.example.deepsource.domain.TimeEntry
import com.example.deepsource.domain.inMonth
import com.example.deepsource.domain.totalSeconds

internal class HomeViewModel(private val app: AppState) : ViewModel() {
    val session get() = requireNotNull(app.session)
    val month get() = app.month
    val projects get() = app.projects
    val recentEntries get() = app.records.take(3)
    val monthEntries get() = app.records.inMonth(month)
    val monthTotal get() = monthEntries.totalSeconds()
    val timer get() = app.timer
    val busy get() = app.busy

    fun elapsedMs() = app.elapsedMs()
    fun openEntry(entry: TimeEntry? = null) = app.openEntry(entry)
    fun openProject(projectId: String) = app.openProject(projectId)
    fun openTimer() = app.navigate("timer")
    fun openSummary() = app.navigate("summary")
    fun openHistory() = app.navigate("history")

    fun createProject(name: String, client: String, estimate: Int, onCreated: () -> Unit) =
        app.launchAction(viewModelScope) {
            app.createProject(name, client, estimate)
            onCreated()
        }
}
