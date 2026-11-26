package com.example.deepsource.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsource.AppState
import com.example.deepsource.domain.TimeEntry
import com.example.deepsource.domain.ProjectTask
import java.time.LocalDate

internal class EntryViewModel(private val app: AppState) : ViewModel() {
    val editing get() = app.editing
    val selectedProject get() = app.selectedProject
    val selectedTask get() = app.selectedTask
    val tasks get() = app.tasks
    val session get() = requireNotNull(app.session)
    val projects get() = app.projects
    val busy get() = app.busy

    fun save(project: String, day: LocalDate, seconds: Long, task: String, taskId: String? = null) =
        app.launchAction(viewModelScope) {
            val account = session
            val entry = TimeEntry(
                id = editing?.id ?: 0L,
                uid = account.uid,
                userName = account.name,
                projectId = project,
                day = day,
                seconds = seconds,
                task = task,
                source = editing?.source ?: "Manual",
                taskId = taskId
            )
            val saved = app.hours.saveEntry(account, entry, app.entryDraftKey)
            app.showReceipt(saved)
        }

    fun delete(entry: TimeEntry) = app.launchAction(viewModelScope) {
        app.hours.deleteEntry(session, entry.id)
        app.reload()
        app.navigate("history")
        app.message = "Registro eliminado"
        app.updateWidgets()
    }

    fun createProject(name: String, client: String, estimate: Int, onCreated: () -> Unit) =
        app.launchAction(viewModelScope) {
            app.createProject(name, client, estimate)
            onCreated()
        }

    fun createTask(projectId: String, name: String, onCreated: (ProjectTask) -> Unit) =
        app.launchAction(viewModelScope) { onCreated(app.createTask(projectId, name)) }
}
