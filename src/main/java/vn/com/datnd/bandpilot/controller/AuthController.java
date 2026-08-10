package vn.com.datnd.bandpilot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.com.datnd.bandpilot.config.AuthProperties;
import vn.com.datnd.bandpilot.config.JwtUtil;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthProperties authProperties;
    private final JwtUtil jwtUtil;

    public AuthController(AuthProperties authProperties, JwtUtil jwtUtil) {
        this.authProperties = authProperties;
        this.jwtUtil = jwtUtil;
    }

    /**
     * POST /api/v1/auth/login
     * Verifies username/password against configured values.
     * Returns a JWT token on success, 401 on failure.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "").trim();

        boolean valid = authProperties.getUsername().equals(username)
                && authProperties.getPassword().equals(password);

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "status", 401,
                        "error", "Unauthorized",
                        "message", "Invalid username or password"
                    ));
        }

        String token = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of("token", token, "username", username));
    }
}
