package edu.hostelhelp.controller;

import edu.hostelhelp.dto.ApiDtos.AuthView;
import edu.hostelhelp.dto.ApiDtos.LoginRequest;
import edu.hostelhelp.dto.ApiDtos.MessageView;
import edu.hostelhelp.dto.ApiDtos.RegisterRequest;
import edu.hostelhelp.dto.ApiDtos.StudentProfile;
import edu.hostelhelp.entity.Student;
import edu.hostelhelp.entity.User;
import edu.hostelhelp.entity.Warden;
import edu.hostelhelp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          SecurityContextRepository contextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/csrf")
    public MessageView csrf(CsrfToken token) {
        return new MessageView(token.getToken());
    }

    @PostMapping("/register")
    public AuthView register(@Valid @RequestBody RegisterRequest input) {
        Student student = authService.register(input);
        return authView(student);
    }

    @PostMapping("/login")
    public AuthView login(@Valid @RequestBody LoginRequest input, HttpServletRequest request,
                          HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(input.email(), input.password()));
        if (!(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account role is not supported.");
        }
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return authView(user);
    }

    @GetMapping("/me")
    public AuthView me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }
        return authView(user);
    }

    @PostMapping("/logout")
    public MessageView logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return new MessageView("Signed out.");
    }

    private StudentProfile profile(User user) {
        Student student = (Student) user;
        return new StudentProfile(student.getId(), student.getFullName(), student.getEmail(),
                student.getRegistrationNumber(), student.getHostelName(), student.getHostelBlock(),
                student.getRoomNumber(), student.getTrustScore());
    }

    private AuthView authView(User user) {
        StudentProfile student = user instanceof Student ? profile(user) : null;
        String role = user instanceof Student ? "STUDENT" : "WARDEN";
        String wardenPhone = user instanceof Warden warden ? warden.getPhoneNumber() : null;
        return new AuthView(user.getId(), user.getFullName(), user.getEmail(), role, student, wardenPhone);
    }
}