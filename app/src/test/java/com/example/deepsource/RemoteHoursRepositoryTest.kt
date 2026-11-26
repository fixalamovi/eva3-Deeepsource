package com.example.deepsource

import com.example.deepsource.data.RemoteHoursRepository
import com.example.deepsource.data.remote.*
import com.example.deepsource.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy
import java.time.LocalDate
import kotlin.coroutines.Continuation

class RemoteHoursRepositoryTest {
    // Dobles de las interfaces Retrofit: registran llamadas, sin HTTP ni datos reales.
    private class ApiDouble<T>(type: Class<T>) {
        val calls = mutableListOf<Pair<String, List<Any?>>>()
        var reply: (String) -> Any? = { error("Llamada inesperada: $it") }
        @Suppress("UNCHECKED_CAST")
        val api = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
            calls += method.name to args.orEmpty().filterNot { it is Continuation<*> }
            reply(method.name)
        } as T
    }
    private val projects = ApiDouble(ProjectsApi::class.java)
    private val records = ApiDouble(RecordsApi::class.java)
    private val repository = RemoteHoursRepository(projects.api, records.api)
    private val session = Session("srv_1", "Persona Prueba", "persona@example.com", token = "ds_test")
    private val day = LocalDate.of(2026, 11, 20)
    private val remote = RemoteEntry(7, session.uid, session.name, "p1", day.toString(), 120, "Pruebas", "Manual", "t1")
    private val entry = remote.domain()

    @Test fun readCombinesProjectsTasksAndServerEntries() = runBlocking {
        val project = Project("p1", "Aplicación", "Cliente", 600, 600, 0)
        val task = ProjectTask("t1", "p1", "Pruebas")
        projects.reply = { if (it == "projects") Response.success(listOf(project)) else Response.success(listOf(task)) }
        records.reply = { Response.success(listOf(remote)) }
        val snapshot = repository.read(session)
        assertEquals(listOf(project), snapshot.projects)
        assertEquals(listOf(task), snapshot.tasks)
        assertEquals(listOf(entry), snapshot.entries)
        assertNull(snapshot.timer)
        assertTrue((projects.calls + records.calls).all { it.second.first() == "Bearer ds_test" })
    }

    @Test fun newEntryUsesPostAndPreservesRetryKey() = runBlocking {
        records.reply = { Response.success(remote) }
        assertEquals(entry, repository.save(session, entry.copy(id = 0), "draft-42"))
        val call = records.calls.single()
        assertEquals("create", call.first)
        assertEquals("draft-42", call.second[1])
        assertEquals(EntryBody("p1", "t1", day.toString(), 120, "Manual"), call.second[2])
    }

    @Test fun retriesReuseTheSameKey() = runBlocking {
        records.reply = { Response.success(remote) }
        repeat(2) { repository.save(session, entry.copy(id = 0), "same-draft") }
        assertEquals(listOf("same-draft", "same-draft"), records.calls.map { it.second[1] })
    }

    @Test fun existingEntryUsesPutWithServerId() = runBlocking {
        records.reply = { Response.success(remote.copy(seconds = 300)) }
        assertEquals(300L, repository.save(session, entry.copy(seconds = 300), "unused").seconds)
        assertEquals("update", records.calls.single().first)
        assertEquals(7L, records.calls.single().second[1])
    }

    @Test fun missingTaskFailsBeforeSendingARecord() = runBlocking {
        val error = runCatching { repository.save(session, entry.copy(taskId = null), "draft") }.exceptionOrNull()
        assertEquals("Selecciona una tarea", error?.message)
        assertTrue(records.calls.isEmpty())
    }

    @Test fun localSessionCannotCallRemoteEndpoints() = runBlocking {
        assertTrue(runCatching { repository.read(session.copy(token = "local-token")) }.isFailure)
        assertTrue(projects.calls.isEmpty())
        assertTrue(records.calls.isEmpty())
    }

    @Test fun deleteUsesTheServerIdAndAccepts204() = runBlocking {
        records.reply = { Response.success<Unit>(204, null) }
        repository.delete(session, 7)
        assertEquals("delete", records.calls.single().first)
        assertEquals(7L, records.calls.single().second[1])
    }

    @Test fun projectEstimateIsConvertedFromHoursToMinutes() = runBlocking {
        projects.reply = { Response.success(Project("p2", "Portal", "Cliente", 1920, 1920, 0)) }
        repository.createProject(session, "Portal", "Cliente", 32)
        assertEquals(ProjectBody("Portal", "Cliente", 1920), projects.calls.single().second[1])
    }

    @Test fun createsTaskInTheSelectedProject() = runBlocking {
        val task = ProjectTask("t2", "p1", "Documentación")
        projects.reply = { Response.success(task) }
        assertEquals(task, repository.createTask(session, "p1", "Documentación"))
        assertEquals(listOf("Bearer ds_test", "p1", TaskBody("Documentación")), projects.calls.single().second)
    }

    @Test fun networkFailureDoesNotReturnAnEmptySuccessfulSnapshot() = runBlocking {
        val offline = object : ProjectsApi by projects.api {
            override suspend fun projects(auth: String): Response<List<Project>> = throw java.io.IOException("offline")
        }
        val error = runCatching { RemoteHoursRepository(offline, records.api).read(session) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("conectar"))
        assertTrue(records.calls.isEmpty())
    }

    @Test fun serverRecordKeepsIdentityDateTaskAndSource() {
        assertEquals(day, entry.day)
        assertEquals(session.uid, entry.uid)
        assertEquals("t1", entry.taskId)
        assertEquals("Manual", entry.source)
        assertEquals("Pruebas", entry.task)
    }

    @Test fun malformedServerDateIsRejected() {
        assertTrue(runCatching { remote.copy(day = "20/11/2026").domain() }.isFailure)
    }
}
