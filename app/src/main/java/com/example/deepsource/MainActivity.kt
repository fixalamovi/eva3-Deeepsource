package com.example.deepsource

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.deepsource.ui.screens.*
import com.example.deepsource.ui.components.*
import com.example.deepsource.ui.theme.*
import com.example.deepsource.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    private lateinit var navigation: NavigationViewModel

    private fun screenModels(app: AppState) = ViewModelProvider(this, viewModelFactory {
        initializer { LoginViewModel(app) }
        initializer { HomeViewModel(app) }
        initializer { SummaryViewModel(app) }
        initializer { HistoryViewModel(app) }
        initializer { EntryViewModel(app) }
        initializer { TimerViewModel(app) }
        initializer { SuccessViewModel(app) }
        initializer { ProfileViewModel(app) }
        initializer { WidgetViewModel(app) }
        initializer { ProjectViewModel(app) }
    })
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        navigation = ViewModelProvider(this)[NavigationViewModel::class.java]
        if (savedInstanceState == null) {
            acceptRoute(intent)
        } else if (navigation.loading) {
            if (savedInstanceState.getBoolean("profile_open")) {
                navigation.state.acceptRoute("profile")
            } else {
                acceptRoute(intent)
            }
        }
        val models = screenModels(navigation.state)
        setContent { DeepsourceTheme { DeepsourceApp(navigation, models) } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("profile_open", navigation.state.screen == "profile")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptRoute(intent)
    }

    override fun onResume() {
        super.onResume()
        navigation.refresh()
    }

    private fun acceptRoute(intent: Intent?) {
        val route = intent?.getStringExtra("route") ?: return
        if (route in listOf("entry", "timer", "home", "widget")) {
            navigation.state.acceptRoute(route, intent.getStringExtra("project"))
        }
    }
}

@Composable
private fun DeepsourceApp(navigation: NavigationViewModel, models: ViewModelProvider) {
    val app = navigation.state
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(app.message) {
        val message = app.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        if (app.message == message) app.message = null
    }
    BackHandler(enabled=app.session!=null && app.screen!="home") { app.navigate("home") }
    val nav=listOf(Triple("home","Inicio","home"),Triple("summary","Resumen","chart"),
        Triple("history","Registros","history"),Triple("widget","Widget","widget"),Triple("profile","Perfil","person"))
    val showNav=app.session!=null && app.screen in nav.map { it.first }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
            Scaffold(Modifier.widthIn(max=640.dp).fillMaxSize(),containerColor=MaterialTheme.colorScheme.background,
                contentWindowInsets=WindowInsets(0,0,0,0),snackbarHost={SnackbarHost(snackbar)},
                topBar={ if(app.session!=null) {
                    Row(Modifier.fillMaxWidth().background(Ink).statusBarsPadding().heightIn(min=66.dp).padding(horizontal=20.dp, vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                        if(app.screen=="home") Brand(true) else {
                            if(!showNav) IconButton({app.navigate("home")}, Modifier.semantics { contentDescription = "Volver al inicio" }) { Glyph("back") }
                            Text(when(app.screen) { "summary"->"Resumen de horas"; "history"->"Mis registros"; "widget"->"Tu widget"; "profile"->"Mi perfil"; "entry"->if(app.editing==null) "Registrar horas" else "Editar registro"; "timer"->"Cronómetro"; "project"->"Detalle del proyecto"; else->"Deepsource" },style=MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.weight(1f))
                        if(app.screen=="home") IconButton({app.navigate("profile")}, Modifier.semantics { contentDescription = "Abrir perfil" }) { Avatar(app.session!!, 36, app.profilePhoto) }
                    }
                } },
                bottomBar={ if(showNav) NavigationBar(containerColor=Ink,tonalElevation=0.dp,windowInsets=WindowInsets.navigationBars) {
                    nav.forEach { (route,label,icon) -> NavigationBarItem(selected=app.screen==route,onClick={app.navigate(route)},
                        icon={Glyph(icon)},label={Text(label)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Mint,selectedTextColor=Mint,indicatorColor=Mint.copy(alpha=.1f),unselectedIconColor=Muted,unselectedTextColor=Muted)) }
                } }
            ) { inner ->
                Box(Modifier.fillMaxSize().padding(inner)) {
                    val route = when {
                        navigation.loading -> "loading"
                        app.session == null -> "login"
                        else -> app.screen
                    }
                    ScreenEntrance(route) {
                        when (route) {
                            "loading" -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator(color=MaterialTheme.colorScheme.secondary) }
                            "login" -> LoginScreen(models[LoginViewModel::class.java])
                            "home" -> HomeScreen(models[HomeViewModel::class.java])
                            "summary" -> SummaryScreen(models[SummaryViewModel::class.java])
                            "history" -> HistoryScreen(models[HistoryViewModel::class.java])
                            "entry" -> EntryScreen(models[EntryViewModel::class.java])
                            "timer" -> TimerScreen(models[TimerViewModel::class.java])
                            "success" -> SuccessScreen(models[SuccessViewModel::class.java])
                            "profile" -> ProfileScreen(models[ProfileViewModel::class.java])
                            "widget" -> WidgetScreen(models[WidgetViewModel::class.java])
                            "project" -> ProjectScreen(models[ProjectViewModel::class.java])
                            else -> HomeScreen(models[HomeViewModel::class.java])
                        }
                    }
                    if(app.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter),color=Mint,trackColor=Color.Transparent)
                }
            }
        }
    }
}
