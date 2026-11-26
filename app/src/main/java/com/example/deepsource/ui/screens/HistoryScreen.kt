package com.example.deepsource.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepsource.domain.durationText
import com.example.deepsource.domain.totalSeconds
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.theme.Muted
import com.example.deepsource.ui.viewmodel.HistoryViewModel

@Composable internal fun HistoryScreen(vm:HistoryViewModel) {
    var search by rememberSaveable {mutableStateOf("")}
    var project by rememberSaveable {mutableStateOf<String?>(null)}
    var picker by remember {mutableStateOf(false)}
    val rows = vm.filterEntries(search, project)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {MonthSelector(vm.month, vm::selectMonth)}
        item {Field(search,{search=it},"Buscar registros",placeholder="Tarea o proyecto",leading="search")}
        item {SelectField("Proyecto",vm.projects.firstOrNull {it.id==project}?.name ?: "Todos los proyectos") {picker=true}}
        item { Text("${rows.size} registros · ${durationText(rows.totalSeconds())}",color=Muted,fontSize=12.sp) }
        if(rows.isEmpty()) item {EmptyState("Sin registros para esta búsqueda","Prueba otro mes o proyecto.","search")}
        items(rows,key={it.id}) {e ->EntryCard(e,vm.projects.firstOrNull {it.id==e.projectId}) {vm.openEntry(e)}}
        item {Primary("Nuevo registro","plus") {vm.openEntry()}}
    }
    if(picker) ProjectPicker(vm.projects,project,{project=it;picker=false},{picker=false},allowAll=true)
}
