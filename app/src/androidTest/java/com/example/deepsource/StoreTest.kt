package com.example.deepsource

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.deepsource.data.LocalStore
import com.example.deepsource.data.HoursSnapshot
import com.example.deepsource.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StoreTest {
    @Test fun serverCacheKeepsDemoRecordsAndUsesSeparateServerIds() {
        LocalStore(ApplicationProvider.getApplicationContext()).use { store ->
            store.seedDemo()
            val demo = Session("demo_luis","Luis","demo@example.test",demo=true)
            val before = store.entries(demo).map { it.id to it.seconds }
            val uid = "srv_" + UUID.randomUUID()
            val user = Session(uid,"Servidor","server@example.test")
            val project = Project(UUID.randomUUID().toString(),"Proyecto servidor","Cliente",480,480,0xFFA99BFF)
            val task = ProjectTask(UUID.randomUUID().toString(),project.id,"Desarrollo")
            val record = TimeEntry(1,user.uid,user.name,project.id,LocalDate.now(),600,task.name,taskId=task.id)
            store.cacheServerData(user,HoursSnapshot(listOf(project),listOf(record),null,listOf(task)))
            assertEquals(before,store.entries(demo).map { it.id to it.seconds })
            assertEquals(1L,store.entries(user).single().id)
            assertEquals(600L,store.entries(user).single().seconds)
            store.cacheServerData(user,HoursSnapshot(listOf(project),emptyList(),null,listOf(task)))
            assertTrue(store.entries(user).isEmpty())
            assertEquals(before,store.entries(demo).map { it.id to it.seconds })
        }
    }
    @Test fun accountIsolationAndOwnershipAreEnforced() {
        LocalStore(ApplicationProvider.getApplicationContext()).use {store ->
            val a=Session(UUID.randomUUID().toString(),"Cuenta A","a@example.test")
            val b=Session(UUID.randomUUID().toString(),"Cuenta B","b@example.test")
            store.createProject(a,"Proyecto de A","Cliente",8)
            val project=store.projects(a).single()
            val entry=TimeEntry(uid=a.uid,userName=a.name,projectId=project.id,day=LocalDate.now(),seconds=60,task="Tarea de prueba")
            val id=store.saveEntry(a,entry)
            assertTrue(store.projects(b).isEmpty());assertTrue(store.entries(b).isEmpty())
            assertTrue(runCatching {store.saveEntry(b,entry)}.isFailure)
            assertTrue(runCatching {store.deleteEntry(b,id)}.isFailure)
            store.createProject(b, "Proyecto de B", "Cliente", 8)
            val otherProject = store.projects(b).single()
            val otherEntry = entry.copy(uid = b.uid, userName = b.name, projectId = otherProject.id)
            val otherId = store.saveEntry(b, otherEntry)

            assertEquals(listOf(id), store.entries(a).map { it.id })
            assertEquals(listOf(otherId), store.entries(b).map { it.id })
        }
    }
    @Test fun timerCompletionIsAtomicAndRoundsActiveTimeToMinutes() {
        LocalStore(ApplicationProvider.getApplicationContext()).use {store ->
            val user=Session(UUID.randomUUID().toString(),"Prueba","test@example.test")
            store.createProject(user,"Proyecto prueba","Cliente",8)
            val project=store.projects(user).single()
            store.saveTimer(user,RunningTimer(project.id,"Tarea del cronómetro",5000))
            val saved=store.finishTimer(user,5)
            assertEquals(60L,saved.seconds);assertNull(store.timer(user))
            assertTrue(runCatching {store.finishTimer(user,5)}.isFailure)
            assertEquals(1,store.entries(user).size)
        }
    }

    @Test fun precreatedTaskAcceptsSeveralRecordsAndSurvivesDeletingOne() {
        LocalStore(ApplicationProvider.getApplicationContext()).use { store ->
            val user = Session(UUID.randomUUID().toString(), "Prueba", "test@example.test")
            store.createProject(user, "Proyecto tareas", "Cliente", 8)
            val project = store.projects(user).single()
            assertTrue(store.tasks(user).map { it.name }.containsAll(DefaultTaskNames))
            val task = store.createTask(user, project.id, "Implementar login")
            assertEquals(task.id, store.createTask(user, project.id, "implementar login").id)
            val entry = TimeEntry(uid = user.uid, userName = user.name, projectId = project.id,
                day = LocalDate.now(), seconds = 600, task = task.name, taskId = task.id)
            val first = store.saveEntry(user, entry)
            val second = store.saveEntry(user, entry.copy(seconds = 1200))
            assertNotEquals(first, second)
            assertEquals(2, store.entries(user).forTask(task).size)
            assertEquals(1800L, store.entries(user).forTask(task).totalSeconds())
            store.deleteEntry(user, first)
            assertEquals(listOf(second), store.entries(user).forTask(task).map { it.id })
            assertTrue(store.tasks(user).any { it.id == task.id })
        }
    }

    @Test fun selectedTaskMustBelongToTheSelectedProject() {
        LocalStore(ApplicationProvider.getApplicationContext()).use { store ->
            val user = Session(UUID.randomUUID().toString(), "Prueba", "test@example.test")
            store.createProject(user, "Primer proyecto", "Cliente", 8)
            store.createProject(user, "Segundo proyecto", "Cliente", 8)
            val projects = store.projects(user)
            val foreignTask = store.tasks(user).first { it.projectId == projects[1].id }
            val entry = TimeEntry(uid = user.uid, userName = user.name, projectId = projects[0].id,
                day = LocalDate.now(), seconds = 600, task = foreignTask.name, taskId = foreignTask.id)
            assertTrue(runCatching { store.saveEntry(user, entry) }.isFailure)
            assertTrue(store.entries(user).isEmpty())
        }
    }

    @Test fun roundedTimeOverDailyLimitKeepsTheTimerAndDoesNotAddAnEntry() {
        LocalStore(ApplicationProvider.getApplicationContext()).use { store ->
            val user = Session(UUID.randomUUID().toString(), "Prueba", "test@example.test")
            store.createProject(user, "Proyecto límite", "Cliente", 8)
            val task = store.tasks(user).first()
            store.saveEntry(user, TimeEntry(uid = user.uid, userName = user.name, projectId = task.projectId,
                day = LocalDate.now(), seconds = 86370, task = task.name, taskId = task.id))
            store.saveTimer(user, RunningTimer(task.projectId, task.name, accumulatedMs = 30000, taskId = task.id))
            assertTrue(runCatching { store.completeTimer(user, 0, 0, 0) }.isFailure)
            assertNotNull(store.timer(user))
            assertEquals(1, store.entries(user).size)
        }
    }
    @Test fun dailyTotalCannotExceedTwentyFourHours() {
        LocalStore(ApplicationProvider.getApplicationContext()).use {store ->
            val user=Session(UUID.randomUUID().toString(),"Prueba","test@example.test")
            store.createProject(user,"Proyecto prueba","Cliente",8)
            val project=store.projects(user).single()
            val e=TimeEntry(uid=user.uid,userName=user.name,projectId=project.id,day=LocalDate.now(),seconds=86400,task="Tarea de prueba")
            store.saveEntry(user,e)
            assertTrue(runCatching {store.saveEntry(user,e.copy(seconds=1))}.isFailure)
        }
    }
}
