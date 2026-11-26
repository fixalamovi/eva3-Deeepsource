package com.example.deepsource.ui.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsource.AppState
import com.example.deepsource.domain.RunningTimer
import com.example.deepsource.domain.ProjectTask
import com.example.deepsource.domain.inMonth
import com.example.deepsource.domain.totalSeconds
import com.example.deepsource.domain.validateTask

internal class TimerViewModel(private val app: AppState) : ViewModel() {
    val session get() = requireNotNull(app.session)
    val projects get() = app.projects
    val selectedProject get() = app.selectedProject
    val selectedTask get() = app.selectedTask
    val tasks get() = app.tasks
    val timer get() = app.timer
    val busy get() = app.busy

    fun elapsedMs() = app.elapsedMs()
    fun monthSeconds(projectId: String) = app.records.inMonth(app.month).totalSeconds(projectId)

    fun startTimer(project: String, task: ProjectTask) = app.launchAction(viewModelScope) {
        require(timer == null) { "Termina o descarta el cronómetro actual antes de iniciar otro" }
        require(task.projectId == project) { "Selecciona una tarea de este proyecto" }
        val taskError = validateTask(task.name)
        require(taskError == null) { taskError.orEmpty() }
        val value = RunningTimer(
            project, task.name,
            startedWall = System.currentTimeMillis(),
            startedElapsed = SystemClock.elapsedRealtime(),
            boot = app.boot,
            taskId = task.id
        )
        app.hours.saveTimer(session, value)
        app.refresh()
    }

    fun toggleTimer() = app.launchAction(viewModelScope) {
        val updated = app.hours.toggleTimer(session, System.currentTimeMillis(), SystemClock.elapsedRealtime(), app.boot)
        app.replaceTimer(updated)
        app.updateWidgets()
    }

    fun finishTimer() = app.launchAction(viewModelScope) {
        val saved = app.hours.completeTimer(session, System.currentTimeMillis(), SystemClock.elapsedRealtime(), app.boot)
        app.showReceipt(saved)
    }

    fun discardTimer() = app.launchAction(viewModelScope) {
        app.hours.saveTimer(session, null)
        app.refresh()
    }

    fun createProject(name: String, client: String, estimate: Int, onCreated: () -> Unit) =
        app.launchAction(viewModelScope) {
            app.createProject(name, client, estimate)
            onCreated()
        }

    fun createTask(projectId: String, name: String, onCreated: (ProjectTask) -> Unit) =
        app.launchAction(viewModelScope) { onCreated(app.createTask(projectId, name)) }
}
