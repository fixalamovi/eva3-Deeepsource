@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.deepsource.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deepsource.domain.ProjectTask
import com.example.deepsource.domain.validateTask

@Composable
fun TaskSelector(
    tasks: List<ProjectTask>,
    projectId: String?,
    selectedTaskId: String?,
    onSelect: (ProjectTask) -> Unit,
    busy: Boolean,
    error: String?,
    onCreate: (String, (ProjectTask) -> Unit) -> Unit
) {
    var picker by rememberSaveable(projectId) { mutableStateOf(false) }
    val available = tasks.filter { it.projectId == projectId }
    val selected = available.firstOrNull { it.id == selectedTaskId }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SelectField("Tarea", selected?.name ?: "Selecciona una tarea",
            error = error, enabled = projectId != null && !busy) { picker = true }
        if (projectId == null) {
            Text("Selecciona primero un proyecto.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CreateTaskControl(!busy, onCreate, onSelect, compact = true)
        }
    }
    if (picker && projectId != null) {
        TaskPicker(available, selectedTaskId, {
            picker = false
            onSelect(it)
        }, { picker = false })
    }
}

@Composable
fun CreateTaskControl(
    enabled: Boolean,
    onCreate: (String, (ProjectTask) -> Unit) -> Unit,
    onCreated: (ProjectTask) -> Unit = {},
    compact: Boolean = false
) {
    var show by remember { mutableStateOf(false) }
    if (compact) {
        TextButton({ show = true }, enabled = enabled, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Glyph("plus")
            Spacer(Modifier.width(6.dp))
            Text("Crear tarea")
        }
    } else {
        Secondary("Crear tarea", "plus", enabled) { show = true }
    }
    if (show) {
        CreateTaskDialog(enabled, onCreate, {
            show = false
            onCreated(it)
        }, { show = false })
    }
}

@Composable
private fun CreateTaskDialog(
    enabled: Boolean,
    onCreate: (String, (ProjectTask) -> Unit) -> Unit,
    onCreated: (ProjectTask) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val error = validateTask(name)
    AlertDialog(
        onDismissRequest = { if (enabled) onDismiss() },
        title = { Text("Crear tarea") },
        text = {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(name, { name = it.take(160) }, "Nombre de la tarea",
                    placeholder = "Ej. Implementar inicio de sesión",
                    error = if (submitted) error else null, readOnly = !enabled)
                Text("Podrás utilizar esta tarea en varios registros y sesiones del cronómetro.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton({
                submitted = true
                if (error == null) onCreate(name.trim(), onCreated)
            }, enabled = enabled) { Text(if (enabled) "Guardar tarea" else "Guardando…") }
        },
        dismissButton = { TextButton(onDismiss, enabled = enabled) { Text("Cancelar") } }
    )
}

@Composable
private fun TaskPicker(
    tasks: List<ProjectTask>,
    selected: String?,
    onSelect: (ProjectTask) -> Unit,
    onDismiss: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    val result = tasks.filter { it.name.contains(search, ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Selecciona una tarea", style = MaterialTheme.typography.titleLarge)
            Field(search, { search = it }, "Buscar tarea", leading = "search")
            LazyColumn(Modifier.heightIn(max = 360.dp), contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.isEmpty()) item {
                    EmptyState("Sin tareas para mostrar", "Crea una tarea o prueba con otro nombre.", "search")
                }
                items(result, key = { it.id }) { task ->
                    Row(Modifier.fillMaxWidth()
                        .background(if (task.id == selected) MaterialTheme.colorScheme.primary.copy(alpha = .15f)
                            else MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                        .clickable { onSelect(task) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(task.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Glyph(if (task.id == selected) "check" else "next", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
