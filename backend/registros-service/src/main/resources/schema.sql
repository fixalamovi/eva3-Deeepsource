CREATE TABLE IF NOT EXISTS entries (
 id INTEGER PRIMARY KEY AUTOINCREMENT, uid TEXT NOT NULL, user_name TEXT NOT NULL,
 project_id TEXT NOT NULL, task_id TEXT NOT NULL, task TEXT NOT NULL, day TEXT NOT NULL,
 seconds INTEGER NOT NULL CHECK(seconds>0 AND seconds<=86400), source TEXT NOT NULL,
 client_key TEXT NOT NULL, UNIQUE(uid,client_key)
);
CREATE INDEX IF NOT EXISTS entries_user_day ON entries(uid,day);
CREATE INDEX IF NOT EXISTS entries_task ON entries(uid,task_id);
