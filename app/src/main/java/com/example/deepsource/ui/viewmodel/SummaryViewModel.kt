package com.example.deepsource.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.deepsource.AppState
import com.example.deepsource.domain.deviation
import com.example.deepsource.domain.inMonth
import com.example.deepsource.domain.totalSeconds
import java.time.YearMonth

internal class SummaryViewModel(private val app: AppState) : ViewModel() {
    val session get() = requireNotNull(app.session)
    val projects get() = app.projects
    val month get() = app.month
    val entries get() = app.records.inMonth(month)
    val totalSeconds get() = entries.totalSeconds()
    val estimateMinutes get() = projects.sumOf { it.estimateMinutes }
    val deviationPercent get() = deviation(totalSeconds, estimateMinutes)

    fun selectMonth(month: YearMonth) {
        app.month = month
    }
}
