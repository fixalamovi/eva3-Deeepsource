package com.example.deepsource.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsource.AppState
import com.example.deepsource.domain.inMonth
import com.example.deepsource.domain.totalSeconds
import com.example.deepsource.widget.HoursWidget
import java.time.YearMonth

internal class WidgetViewModel(private val app: AppState) : ViewModel() {
    val session get() = requireNotNull(app.session)
    val timer get() = app.timer
    val busy get() = app.busy
    val month get() = YearMonth.now()
    val entries get() = app.records.inMonth(month)

    fun topProjects() = entries.let { entries ->
        app.projects.sortedByDescending { entries.totalSeconds(it.id) }.take(4)
    }

    fun elapsedMs() = app.elapsedMs()
    fun openEntry() = app.openEntry()
    fun openTimer() = app.navigate("timer")
    fun refresh() = app.launchAction(viewModelScope) { app.refresh() }

    fun pinWidget() {
        if (!HoursWidget.pin(app.application)) {
            app.message = "Mantén pulsada la pantalla de inicio, entra a Widgets y busca Deepsource"
        }
    }
}
