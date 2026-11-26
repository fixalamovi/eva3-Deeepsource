@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.deepsource.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.deepsource.domain.*
import com.example.deepsource.ui.theme.*
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

@Composable fun ProjectPicker(projects:List<Project>,selected:String?,onSelect:(String?)->Unit,onDismiss:()->Unit,allowAll:Boolean=false) {
    var search by rememberSaveable {mutableStateOf("")}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=Panel) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Selecciona un proyecto",style=MaterialTheme.typography.titleLarge)
            Text("El proyecto se asociará a tu registro.",color=Muted,style=MaterialTheme.typography.bodyMedium)
            Field(search,{search=it},"Buscar",placeholder="Proyecto o cliente",leading="search")
            LazyColumn(Modifier.heightIn(max=380.dp),contentPadding=PaddingValues(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(allowAll) item {PickerRow("Todos los proyectos","Sin filtro",selected==null,Violet) {onSelect(null)}}
                val result=projects.filter {it.name.contains(search,true)||it.client.contains(search,true)}
                if(result.isEmpty()) item {EmptyState("Sin resultados","Prueba con otro nombre.","search")}
                items(result,key={it.id}) {p ->PickerRow(p.name,p.client,p.id==selected,Color(p.color)) {onSelect(p.id)}}
            }
        }
    }
}

@Composable private fun PickerRow(title:String,subtitle:String,selected:Boolean,color:Color,onClick:()->Unit) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha=.18f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200), label = "Proyecto seleccionado")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(background)
        .clickable(onClick=onClick).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(color.copy(alpha=.15f)),contentAlignment=Alignment.Center) {Text(title.take(2).uppercase(Spanish),color=color,fontWeight=FontWeight.Bold,fontSize=12.sp)}
        Column(Modifier.weight(1f)) {Text(title,fontWeight=FontWeight.SemiBold,fontSize=14.sp);Text(subtitle,color=Muted,style=MaterialTheme.typography.bodySmall)}
        Glyph(if(selected) "check" else "next",tint=if(selected) Mint else Muted)
    }
}

@Composable fun EntryCard(entry:TimeEntry,project:Project?,onClick:()->Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).clickable(onClick=onClick).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(project?.name.orEmpty().uppercase(Spanish),Modifier.weight(1f),color=Color(project?.color ?: 0xFFA99BFF),style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(durationText(entry.seconds),fontWeight=FontWeight.SemiBold,fontSize=15.sp)
        }
        Text(entry.task,maxLines=2,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Medium)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(entry.day.format(DateTimeFormatter.ofPattern("d MMM yyyy",Spanish)),color=Muted,style=MaterialTheme.typography.bodySmall)
            Text(entry.source,color=Muted,style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable fun BalanceCard(p:Project,seconds:Long,estimate:Int) {
    val deviation=deviation(seconds,estimate)
    val progress by animateFloatAsState(
        targetValue = (seconds / (estimate * 60f).coerceAtLeast(1f)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "Balance de horas"
    )
    PanelCard {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {Box(Modifier.size(8.dp).clip(CircleShape).background(Color(p.color)));Text(p.name,fontWeight=FontWeight.SemiBold)}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("${durationText(seconds)} / ${durationText(estimate*60L)} est.",fontSize=14.sp)
            Badge(if(deviation==null) "Sin estimación" else "${if(deviation>0) "+" else ""}$deviation %",if((deviation ?: 0)>0) Danger else Mint)
        }
        LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),color=if(seconds>estimate*60L) Danger else Color(p.color),trackColor=Line)
    }
}

@Composable fun CreateProjectControl(
    busy: Boolean,
    onCreateProject: (String, String, Int, () -> Unit) -> Unit
) {
    var show by remember {mutableStateOf(false)}
    var name by rememberSaveable {mutableStateOf("")}
    var client by rememberSaveable {mutableStateOf("")}
    var hours by rememberSaveable {mutableStateOf("32")}
    var submitted by remember { mutableStateOf(false) }

    Secondary("Crear proyecto", "plus", !busy) {
        submitted = false
        show = true
    }
    if (show) {
        val estimate = hours.toIntOrNull()
        val errors = validateProject(name, client, estimate)
        AlertDialog(
            onDismissRequest = { if (!busy) show = false },
            title = { Text("Nuevo proyecto") },
            text = {
                Column(
                    Modifier.imePadding().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Field(name, { name = it }, "Nombre del proyecto", readOnly = busy,
                        error = if (submitted) errors.name else null)
                    Field(client, { client = it }, "Cliente", readOnly = busy,
                        error = if (submitted) errors.client else null)
                    Field(hours, { hours = it.filter(Char::isDigit).take(5) }, "Horas estimadas por mes",
                        keyboard = KeyboardType.Number, readOnly = busy, error = if (submitted) errors.estimate else null)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    submitted = true
                    val currentEstimate = hours.toIntOrNull()
                    if (currentEstimate != null && validateProject(name, client, currentEstimate).valid) {
                        onCreateProject(name, client, currentEstimate) {
                            show = false
                            name = ""
                            client = ""
                        }
                    }
                }, enabled = !busy) { Text(if (busy) "Creando…" else "Crear") }
            },
            dismissButton = { TextButton({ show = false }, enabled = !busy) { Text("Cancelar") } }
        )
    }
}

@Composable
internal fun rememberTimerElapsed(timer: RunningTimer?, elapsedMs: () -> Long): Long {
    var elapsed by remember { mutableLongStateOf(elapsedMs()) }
    val lifecycle = LocalContext.current as LifecycleOwner
    val readElapsed by rememberUpdatedState(elapsedMs)
    LaunchedEffect(timer, lifecycle) {
        elapsed = readElapsed()
        if (timer?.running == true) {
            lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    elapsed = readElapsed()
                    // Solo cambia la cifra al llegar al siguiente minuto.
                    // El primer avance permite finalizar también una sesión breve.
                    delay(if (elapsed == 0L) 250 else 60_000 - elapsed % 60_000)
                }
            }
        }
    }
    return elapsed
}

@Composable internal fun TimerBanner(timer: RunningTimer?, elapsedMs: () -> Long, onOpenTimer: () -> Unit) {
    if (timer == null) return
    val elapsed = rememberTimerElapsed(timer, elapsedMs)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mint.copy(alpha=.08f)).border(1.dp,Mint.copy(alpha=.18f),RoundedCornerShape(16.dp)).clickable(onClick = onOpenTimer).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Glyph(if(timer.running) "timer" else "pause",tint=Mint)
        Column(Modifier.weight(1f)) {Text(if(timer.running) "Cronómetro en marcha" else "Cronómetro en pausa",color=Mint,fontSize=13.sp,fontWeight=FontWeight.SemiBold);Text(timer.task,color=Muted,maxLines=1,fontSize=12.sp)}
        Text(minuteClockText(elapsed, roundUp = !timer.running),fontFamily=FontFamily.Monospace,fontSize=13.sp,color=Mint)
    }
}
