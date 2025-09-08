package Notes.notes_crud.Controller;

import Notes.notes_crud.Dto.AuthRequest;
import Notes.notes_crud.Dto.AuthResponse;
import Notes.notes_crud.Dto.RegisterRequest;
import Notes.notes_crud.Exception.DuplicateUsernameException;
import Notes.notes_crud.Exception.InvalidCredentialsException;
import Notes.notes_crud.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            log.info("Register request received for user: {}", request.getUsername());
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DuplicateUsernameException e) {
            log.warn("Registration failed - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed - {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Failed to register user"
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthRequest request
    ) {
        try {
            log.info("Login attempt for user: {}", request.getUsername());
            
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                log.warn("Login failed - Username is required");
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Username is required"
                ));
            }
            
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                log.warn("Login failed - Password is required");
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Password is required"
                ));
            }
            
            AuthResponse response = authService.authenticate(request);
            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (InvalidCredentialsException e) {
            log.warn("Login failed - Invalid credentials for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "Invalid username or password"
            ));
        } catch (Exception e) {
            log.error("Login error for user {}: {}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Login failed: " + e.getMessage()
            ));
        }
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Validation failed",
                "errors", errors
        ));
    }
}
