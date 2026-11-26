package com.deepsource.projects.auth;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final GoogleIdentity google;
    public AuthController(AuthService auth, GoogleIdentity google) {
        this.auth = auth;
        this.google = google;
    }

    public record GoogleLogin(String idToken) {}
    public record GoogleChallenge(String clientId, String nonce) {}

    @PostMapping("/google/challenge")
    public GoogleChallenge googleChallenge() {
        return new GoogleChallenge(google.clientId(), auth.createGoogleChallenge());
    }

    @PostMapping("/google")
    public SessionDto googleLogin(@RequestBody GoogleLogin request) {
        return auth.loginWithGoogle(google.verify(request.idToken()));
    }

    public record Register(String name,String email,String password) {}
    public record Login(String email,String password) {}
    public record Profile(String name) {}
    public record Password(String current,String next) {}
    @PostMapping("/register") public SessionDto register(@RequestBody Register r) {return auth.register(r.name(),r.email(),r.password());}
    @PostMapping("/login") public SessionDto login(@RequestBody Login r) {return auth.login(r.email(),r.password());}
    @GetMapping("/session") public SessionDto session(@RequestHeader(value="Authorization",required=false) String h) {return auth.requireSession(h);}
    @PostMapping("/logout") public void logout(@RequestHeader(value="Authorization",required=false) String h) {auth.logout(h);}
    @PutMapping("/profile") public SessionDto profile(@RequestHeader(value="Authorization",required=false) String h,@RequestBody Profile r) {return auth.updateName(h,r.name());}
    @PutMapping("/password") public void password(@RequestHeader(value="Authorization",required=false) String h,@RequestBody Password r) {auth.changePassword(h,r.current(),r.next());}
}
