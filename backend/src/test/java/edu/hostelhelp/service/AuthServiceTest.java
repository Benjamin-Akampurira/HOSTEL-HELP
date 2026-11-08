package edu.hostelhelp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.hostelhelp.dto.ApiDtos.RegisterRequest;
import edu.hostelhelp.entity.Student;
import edu.hostelhelp.repository.StudentRepository;
import edu.hostelhelp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private StudentRepository students;
    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthService authService;

    @Test
    void rejectsPasswordConfirmationMismatchBeforeSaving() {
        RegisterRequest input = input("different-password");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(input));

        assertEquals(400, exception.getStatusCode().value());
        verify(students, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateEmail() {
        when(users.existsByEmail("student@example.edu")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(input("a-secure-password")));

        assertEquals(409, exception.getStatusCode().value());
        verify(students, never()).saveAndFlush(any());
    }

    @Test
    void storesOnlyEncodedPassword() {
        when(users.existsByEmail("student@example.edu")).thenReturn(false);
        when(students.existsByRegistrationNumber("STU-001")).thenReturn(false);
        when(passwordEncoder.encode("a-secure-password")).thenReturn("$2a$encoded-value");
        when(students.saveAndFlush(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student result = authService.register(input("a-secure-password"));

        assertEquals("student@example.edu", result.getEmail());
        assertEquals("$2a$encoded-value", result.getPasswordHash());
        assertNotEquals("a-secure-password", result.getPasswordHash());
        verify(students).saveAndFlush(any(Student.class));
    }

    private RegisterRequest input(String confirmation) {
        return new RegisterRequest("Student One", "STU-001", "Student@Example.edu", "a-secure-password",
                confirmation, "North Hostel", "B", "204");
    }
}