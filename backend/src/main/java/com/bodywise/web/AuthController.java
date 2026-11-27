package com.bodywise.web;

import com.bodywise.domain.User;
import com.bodywise.repo.UserRepo;
import com.bodywise.service.SeedData;
import com.bodywise.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepo users;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final SeedData seed;
    private final Support support;
    private final long ttlDays;

    public AuthController(UserRepo users, PasswordEncoder encoder, JwtEncoder jwtEncoder, SeedData seed,
                          Support support, @Value("${app.jwt.ttl-days}") long ttlDays) {
        this.users = users;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.seed = seed;
        this.support = support;
        this.ttlDays = ttlDays;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User u = new User();
        u.email = email;
        u.passwordHash = encoder.encode(req.password());
        u.name = req.name() != null ? req.name().trim() : null;
        u.reminderEmail = email;
        u.createdOn = support.today();
        users.save(u);
        seed.seedCardsFor(u.id);
        return new AuthResponse(token(u), support.profile(u));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No account for this email yet. Use \"Create account\" first."));
        if (!encoder.matches(req.password(), u.passwordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");
        }
        return new AuthResponse(token(u), support.profile(u));
    }

    private String token(User u) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bodywise")
                .subject(String.valueOf(u.id))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofDays(ttlDays)))
                .claim("email", u.email)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
