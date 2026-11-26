package com.deepsource.records.hours;
import com.deepsource.records.support.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class HoursService {
    public record EntryBody(String projectId,String taskId,String day,Long seconds,String source) {}
    private final HoursRepository repo;private final ProjectsGateway projects;
    public HoursService(HoursRepository repo,ProjectsGateway projects){this.repo=repo;this.projects=projects;}
    public List<HoursRepository.Entry> all(String h){return repo.all(projects.identity(h).uid());}
    public HoursRepository.Entry entry(String h,long id){return repo.entry(projects.identity(h).uid(),id);}
    public static long minutesUp(long seconds){
        if(seconds<1||seconds>86400)throw new ApiException(400,"La duración debe estar entre 1 segundo y 24 horas");
        return (seconds/60+(seconds%60==0?0:1))*60;
    }
    private record Values(ProjectsGateway.Task task,String day,long seconds,String source) {}
    private Values validate(String h,ProjectsGateway.Identity user,EntryBody body,long excluded){
        String taskId=Rules.id(body.taskId());
        if(!taskId.matches("[a-zA-Z0-9_-]+"))throw new ApiException(400,"Identificador de tarea no válido");
        var task=projects.task(taskId,h);
        if(!task.projectId().equals(body.projectId()))throw new ApiException(400,"La tarea no pertenece al proyecto");
        LocalDate day;
        try{day=LocalDate.parse(body.day());}catch(Exception e){throw new ApiException(400,"Indica una fecha válida");}
        if(day.isAfter(LocalDate.now()))throw new ApiException(400,"La fecha no puede ser futura");
        if(body.seconds()==null||body.seconds()<1||body.seconds()>86400)throw new ApiException(400,"La duración debe estar entre 1 segundo y 24 horas");
        String source=body.source()==null?"Manual":body.source();
        if(!source.equals("Manual")&&!source.equals("Cronómetro"))throw new ApiException(400,"Origen de registro no válido");
        long seconds=source.equals("Cronómetro")?minutesUp(body.seconds()):body.seconds();
        if(repo.daily(user.uid(),day.toString(),excluded)+seconds>86400)throw new ApiException(400,"Los registros del día no pueden superar 24 horas");
        return new Values(task,day.toString(),seconds,source);
    }
    @Transactional public HoursRepository.Entry create(String h,EntryBody body,String clientKey){
        var user=projects.identity(h);
        String key=clientKey==null||clientKey.isBlank()?UUID.randomUUID().toString():Rules.text(clientKey,1,100,"Clave del registro");
        var existing=repo.byKey(user.uid(),key);
        if(existing.isPresent())return existing.get();
        var v=validate(h,user,body,0);
        return repo.create(user,v.task(),v.day(),v.seconds(),v.source(),key);
    }
    @Transactional public HoursRepository.Entry update(String h,long id,EntryBody body){
        var user=projects.identity(h);repo.entry(user.uid(),id);
        var v=validate(h,user,body,id);
        return repo.update(user,id,v.task(),v.day(),v.seconds(),v.source());
    }
    @Transactional public void delete(String h,long id){repo.delete(projects.identity(h).uid(),id);}
    public long references(String h,String project,String task){return repo.references(projects.identity(h).uid(),Rules.id(project),task);}
    public Map<String,Object> summary(String h,String month){
        YearMonth selected;
        try{selected=YearMonth.parse(month);}catch(Exception e){throw new ApiException(400,"Usa el mes en formato AAAA-MM");}
        var entries=all(h).stream().filter(e->YearMonth.from(LocalDate.parse(e.day())).equals(selected)).toList();
        Map<String,Long> totals=new LinkedHashMap<>();
        for(var entry:entries)totals.merge(entry.taskId(),entry.seconds(),Long::sum);
        return Map.of("month",month,"records",entries.size(),"totalSeconds",entries.stream().mapToLong(HoursRepository.Entry::seconds).sum(),"tasks",totals);
    }
}
