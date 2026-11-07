package edu.hostelhelp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import edu.hostelhelp.entity.BorrowRequest;
import edu.hostelhelp.entity.Offer;
import edu.hostelhelp.entity.Student;
import edu.hostelhelp.repository.BorrowRequestRepository;
import edu.hostelhelp.repository.BorrowingTransactionRepository;
import edu.hostelhelp.repository.NotificationRepository;
import edu.hostelhelp.repository.OfferRepository;
import edu.hostelhelp.repository.RatingRepository;
import edu.hostelhelp.repository.ReportRepository;
import edu.hostelhelp.repository.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class HostelServiceTest {
    @Mock private BorrowRequestRepository requests;
    @Mock private OfferRepository offers;
    @Mock private BorrowingTransactionRepository borrowings;
    @Mock private RatingRepository ratings;
    @Mock private ReportRepository reports;
    @Mock private NotificationRepository notifications;
    @Mock private StudentRepository students;

    @Test
    void studentCannotOfferOnOwnRequest() {
        Student requester = new Student();
        requester.setId(41L);
        requester.setHostelBlock("B");
        BorrowRequest request = new BorrowRequest();
        request.setId(13L);
        request.setRequester(requester);
        request.setHostelBlock("B");
        when(requests.findLockedById(13L)).thenReturn(Optional.of(request));
        HostelService service = service();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.offer(13L, null, requester));

        assertEquals(400, exception.getStatusCode().value());
        verify(offers, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void browseIncludesRequestsFromOtherBlocks() {
        Student requester = new Student();
        requester.setId(52L);
        requester.setFullName("Other Block Member");
        requester.setHostelBlock("B");
        BorrowRequest request = new BorrowRequest();
        request.setId(19L);
        request.setRequester(requester);
        request.setHostelBlock("B");
        request.setStatus(edu.hostelhelp.enums.RequestStatus.OPEN);
        when(requests.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of(request));

        Student viewer = new Student();
        viewer.setId(41L);
        viewer.setHostelBlock("A");
        var result = service().browse(viewer, null, null, null);

        assertEquals(1, result.size());
        assertEquals("B", result.getFirst().hostelBlock());
    }

    @Test
    void memberCanOpenRequestFromAnotherBlock() {
        Student requester = new Student();
        requester.setId(52L);
        requester.setFullName("Other Block Member");
        requester.setHostelBlock("B");
        BorrowRequest request = new BorrowRequest();
        request.setId(19L);
        request.setRequester(requester);
        request.setHostelBlock("B");
        when(requests.findById(19L)).thenReturn(Optional.of(request));
        when(offers.findByRequestId(19L)).thenReturn(java.util.List.of());

        Student viewer = new Student();
        viewer.setId(41L);
        viewer.setHostelBlock("A");

        assertEquals("B", service().request(19L, viewer).hostelBlock());
    }

    @Test
    void memberCanOfferToRequestFromAnotherBlock() {
        Student requester = new Student();
        requester.setId(52L);
        requester.setFullName("Requester");
        requester.setHostelBlock("B");
        BorrowRequest request = new BorrowRequest();
        request.setId(19L);
        request.setRequester(requester);
        request.setHostelBlock("B");
        request.setStatus(edu.hostelhelp.enums.RequestStatus.OPEN);
        when(requests.findLockedById(19L)).thenReturn(Optional.of(request));
        when(offers.existsByRequestIdAndHelperId(19L, 41L)).thenReturn(false);
        when(offers.save(org.mockito.ArgumentMatchers.any(Offer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Student helper = new Student();
        helper.setId(41L);
        helper.setFullName("Helper");
        helper.setHostelBlock("A");

        var result = service().offer(19L, null, helper);

        assertEquals("Helper", result.helperName());
        verify(offers).save(org.mockito.ArgumentMatchers.any(Offer.class));
    }

    @Test
    void memberDirectoryReturnsNamesAndRoomLocations() {
        Student member = new Student();
        member.setId(52L);
        member.setFullName("A Member");
        member.setHostelName("Main Hostel");
        member.setHostelBlock("B");
        member.setRoomNumber("12");
        when(students.findAllByOrderByFullNameAsc()).thenReturn(java.util.List.of(member));

        var result = service().members();

        assertEquals(1, result.size());
        assertEquals("A Member", result.getFirst().fullName());
        assertEquals("12", result.getFirst().roomNumber());
        assertEquals("B", result.getFirst().hostelBlock());
    }

    private HostelService service() {
        return new HostelService(requests, offers, borrowings, ratings, reports, notifications, students);
    }
}