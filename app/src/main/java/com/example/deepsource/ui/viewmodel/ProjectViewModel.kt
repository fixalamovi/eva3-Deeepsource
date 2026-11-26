package com.example.deepsource.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsource.domain.ProjectTask
import com.example.deepsource.AppState
import com.example.deepsource.domain.TimeEntry
import com.example.deepsource.domain.inMonth
import java.time.YearMonth

internal class ProjectViewModel(private val app: AppState) : ViewModel() {
    val project get() = app.projects.firstOrNull { it.id == app.selectedProject }
    val month get() = app.month
    val entries get() = app.records.inMonth(month).filter { it.projectId == app.selectedProject }
    val tasks get() = app.tasks.filter { it.projectId == app.selectedProject }
    val busy get() = app.busy

    fun selectMonth(month: YearMonth) {
        app.month = month
    }

    fun openEntry(entry: TimeEntry) = app.openEntry(entry)
    fun addEntry(projectId: String, task: ProjectTask? = null) = app.openEntry(project = projectId, task = task)
    fun openTimer(projectId: String, task: ProjectTask? = null) = app.openTimer(projectId, task)

    fun createTask(name: String, onCreated: (ProjectTask) -> Unit) =
        app.launchAction(viewModelScope) {
            val project = requireNotNull(project) { "Proyecto no disponible" }
            onCreated(app.createTask(project.id, name))
        }
}
