package com.deepsource.projects.catalog;
import com.deepsource.projects.support.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.Map;
@Component
public class RecordReferencesClient {
    private final RestClient client;
    public RecordReferencesClient(@Value("${app.records-url}") String url) {
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(3000);factory.setReadTimeout(5000);
        client=RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    public void requireNoRecords(String authorization,String project,String task) {
        try {
            Map<?,?> result=client.get().uri(b->b.path("/api/registros/referencias")
                .queryParam("proyecto",project).queryParam("tarea",task==null?"":task).build())
                .header("Authorization",authorization).retrieve().body(Map.class);
            if(result==null || !(result.get("count") instanceof Number count)) throw new ApiException(503,"No pudimos comprobar los registros");
            if(count.longValue()>0) throw new ApiException(409,"Primero elimina los registros asociados a esta tarea o proyecto");
        } catch(RestClientException e) {throw new ApiException(503,"El servicio de registros debe estar disponible para eliminar");}
    }
}
