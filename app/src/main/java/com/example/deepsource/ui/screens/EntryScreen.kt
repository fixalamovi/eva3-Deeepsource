@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.deepsource.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.deepsource.domain.*
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.theme.*
import com.example.deepsource.ui.viewmodel.EntryViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable internal fun EntryScreen(vm:EntryViewModel) {
    val original=vm.editing
    var project by rememberSaveable(original?.id, vm.selectedProject) {mutableStateOf(vm.selectedProject)}
    var dayText by rememberSaveable(original?.id) {mutableStateOf((original?.day ?: LocalDate.now()).toString())}
    val day=LocalDate.parse(dayText)
    var hours by rememberSaveable(original?.id) {mutableStateOf(if(original==null) "0" else (original.seconds/3600).toString())}
    var minutes by rememberSaveable(original?.id) {mutableStateOf(if(original==null) "0" else (original.seconds%3600/60).toString())}
    var taskId by rememberSaveable(original?.id, vm.selectedTask) {mutableStateOf(original?.taskId ?: vm.selectedTask)}
    var submitted by rememberSaveable(original?.id) {mutableStateOf(false)}
    var picker by remember {mutableStateOf(false)}
    var calendar by remember {mutableStateOf(false)}
    var delete by remember {mutableStateOf(false)}
    val extraSeconds=(original?.seconds?.rem(60) ?: 0L).toInt()
    val selected = vm.projects.firstOrNull { it.id == project }
    val selectedTask = vm.tasks.firstOrNull { it.id == taskId && it.projectId == project }
    val task = selectedTask?.name.orEmpty()
    val validation = validateEntry(selected?.id, hours, minutes, task, day, seconds = extraSeconds)
    val errors = if (submitted) validation else EntryErrors()
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Text(if(original==null) "Haz que tu trabajo cuente." else "Actualiza los detalles de tu registro.",color=Muted)
        Identity(vm.session)
        SelectField("Proyecto",selected?.let {"${it.name} · ${it.client}"} ?: "Selecciona un proyecto",error=errors.project) {picker=true}
        if(vm.projects.isEmpty()) CreateProjectControl(vm.busy, vm::createProject)
        SelectField("Día",day.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy",Spanish)),"calendar",errors.date) {calendar=true}
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Tiempo dedicado",fontWeight=FontWeight.SemiBold)
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Field(hours,{hours=it.filter(Char::isDigit).take(2)},"Horas",Modifier.weight(1f),keyboard=KeyboardType.Number,error=errors.duration)
                Field(minutes,{minutes=it.filter(Char::isDigit).take(2)},"Minutos",Modifier.weight(1f),keyboard=KeyboardType.Number,error=errors.minutes)
            }
            if(original?.source=="Cronómetro" && original.seconds%60>0) Text("El registro incluye ${original.seconds%60} segundos adicionales, que se conservarán al editar.",color=Muted,style=MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf(15,30,60).forEach {value -> SuggestionChip(onClick={val total=(hours.toIntOrNull() ?: 0)*60+(minutes.toIntOrNull() ?: 0)+value;if(total<=1440){hours=(total/60).toString();minutes=(total%60).toString()}},label={Text("+$value min")}) }
        }
        TaskSelector(vm.tasks, project, taskId, { taskId = it.id }, vm.busy,
            if (errors.task != null) "Selecciona o crea una tarea" else null,
            onCreate = { name, onCreated -> selected?.let { vm.createTask(it.id, name, onCreated) } })
        Primary(if(original==null) "Guardar registro" else "Guardar cambios","check",!vm.busy) {
            submitted=true
            if (selected != null && validation.valid) {
                val seconds = hours.toLong() * 3600 + minutes.toLong() * 60 + extraSeconds
                vm.save(selected.id, day, seconds, task, selectedTask?.id)
            }
        }
        if(original!=null) TextButton({delete=true},Modifier.align(Alignment.CenterHorizontally),enabled=!vm.busy) {Glyph("trash",tint=Danger);Spacer(Modifier.width(8.dp));Text("Eliminar registro",color=Danger)}
        Text("El registro se asigna siempre a ${vm.session.name}.",color=Muted,style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
    }
    if(picker) ProjectPicker(vm.projects,project,{
        if (project != it) taskId = null
        project=it;picker=false
    },{picker=false})
    if(calendar) {
        val state=rememberDatePickerState(initialSelectedDateMillis=day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),selectableDates=object:SelectableDates {
            override fun isSelectableDate(utcTimeMillis:Long)=utcTimeMillis<=LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        })
        DatePickerDialog(onDismissRequest={calendar=false},confirmButton={TextButton({state.selectedDateMillis?.let {dayText=Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()};calendar=false}) {Text("Aceptar")}},dismissButton={TextButton({calendar=false}) {Text("Cancelar")}}) {DatePicker(state=state)}
    }
    if(delete) AlertDialog(onDismissRequest={delete=false},title={Text("¿Eliminar este registro?")},text={Text("Se descontarán ${durationText(original!!.seconds)} del proyecto. Esta acción no se puede deshacer.")},confirmButton={TextButton({delete=false;vm.delete(original!!)}) {Text("Eliminar",color=Danger)}},dismissButton={TextButton({delete=false}) {Text("Cancelar")}})
}
