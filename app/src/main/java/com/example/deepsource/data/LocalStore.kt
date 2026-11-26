package com.example.deepsource.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.deepsource.domain.*
import java.time.LocalDate
import java.util.UUID

/** Tareas por proyecto y registros por usuario. La versión 2 conserva los datos anteriores. */
class LocalStore(context: Context) : SQLiteOpenHelper(context, "deepsource.db", null, 3) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE projects(id TEXT PRIMARY KEY, workspace TEXT NOT NULL, name TEXT NOT NULL, client TEXT NOT NULL, estimate INTEGER NOT NULL, team_estimate INTEGER NOT NULL, color INTEGER NOT NULL)")
        createTasksTable(db)
        db.execSQL("CREATE TABLE entries(id INTEGER PRIMARY KEY AUTOINCREMENT, uid TEXT NOT NULL, user_name TEXT NOT NULL, project_id TEXT NOT NULL REFERENCES projects(id), day TEXT NOT NULL, seconds INTEGER NOT NULL CHECK(seconds > 0), task TEXT NOT NULL, source TEXT NOT NULL, task_id TEXT REFERENCES tasks(id), server_id INTEGER)")
        db.execSQL("CREATE INDEX entries_user_day ON entries(uid,day)")
        db.execSQL("CREATE INDEX entries_task ON entries(task_id,uid)")
        db.execSQL("CREATE TABLE timers(uid TEXT PRIMARY KEY, project_id TEXT NOT NULL REFERENCES projects(id), task TEXT NOT NULL, accumulated INTEGER NOT NULL, wall INTEGER NOT NULL, elapsed INTEGER NOT NULL, boot INTEGER NOT NULL, task_id TEXT REFERENCES tasks(id), client_id TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX entries_server_identity ON entries(uid,server_id)")
        db.execSQL("CREATE TABLE flags(name TEXT PRIMARY KEY)")
    }

    private fun createTasksTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE tasks(id TEXT PRIMARY KEY, project_id TEXT NOT NULL REFERENCES projects(id), name TEXT NOT NULL COLLATE NOCASE, UNIQUE(project_id,name))")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createTasksTable(db)
            db.execSQL("ALTER TABLE entries ADD COLUMN task_id TEXT REFERENCES tasks(id)")
            db.execSQL("ALTER TABLE timers ADD COLUMN task_id TEXT REFERENCES tasks(id)")
            db.execSQL("CREATE INDEX entries_task ON entries(task_id,uid)")
            val projectIds = db.rawQuery("SELECT id FROM projects", null).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            projectIds.forEach { project -> DefaultTaskNames.forEach { ensureTask(db, project, it) } }
            // Las descripciones antiguas se convierten en tareas reutilizables sin borrar registros.
            for (table in listOf("entries", "timers")) {
                val previous = db.rawQuery("SELECT rowid,project_id,task FROM $table", null).use { cursor ->
                    buildList { while (cursor.moveToNext()) add(Triple(cursor.getLong(0), cursor.getString(1), cursor.getString(2))) }
                }
                previous.forEach { (row, project, name) ->
                    val task = ensureTask(db, project, name)
                    db.update(table, ContentValues().apply { put("task_id", task.id) }, "rowid=?", arrayOf(row.toString()))
                }
            }
        }
        if (oldVersion < 3 && newVersion >= 3) {
            db.execSQL("ALTER TABLE timers ADD COLUMN client_id TEXT")
            db.execSQL("UPDATE timers SET client_id=lower(hex(randomblob(16))) WHERE client_id IS NULL")
            db.execSQL("ALTER TABLE entries ADD COLUMN server_id INTEGER")
            db.execSQL("CREATE UNIQUE INDEX entries_server_identity ON entries(uid,server_id)")
        }
    }

    fun projects(session: Session): List<Project> = readableDatabase.query("projects", null,
        "workspace=?", arrayOf(session.workspace), null, null, "rowid").use { c ->
        buildList { while(c.moveToNext()) add(Project(c.getString(c.getColumnIndexOrThrow("id")),
            c.getString(c.getColumnIndexOrThrow("name")), c.getString(c.getColumnIndexOrThrow("client")),
            c.getInt(c.getColumnIndexOrThrow("estimate")), c.getInt(c.getColumnIndexOrThrow("team_estimate")),
            c.getLong(c.getColumnIndexOrThrow("color")))) }
    }

    fun tasks(session: Session): List<ProjectTask> = readableDatabase.rawQuery(
        "SELECT t.id,t.project_id,t.name FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.workspace=? ORDER BY t.name COLLATE NOCASE",
        arrayOf(session.workspace)
    ).use { c -> buildList { while(c.moveToNext()) add(ProjectTask(c.getString(0), c.getString(1), c.getString(2))) } }

    private fun ensureTask(db: SQLiteDatabase, projectId: String, name: String): ProjectTask {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        require(validateTask(clean) == null) { "Ingresa una tarea de 3 a 160 caracteres" }
        val existing = db.query("tasks", arrayOf("id", "name"), "project_id=?", arrayOf(projectId), null, null, null).use { c ->
            var found: ProjectTask? = null
            while (c.moveToNext()) {
                if (c.getString(1).equals(clean, ignoreCase = true)) {
                    found = ProjectTask(c.getString(0), projectId, c.getString(1))
                    break
                }
            }
            found
        }
        if (existing != null) return existing
        val task = ProjectTask(UUID.randomUUID().toString(), projectId, clean)
        db.insertOrThrow("tasks", null, ContentValues().apply {
            put("id", task.id); put("project_id", task.projectId); put("name", task.name)
        })
        return task
    }

    fun createTask(session: Session, projectId: String, name: String): ProjectTask {
        require(projects(session).any { it.id == projectId }) { "Proyecto no disponible" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val task = ensureTask(db, projectId, name)
            db.setTransactionSuccessful()
            return task
        } finally { db.endTransaction() }
    }

    private fun resolveTask(db: SQLiteDatabase, projectId: String, taskId: String?, name: String): ProjectTask {
        if (taskId == null) return ensureTask(db, projectId, name)
        return db.query("tasks", arrayOf("id","name"), "id=? AND project_id=?",
            arrayOf(taskId, projectId), null, null, null).use { c ->
            require(c.moveToFirst()) { "La tarea no pertenece al proyecto seleccionado" }
            ProjectTask(c.getString(0), projectId, c.getString(1))
        }
    }

    fun entries(session: Session): List<TimeEntry> = readableDatabase.rawQuery(
        "SELECT e.* FROM entries e JOIN projects p ON p.id=e.project_id WHERE e.uid=? AND p.workspace=? ORDER BY e.day DESC,e.id DESC",
        arrayOf(session.uid, session.workspace)
    ).use { c ->
        buildList { while(c.moveToNext()) add(TimeEntry(
            if (c.isNull(c.getColumnIndexOrThrow("server_id"))) c.getLong(0) else c.getLong(c.getColumnIndexOrThrow("server_id")),
            c.getString(1),c.getString(2),
            c.getString(3),LocalDate.parse(c.getString(4)),c.getLong(5),c.getString(6),c.getString(7),
            c.getString(c.getColumnIndexOrThrow("task_id")))) }
    }

    fun saveEntry(session: Session, entry: TimeEntry): Long {
        require(entry.uid == session.uid) { "Solo puedes registrar tus propias horas" }
        require(projects(session).any { it.id == entry.projectId }) { "Proyecto no disponible" }
        require(entry.day <= LocalDate.now() && entry.seconds in 1..86400 && validateTask(entry.task) == null) {
            "Revisa la fecha, duración y tarea"
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val already = db.rawQuery("SELECT COALESCE(SUM(seconds),0) FROM entries WHERE uid=? AND day=? AND id<>?",
                arrayOf(session.uid, entry.day.toString(), entry.id.toString())).use { it.moveToFirst(); it.getLong(0) }
            require(already + entry.seconds <= 86400) { "Los registros de un día no pueden superar 24 horas" }
            val task = resolveTask(db, entry.projectId, entry.taskId, entry.task)
            val values = ContentValues().apply {
                put("uid",session.uid); put("user_name",session.name); put("project_id",entry.projectId)
                put("day",entry.day.toString()); put("seconds",entry.seconds); put("task",task.name)
                put("source",entry.source); put("task_id",task.id)
            }
            val id = if(entry.id == 0L) db.insertOrThrow("entries",null,values) else {
                require(db.update("entries",values,"id=? AND uid=?",arrayOf(entry.id.toString(),session.uid)) == 1) { "Registro no disponible" }
                entry.id
            }
            db.setTransactionSuccessful()
            return id
        } finally { db.endTransaction() }
    }

    fun deleteEntry(session: Session, id: Long) {
        require(writableDatabase.delete("entries","id=? AND uid=?",arrayOf(id.toString(),session.uid)) == 1)
    }

    fun createProject(session: Session, name: String, client: String, estimate: Int) {
        require(validateProject(name, client, estimate).valid) { "Revisa los datos del proyecto" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val id = UUID.randomUUID().toString()
            db.insertOrThrow("projects",null,ContentValues().apply {
                put("id",id); put("workspace",session.workspace); put("name",name.trim()); put("client",client.trim())
                put("estimate",estimate * 60); put("team_estimate",estimate * 60); put("color",0xFFA99BFF)
            })
            DefaultTaskNames.forEach { ensureTask(db, id, it) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun timer(session: Session): RunningTimer? = readableDatabase.rawQuery(
        "SELECT t.* FROM timers t JOIN projects p ON p.id=t.project_id WHERE t.uid=? AND p.workspace=?",
        arrayOf(session.uid, session.workspace)
    ).use { c ->
        if(!c.moveToFirst()) null else RunningTimer(c.getString(1),c.getString(2),c.getLong(3),
            c.getLong(4),c.getLong(5),c.getInt(6),c.getString(c.getColumnIndexOrThrow("task_id")),
            c.getString(c.getColumnIndexOrThrow("client_id")))
    }

    fun saveTimer(session: Session, timer: RunningTimer?) {
        if(timer == null) { writableDatabase.delete("timers","uid=?",arrayOf(session.uid)); return }
        require(projects(session).any { it.id == timer.projectId }) { "Proyecto no disponible" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val task = resolveTask(db, timer.projectId, timer.taskId, timer.task)
            db.insertWithOnConflict("timers",null,ContentValues().apply {
                put("uid",session.uid); put("project_id",timer.projectId); put("task",task.name); put("task_id",task.id)
                put("client_id",timer.clientId)
                put("accumulated",timer.accumulatedMs); put("wall",timer.startedWall)
                put("elapsed",timer.startedElapsed); put("boot",timer.boot)
            },SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun toggleTimer(session:Session,wall:Long,elapsed:Long,boot:Int,pauseOnly:Boolean=false):RunningTimer? {
        val db=writableDatabase
        db.beginTransaction()
        try {
            val current=timer(session) ?: return null
            val next=if(current.running) current.copy(accumulatedMs=current.elapsed(wall,elapsed,boot),startedWall=0,startedElapsed=0)
                else if(pauseOnly) current else current.copy(startedWall=wall,startedElapsed=elapsed,boot=boot)
            saveTimer(session,next)
            db.setTransactionSuccessful()
            return next
        } finally {db.endTransaction()}
    }

    fun completeTimer(session:Session,wall:Long,elapsed:Long,boot:Int):TimeEntry {
        val db=writableDatabase
        db.beginTransaction()
        try {
            val current=timer(session) ?: error("No hay un cronómetro activo")
            val seconds=roundedTimerSeconds(current.elapsed(wall,elapsed,boot))
            val result=finishTimer(session,seconds)
            db.setTransactionSuccessful()
            return result
        } finally {db.endTransaction()}
    }

    fun clearCompletedTimer(session: Session, clientId: String) {
        writableDatabase.delete("timers","uid=? AND client_id=?",arrayOf(session.uid,clientId))
    }

    /** Caché para el widget. Los IDs remotos no reemplazan los IDs de registros locales/demo. */
    internal fun cacheServerData(session: Session, snapshot: HoursSnapshot) {
        val projectIds = snapshot.projects.map { it.id }.toSet()
        val taskIds = snapshot.tasks.map { it.id }.toSet()
        require(snapshot.entries.all { it.uid == session.uid && it.projectId in projectIds && it.taskId in taskIds }) {
            "El servidor devolvió relaciones de datos incompletas"
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            snapshot.projects.forEach { p ->
                val values = ContentValues().apply {
                    put("id",p.id);put("workspace",session.workspace);put("name",p.name);put("client",p.client)
                    put("estimate",p.estimateMinutes);put("team_estimate",p.teamEstimateMinutes);put("color",p.color)
                }
                if(db.update("projects",values,"id=? AND workspace=?",arrayOf(p.id,session.workspace))==0)
                    db.insertOrThrow("projects",null,values)
            }
            snapshot.tasks.forEach { task ->
                require(task.projectId in projectIds)
                val values=ContentValues().apply {put("id",task.id);put("project_id",task.projectId);put("name",task.name)}
                if(db.update("tasks",values,"id=? AND project_id=?",arrayOf(task.id,task.projectId))==0)
                    db.insertOrThrow("tasks",null,values)
            }
            db.delete("entries","uid=?",arrayOf(session.uid))
            snapshot.entries.forEach { e ->
                db.insertOrThrow("entries",null,ContentValues().apply {
                    put("uid",e.uid);put("user_name",e.userName);put("project_id",e.projectId)
                    put("day",e.day.toString());put("seconds",e.seconds);put("task",e.task)
                    put("source",e.source);put("task_id",e.taskId);put("server_id",e.id)
                })
            }
            tasks(session).filter { it.id !in taskIds }.forEach {
                db.delete("tasks","id=? AND NOT EXISTS(SELECT 1 FROM timers WHERE task_id=?)",arrayOf(it.id,it.id))
            }
            projects(session).filter { it.id !in projectIds }.forEach {
                db.delete("projects","id=? AND NOT EXISTS(SELECT 1 FROM timers WHERE project_id=?)",arrayOf(it.id,it.id))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    /** Terminar y borrar el cronómetro forman una sola transacción. */
    fun finishTimer(session: Session, seconds: Long): TimeEntry {
        require(seconds in 1..86400) { "El tiempo debe estar entre 1 segundo y 24 horas" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val timer = timer(session) ?: error("No hay un cronómetro activo")
            val entry = TimeEntry(uid=session.uid,userName=session.name,projectId=timer.projectId,
                day=LocalDate.now(),seconds=roundedTimerSeconds(seconds * 1000),task=timer.task,
                source="Cronómetro",taskId=timer.taskId)
            val saved = entry.copy(id=saveEntry(session,entry))
            saveTimer(session,null)
            db.setTransactionSuccessful()
            return saved
        } finally { db.endTransaction() }
    }

    fun seedDemo() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery("SELECT name FROM flags WHERE name='demo_seeded'",null).use { if(it.moveToFirst()) return }
            val names = listOf("Portal Cliente AFC","Rediseño Web","Dashboard Admin","API Integración")
            val clients = listOf("AFC","NOVA","AFC","NOVA")
            val estimates = listOf(32,32,16,8)
            val teamEstimates = listOf(40,36,24,12)
            val colors = listOf(0xFFB6A0FF,0xFFF5BA78,0xFF81B8FF,0xFFE79CD4)
            val dev = listOf(24,38,12,7)
            val leader = listOf(8,4,4,3)
            val today=LocalDate.now()
            val base = (if(today.dayOfMonth>=16) today else today.minusMonths(1)).withDayOfMonth(1)
            names.forEachIndexed { i,name ->
                val id = "demo_$i"
                db.execSQL("INSERT INTO projects VALUES(?,?,?,?,?,?,?)",arrayOf<Any>(id,"demo",name,clients[i],estimates[i]*60,teamEstimates[i]*60,colors[i]))
                DefaultTaskNames.forEach { ensureTask(db, id, it) }
                val task = ensureTask(db, id, listOf("Desarrollo del portal de clientes","Diseño y componentes de la interfaz",
                    "Métricas y visualización de datos","Pruebas de integración")[i])
                listOf("demo_luis" to dev[i],"demo_ana" to leader[i]).forEach { (uid,hours) ->
                    var remaining = hours
                    var n = i * 5
                    while(remaining > 0) {
                        val chunk = minOf(remaining,8)
                        val day = base.plusDays(n.toLong()).coerceAtMost(LocalDate.now())
                        db.execSQL("INSERT INTO entries(uid,user_name,project_id,day,seconds,task,source,task_id) VALUES(?,?,?,?,?,?,?,?)",
                            arrayOf<Any>(uid,if(uid=="demo_luis") "Luis Gómez" else "Ana Pérez",id,day.toString(),chunk*3600,
                                task.name,"Ejemplo",task.id))
                        remaining -= chunk; n++
                    }
                }
            }
            db.execSQL("INSERT INTO flags VALUES('demo_seeded')")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
