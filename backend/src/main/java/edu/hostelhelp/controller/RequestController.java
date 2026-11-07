package edu.hostelhelp.controller;

import edu.hostelhelp.dto.ApiDtos.OfferInput;
import edu.hostelhelp.dto.ApiDtos.OfferView;
import edu.hostelhelp.dto.ApiDtos.RequestInput;
import edu.hostelhelp.dto.ApiDtos.RequestView;
import edu.hostelhelp.entity.User;
import edu.hostelhelp.service.HostelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import edu.hostelhelp.entity.Student;

@RestController
@RequestMapping("/api/requests")
@PreAuthorize("hasRole('STUDENT')")
public class RequestController {
    private final HostelService service;

    public RequestController(HostelService service) {
        this.service = service;
    }

    @GetMapping
    public List<RequestView> browse(@AuthenticationPrincipal User user,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) String urgency,
                                    @RequestParam(required = false) String status) {
        return service.browse(student(user), search, urgency, status);
    }

    @GetMapping("/mine")
    public List<RequestView> mine(@AuthenticationPrincipal User user) {
        return service.myRequests(student(user));
    }

    @PostMapping
    public RequestView create(@AuthenticationPrincipal User user, @Valid @RequestBody RequestInput input) {
        return service.createRequest(input, student(user));
    }

    @GetMapping("/{id}")
    public RequestView get(@PathVariable long id, @AuthenticationPrincipal User user) {
        return service.request(id, student(user));
    }

    @PostMapping("/{id}/offers")
    public OfferView offer(@PathVariable long id, @AuthenticationPrincipal User user,
                           @Valid @RequestBody(required = false) OfferInput input) {
        return service.offer(id, input, student(user));
    }

    @GetMapping("/{id}/offers")
    public List<OfferView> offers(@PathVariable long id, @AuthenticationPrincipal User user) {
        return service.offersForRequest(id, student(user));
    }

    private Student student(User user) {
        if (user instanceof Student student) {
            return student;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student account required.");
    }
}