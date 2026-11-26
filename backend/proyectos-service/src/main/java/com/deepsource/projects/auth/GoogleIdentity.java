package com.deepsource.projects.auth;

import com.deepsource.projects.support.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdentity {
    public record Identity(String subject, String email, String name, String nonce) {}
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentity(@Value("${GOOGLE_WEB_CLIENT_ID:}") String clientId) {
        this.clientId = clientId.strip();
        verifier = this.clientId.isEmpty() ? null : new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(List.of(this.clientId))
            .build();
    }

    public String clientId() {
        if (verifier == null)
            throw new ApiException(503, "Activa el acceso con Google configurando GOOGLE_WEB_CLIENT_ID en el servidor.");
        return clientId;
    }

    public Identity verify(String rawToken) {
        clientId();
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 8192) throw invalidCredential();
        GoogleIdToken token;
        try {
            token = verifier.verify(rawToken);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw invalidCredential();
        } catch (IOException e) {
            throw new ApiException(503, "No pudimos verificar la credencial con Google. Intenta nuevamente.");
        }
        if (token == null) throw invalidCredential();
        var claims = token.getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()
            || claims.getEmail() == null || claims.getEmail().isBlank()
            || !Boolean.TRUE.equals(claims.getEmailVerified())
            || !(claims.get("nonce") instanceof String nonce) || nonce.isBlank())
            throw invalidCredential();

        // El identificador de cuenta es el subject verificado. No se enlaza una cuenta por correo.
        String name = claims.get("name") instanceof String claimName ? claimName : null;
        return new Identity(claims.getSubject(), claims.getEmail(), name, nonce);
    }

    private ApiException invalidCredential() {
        return new ApiException(401, "Google no pudo verificar tu identidad. Elige tu cuenta e inténtalo de nuevo.");
    }
}
