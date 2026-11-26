package com.deepsource.projects.auth;

import com.deepsource.projects.support.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.*;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TTL = 7L * 24 * 3600 * 1000;
    private final JdbcTemplate db;
    private record User(String uid, String name, String email, byte[] salt, byte[] hash,
        String provider, int attempts, long lockedUntil) {}

    public AuthService(JdbcTemplate db) { this.db = db; }
    private List<User> users(String where, Object value) {
        return db.query("SELECT * FROM users WHERE " + where,
            (r,n) -> new User(r.getString("uid"),r.getString("name"),r.getString("email"),
                r.getBytes("salt"),r.getBytes("password_hash"),r.getString("provider"),
                r.getInt("attempts"),r.getLong("locked_until")), value);
    }
    private String email(String value) {
        String result = Rules.text(value, 3, 254, "Correo").toLowerCase(Locale.ROOT);
        if (!result.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new ApiException(400,"Ingresa un correo válido");
        return result;
    }
    private void password(String value) {
        if (value == null || value.length() < 8 || value.length() > 128 ||
            value.chars().noneMatch(Character::isLetter) || value.chars().noneMatch(Character::isDigit))
            throw new ApiException(400,"Usa entre 8 y 128 caracteres, con letras y números");
    }
    private byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo proteger la contraseña", e); }
        finally { spec.clearPassword(); }
    }
    private String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    private SessionDto dto(User user, String token, long expiry) {
        return new SessionDto(user.uid(),user.name(),user.email(),"DEV",false,user.provider(),token,"",expiry);
    }
    private SessionDto issue(User user) {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = "ds_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expires = System.currentTimeMillis() + TTL;
        db.update("DELETE FROM sessions WHERE expires_at<=?",System.currentTimeMillis());
        db.update("INSERT INTO sessions(token_hash,uid,expires_at) VALUES(?,?,?)",digest(token),user.uid(),expires);
        return dto(user,token,expires);
    }
    @Transactional
    public SessionDto register(String name, String mail, String pass) {
        name = Rules.text(name,3,80,"Nombre"); mail = email(mail); password(pass);
        if (!users("email=?",mail).isEmpty()) throw new ApiException(409,"Ya existe una cuenta con ese correo");
        String uid = "srv_" + UUID.randomUUID();
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        db.update("INSERT INTO users(uid,name,email,salt,password_hash,provider) VALUES(?,?,?,?,?,?)",
            uid,name,mail,salt,derive(pass,salt),"Correo servidor");
        return issue(users("uid=?",uid).get(0));
    }
    @Transactional
    public String createGoogleChallenge() {
        long now = System.currentTimeMillis();
        db.update("DELETE FROM google_login_challenges WHERE expires_at <= ?", now);
        String nonce = UUID.randomUUID().toString();
        db.update("INSERT INTO google_login_challenges(nonce, expires_at) VALUES (?, ?)", nonce, now + 300_000);
        return nonce;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public SessionDto loginWithGoogle(GoogleIdentity.Identity identity) {
        int consumed = db.update(
            "DELETE FROM google_login_challenges WHERE nonce = ? AND expires_at > ?",
            identity.nonce(), System.currentTimeMillis());
        if (consumed != 1)
            throw new ApiException(401, "La solicitud con Google venció o ya se utilizó. Intenta otra vez.");

        var ids = db.query("SELECT uid FROM google_accounts WHERE subject = ?",
            (row, number) -> row.getString(1), identity.subject());
        if (!ids.isEmpty()) return issue(users("uid=?", ids.get(0)).get(0));

        String mail = email(identity.email());
        if (!users("email=?", mail).isEmpty())
            throw new ApiException(409, "Ese correo ya tiene una cuenta. Usa su método de acceso original.");
        String name = identity.name() == null ? "" : identity.name().strip();
        if (name.length() < 3) name = "Cuenta Google";
        if (name.length() > 80) name = name.substring(0, 80);
        String uid = "srv_" + UUID.randomUUID();
        db.update("INSERT INTO users(uid,name,email,provider) VALUES(?,?,?,?)",
            uid, name, mail, "Google");
        db.update("INSERT INTO google_accounts(subject,uid) VALUES(?,?)", identity.subject(), uid);
        return issue(users("uid=?", uid).get(0));
    }

    public SessionDto login(String mail, String pass) {
        mail = email(mail);
        if (pass == null || pass.length()>128) throw new ApiException(401,"El correo o la contraseña no son correctos");
        var matches = users("email=?",mail);
        if (matches.isEmpty() || matches.get(0).hash() == null) {
            derive(pass,new byte[16]);
            throw new ApiException(401,"El correo o la contraseña no son correctos");
        }
        User user = matches.get(0);
        long now = System.currentTimeMillis();
        if (user.lockedUntil()>now) throw new ApiException(429,"Espera un minuto antes de volver a ingresar");
        if (!MessageDigest.isEqual(user.hash(),derive(pass,user.salt()))) {
            if (user.lockedUntil()>0) db.update("UPDATE users SET attempts=0,locked_until=0 WHERE uid=?",user.uid());
            db.update("UPDATE users SET locked_until=CASE WHEN attempts+1>=5 THEN ? ELSE 0 END,attempts=attempts+1 WHERE uid=?",
                now+60000,user.uid());
            throw new ApiException(401,"El correo o la contraseña no son correctos");
        }
        db.update("UPDATE users SET attempts=0,locked_until=0 WHERE uid=?",user.uid());
        return issue(user);
    }
    public String bearer(String header) {
        if (header == null || !header.regionMatches(true,0,"Bearer ",0,7))
            throw new ApiException(401,"Inicia sesión para continuar");
        String token = header.substring(7).trim();
        if (!token.matches("ds_[A-Za-z0-9_-]{43}")) throw new ApiException(401,"La sesión no es válida");
        return token;
    }
    public SessionDto requireSession(String header) {
        String token = bearer(header);
        var ids = db.query("SELECT uid,expires_at FROM sessions WHERE token_hash=?",
            (r,n)->Map.entry(r.getString(1),r.getLong(2)),digest(token));
        if (ids.isEmpty() || ids.get(0).getValue()<=System.currentTimeMillis())
            throw new ApiException(401,"Tu sesión venció. Vuelve a iniciar sesión");
        var found = users("uid=?",ids.get(0).getKey());
        if (found.isEmpty()) throw new ApiException(401,"La sesión no está disponible");
        return dto(found.get(0),"",ids.get(0).getValue());
    }
    public void logout(String header) {
        db.update("DELETE FROM sessions WHERE token_hash=?",digest(bearer(header)));
    }
    @Transactional
    public SessionDto updateName(String header, String name) {
        SessionDto current = requireSession(header);
        db.update("UPDATE users SET name=? WHERE uid=?",Rules.text(name,3,80,"Nombre"),current.uid());
        return requireSession(header);
    }
    @Transactional
    public void changePassword(String header, String current, String next) {
        SessionDto identity = requireSession(header);
        User user = users("uid=?",identity.uid()).get(0);
        if (user.hash()==null) throw new ApiException(400,"Esta cuenta no tiene acceso por contraseña");
        if (current==null || current.length()>128 || !MessageDigest.isEqual(user.hash(),derive(current,user.salt())))
            throw new ApiException(400,"La contraseña actual no es correcta");
        password(next);
        byte[] salt=new byte[16];RANDOM.nextBytes(salt);
        db.update("UPDATE users SET salt=?,password_hash=? WHERE uid=?",salt,derive(next,salt),identity.uid());
        db.update("DELETE FROM sessions WHERE uid=? AND token_hash<>?",identity.uid(),digest(bearer(header)));
    }
}
