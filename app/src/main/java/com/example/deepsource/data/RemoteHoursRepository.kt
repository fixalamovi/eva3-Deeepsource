package com.example.deepsource.data

import com.example.deepsource.data.remote.*
import com.example.deepsource.domain.*

// Las interfaces de Retrofit se pueden sustituir en las pruebas, sin abrir conexiones.
internal class RemoteHoursRepository(
    private val projectsApi: ProjectsApi = BackendClient.projects,
    private val recordsApi: RecordsApi = BackendClient.records
) {
    suspend fun read(session: Session): HoursSnapshot {
        val auth = BackendClient.auth(session)
        val projects = backendCall("Proyectos (8081)") { projectsApi.projects(auth) }
        val tasks = backendCall("Proyectos (8081)") { projectsApi.tasks(auth) }
        val entries = backendCall("Registros (8082)") { recordsApi.entries(auth) }.map { it.domain() }
        return HoursSnapshot(projects, entries, null, tasks)
    }
    suspend fun save(session: Session, entry: TimeEntry, key: String): TimeEntry {
        val auth = BackendClient.auth(session)
        val body = EntryBody(entry.projectId, requireNotNull(entry.taskId) { "Selecciona una tarea" },
            entry.day.toString(), entry.seconds, entry.source)
        val result = if (entry.id == 0L) backendCall { recordsApi.create(auth, key, body) }
            else backendCall { recordsApi.update(auth, entry.id, body) }
        return result.domain()
    }
    suspend fun delete(session: Session, id: Long) =
        backendUnit { recordsApi.delete(BackendClient.auth(session), id) }
    suspend fun createProject(session: Session, name: String, client: String, estimate: Int) {
        backendCall { projectsApi.createProject(BackendClient.auth(session), ProjectBody(name, client, estimate * 60)) }
    }
    suspend fun createTask(session: Session, project: String, name: String): ProjectTask =
        backendCall { projectsApi.createTask(BackendClient.auth(session), project, TaskBody(name)) }
}
