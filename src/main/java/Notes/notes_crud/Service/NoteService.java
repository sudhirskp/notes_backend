package Notes.notes_crud.Service;

import Notes.notes_crud.Dto.NoteRequest;
import Notes.notes_crud.Dto.NoteResponse;
import Notes.notes_crud.Entity.Note;
import Notes.notes_crud.Entity.User;
import Notes.notes_crud.Exception.NoteNotFoundException;
import Notes.notes_crud.Repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteService{
    private final NoteRepository noteRepository;
    private final UserService userService;

    public List<NoteResponse> getAllUserNotes(UserDetails userDetails) {
        User user = userService.getUserFromUserDetails(userDetails);
        return noteRepository.findByUser(user).stream()
                .map(this::mapToNoteResponse)
                .collect(Collectors.toList());
    }

    public NoteResponse getNoteById(Long id, UserDetails userDetails) {
        User user = userService.getUserFromUserDetails(userDetails);
        Note note = noteRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NoteNotFoundException("Note not found with id: " + id));
        return mapToNoteResponse(note);
    }

    @Transactional
    public NoteResponse createNote(NoteRequest noteRequest, UserDetails userDetails) {
        User user = userService.getUserFromUserDetails(userDetails);
        
        Note note = Note.builder()
                .title(noteRequest.getTitle())
                .content(noteRequest.getContent())
                .user(user)
                .build();
        
        Note savedNote = noteRepository.save(note);
        return mapToNoteResponse(savedNote);
    }

    @Transactional
    public NoteResponse updateNote(Long id, NoteRequest noteRequest, UserDetails userDetails) {
        try {
            User user = userService.getUserFromUserDetails(userDetails);
            Note note = noteRepository.findByIdAndUser(id, user)
                    .orElseThrow(() -> new NoteNotFoundException("Note not found with id: " + id));
            
            note.setTitle(noteRequest.getTitle());
            note.setContent(noteRequest.getContent());
            
            Note updatedNote = noteRepository.save(note);
            return mapToNoteResponse(updatedNote);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("This note was updated by another user. Please refresh and try again.");
        }
    }

    @Transactional
    public void deleteNote(Long id, UserDetails userDetails) {
        User user = userService.getUserFromUserDetails(userDetails);
        if (!noteRepository.existsByIdAndUser(id, user)) {
            throw new NoteNotFoundException("Note not found with id: " + id);
        }
        noteRepository.deleteByIdAndUser(id, user);
    }

    private NoteResponse mapToNoteResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
