package com.example.deepsource

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.deepsource.data.LocalStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskMigrationTest {
    @Test fun versionOneKeepsDurationsAndLinksRepeatedDescriptionsToOneTask() {
        SQLiteDatabase.create(null).use { db ->
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL("CREATE TABLE projects(id TEXT PRIMARY KEY, workspace TEXT, name TEXT, client TEXT, estimate INTEGER, team_estimate INTEGER, color INTEGER)")
            db.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY AUTOINCREMENT, uid TEXT, user_name TEXT, project_id TEXT REFERENCES projects(id), day TEXT, seconds INTEGER, task TEXT, source TEXT)")
            db.execSQL("CREATE TABLE timers(uid TEXT PRIMARY KEY, project_id TEXT REFERENCES projects(id), task TEXT, accumulated INTEGER, wall INTEGER, elapsed INTEGER, boot INTEGER)")
            db.execSQL("INSERT INTO projects VALUES('p','u','Proyecto','Cliente',480,480,0)")
            db.execSQL("INSERT INTO entries VALUES(1,'u','Usuario','p','2026-11-25',90,'Implementar login','Manual')")
            db.execSQL("INSERT INTO entries VALUES(2,'u','Usuario','p','2026-11-25',120,'implementar login','Cronómetro')")
            db.execSQL("INSERT INTO timers VALUES('u','p','Implementar login',30000,0,0,1)")
            LocalStore(ApplicationProvider.getApplicationContext()).use { store -> store.onUpgrade(db, 1, 3) }
            db.rawQuery("SELECT COUNT(*),SUM(seconds),COUNT(DISTINCT task_id) FROM entries", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(2, c.getInt(0))
                assertEquals(210L, c.getLong(1))
                assertEquals(1, c.getInt(2))
            }
            db.rawQuery("SELECT t.accumulated,t.task_id,e.task_id FROM timers t JOIN entries e ON e.id=1", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(30000L, c.getLong(0))
                assertNotNull(c.getString(1))
                assertEquals(c.getString(1), c.getString(2))
            }
            db.rawQuery("PRAGMA foreign_key_check", null).use { c -> assertFalse(c.moveToFirst()) }
        }
    }
}
