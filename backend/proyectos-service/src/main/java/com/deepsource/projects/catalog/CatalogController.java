package com.deepsource.projects.catalog;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService service;private final JdbcTemplate db;
    public CatalogController(CatalogService service,JdbcTemplate db){this.service=service;this.db=db;}
    public record TaskBody(String name) {}
    @GetMapping("/health") public Map<?,?> health(){return Map.of("service","proyectos","database","SQLite","ok",db.queryForObject("SELECT 1",Integer.class)==1);}
    @GetMapping("/proyectos") public List<CatalogRepository.Project> projects(@RequestHeader(value="Authorization",required=false) String h){return service.projects(h);}
    @GetMapping("/proyectos/{id}") public CatalogRepository.Project project(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id){return service.project(h,id);}
    @PostMapping("/proyectos") public CatalogRepository.Project create(@RequestHeader(value="Authorization",required=false) String h,@RequestBody CatalogService.ProjectBody p){return service.create(h,p);}
    @PutMapping("/proyectos/{id}") public CatalogRepository.Project update(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id,@RequestBody CatalogService.ProjectBody p){return service.update(h,id,p);}
    @DeleteMapping("/proyectos/{id}") public void delete(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id){service.deleteProject(h,id);}
    @GetMapping("/tareas") public List<CatalogRepository.Task> tasks(@RequestHeader(value="Authorization",required=false) String h){return service.tasks(h);}
    @GetMapping("/tareas/{id}") public CatalogRepository.Task task(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id){return service.task(h,id);}
    @GetMapping("/proyectos/{id}/tareas") public List<CatalogRepository.Task> projectTasks(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id){
        service.project(h,id);return service.tasks(h).stream().filter(t->t.projectId().equals(id)).toList();
    }
    @PostMapping("/proyectos/{id}/tareas") public CatalogRepository.Task createTask(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id,@RequestBody TaskBody p){return service.createTask(h,id,p.name());}
    @PutMapping("/tareas/{id}") public CatalogRepository.Task updateTask(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id,@RequestBody TaskBody p){return service.updateTask(h,id,p.name());}
    @DeleteMapping("/tareas/{id}") public void deleteTask(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id){service.deleteTask(h,id);}
}
