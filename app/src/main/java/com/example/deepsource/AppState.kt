package com.example.deepsource

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.deepsource.data.AuthRepository
import com.example.deepsource.data.HoursRepository
import com.example.deepsource.data.HoursSnapshot
import com.example.deepsource.data.ProfilePhotos
import com.example.deepsource.data.SessionVault
import com.example.deepsource.domain.RunningTimer
import com.example.deepsource.domain.ProjectTask
import com.example.deepsource.domain.Session
import com.example.deepsource.domain.TimeEntry
import com.example.deepsource.widget.HoursWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.util.UUID

internal class AppState(val application: Application) {
    val hours = HoursRepository(application)
    val auth = AuthRepository(application)
    val photos = ProfilePhotos(application)
    private val vault = SessionVault(application)

    private var snapshot by mutableStateOf(HoursSnapshot(emptyList(), emptyList(), null))
    val projects get() = snapshot.projects
    val records get() = snapshot.entries
    val timer get() = snapshot.timer
    val tasks get() = snapshot.tasks

    var session by mutableStateOf<Session?>(null)
        private set
    var profilePhoto by mutableStateOf<Bitmap?>(null)
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    var month by mutableStateOf(YearMonth.now())

    var screen by mutableStateOf("home")
        private set
    var editing by mutableStateOf<TimeEntry?>(null)
        private set
    var selectedProject by mutableStateOf<String?>(null)
        private set
    var selectedTask by mutableStateOf<String?>(null)
        private set
    var entryDraftKey: String = UUID.randomUUID().toString()
        private set
    var lastSaved by mutableStateOf<TimeEntry?>(null)
        private set
    private var pendingRoute: String? = null

    val boot: Int
        get() = Settings.Global.getInt(application.contentResolver, Settings.Global.BOOT_COUNT, 0)

    fun elapsedMs(): Long =
        timer?.elapsed(System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot) ?: 0L

    fun navigate(destination: String) {
        screen = destination
    }

    fun acceptRoute(destination: String, project: String? = null) {
        if (project != null) selectedProject = project
        selectedTask = null
        if (destination == "entry") editing = null
        if (session == null) pendingRoute = destination else navigate(destination)
    }

    fun openEntry(entry: TimeEntry? = null, project: String? = null, task: ProjectTask? = null) {
        entryDraftKey = UUID.randomUUID().toString()
        editing = entry
        selectedProject = project ?: entry?.projectId ?: task?.projectId
        selectedTask = entry?.taskId ?: task?.id
        navigate("entry")
    }

    fun openProject(project: String) {
        selectedProject = project
        selectedTask = null
        navigate("project")
    }

    fun openTimer(project: String, task: ProjectTask? = null) {
        selectedProject = project
        selectedTask = task?.id
        navigate("timer")
    }

    suspend fun restoreSession() {
        val restored = withContext(Dispatchers.IO) { vault.read() } ?: return
        if (!auth.canRestore(restored)) {
            withContext(Dispatchers.IO) { vault.save(null) }
            return
        }
        val restoredPhoto = readProfilePhoto(restored)
        val restoredHours = hours.read(restored)
        profilePhoto = restoredPhoto
        snapshot = restoredHours
        session = restored
        applyPendingRoute()
    }

    suspend fun startSession(account: Session) {
        if (account.demo) hours.seedDemo()
        val photo = readProfilePhoto(account)
        val loadedHours = hours.read(account)
        withContext(Dispatchers.IO) { vault.save(account) }
        profilePhoto = photo
        snapshot = loadedHours
        session = account
        applyPendingRoute()
        if (account.demo && records.isNotEmpty()) {
            month = YearMonth.from(records.maxBy { it.day }.day)
        }
        updateWidgets()
    }

    suspend fun updateSession(account: Session) {
        withContext(Dispatchers.IO) { vault.save(account) }
        session = account
    }

    suspend fun endSession() {
        withContext(Dispatchers.IO) { vault.save(null) }
        session = null
        profilePhoto = null
        snapshot = HoursSnapshot(emptyList(), emptyList(), null)
        navigate("home")
        editing = null
        selectedProject = null
        selectedTask = null
        lastSaved = null
        updateWidgets()
    }

    private fun applyPendingRoute() {
        navigate(pendingRoute ?: "home")
        pendingRoute = null
    }

    private suspend fun readProfilePhoto(account: Session): Bitmap? =
        withContext(Dispatchers.IO) { photos.load(account.uid) }

    suspend fun reload() {
        snapshot = session?.let { hours.read(it) } ?: HoursSnapshot(emptyList(), emptyList(), null)
    }

    fun replaceTimer(value: RunningTimer?) {
        snapshot = snapshot.copy(timer = value)
    }

    suspend fun showReceipt(entry: TimeEntry) {
        lastSaved = entry
        month = YearMonth.from(entry.day)
        editing = null
        reload()
        navigate("success")
        updateWidgets()
    }

    suspend fun createProject(name: String, client: String, estimate: Int) {
        hours.createProject(requireNotNull(session), name, client, estimate)
        reload()
        message = "Proyecto creado"
    }

    suspend fun createTask(projectId: String, name: String): ProjectTask {
        val task = hours.createTask(requireNotNull(session), projectId, name)
        reload()
        message = "Tarea disponible"
        return task
    }

    suspend fun refresh() {
        reload()
        updateWidgets()
    }

    suspend fun updateWidgets() = withContext(Dispatchers.IO) {
        HoursWidget.updateAll(application)
    }

    fun launchAction(scope: CoroutineScope, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message = error.message ?: "No pudimos completar la acción. Inténtalo de nuevo"
            } finally {
                busy = false
            }
        }
    }
}
