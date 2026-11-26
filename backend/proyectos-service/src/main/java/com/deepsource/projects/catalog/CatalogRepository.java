package com.deepsource.projects.catalog;
import com.deepsource.projects.support.ApiException;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@Repository
public class CatalogRepository {
    public record Project(String id,String name,String client,int estimateMinutes,int teamEstimateMinutes,long color) {}
    public record Task(String id,String projectId,String name) {}
    private final JdbcTemplate db;
    public CatalogRepository(JdbcTemplate db) {this.db=db;}
    public List<Project> projects(String uid) {
        return db.query("SELECT * FROM projects WHERE uid=? ORDER BY rowid",
            (r,n)->new Project(r.getString("id"),r.getString("name"),r.getString("client"),
                r.getInt("estimate_minutes"),r.getInt("estimate_minutes"),r.getLong("color")),uid);
    }
    public Project project(String uid,String id) {
        return projects(uid).stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new ApiException(404,"Proyecto no disponible"));
    }
    public List<Task> tasks(String uid) {
        return db.query("SELECT t.* FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.uid=? ORDER BY t.name COLLATE NOCASE",
            (r,n)->new Task(r.getString("id"),r.getString("project_id"),r.getString("name")),uid);
    }
    public Task task(String uid,String id) {
        return tasks(uid).stream().filter(t->t.id().equals(id)).findFirst().orElseThrow(()->new ApiException(404,"Tarea no disponible"));
    }
    public Project createProject(String uid,String name,String client,int estimate) {
        String id=UUID.randomUUID().toString();
        db.update("INSERT INTO projects(id,uid,name,client,estimate_minutes,color) VALUES(?,?,?,?,?,?)",id,uid,name,client,estimate,0xFFA99BFFL);
        for(String task:List.of("Planificación","Desarrollo","Pruebas","Documentación")) createTask(uid,id,task);
        return project(uid,id);
    }
    public Project updateProject(String uid,String id,String name,String client,int estimate) {
        project(uid,id);
        db.update("UPDATE projects SET name=?,client=?,estimate_minutes=? WHERE id=? AND uid=?",name,client,estimate,id,uid);
        return project(uid,id);
    }
    public Task createTask(String uid,String projectId,String name) {
        project(uid,projectId);
        var existing=tasks(uid).stream().filter(t->t.projectId().equals(projectId)&&t.name().equalsIgnoreCase(name)).findFirst();
        if(existing.isPresent()) return existing.get();
        String id=UUID.randomUUID().toString();
        db.update("INSERT INTO tasks(id,project_id,name) VALUES(?,?,?)",id,projectId,name);
        return task(uid,id);
    }
    public Task updateTask(String uid,String id,String name) {
        Task old=task(uid,id);
        if(tasks(uid).stream().anyMatch(t->!t.id().equals(id)&&t.projectId().equals(old.projectId())&&t.name().equalsIgnoreCase(name)))
            throw new ApiException(409,"Ya existe una tarea con ese nombre");
        db.update("UPDATE tasks SET name=? WHERE id=?",name,id);
        return task(uid,id);
    }
    public void deleteTask(String uid,String id) {task(uid,id);db.update("DELETE FROM tasks WHERE id=?",id);}
    public void deleteProject(String uid,String id) {
        project(uid,id);db.update("DELETE FROM tasks WHERE project_id=?",id);db.update("DELETE FROM projects WHERE id=? AND uid=?",id,uid);
    }
}
