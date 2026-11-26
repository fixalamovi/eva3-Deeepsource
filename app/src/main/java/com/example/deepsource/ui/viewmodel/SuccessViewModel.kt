package com.example.deepsource.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.deepsource.AppState

internal class SuccessViewModel(private val app: AppState) : ViewModel() {
    val entry get() = app.lastSaved
    val projectName get() = app.projects.firstOrNull { it.id == entry?.projectId }?.name.orEmpty()
    val accountName get() = requireNotNull(app.session).name

    fun openHome() = app.navigate("home")
    fun addEntry(projectId: String) = app.openEntry(project = projectId,
        task = app.tasks.firstOrNull { it.id == entry?.taskId })
}
