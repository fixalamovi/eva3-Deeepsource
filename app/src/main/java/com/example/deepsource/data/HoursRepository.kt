package com.example.deepsource.data

import android.content.Context
import com.example.deepsource.domain.Project
import com.example.deepsource.domain.ProjectTask
import com.example.deepsource.domain.RunningTimer
import com.example.deepsource.domain.Session
import com.example.deepsource.domain.TimeEntry
import com.example.deepsource.domain.roundedTimerSeconds
import com.example.deepsource.BuildConfig
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

internal data class HoursSnapshot(
    val projects: List<Project>,
    val entries: List<TimeEntry>,
    val timer: RunningTimer?,
    val tasks: List<ProjectTask> = emptyList()
)

internal class HoursRepository(context: Context) : Closeable {
    private val store = LocalStore(context.applicationContext)
    private val remote = RemoteHoursRepository()
    private fun usesServer(session: Session) = BuildConfig.USE_SERVER && !session.demo

    suspend fun read(session: Session): HoursSnapshot = withContext(Dispatchers.IO) {
        if (usesServer(session)) {
            val snapshot = remote.read(session)
            store.cacheServerData(session, snapshot)
            return@withContext snapshot.copy(timer = store.timer(session))
        }
        HoursSnapshot(
            projects = store.projects(session),
            entries = store.entries(session),
            timer = store.timer(session),
            tasks = store.tasks(session)
        )
    }

    suspend fun seedDemo() = withContext(Dispatchers.IO) { store.seedDemo() }

    suspend fun saveEntry(session: Session, entry: TimeEntry, key: String = UUID.randomUUID().toString()): TimeEntry =
        withContext(Dispatchers.IO) {
            if (usesServer(session)) remote.save(session, entry, key)
            else entry.copy(id = store.saveEntry(session, entry))
        }

    suspend fun deleteEntry(session: Session, id: Long) =
        withContext(Dispatchers.IO) {
            if (usesServer(session)) remote.delete(session, id) else store.deleteEntry(session, id)
        }

    suspend fun createProject(session: Session, name: String, client: String, estimate: Int) =
        withContext(Dispatchers.IO) {
            if (usesServer(session)) remote.createProject(session, name, client, estimate)
            else store.createProject(session, name, client, estimate)
        }

    suspend fun createTask(session: Session, projectId: String, name: String): ProjectTask =
        withContext(Dispatchers.IO) {
            if (usesServer(session)) remote.createTask(session, projectId, name)
            else store.createTask(session, projectId, name)
        }
    suspend fun saveTimer(session: Session, timer: RunningTimer?) =
        withContext(Dispatchers.IO) { store.saveTimer(session, timer) }

    suspend fun toggleTimer(session: Session, wall: Long, elapsed: Long, boot: Int, pauseOnly: Boolean = false) =
        withContext(Dispatchers.IO) { store.toggleTimer(session, wall, elapsed, boot, pauseOnly) }

    suspend fun completeTimer(session: Session, wall: Long, elapsed: Long, boot: Int): TimeEntry =
        withContext(Dispatchers.IO) {
            if (!usesServer(session)) return@withContext store.completeTimer(session, wall, elapsed, boot)
            val timer = store.timer(session) ?: error("No hay un cronómetro activo")
            val entry = TimeEntry(uid=session.uid,userName=session.name,projectId=timer.projectId,
                day=LocalDate.now(),seconds=roundedTimerSeconds(timer.elapsed(wall,elapsed,boot)),
                task=timer.task,source="Cronómetro",taskId=timer.taskId)
            val saved = remote.save(session,entry,timer.clientId)
            store.clearCompletedTimer(session,timer.clientId)
            saved
        }

    override fun close() = store.close()
}
