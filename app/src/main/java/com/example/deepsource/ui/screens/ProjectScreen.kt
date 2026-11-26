package com.example.deepsource.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.deepsource.domain.*
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.viewmodel.ProjectViewModel

@Composable
internal fun ProjectScreen(vm: ProjectViewModel) {
    val project = vm.project ?: return
    val entries = vm.entries
    var taskFilter by rememberSaveable(project.id) { mutableStateOf<String?>(null) }
    val selectedTask = vm.tasks.firstOrNull { it.id == taskFilter }
    val visibleEntries = if (selectedTask == null) entries else entries.forTask(selectedTask)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Badge(project.client, Color(project.color))
            Spacer(Modifier.height(12.dp))
            Text(project.name, style = MaterialTheme.typography.headlineLarge)
        }
        item { MonthSelector(vm.month, vm::selectMonth) }
        item { BalanceCard(project, entries.totalSeconds(), project.estimateMinutes) }
        item { Primary("Registrar horas en este proyecto", "plus", !vm.busy) { vm.addEntry(project.id) } }
        item { Secondary("Abrir cronómetro", "timer", !vm.busy) { vm.openTimer(project.id) } }
        item {
            Section("Tareas del proyecto")
            Text("Utiliza una tarea existente o crea otra. Cada una puede tener varios registros.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        item { CreateTaskControl(!vm.busy, vm::createTask, { taskFilter = it.id }) }
        items(vm.tasks, key = { "task_" + it.id }) { task ->
            val records = entries.forTask(task)
            PanelCard {
                Text(task.name, style = MaterialTheme.typography.titleMedium)
                Text("${records.size} registros este mes · ${durationText(records.totalSeconds())}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ taskFilter = task.id }) { Text("Registros") }
                    Spacer(Modifier.weight(1f))
                    TextButton({ vm.addEntry(project.id, task) }, enabled = !vm.busy) { Text("Registrar") }
                    IconButton({ vm.openTimer(project.id, task) }, enabled = !vm.busy,
                        modifier = Modifier.semantics { contentDescription = "Cronometrar ${task.name}" }) {
                        Glyph("timer", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        item {
            Section(
                selectedTask?.let { "Registros: ${it.name}" } ?: "Todos los registros",
                if (selectedTask != null) "Ver todos" else null
            ) { taskFilter = null }
        }
        if (visibleEntries.isEmpty()) item {
            EmptyState("Todavía no hay registros", "Registra tiempo manualmente o utiliza el cronómetro.")
        }
        items(visibleEntries, key = { "entry_" + it.id }) { entry ->
            EntryCard(entry, project) { vm.openEntry(entry) }
        }
    }
}
