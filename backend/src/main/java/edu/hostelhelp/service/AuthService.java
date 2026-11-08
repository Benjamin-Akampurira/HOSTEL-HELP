package edu.hostelhelp.service;

import edu.hostelhelp.dto.ApiDtos.RegisterRequest;
import edu.hostelhelp.entity.Student;
import edu.hostelhelp.repository.StudentRepository;
import edu.hostelhelp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class AuthService {
    private final StudentRepository students;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(StudentRepository students, UserRepository users, PasswordEncoder passwordEncoder) {
        this.students = students;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Student register(RegisterRequest input) {
        String email = input.email().trim().toLowerCase();
        String registrationNumber = input.registrationNumber().trim();
        if (!input.password().equals(input.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
        }
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already registered.");
        }
        if (students.existsByRegistrationNumber(registrationNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That registration number is already registered.");
        }
        Student student = new Student();
        student.setFullName(input.fullName().trim());
        student.setEmail(email);
        student.setPasswordHash(passwordEncoder.encode(input.password()));
        student.setRegistrationNumber(registrationNumber);
        student.setHostelName(input.hostelName().trim());
        student.setHostelBlock(input.hostelBlock().trim());
        student.setRoomNumber(input.roomNumber().trim());
        try {
            return students.saveAndFlush(student);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email or registration number is already registered.");
        }
    }
}