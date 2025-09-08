package Notes.notes_crud.Controller;

import Notes.notes_crud.Dto.NoteRequest;
import Notes.notes_crud.Dto.NoteResponse;
import Notes.notes_crud.Service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getAllNotes(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(noteService.getAllUserNotes(userDetails));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNoteById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(noteService.getNoteById(id, userDetails));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            @Valid @RequestBody NoteRequest noteRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        NoteResponse noteResponse = noteService.createNote(noteRequest, userDetails);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(noteResponse.getId())
                .toUri();
                
        return ResponseEntity.created(location).body(noteResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteRequest noteRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(noteService.updateNote(id, noteRequest, userDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        noteService.deleteNote(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
