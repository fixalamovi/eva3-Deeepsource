package com.example.deepsource.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepsource.domain.durationText
import com.example.deepsource.domain.totalSeconds
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.theme.*
import com.example.deepsource.ui.viewmodel.SummaryViewModel

@Composable internal fun SummaryScreen(vm:SummaryViewModel) {
    val entries=vm.entries
    val total=vm.totalSeconds
    val estimate=vm.estimateMinutes
    val percent=vm.deviationPercent
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Identity(vm.session)}
        item {MonthSelector(vm.month, vm::selectMonth)}
        item { PanelCard {
            Text("MIS HORAS",color=Muted,style=MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {Text(durationText(total),style=MaterialTheme.typography.headlineLarge,fontSize=36.sp); Text("de ${durationText(estimate*60L)} estimadas",color=Muted,style=MaterialTheme.typography.bodySmall)}
                if(percent!=null) Column(horizontalAlignment=Alignment.End) {Text("${if(percent>0) "+" else ""}$percent %",fontSize=24.sp,fontWeight=FontWeight.Bold,color=if(percent>0) Danger else Mint);Text(if(total>estimate*60L) "Sobre estimación" else "Dentro de estimación",color=Muted,fontSize=10.sp)}
            }
        } }
        item {Section("Registradas frente a estimadas")}
        if(vm.projects.isEmpty()) item {EmptyState("Aún no hay proyectos","Crea un proyecto desde Inicio.","chart")}
        items(vm.projects,key={it.id}) {p -> BalanceCard(p,entries.totalSeconds(p.id),p.estimateMinutes)}
        item {Text("Las estimaciones se comparan con las horas del mes seleccionado. El porcentaje indica desviación de tiempo, no avance de tareas.",color=Muted,style=MaterialTheme.typography.bodySmall)}
    }
}
