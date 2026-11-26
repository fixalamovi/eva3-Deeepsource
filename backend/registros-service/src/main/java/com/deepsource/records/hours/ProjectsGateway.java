package com.deepsource.records.hours;
import com.deepsource.records.support.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
@Component
public class ProjectsGateway {
    @JsonIgnoreProperties(ignoreUnknown=true) public record Identity(String uid,String name,String email) {}
    @JsonIgnoreProperties(ignoreUnknown=true) public record Task(String id,String projectId,String name) {}
    private final RestClient client;
    @Autowired public ProjectsGateway(@Value("${app.projects-url}") String url) {
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(3000);factory.setReadTimeout(5000);
        client=RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    ProjectsGateway(RestClient client) { this.client=client; }
    public Identity identity(String authorization) {
        if(authorization==null||authorization.isBlank())throw new ApiException(401,"Inicia sesión para continuar");
        return get("/api/auth/session",authorization,Identity.class);
    }
    public Task task(String id,String authorization) {
        return get("/api/tareas/"+id,authorization,Task.class);
    }
    private <T> T get(String path,String auth,Class<T> type) {
        try {
            T body=client.get().uri(path).header("Authorization",auth).retrieve().body(type);
            if(body==null)throw new ApiException(503,"El servicio de proyectos devolvió una respuesta vacía");
            return body;
        } catch(RestClientResponseException e) {
            int status=e.getStatusCode().value();
            throw new ApiException(status==401?401:status==404?404:503,
                status==401?"Tu sesión venció. Vuelve a ingresar":status==404?"Tarea no disponible":"No pudimos validar los datos");
        } catch(RestClientException e) {throw new ApiException(503,"El servicio de proyectos no está disponible");}
    }
}
