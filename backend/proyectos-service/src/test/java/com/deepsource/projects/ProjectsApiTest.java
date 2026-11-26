package com.deepsource.projects;
import com.deepsource.projects.auth.GoogleIdentity;
import com.deepsource.projects.catalog.RecordReferencesClient;
import com.deepsource.projects.support.ApiException;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import java.nio.file.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@AutoConfigureMockMvc
class ProjectsApiTest {
    static final Path DATABASE;
    static { try { DATABASE=Files.createTempDirectory("ds-projects-test-").resolve("projects.db"); } catch(Exception e){throw new ExceptionInInitializerError(e);} }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.database",()->DATABASE.toString());}
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @MockBean RecordReferencesClient refs;
    @MockBean GoogleIdentity google;

    @Test void googleUsesVerifiedSubjectAndRejectsARepeatedChallenge() throws Exception {
        when(google.clientId()).thenReturn("web-client-example");
        String nonce = googleNonce();
        String subject = UUID.randomUUID().toString();
        String mail = UUID.randomUUID() + "@example.test";
        when(google.verify("first-token")).thenReturn(
            new GoogleIdentity.Identity(subject, mail, "Cuenta Ejemplo", nonce));
        JsonNode first = node(api.perform(post("/api/auth/google").contentType("application/json")
            .content(body(Map.of("idToken", "first-token")))).andExpect(status().isOk()).andReturn());
        assertEquals("Google", first.get("provider").asText());
        api.perform(post("/api/auth/google").contentType("application/json")
            .content(body(Map.of("idToken", "first-token")))).andExpect(status().isUnauthorized());

        String nextNonce = googleNonce();
        when(google.verify("second-token")).thenReturn(
            new GoogleIdentity.Identity(subject, mail, "Cuenta Ejemplo", nextNonce));
        api.perform(post("/api/auth/google").contentType("application/json")
            .content(body(Map.of("idToken", "second-token")))).andExpect(status().isOk())
            .andExpect(jsonPath("$.uid").value(first.get("uid").asText()));
    }

    @Test void googleDoesNotTakeOverAnExistingEmailAccount() throws Exception {
        JsonNode existing = account();
        when(google.clientId()).thenReturn("web-client-example");
        String nonce = googleNonce();
        when(google.verify("collision-token")).thenReturn(new GoogleIdentity.Identity(
            "other-subject", existing.get("email").asText(), "Cuenta Ejemplo", nonce));

        api.perform(post("/api/auth/google").contentType("application/json")
            .content(body(Map.of("idToken", "collision-token")))).andExpect(status().isConflict());
    }

    private String googleNonce() throws Exception {
        return node(api.perform(post("/api/auth/google/challenge"))
            .andExpect(status().isOk()).andReturn()).get("nonce").asText();
    }

