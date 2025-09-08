package Notes.notes_crud.Service;

import Notes.notes_crud.Dto.AuthRequest;
import Notes.notes_crud.Dto.AuthResponse;
import Notes.notes_crud.Dto.RegisterRequest;
import Notes.notes_crud.Entity.User;
import Notes.notes_crud.Exception.DuplicateUsernameException;
import Notes.notes_crud.Exception.InvalidCredentialsException;
import Notes.notes_crud.Repository.UserRepository;
import Notes.notes_crud.Security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("Username and password are required");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed - Username already exists: {}", request.getUsername());
            throw new DuplicateUsernameException("Username is already taken");
        }

        try {
            var user = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .build();
            
            user = userRepository.save(user);
            log.info("User registered successfully: {}", user.getUsername());
            
            var jwtToken = jwtService.generateToken(user);
            
            return AuthResponse.builder()
                    .accessToken(jwtToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .build();
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register user: " + e.getMessage(), e);
        }
    }

    public AuthResponse authenticate(AuthRequest request) {
        log.info("Authentication attempt for user: {}", request.getUsername());
        
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            log.warn("Login failed - Missing username or password");
            throw new InvalidCredentialsException("Username and password are required");
        }

        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            
            // If authentication is successful, generate token
            var user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        log.error("User authenticated but not found in database: {}", request.getUsername());
                        return new InvalidCredentialsException("User not found");
                    });
            
            var jwtToken = jwtService.generateToken(user);
            log.info("User authenticated successfully: {}", request.getUsername());
            
            return AuthResponse.builder()
                    .accessToken(jwtToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .build();
                    
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed - Invalid credentials for user: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        } catch (Exception e) {
            log.error("Authentication error for user {}: {}", request.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }
}
