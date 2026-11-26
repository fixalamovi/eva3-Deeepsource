package com.deepsource.records.hours;
import com.deepsource.records.support.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;
class ProjectsGatewayTest {
    @Test void sessionContractAcceptsTheProjectsServiceResponseAndForwardsTheToken() {
        var builder=RestClient.builder().baseUrl("http://projects.test");
        var server=MockRestServiceServer.bindTo(builder).build();
        var gateway=new ProjectsGateway(builder.build());
        server.expect(requestTo("http://projects.test/api/auth/session"))
            .andExpect(header("Authorization","Bearer prueba"))
            .andRespond(withSuccess("{\"uid\":\"u\",\"name\":\"Persona\",\"email\":\"p@example.test\",\"role\":\"DEV\",\"demo\":false,\"provider\":\"Correo servidor\",\"token\":\"\",\"refreshToken\":\"\",\"expiresAt\":9999999999999}",MediaType.APPLICATION_JSON));
        assertEquals("u",gateway.identity("Bearer prueba").uid());
        server.verify();
    }
    @Test void taskContractRetainsItsProjectRelationship() {
        var builder=RestClient.builder().baseUrl("http://projects.test");
        var server=MockRestServiceServer.bindTo(builder).build();
        var gateway=new ProjectsGateway(builder.build());
        server.expect(requestTo("http://projects.test/api/tareas/task-a"))
            .andRespond(withSuccess("{\"id\":\"task-a\",\"projectId\":\"p\",\"name\":\"Desarrollo\"}",MediaType.APPLICATION_JSON));
        assertEquals("p",gateway.task("task-a","Bearer prueba").projectId());
        server.verify();
    }
    @Test void rejectedSessionIsNotTreatedAsAnAuthenticatedUser() {
        var builder=RestClient.builder().baseUrl("http://projects.test");
        var server=MockRestServiceServer.bindTo(builder).build();
        var gateway=new ProjectsGateway(builder.build());
        server.expect(requestTo("http://projects.test/api/auth/session")).andRespond(withUnauthorizedRequest());
        assertEquals(401,assertThrows(ApiException.class,()->gateway.identity("Bearer invalida")).status());
        server.verify();
    }
}
