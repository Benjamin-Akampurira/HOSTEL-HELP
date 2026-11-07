CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'WARDEN')),
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    enabled BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE students (
    user_id BIGINT PRIMARY KEY,
    registration_number VARCHAR(50) NOT NULL UNIQUE,
    hostel_name VARCHAR(120) NOT NULL,
    hostel_block VARCHAR(80) NOT NULL,
    room_number VARCHAR(30) NOT NULL,
    trust_score DECIMAL(4, 2) NOT NULL DEFAULT 0 CHECK (trust_score >= 0 AND trust_score <= 5),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wardens (
    user_id BIGINT PRIMARY KEY,
    staff_number VARCHAR(50) UNIQUE,
    CONSTRAINT fk_wardens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    item_name VARCHAR(120) NOT NULL,
    category VARCHAR(60) NOT NULL,
    description TEXT NOT NULL,
    urgency VARCHAR(20) NOT NULL CHECK (urgency IN ('NOW', 'TODAY', 'THIS_WEEK')),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'OFFERED', 'FULFILLED', 'RETURNED', 'CANCELLED')),
    hostel_block VARCHAR(80) NOT NULL,
    expected_return_date DATE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_requests_requester FOREIGN KEY (requester_id) REFERENCES students(user_id),
    CONSTRAINT uq_request_id_requester UNIQUE (id, requester_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE offers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    helper_id BIGINT NOT NULL,
    message VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    offered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    accepted_at DATETIME(6),
    CONSTRAINT fk_offers_request FOREIGN KEY (request_id) REFERENCES requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_offers_helper FOREIGN KEY (helper_id) REFERENCES students(user_id),
    CONSTRAINT uq_offer_request_helper UNIQUE (request_id, helper_id),
    CONSTRAINT uq_offer_request_helper_id UNIQUE (id, request_id, helper_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE borrowing_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    offer_id BIGINT NOT NULL UNIQUE,
    borrower_id BIGINT NOT NULL,
    helper_id BIGINT NOT NULL,
    received_at DATETIME(6),
    expected_return_date DATE,
    return_initiated_at DATETIME(6),
    returned_at DATETIME(6),
    status VARCHAR(20) NOT NULL DEFAULT 'AWAITING_RECEIPT'
        CHECK (status IN ('AWAITING_RECEIPT', 'BORROWED', 'RETURNED', 'CANCELLED')),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_borrowing_request FOREIGN KEY (request_id) REFERENCES requests(id),
    CONSTRAINT fk_borrowing_offer FOREIGN KEY (offer_id) REFERENCES offers(id),
    CONSTRAINT fk_borrowing_borrower FOREIGN KEY (borrower_id) REFERENCES students(user_id),
    CONSTRAINT fk_borrowing_helper FOREIGN KEY (helper_id) REFERENCES students(user_id),
    CONSTRAINT chk_borrower_not_helper CHECK (borrower_id <> helper_id),
    CONSTRAINT fk_borrowing_request_borrower FOREIGN KEY (request_id, borrower_id)
        REFERENCES requests(id, requester_id),
    CONSTRAINT fk_borrowing_offer_consistency FOREIGN KEY (offer_id, request_id, helper_id)
        REFERENCES offers(id, request_id, helper_id),
    CONSTRAINT uq_borrowing_id_participants UNIQUE (id, borrower_id, helper_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ratings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    borrower_id BIGINT NOT NULL,
    helper_id BIGINT NOT NULL,
    borrowing_transaction_id BIGINT NOT NULL UNIQUE,
    score SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ratings_borrower FOREIGN KEY (borrower_id) REFERENCES students(user_id),
    CONSTRAINT fk_ratings_helper FOREIGN KEY (helper_id) REFERENCES students(user_id),
    CONSTRAINT fk_ratings_transaction FOREIGN KEY (borrowing_transaction_id)
        REFERENCES borrowing_transactions(id),
    CONSTRAINT chk_rating_participants CHECK (borrower_id <> helper_id),
    CONSTRAINT fk_rating_transaction_participants
        FOREIGN KEY (borrowing_transaction_id, borrower_id, helper_id)
        REFERENCES borrowing_transactions(id, borrower_id, helper_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    borrowing_transaction_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6),
    resolution_notes TEXT,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES students(user_id),
    CONSTRAINT fk_reports_transaction FOREIGN KEY (borrowing_transaction_id)
        REFERENCES borrowing_transactions(id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('NEW_OFFER', 'OFFER_ACCEPTED', 'OFFER_REJECTED', 'ITEM_RECEIVED',
                        'ITEM_RETURNED', 'RATING_RECEIVED', 'REPORT_UPDATE', 'GENERAL')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_requests_block_status_created ON requests(hostel_block, status, created_at DESC);
CREATE INDEX idx_offers_request_status ON offers(request_id, status);
CREATE INDEX idx_borrowings_participants ON borrowing_transactions(borrower_id, helper_id, status);
CREATE INDEX idx_reports_status_created ON reports(status, created_at DESC);
CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, is_read, created_at DESC);