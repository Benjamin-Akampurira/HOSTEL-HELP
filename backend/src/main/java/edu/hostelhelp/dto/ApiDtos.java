package edu.hostelhelp.dto;

import edu.hostelhelp.enums.BorrowingStatus;
import edu.hostelhelp.enums.NotificationType;
import edu.hostelhelp.enums.OfferStatus;
import edu.hostelhelp.enums.ReportStatus;
import edu.hostelhelp.enums.RequestStatus;
import edu.hostelhelp.enums.Urgency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(max = 50) String registrationNumber,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 72) String password,
            @NotBlank String confirmPassword,
            @NotBlank @Size(max = 120) String hostelName,
            @NotBlank @Size(max = 80) String hostelBlock,
            @NotBlank @Size(max = 30) String roomNumber) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record StudentProfile(Long id, String fullName, String email, String registrationNumber,
                                 String hostelName, String hostelBlock, String roomNumber,
                                 BigDecimal trustScore) {
    }

        public record MemberDirectoryEntry(Long id, String fullName, String hostelName,
                                                                           String hostelBlock, String roomNumber) {
        }

        public record WardenMemberDetails(Long id, String fullName, String email,
                                          String registrationNumber, String hostelName,
                                          String hostelBlock, String roomNumber, BigDecimal trustScore,
                                          OffsetDateTime createdAt, boolean enabled) {
        }

    public record RequestInput(
            @NotBlank @Size(max = 120) String itemName,
            @NotBlank @Size(max = 60) String category,
            @NotBlank @Size(max = 2000) String description,
            @NotNull Urgency urgency,
            @NotNull LocalDate expectedReturnDate) {
    }

    public record OfferInput(@Size(max = 1000) String message) {
    }

    public record RequestView(Long id, Long requesterId, String requesterName, String itemName,
                              String category, String description, Urgency urgency,
                              RequestStatus status, String hostelBlock, LocalDate expectedReturnDate,
                              OffsetDateTime createdAt, long offerCount) {
    }

    public record OfferView(Long id, Long requestId, Long requesterId, String itemName, Long helperId,
                            String helperName, String message, OfferStatus status,
                            OffsetDateTime offeredAt) {
    }

    public record BorrowingView(Long id, Long requestId, String itemName, Long borrowerId,
                                String borrowerName, Long helperId, String helperName,
                                LocalDate expectedReturnDate, OffsetDateTime receivedAt,
                                OffsetDateTime returnInitiatedAt, OffsetDateTime returnedAt,
                                BorrowingStatus status, boolean rated) {
    }

    public record RatingInput(@Min(1) @Max(5) short score, @Size(max = 1000) String comment) {
    }

    public record RatingView(Long id, Long borrowerId, String borrowerName, Long helperId,
                             String helperName, Long borrowingTransactionId, short score,
                             String comment, OffsetDateTime createdAt) {
    }

    public record ReportInput(@NotNull Long borrowingTransactionId,
                              @NotBlank @Size(max = 100) String reason,
                              @NotBlank @Size(max = 3000) String description) {
    }

    public record ReportUpdate(@NotNull ReportStatus status,
                               @Size(max = 3000) String resolutionNotes) {
    }

    public record ReportView(Long id, Long reporterId, String reporterName,
                             Long borrowingTransactionId, String itemName, String reason,
                             String description, ReportStatus status, OffsetDateTime createdAt,
                             OffsetDateTime resolvedAt, String resolutionNotes) {
    }

    public record WardenRequestDetails(Long id, String itemName, String category,
                                       String description, Urgency urgency, RequestStatus status,
                                       String hostelBlock, LocalDate expectedReturnDate,
                                       OffsetDateTime createdAt) {
    }

    public record WardenOfferDetails(Long id, String message, OfferStatus status,
                                     OffsetDateTime offeredAt, OffsetDateTime acceptedAt) {
    }

    public record WardenReportDetails(ReportView report, WardenMemberDetails reporter,
                                     WardenMemberDetails borrower, WardenMemberDetails helper,
                                     WardenRequestDetails request, WardenOfferDetails offer,
                                     BorrowingView borrowing) {
    }

    public record WardenOverview(long memberCount, long requestCount, long offerCount,
                                 long borrowingCount, long ratingCount, long notificationCount,
                                 long reportCount, List<RequestView> recentRequests,
                                 List<OfferView> recentOffers, List<BorrowingView> recentBorrowings,
                                 List<RatingView> recentRatings, List<WardenMemberDetails> members,
                                 List<ReportView> reports) {
    }

    public record NotificationView(Long id, String message, NotificationType type,
                                   boolean read, OffsetDateTime createdAt) {
    }

    public record AuthView(Long id, String fullName, String email, String role, StudentProfile student,
                           String wardenPhone) {
    }

    public record MessageView(String message) {
    }
}