    String body(Object value)throws Exception{return json.writeValueAsString(value);}
    JsonNode node(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));}
    JsonNode account()throws Exception{return node(api.perform(post("/api/auth/register").contentType("application/json")
        .content(body(Map.of("name","Usuario Prueba","email",UUID.randomUUID()+"@example.test","password","Prueba1234")))).andExpect(status().isOk()).andReturn());}
    String auth(JsonNode a){return "Bearer "+a.get("token").asText();}
    JsonNode project(String h)throws Exception{return node(api.perform(post("/api/proyectos").header("Authorization",h).contentType("application/json")
        .content(body(Map.of("name","Proyecto prueba","client","Cliente","estimateMinutes",480)))).andExpect(status().isOk()).andReturn());}
    @Test void registrationSessionAndLogoutAreRealAndUnauthorizedReadsAreRejected()throws Exception{
        var a=account();String h=auth(a);
        api.perform(get("/api/auth/session").header("Authorization",h)).andExpect(status().isOk()).andExpect(jsonPath("$.uid").value(a.get("uid").asText()));
        api.perform(get("/api/proyectos")).andExpect(status().isUnauthorized());
        api.perform(post("/api/auth/logout").header("Authorization",h)).andExpect(status().isOk());
        api.perform(get("/api/proyectos").header("Authorization",h)).andExpect(status().isUnauthorized());
    }
    @Test void projectsAreIsolatedAndCanBeUpdatedAndDeleted()throws Exception{
        String a=auth(account()),b=auth(account());String id=project(a).get("id").asText();
        api.perform(get("/api/proyectos/"+id).header("Authorization",b)).andExpect(status().isNotFound());
        api.perform(put("/api/proyectos/"+id).header("Authorization",a).contentType("application/json")
            .content(body(Map.of("name","Nombre actualizado","client","Cliente","estimateMinutes",600))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nombre actualizado"));
        api.perform(delete("/api/proyectos/"+id).header("Authorization",a)).andExpect(status().isOk());
        api.perform(get("/api/proyectos/"+id).header("Authorization",a)).andExpect(status().isNotFound());
    }
    @Test void defaultTasksAndTaskCrudWorkAndDuplicateNamesReuseTheTask()throws Exception{
        String h=auth(account());String p=project(h).get("id").asText();
        var initial=node(api.perform(get("/api/proyectos/"+p+"/tareas").header("Authorization",h)).andExpect(status().isOk()).andReturn());
        assertEquals(4,initial.size());
        String id=node(api.perform(post("/api/proyectos/"+p+"/tareas").header("Authorization",h).contentType("application/json")
            .content(body(Map.of("name","Implementar login")))).andExpect(status().isOk()).andReturn()).get("id").asText();
        api.perform(post("/api/proyectos/"+p+"/tareas").header("Authorization",h).contentType("application/json")
            .content(body(Map.of("name","implementar login")))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        api.perform(put("/api/tareas/"+id).header("Authorization",h).contentType("application/json")
            .content(body(Map.of("name","Login terminado")))).andExpect(status().isOk());
        api.perform(delete("/api/tareas/"+id).header("Authorization",h)).andExpect(status().isOk());
        api.perform(get("/api/tareas/"+id).header("Authorization",h)).andExpect(status().isNotFound());
    }
    @Test void projectWithRecordsCannotBeDeleted()throws Exception{
        String h=auth(account());String id=project(h).get("id").asText();
        doThrow(new ApiException(409,"Primero elimina los registros")).when(refs).requireNoRecords(anyString(),eq(id),isNull());
        api.perform(delete("/api/proyectos/"+id).header("Authorization",h)).andExpect(status().isConflict());
        api.perform(get("/api/proyectos/"+id).header("Authorization",h)).andExpect(status().isOk());
    }
    @Test void passwordChangeRejectsWrongCurrentPasswordAndPersistsTheNewOne()throws Exception{
        var a=account();String h=auth(a),mail=a.get("email").asText();
        api.perform(put("/api/auth/password").header("Authorization",h).contentType("application/json")
            .content(body(Map.of("current","Equivocada123","next","NuevaClave123")))).andExpect(status().isBadRequest());
        api.perform(put("/api/auth/password").header("Authorization",h).contentType("application/json")
            .content(body(Map.of("current","Prueba1234","next","NuevaClave123")))).andExpect(status().isOk());
        api.perform(post("/api/auth/login").contentType("application/json").content(body(Map.of("email",mail,"password","Prueba1234")))).andExpect(status().isUnauthorized());
        api.perform(post("/api/auth/login").contentType("application/json").content(body(Map.of("email",mail,"password","NuevaClave123")))).andExpect(status().isOk());
    }
    @Test void invalidProjectAndInvalidEmailCannotCreateData()throws Exception{
        String h=auth(account());
        api.perform(post("/api/proyectos").header("Authorization",h).contentType("application/json")
            .content(body(Map.of("name","X","client","Cliente","estimateMinutes",480)))).andExpect(status().isBadRequest());
        api.perform(post("/api/auth/register").contentType("application/json")
            .content(body(Map.of("name","Persona Prueba","email","correo-invalido","password","Clave1234")))).andExpect(status().isBadRequest());
    }
}
