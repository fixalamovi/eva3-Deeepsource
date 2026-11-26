package com.example.deepsource.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepsource.domain.Spanish
import com.example.deepsource.domain.durationText
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.theme.*
import com.example.deepsource.ui.viewmodel.SuccessViewModel
import java.time.format.DateTimeFormatter

@Composable internal fun SuccessScreen(vm:SuccessViewModel) {
    val entry = vm.entry ?: return
    var confirmed by remember(entry.id) { mutableStateOf(false) }
    LaunchedEffect(entry.id) { confirmed = true }
    val badgeSize by animateDpAsState(
        if (confirmed) 88.dp else 64.dp,
        animationSpec = tween(280), label = "Confirmación de registro")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(badgeSize).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha=.12f)).border(1.dp,MaterialTheme.colorScheme.secondary.copy(alpha=.5f),CircleShape),contentAlignment=Alignment.Center) {Glyph("check",Modifier.size(40.dp),MaterialTheme.colorScheme.secondary)}
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {Text("¡Tiempo bien registrado!",style=MaterialTheme.typography.headlineMedium);Text("${durationText(entry.seconds)} en ${vm.projectName}",color=Muted,fontSize=14.sp)}
        PanelCard {
            listOf("Usuario" to vm.accountName,"Día" to entry.day.format(DateTimeFormatter.ofPattern("d MMM yyyy",Spanish)),"Tarea" to entry.task).forEach {(label,value)->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {Text(label,Modifier.width(62.dp),color=Muted,fontSize=13.sp);Text(value,Modifier.weight(1f),fontSize=13.sp,fontWeight=FontWeight.Medium)}
            }
            HorizontalDivider(color=Line)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {Text("Tiempo dedicado",color=Muted,fontSize=13.sp);Badge(durationText(entry.seconds))}
        }
        Text("El resumen y tu widget ya están actualizados.",color=Mint,style=MaterialTheme.typography.bodySmall)
        Primary("Volver al inicio","home") {vm.openHome()}
        Secondary("Agregar otro registro","plus") {vm.addEntry(entry.projectId)}
    }
}
