package com.deepsource.records.hours;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@RestController
@RequestMapping("/api")
public class HoursController {
    private final HoursService service;private final JdbcTemplate db;
    public HoursController(HoursService service,JdbcTemplate db){this.service=service;this.db=db;}
    @GetMapping("/health")public Map<?,?> health(){return Map.of("service","registros","database","SQLite","ok",db.queryForObject("SELECT 1",Integer.class)==1);}
    @GetMapping("/registros")public List<HoursRepository.Entry> all(@RequestHeader(value="Authorization",required=false)String h){return service.all(h);}
    @GetMapping("/registros/referencias")public Map<?,?> references(@RequestHeader(value="Authorization",required=false)String h,@RequestParam String proyecto,@RequestParam(required=false)String tarea){return Map.of("count",service.references(h,proyecto,tarea));}
    @GetMapping("/registros/{id}")public HoursRepository.Entry entry(@RequestHeader(value="Authorization",required=false)String h,@PathVariable long id){return service.entry(h,id);}
    @PostMapping("/registros")public HoursRepository.Entry create(@RequestHeader(value="Authorization",required=false)String h,
        @RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody HoursService.EntryBody body){return service.create(h,body,key);}
    @PutMapping("/registros/{id}")public HoursRepository.Entry update(@RequestHeader(value="Authorization",required=false)String h,@PathVariable long id,@RequestBody HoursService.EntryBody body){return service.update(h,id,body);}
    @DeleteMapping("/registros/{id}")public void delete(@RequestHeader(value="Authorization",required=false)String h,@PathVariable long id){service.delete(h,id);}
    @GetMapping("/resumen")public Map<String,Object> summary(@RequestHeader(value="Authorization",required=false)String h,@RequestParam String mes){return service.summary(h,mes);}
}
