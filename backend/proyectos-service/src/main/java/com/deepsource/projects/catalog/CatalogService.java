package com.deepsource.projects.catalog;
import com.deepsource.projects.auth.AuthService;
import com.deepsource.projects.support.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class CatalogService {
    private final CatalogRepository repo; private final AuthService auth; private final RecordReferencesClient refs;
    public CatalogService(CatalogRepository repo,AuthService auth,RecordReferencesClient refs) {this.repo=repo;this.auth=auth;this.refs=refs;}
    public record ProjectBody(String name,String client,Integer estimateMinutes) {}
    private int estimate(ProjectBody p) {
        if(p.estimateMinutes()==null||p.estimateMinutes()<1||p.estimateMinutes()>600000) throw new ApiException(400,"Revisa la estimación del proyecto");
        return p.estimateMinutes();
    }
    public List<CatalogRepository.Project> projects(String h) {return repo.projects(auth.requireSession(h).uid());}
    public CatalogRepository.Project project(String h,String id) {return repo.project(auth.requireSession(h).uid(),Rules.id(id));}
    public List<CatalogRepository.Task> tasks(String h) {return repo.tasks(auth.requireSession(h).uid());}
    public CatalogRepository.Task task(String h,String id) {return repo.task(auth.requireSession(h).uid(),Rules.id(id));}
    @Transactional public CatalogRepository.Project create(String h,ProjectBody p) {
        return repo.createProject(auth.requireSession(h).uid(),Rules.text(p.name(),3,80,"Nombre"),
            Rules.text(p.client(),2,60,"Cliente"),estimate(p));
    }
    @Transactional public CatalogRepository.Project update(String h,String id,ProjectBody p) {
        return repo.updateProject(auth.requireSession(h).uid(),Rules.id(id),Rules.text(p.name(),3,80,"Nombre"),
            Rules.text(p.client(),2,60,"Cliente"),estimate(p));
    }
    @Transactional public CatalogRepository.Task createTask(String h,String project,String name) {
        return repo.createTask(auth.requireSession(h).uid(),Rules.id(project),Rules.text(name,3,160,"Tarea"));
    }
    @Transactional public CatalogRepository.Task updateTask(String h,String id,String name) {
        return repo.updateTask(auth.requireSession(h).uid(),Rules.id(id),Rules.text(name,3,160,"Tarea"));
    }
    @Transactional public void deleteProject(String h,String id) {
        String uid=auth.requireSession(h).uid();repo.project(uid,Rules.id(id));refs.requireNoRecords(h,id,null);repo.deleteProject(uid,id);
    }
    @Transactional public void deleteTask(String h,String id) {
        String uid=auth.requireSession(h).uid();var task=repo.task(uid,Rules.id(id));refs.requireNoRecords(h,task.projectId(),id);repo.deleteTask(uid,id);
    }
}
