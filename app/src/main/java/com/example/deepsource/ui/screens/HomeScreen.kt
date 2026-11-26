package com.example.deepsource.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepsource.domain.*
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.theme.*
import com.example.deepsource.ui.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable internal fun HomeScreen(vm:HomeViewModel) {
    val session=vm.session
    val monthRecords=vm.monthEntries
    val total=vm.monthTotal
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item { DemoBanner(session.demo) }
        item { Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",Spanish)).uppercase(Spanish),color=Muted,style=MaterialTheme.typography.labelSmall)
            Text("Hola, ${session.name.substringBefore(' ')}.",style=MaterialTheme.typography.headlineLarge)
            Text("Dale espacio a lo que importa.",color=Muted)
        } }
        item { HeroStats(vm,total) }
        item { Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Primary("Registrar horas","plus",!vm.busy) {vm.openEntry()}
            Secondary(if(vm.timer==null) "Abrir cronómetro" else "Volver al cronómetro","timer") {vm.openTimer()}
        } }
        if(vm.timer!=null) item { TimerBanner(vm.timer, vm::elapsedMs, vm::openTimer) }
        item { Section("Tus proyectos","Ver resumen") {vm.openSummary()} }
        if(vm.projects.isEmpty()) item { PanelCard { EmptyState("Tu próximo proyecto empieza aquí","Crea tu primer proyecto para registrar horas.","plus"); CreateProjectControl(vm.busy, vm::createProject) } }
        else item { PanelCard { vm.projects.forEachIndexed { index,p ->
            ProjectRow(p,monthRecords.totalSeconds(p.id)) {vm.openProject(p.id)}
            if(index<vm.projects.lastIndex) HorizontalDivider(color=Line.copy(alpha=.55f))
        } } }
        if(vm.projects.isNotEmpty()) item {CreateProjectControl(vm.busy, vm::createProject)}
        item { Section("Registros recientes","Ver todos") {vm.openHistory()} }
        if(vm.recentEntries.isEmpty()) item {EmptyState("Todavía no registras horas","Tu primer registro aparecerá aquí.")}
        else items(vm.recentEntries,key={it.id}) {EntryCard(it,vm.projects.firstOrNull {p->p.id==it.projectId}) {vm.openEntry(it)}}
        item { Text("Hecho para mantener tu enfoque.",Modifier.fillMaxWidth().padding(top=4.dp),color=Muted,style=MaterialTheme.typography.bodySmall,textAlign=androidx.compose.ui.text.style.TextAlign.Center) }
    }
}

@Composable private fun HeroStats(vm:HomeViewModel,total:Long) {
    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer,MaterialTheme.colorScheme.surfaceVariant))).border(1.dp,MaterialTheme.colorScheme.primary.copy(alpha=.22f),MaterialTheme.shapes.large).padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("TUS HORAS · ${vm.month.format(DateTimeFormatter.ofPattern("MMMM",Spanish)).uppercase(Spanish)}",Modifier.weight(1f),color=MaterialTheme.colorScheme.onPrimaryContainer,style=MaterialTheme.typography.labelSmall)
            Glyph("clock",tint=Mint)
        }
        Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(durationText(total),style=MaterialTheme.typography.displayMedium)
            Text("bien invertidas",Modifier.padding(bottom=7.dp),color=Muted,style=MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            vm.projects.forEach {p -> val seconds=vm.monthEntries.totalSeconds(p.id); if(seconds>0) Box(Modifier.weight(seconds.toFloat()).fillMaxHeight().background(Color(p.color))) }
            if(total==0L) Box(Modifier.fillMaxSize().background(Line))
        }
        Text("${vm.projects.size} proyectos · ${vm.monthEntries.size} registros este mes",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun ProjectRow(project:Project,seconds:Long,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick=onClick).padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(project.color)))
        Column(Modifier.weight(1f)) { Text(project.name,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis); Text(project.client,color=Muted,style=MaterialTheme.typography.bodySmall) }
        Text(durationText(seconds),fontWeight=FontWeight.SemiBold,fontSize=14.sp); Glyph("next",Modifier.size(17.dp),Muted)
    }
}
