package com.example.deepsource.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepsource.domain.minuteClockText
import com.example.deepsource.domain.validateTimer
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.theme.*
import com.example.deepsource.ui.viewmodel.TimerViewModel

@Composable internal fun TimerScreen(vm:TimerViewModel) {
    var project by rememberSaveable(vm.selectedProject) {mutableStateOf(vm.selectedProject ?: vm.projects.firstOrNull()?.id)}
    var taskId by rememberSaveable(vm.selectedTask) {mutableStateOf(vm.selectedTask)}
    var submitted by rememberSaveable { mutableStateOf(false) }
    var picker by remember {mutableStateOf(false)}
    var discard by remember {mutableStateOf(false)}
    val timer=vm.timer
    val elapsed=rememberTimerElapsed(timer, vm::elapsedMs)
    val selected = vm.projects.firstOrNull { it.id == project }
    val selectedTask = vm.tasks.firstOrNull { it.id == taskId && it.projectId == project }
    val task = selectedTask?.name.orEmpty()
    val errors = validateTimer(selected?.id, task)
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        if(timer==null) {
            Text("Un proyecto. Una tarea. Todo tu enfoque.",Modifier.fillMaxWidth(),color=Muted)
            Identity(vm.session)
            SelectField("Proyecto", selected?.name ?: "Selecciona un proyecto",
                error = if (submitted) errors.project else null) { picker = true }
            if(vm.projects.isEmpty()) CreateProjectControl(vm.busy, vm::createProject)
            TaskSelector(vm.tasks, project, taskId, { taskId = it.id }, vm.busy,
                if (submitted && errors.task != null) "Selecciona o crea una tarea" else null,
                onCreate = { name, onCreated -> selected?.let { vm.createTask(it.id, name, onCreated) } })
        } else PanelCard {
            val p=vm.projects.firstOrNull {it.id==timer.projectId}
            Text(p?.name.orEmpty(),color=Color(p?.color ?: 0xFFA99BFF),style=MaterialTheme.typography.labelSmall)
            Text(timer.task,style=MaterialTheme.typography.titleMedium)
            Text(vm.session.name,color=Muted,style=MaterialTheme.typography.bodySmall)
        }
        TimerDial(elapsed,timer?.running==true,if(timer==null) "Listo para empezar" else if(timer.running) "En marcha" else "En pausa")
        Text(if(timer?.running==true) "Concéntrate. Nosotros llevamos la cuenta." else if(timer!=null) "La pausa no suma tiempo." else "Cada sesión empieza con un primer paso.",color=Muted,style=MaterialTheme.typography.bodySmall)
        if (timer == null) Primary("Iniciar cronómetro", "play", !vm.busy) {
            submitted = true
            if (selected != null && selectedTask != null && errors.valid) {
                submitted = false
                vm.startTimer(selected.id, selectedTask)
            }
        }
        else {
            Primary(if(timer.running) "Pausar" else "Reanudar",if(timer.running) "pause" else "play",!vm.busy) {vm.toggleTimer()}
            Secondary("Terminar y registrar","stop",!vm.busy && elapsed>0) {vm.finishTimer()}
            Text("Al guardar se redondea hacia arriba al minuto completo. Las pausas no suman tiempo.",color=Muted,style=MaterialTheme.typography.bodySmall)
            val p=vm.projects.firstOrNull {it.id==timer.projectId}
            if(p!=null) BalanceCard(p,vm.monthSeconds(p.id),p.estimateMinutes)
            TextButton({discard=true},enabled=!vm.busy) {Text("Descartar sesión",color=Muted)}
        }
        PanelCard {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {Glyph("lock",tint=Mint);Text("Puedes salir de la app. Tu sesión del cronómetro se conserva y las pausas se excluyen del registro.",color=Muted,style=MaterialTheme.typography.bodySmall)}}
    }
    if(picker) ProjectPicker(vm.projects,project,{
        if (project != it) taskId = null
        project=it;picker=false
    },{picker=false})
    if(discard) AlertDialog(onDismissRequest={discard=false},title={Text("¿Descartar la sesión?")},text={Text("El tiempo de este cronómetro no se registrará.")},confirmButton={TextButton({discard=false;vm.discardTimer()}) {Text("Descartar",color=Danger)}},dismissButton={TextButton({discard=false}) {Text("Conservar")}})
}

@Composable private fun TimerDial(elapsed:Long,running:Boolean,label:String) {
    val color by animateColorAsState(
        if(running) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(280), label = "Estado del cronómetro")
    val stroke by animateDpAsState(
        if(running) 5.dp else 3.dp,
        animationSpec = tween(280), label = "Borde del cronómetro")
    val outline = MaterialTheme.colorScheme.outline
    Box(Modifier.size(246.dp).padding(8.dp),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(outline,style=Stroke(3.dp.toPx()))
            drawArc(color,-90f,if(running) ((elapsed/60000%60)/60f*360f).coerceAtLeast(8f) else 360f,false,
                topLeft=Offset.Zero,size=Size(size.width,size.height),style=Stroke(stroke.toPx(),cap=StrokeCap.Round))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Badge(label,color)
            Text(minuteClockText(elapsed, roundUp = !running),fontSize=34.sp,fontWeight=FontWeight.SemiBold,fontFamily=FontFamily.Monospace,letterSpacing=(-1).sp)
            Text("HORAS : MINUTOS",color=Muted,style=MaterialTheme.typography.labelSmall)
        }
    }
}
