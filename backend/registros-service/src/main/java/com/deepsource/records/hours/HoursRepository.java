package com.deepsource.records.hours;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.deepsource.records.support.ApiException;
import java.util.*;
@Repository
public class HoursRepository {
    public record Entry(long id,String uid,String userName,String projectId,String day,long seconds,String task,String source,String taskId) {}
    private final JdbcTemplate db;
    public HoursRepository(JdbcTemplate db){this.db=db;}
    private List<Entry> query(String where,Object...args){
        return db.query("SELECT * FROM entries WHERE "+where+" ORDER BY day DESC,id DESC",
            (r,n)->new Entry(r.getLong("id"),r.getString("uid"),r.getString("user_name"),r.getString("project_id"),
                r.getString("day"),r.getLong("seconds"),r.getString("task"),r.getString("source"),r.getString("task_id")),args);
    }
    public List<Entry> all(String uid){return query("uid=?",uid);}
    public Entry entry(String uid,long id){return query("uid=? AND id=?",uid,id).stream().findFirst().orElseThrow(()->new ApiException(404,"Registro no disponible"));}
    public Optional<Entry> byKey(String uid,String key){return query("uid=? AND client_key=?",uid,key).stream().findFirst();}
    public long daily(String uid,String day,long excluded){
        return db.queryForObject("SELECT COALESCE(SUM(seconds),0) FROM entries WHERE uid=? AND day=? AND id<>?",Long.class,uid,day,excluded);
    }
    public Entry create(ProjectsGateway.Identity user,ProjectsGateway.Task task,String day,long seconds,String source,String key){
        db.update("INSERT INTO entries(uid,user_name,project_id,task_id,task,day,seconds,source,client_key) VALUES(?,?,?,?,?,?,?,?,?)",
            user.uid(),user.name(),task.projectId(),task.id(),task.name(),day,seconds,source,key);
        return byKey(user.uid(),key).orElseThrow();
    }
    public Entry update(ProjectsGateway.Identity user,long id,ProjectsGateway.Task task,String day,long seconds,String source){
        entry(user.uid(),id);
        db.update("UPDATE entries SET user_name=?,project_id=?,task_id=?,task=?,day=?,seconds=?,source=? WHERE id=? AND uid=?",
            user.name(),task.projectId(),task.id(),task.name(),day,seconds,source,id,user.uid());
        return entry(user.uid(),id);
    }
    public void delete(String uid,long id){entry(uid,id);db.update("DELETE FROM entries WHERE id=? AND uid=?",id,uid);}
    public long references(String uid,String project,String task){
        return task==null||task.isBlank()
            ?db.queryForObject("SELECT COUNT(*) FROM entries WHERE uid=? AND project_id=?",Long.class,uid,project)
            :db.queryForObject("SELECT COUNT(*) FROM entries WHERE uid=? AND project_id=? AND task_id=?",Long.class,uid,project,task);
    }
}
