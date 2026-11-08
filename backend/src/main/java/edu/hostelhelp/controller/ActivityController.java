package edu.hostelhelp.controller;

import edu.hostelhelp.dto.ApiDtos.BorrowingView;
import edu.hostelhelp.dto.ApiDtos.MessageView;
import edu.hostelhelp.dto.ApiDtos.MemberDirectoryEntry;
import edu.hostelhelp.dto.ApiDtos.NotificationView;
import edu.hostelhelp.dto.ApiDtos.OfferView;
import edu.hostelhelp.dto.ApiDtos.RatingInput;
import edu.hostelhelp.dto.ApiDtos.RatingView;
import edu.hostelhelp.dto.ApiDtos.ReportInput;
import edu.hostelhelp.dto.ApiDtos.ReportView;
import edu.hostelhelp.entity.Student;
import edu.hostelhelp.entity.User;
import edu.hostelhelp.service.HostelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('STUDENT')")
public class ActivityController {
    private final HostelService service;

    public ActivityController(HostelService service) {
        this.service = service;
    }

    @GetMapping("/students")
    public List<MemberDirectoryEntry> members() {
        return service.members();
    }

    @GetMapping("/offers")
    public List<OfferView> offers(@AuthenticationPrincipal User user) {
        return service.offersForStudent(student(user));
    }

    @PostMapping("/offers/{id}/accept")
    public BorrowingView accept(@PathVariable long id, @AuthenticationPrincipal User user) {
        return service.acceptOffer(id, student(user));
    }

    @PostMapping("/offers/{id}/reject")
    public MessageView reject(@PathVariable long id, @AuthenticationPrincipal User user) {
        service.rejectOffer(id, student(user));
        return new MessageView("Offer rejected.");
    }

    @GetMapping("/borrowings")
    public List<BorrowingView> borrowings(@AuthenticationPrincipal User user) {
        return service.borrowings(student(user));
    }

    @PostMapping("/borrowings/{id}/confirm-received")
    public BorrowingView received(@PathVariable long id, @AuthenticationPrincipal User user) {
        return service.confirmReceived(id, student(user));
    }

    @PostMapping("/borrowings/{id}/confirm-return")
    public BorrowingView returned(@PathVariable long id, @AuthenticationPrincipal User user) {
        return service.confirmReturn(id, student(user));
    }

    @PostMapping("/borrowings/{id}/rating")
    public RatingView rate(@PathVariable long id, @AuthenticationPrincipal User user,
                           @Valid @RequestBody RatingInput input) {
        return service.rate(id, input, student(user));
    }

    @GetMapping("/students/{id}/ratings")
    public List<RatingView> ratings(@PathVariable long id) {
        return service.ratingsForStudent(id);
    }

    @GetMapping("/notifications")
    public List<NotificationView> notifications(@AuthenticationPrincipal User user) {
        return service.notifications(student(user));
    }

    @PatchMapping("/notifications/{id}/read")
    public MessageView markRead(@PathVariable long id, @AuthenticationPrincipal User user) {
        service.markNotificationRead(id, student(user));
        return new MessageView("Notification marked as read.");
    }

    @PostMapping("/reports")
    public ReportView report(@AuthenticationPrincipal User user, @Valid @RequestBody ReportInput input) {
        return service.createReport(input, student(user));
    }

    private Student student(User user) {
        if (user instanceof Student student) {
            return student;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student account required.");
    }
}