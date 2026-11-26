package com.deepsource.records;
import com.deepsource.records.hours.ProjectsGateway;
import com.deepsource.records.support.ApiException;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@AutoConfigureMockMvc
class RecordsApiTest {
    static final Path DATABASE;
    static { try { DATABASE=Files.createTempDirectory("ds-records-test-").resolve("records.db"); } catch(Exception e){throw new ExceptionInInitializerError(e);} }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.database",()->DATABASE.toString());}
    @Autowired MockMvc api; @Autowired ObjectMapper json; @Autowired JdbcTemplate db;
    @MockBean ProjectsGateway projects;
    final String h="Bearer account-a";
    @BeforeEach void prepare(){
        db.update("DELETE FROM entries");
        when(projects.identity(anyString())).thenReturn(new ProjectsGateway.Identity("u-a","Usuario A","a@example.test"));
        when(projects.identity("Bearer account-b")).thenReturn(new ProjectsGateway.Identity("u-b","Usuario B","b@example.test"));
        when(projects.identity(isNull())).thenThrow(new ApiException(401,"Inicia sesión"));
        when(projects.task(eq("task-a"),anyString())).thenReturn(new ProjectsGateway.Task("task-a","project-a","Desarrollo"));
    }
    String body(long seconds,String source,String day)throws Exception{
        return json.writeValueAsString(Map.of("projectId","project-a","taskId","task-a","day",day,"seconds",seconds,"source",source));
    }
    JsonNode create(long seconds,String source,String key)throws Exception{
        return json.readTree(api.perform(post("/api/registros").header("Authorization",h).header("Idempotency-Key",key)
            .contentType("application/json").content(body(seconds,source,LocalDate.now().toString())))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test void timerRoundsUpAndRetryDoesNotDuplicateTheRecord()throws Exception{
        var first=create(5610,"Cronómetro","timer-one");
        assertEquals(5640,first.get("seconds").asLong());
        var retry=create(5610,"Cronómetro","timer-one");
        assertEquals(first.get("id").asLong(),retry.get("id").asLong());
        api.perform(get("/api/registros").header("Authorization",h)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void severalEntriesForOneTaskCanBeUpdatedDeletedAndSummed()throws Exception{
        var a=create(600,"Manual","manual-one");create(1200,"Manual","manual-two");
        api.perform(get("/api/resumen").param("mes",YearMonth.now().toString()).header("Authorization",h))
            .andExpect(status().isOk()).andExpect(jsonPath("$.records").value(2)).andExpect(jsonPath("$.totalSeconds").value(1800));
        api.perform(put("/api/registros/"+a.get("id").asLong()).header("Authorization",h).contentType("application/json")
            .content(body(900,"Manual",LocalDate.now().toString()))).andExpect(status().isOk()).andExpect(jsonPath("$.seconds").value(900));
        api.perform(delete("/api/registros/"+a.get("id").asLong()).header("Authorization",h)).andExpect(status().isOk());
        api.perform(get("/api/registros").header("Authorization",h)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void recordsFromAnotherUserCannotBeReadOrDeleted()throws Exception{
        var a=create(600,"Manual","owned");
        api.perform(get("/api/registros").header("Authorization","Bearer account-b")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        api.perform(delete("/api/registros/"+a.get("id").asLong()).header("Authorization","Bearer account-b")).andExpect(status().isNotFound());
        api.perform(get("/api/registros")).andExpect(status().isUnauthorized());
    }
    @Test void roundingCannotExceedTheDailyLimit()throws Exception{
        create(86370,"Manual","long");
        api.perform(post("/api/registros").header("Authorization",h).header("Idempotency-Key","too-much").contentType("application/json")
            .content(body(30,"Cronómetro",LocalDate.now().toString()))).andExpect(status().isBadRequest());
        api.perform(get("/api/registros").header("Authorization",h)).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void futureDatesAndMismatchedProjectsAreRejected()throws Exception{
        api.perform(post("/api/registros").header("Authorization",h).contentType("application/json")
            .content(body(600,"Manual",LocalDate.now().plusDays(1).toString()))).andExpect(status().isBadRequest());
        when(projects.task(eq("task-a"),anyString())).thenReturn(new ProjectsGateway.Task("task-a","another-project","Tarea"));
        api.perform(post("/api/registros").header("Authorization",h).contentType("application/json")
            .content(body(600,"Manual",LocalDate.now().toString()))).andExpect(status().isBadRequest());
    }
    @Test void referencesAndExactMinutesArePreserved()throws Exception{
        create(60,"Cronómetro","exact");
        api.perform(get("/api/registros/referencias").header("Authorization",h).param("proyecto","project-a").param("tarea","task-a"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        assertEquals(60,com.deepsource.records.hours.HoursService.minutesUp(60));
    }
}
