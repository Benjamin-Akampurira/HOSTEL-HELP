package edu.hostelhelp.service;

import edu.hostelhelp.dto.ApiDtos.BorrowingView;
import edu.hostelhelp.dto.ApiDtos.MemberDirectoryEntry;
import edu.hostelhelp.dto.ApiDtos.NotificationView;
import edu.hostelhelp.dto.ApiDtos.OfferInput;
import edu.hostelhelp.dto.ApiDtos.OfferView;
import edu.hostelhelp.dto.ApiDtos.RatingInput;
import edu.hostelhelp.dto.ApiDtos.RatingView;
import edu.hostelhelp.dto.ApiDtos.ReportInput;
import edu.hostelhelp.dto.ApiDtos.ReportUpdate;
import edu.hostelhelp.dto.ApiDtos.ReportView;
import edu.hostelhelp.dto.ApiDtos.RequestInput;
import edu.hostelhelp.dto.ApiDtos.RequestView;
import edu.hostelhelp.dto.ApiDtos.WardenOverview;
import edu.hostelhelp.dto.ApiDtos.WardenMemberDetails;
import edu.hostelhelp.dto.ApiDtos.WardenOfferDetails;
import edu.hostelhelp.dto.ApiDtos.WardenReportDetails;
import edu.hostelhelp.dto.ApiDtos.WardenRequestDetails;
import edu.hostelhelp.entity.BorrowRequest;
import edu.hostelhelp.entity.BorrowingTransaction;
import edu.hostelhelp.entity.Notification;
import edu.hostelhelp.entity.Offer;
import edu.hostelhelp.entity.Rating;
import edu.hostelhelp.entity.Report;
import edu.hostelhelp.entity.Student;
import edu.hostelhelp.entity.User;
import edu.hostelhelp.enums.BorrowingStatus;
import edu.hostelhelp.enums.NotificationType;
import edu.hostelhelp.enums.OfferStatus;
import edu.hostelhelp.enums.ReportStatus;
import edu.hostelhelp.enums.RequestStatus;
import edu.hostelhelp.repository.BorrowRequestRepository;
import edu.hostelhelp.repository.BorrowingTransactionRepository;
import edu.hostelhelp.repository.NotificationRepository;
import edu.hostelhelp.repository.OfferRepository;
import edu.hostelhelp.repository.RatingRepository;
import edu.hostelhelp.repository.ReportRepository;
import edu.hostelhelp.repository.StudentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class HostelService {
    private final BorrowRequestRepository requests;
    private final OfferRepository offers;
    private final BorrowingTransactionRepository borrowings;
    private final RatingRepository ratings;
    private final ReportRepository reports;
    private final NotificationRepository notifications;
    private final StudentRepository students;

    public HostelService(BorrowRequestRepository requests, OfferRepository offers,
                         BorrowingTransactionRepository borrowings, RatingRepository ratings,
                         ReportRepository reports, NotificationRepository notifications,
                         StudentRepository students) {
        this.requests = requests;
        this.offers = offers;
        this.borrowings = borrowings;
        this.ratings = ratings;
        this.reports = reports;
        this.notifications = notifications;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public List<RequestView> browse(Student student, String search, String urgency, String status) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return requests.findAllByOrderByCreatedAtDesc().stream()
                .filter(request -> !request.getRequester().getId().equals(student.getId()))
                .filter(request -> request.getStatus() == RequestStatus.OPEN || request.getStatus() == RequestStatus.OFFERED)
                .filter(request -> query.isBlank() || request.getItemName().toLowerCase(Locale.ROOT).contains(query)
                        || request.getDescription().toLowerCase(Locale.ROOT).contains(query))
                .filter(request -> urgency == null || request.getUrgency().name().equalsIgnoreCase(urgency))
                .filter(request -> status == null || request.getStatus().name().equalsIgnoreCase(status))
                .map(this::requestView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RequestView> myRequests(Student student) {
        return requests.findByRequesterIdOrderByCreatedAtDesc(student.getId()).stream().map(this::requestView).toList();
    }

    @Transactional(readOnly = true)
    public RequestView request(long id, Student viewer) {
        BorrowRequest request = requestEntity(id);
        return requestView(request);
    }

    public RequestView createRequest(RequestInput input, Student requester) {
        BorrowRequest request = new BorrowRequest();
        request.setRequester(requester);
        request.setItemName(input.itemName().trim());
        request.setCategory(input.category().trim());
        request.setDescription(input.description().trim());
        request.setUrgency(input.urgency());
        request.setExpectedReturnDate(input.expectedReturnDate());
        request.setHostelBlock(requester.getHostelBlock());
        return requestView(requests.save(request));
    }

    public OfferView offer(long requestId, OfferInput input, Student helper) {
        BorrowRequest request = requests.findLockedById(requestId)
            .orElseThrow(() -> notFound("Request not found."));
        if (request.getRequester().getId().equals(helper.getId())) {
            throw badRequest("You cannot offer help on your own request.");
        }
        if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.OFFERED) {
            throw badRequest("This request is no longer accepting offers.");
        }
        if (offers.existsByRequestIdAndHelperId(requestId, helper.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already offered to help.");
        }
        Offer offer = new Offer();
        offer.setRequest(request);
        offer.setHelper(helper);
        offer.setMessage(input == null ? null : clean(input.message()));
        offer.setStatus(OfferStatus.PENDING);
        request.setStatus(RequestStatus.OFFERED);
        Offer saved = offers.save(offer);
        send(request.getRequester(), "Someone offered to help with " + request.getItemName() + ".", NotificationType.NEW_OFFER);
        return offerView(saved);
    }

    public BorrowingView acceptOffer(long offerId, Student requester) {
        Offer offer = offerEntity(offerId);
        BorrowRequest request = requests.findLockedById(offer.getRequest().getId())
            .orElseThrow(() -> notFound("Request not found."));
        if (!request.getRequester().getId().equals(requester.getId())) {
            throw forbidden("Only the requester can accept an offer.");
        }
        if (offer.getStatus() != OfferStatus.PENDING || request.getStatus() == RequestStatus.FULFILLED
                || request.getStatus() == RequestStatus.RETURNED || request.getStatus() == RequestStatus.CANCELLED) {
            throw badRequest("This offer is no longer available.");
        }
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setAcceptedAt(OffsetDateTime.now());
        offers.findByRequestId(request.getId()).stream()
                .filter(other -> !other.getId().equals(offerId) && other.getStatus() == OfferStatus.PENDING)
            .forEach(other -> {
                other.setStatus(OfferStatus.REJECTED);
                send(other.getHelper(), "Another offer was selected for " + request.getItemName() + ".",
                    NotificationType.OFFER_REJECTED);
            });
        request.setStatus(RequestStatus.FULFILLED);
        BorrowingTransaction transaction = new BorrowingTransaction();
        transaction.setRequest(request);
        transaction.setOffer(offer);
        transaction.setBorrower(requester);
        transaction.setHelper(offer.getHelper());
        transaction.setExpectedReturnDate(request.getExpectedReturnDate());
        transaction.setStatus(BorrowingStatus.AWAITING_RECEIPT);
        BorrowingTransaction saved = borrowings.save(transaction);
        send(offer.getHelper(), "Your offer for " + request.getItemName() + " was accepted.", NotificationType.OFFER_ACCEPTED);
        return borrowingView(saved);
    }

    public void rejectOffer(long offerId, Student requester) {
        Offer offer = offerEntity(offerId);
        if (!offer.getRequest().getRequester().getId().equals(requester.getId())) {
            throw forbidden("Only the requester can reject an offer.");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw badRequest("Only pending offers can be rejected.");
        }
        offer.setStatus(OfferStatus.REJECTED);
        send(offer.getHelper(), "Your offer for " + offer.getRequest().getItemName() + " was not selected.",
                NotificationType.OFFER_REJECTED);
        if (offers.findByRequestId(offer.getRequest().getId()).stream()
                .noneMatch(other -> other.getStatus() == OfferStatus.PENDING)) {
            offer.getRequest().setStatus(RequestStatus.OPEN);
        }
    }

    @Transactional(readOnly = true)
    public List<OfferView> offersForStudent(Student student) {
        List<OfferView> made = offers.findByHelperIdOrderByOfferedAtDesc(student.getId()).stream()
                .map(this::offerView).toList();
        List<OfferView> received = requests.findByRequesterIdOrderByCreatedAtDesc(student.getId()).stream()
                .flatMap(request -> offers.findByRequestId(request.getId()).stream())
                .map(this::offerView).toList();
        return java.util.stream.Stream.concat(made.stream(), received.stream()).distinct().toList();
    }

    @Transactional(readOnly = true)
    public List<OfferView> offersForRequest(long requestId, Student requester) {
        BorrowRequest request = requestEntity(requestId);
        if (!request.getRequester().getId().equals(requester.getId())) {
            throw forbidden("Only the requester can view offers for this request.");
        }
        return offers.findByRequestId(requestId).stream().map(this::offerView).toList();
    }

    @Transactional(readOnly = true)
    public List<RatingView> ratingsForStudent(long studentId) {
        return ratings.findByHelperIdOrderByCreatedAtDesc(studentId).stream().map(this::ratingView).toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowingView> borrowings(Student student) {
        return borrowings.findByBorrowerIdOrHelperIdOrderByCreatedAtDesc(student.getId(), student.getId())
                .stream().map(this::borrowingView).toList();
    }

    public BorrowingView confirmReceived(long id, Student borrower) {
        BorrowingTransaction transaction = borrowingEntity(id);
        if (!transaction.getBorrower().getId().equals(borrower.getId())) {
            throw forbidden("Only the requester can confirm receipt.");
        }
        if (transaction.getStatus() != BorrowingStatus.AWAITING_RECEIPT) {
            throw badRequest("This transaction is not waiting for receipt confirmation.");
        }
        transaction.setReceivedAt(OffsetDateTime.now());
        transaction.setStatus(BorrowingStatus.BORROWED);
        send(transaction.getHelper(), "The borrower confirmed receiving " + transaction.getRequest().getItemName() + ".",
                NotificationType.ITEM_RECEIVED);
        return borrowingView(transaction);
    }

    public BorrowingView confirmReturn(long id, Student actor) {
        BorrowingTransaction transaction = borrowingEntity(id);
        if (transaction.getStatus() != BorrowingStatus.BORROWED) {
            throw badRequest("Only an active borrowing can be returned.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (transaction.getBorrower().getId().equals(actor.getId())) {
            if (transaction.getReturnInitiatedAt() != null) {
                throw badRequest("You have already reported this item as returned.");
            }
            transaction.setReturnInitiatedAt(now);
            send(transaction.getHelper(), "The borrower says " + transaction.getRequest().getItemName()
                    + " has been returned. Please confirm.", NotificationType.ITEM_RETURNED);
        } else if (transaction.getHelper().getId().equals(actor.getId())) {
            if (transaction.getReturnInitiatedAt() == null) {
                throw badRequest("The borrower must report the return first.");
            }
            transaction.setReturnedAt(now);
            transaction.setStatus(BorrowingStatus.RETURNED);
            transaction.getRequest().setStatus(RequestStatus.RETURNED);
            send(transaction.getBorrower(), "Return of " + transaction.getRequest().getItemName() + " was confirmed.",
                    NotificationType.ITEM_RETURNED);
        } else {
            throw forbidden("Only the borrower or helper can confirm this return.");
        }
        return borrowingView(transaction);
    }

    public RatingView rate(long id, RatingInput input, Student borrower) {
        BorrowingTransaction transaction = borrowingEntity(id);
        if (!transaction.getBorrower().getId().equals(borrower.getId())) {
            throw forbidden("Only the borrower can rate the helper.");
        }
        if (transaction.getStatus() != BorrowingStatus.RETURNED || transaction.getReturnedAt() == null) {
            throw badRequest("Ratings are available after the return is confirmed.");
        }
        if (ratings.existsByBorrowingTransactionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This transaction has already been rated.");
        }
        Rating rating = new Rating();
        rating.setBorrower(borrower);
        rating.setHelper(transaction.getHelper());
        rating.setBorrowingTransaction(transaction);
        rating.setScore(input.score());
        rating.setComment(clean(input.comment()));
        Rating saved = ratings.save(rating);
        List<Rating> helperRatings = ratings.findByHelperIdOrderByCreatedAtDesc(transaction.getHelper().getId());
        BigDecimal average = helperRatings.stream().map(item -> BigDecimal.valueOf(item.getScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(helperRatings.size()), 2, RoundingMode.HALF_UP);
        transaction.getHelper().updateTrustScore(average);
        send(transaction.getHelper(), "You received a new rating.", NotificationType.RATING_RECEIVED);
        return ratingView(saved);
    }

    public ReportView createReport(ReportInput input, Student reporter) {
        BorrowingTransaction transaction = borrowingEntity(input.borrowingTransactionId());
        boolean participant = transaction.getBorrower().getId().equals(reporter.getId())
                || transaction.getHelper().getId().equals(reporter.getId());
        if (!participant) {
            throw forbidden("Only participants in the borrowing can report it.");
        }
        Report report = new Report();
        report.setReporter(reporter);
        report.setBorrowingTransaction(transaction);
        report.setReason(input.reason().trim());
        report.setDescription(input.description().trim());
        return reportView(reports.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportView> reports(ReportStatus status) {
        List<Report> result = status == null ? reports.findAllByOrderByCreatedAtDesc()
                : reports.findByStatusOrderByCreatedAtDesc(status);
        return result.stream().map(this::reportView).toList();
    }

        @Transactional(readOnly = true)
        public List<MemberDirectoryEntry> members() {
        return students.findAllByOrderByFullNameAsc().stream()
            .map(student -> new MemberDirectoryEntry(student.getId(), student.getFullName(),
                student.getHostelName(), student.getHostelBlock(), student.getRoomNumber()))
            .toList();
        }

        @Transactional(readOnly = true)
        public WardenOverview wardenOverview() {
        List<RequestView> recentRequests = requests.findAllByOrderByCreatedAtDesc().stream()
            .limit(10).map(this::requestView).toList();
        List<OfferView> recentOffers = offers.findAllByOrderByOfferedAtDesc().stream()
            .limit(10).map(this::offerView).toList();
        List<BorrowingView> recentBorrowings = borrowings.findAllByOrderByCreatedAtDesc().stream()
            .limit(10).map(this::borrowingView).toList();
        List<RatingView> recentRatings = ratings.findAllByOrderByCreatedAtDesc().stream()
            .limit(10).map(this::ratingView).toList();
        List<WardenMemberDetails> allMembers = students.findAllByOrderByFullNameAsc().stream()
                .map(this::wardenMemberDetails).toList();
        List<ReportView> allReports = reports.findAllByOrderByCreatedAtDesc().stream()
            .map(this::reportView).toList();
        return new WardenOverview(students.count(), requests.count(), offers.count(), borrowings.count(),
            ratings.count(), notifications.count(), reports.count(), recentRequests, recentOffers,
            recentBorrowings, recentRatings, allMembers, allReports);
        }

    @Transactional(readOnly = true)
    public WardenReportDetails reportDetails(long id) {
        Report report = reports.findById(id).orElseThrow(() -> notFound("Report not found."));
        BorrowingTransaction transaction = report.getBorrowingTransaction();
        BorrowRequest request = transaction.getRequest();
        Offer offer = transaction.getOffer();
        WardenRequestDetails requestDetails = new WardenRequestDetails(request.getId(), request.getItemName(),
                request.getCategory(), request.getDescription(), request.getUrgency(), request.getStatus(),
                request.getHostelBlock(), request.getExpectedReturnDate(), request.getCreatedAt());
        WardenOfferDetails offerDetails = new WardenOfferDetails(offer.getId(), offer.getMessage(),
                offer.getStatus(), offer.getOfferedAt(), offer.getAcceptedAt());
        return new WardenReportDetails(reportView(report), wardenMemberDetails(report.getReporter()),
                wardenMemberDetails(transaction.getBorrower()), wardenMemberDetails(transaction.getHelper()),
                requestDetails, offerDetails, borrowingView(transaction));
    }

    private WardenMemberDetails wardenMemberDetails(Student student) {
        return new WardenMemberDetails(student.getId(), student.getFullName(), student.getEmail(),
                student.getRegistrationNumber(), student.getHostelName(), student.getHostelBlock(),
                student.getRoomNumber(), student.getTrustScore(), student.getCreatedAt(), student.isEnabled());
    }

    public ReportView updateReport(long id, ReportUpdate update) {
        Report report = reports.findById(id).orElseThrow(() -> notFound("Report not found."));
        report.setStatus(update.status());
        report.setResolutionNotes(clean(update.resolutionNotes()));
        if (update.status() == ReportStatus.RESOLVED || update.status() == ReportStatus.DISMISSED) {
            report.setResolvedAt(OffsetDateTime.now());
        } else {
            report.setResolvedAt(null);
        }
        send(report.getReporter(), "A warden updated your report.", NotificationType.REPORT_UPDATE);
        return reportView(report);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> notifications(Student student) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(student.getId()).stream()
                .map(item -> new NotificationView(item.getId(), item.getMessage(), item.getType(),
                        item.isRead(), item.getCreatedAt())).toList();
    }

    public void markNotificationRead(long id, Student student) {
        Notification notification = notifications.findById(id).orElseThrow(() -> notFound("Notification not found."));
        if (!notification.getRecipient().getId().equals(student.getId())) {
            throw forbidden("This notification belongs to another account.");
        }
        notification.setRead(true);
    }

    private void send(User recipient, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setType(type);
        notification.send();
        notifications.save(notification);
    }

    private BorrowRequest requestEntity(long id) {
        return requests.findById(id).orElseThrow(() -> notFound("Request not found."));
    }

    private Offer offerEntity(long id) {
        return offers.findById(id).orElseThrow(() -> notFound("Offer not found."));
    }

    private BorrowingTransaction borrowingEntity(long id) {
        return borrowings.findById(id).orElseThrow(() -> notFound("Borrowing transaction not found."));
    }

    private RequestView requestView(BorrowRequest request) {
        return new RequestView(request.getId(), request.getRequester().getId(), request.getRequester().getFullName(),
                request.getItemName(), request.getCategory(), request.getDescription(), request.getUrgency(),
                request.getStatus(), request.getHostelBlock(), request.getExpectedReturnDate(), request.getCreatedAt(),
                offers.findByRequestId(request.getId()).size());
    }

    private OfferView offerView(Offer offer) {
        return new OfferView(offer.getId(), offer.getRequest().getId(), offer.getRequest().getRequester().getId(),
            offer.getRequest().getItemName(),
                offer.getHelper().getId(), offer.getHelper().getFullName(), offer.getMessage(),
                offer.getStatus(), offer.getOfferedAt());
    }

    private BorrowingView borrowingView(BorrowingTransaction item) {
        return new BorrowingView(item.getId(), item.getRequest().getId(), item.getRequest().getItemName(),
                item.getBorrower().getId(), item.getBorrower().getFullName(), item.getHelper().getId(),
                item.getHelper().getFullName(), item.getExpectedReturnDate(), item.getReceivedAt(),
            item.getReturnInitiatedAt(), item.getReturnedAt(), item.getStatus(),
            ratings.existsByBorrowingTransactionId(item.getId()));
    }

    private RatingView ratingView(Rating rating) {
        return new RatingView(rating.getId(), rating.getBorrower().getId(), rating.getBorrower().getFullName(),
                rating.getHelper().getId(), rating.getHelper().getFullName(),
                rating.getBorrowingTransaction().getId(), rating.getScore(), rating.getComment(), rating.getCreatedAt());
    }

    private ReportView reportView(Report report) {
        return new ReportView(report.getId(), report.getReporter().getId(), report.getReporter().getFullName(),
                report.getBorrowingTransaction().getId(), report.getBorrowingTransaction().getRequest().getItemName(),
                report.getReason(), report.getDescription(), report.getStatus(), report.getCreatedAt(),
                report.getResolvedAt(), report.getResolutionNotes());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}