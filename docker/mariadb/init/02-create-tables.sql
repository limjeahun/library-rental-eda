-- book-service: 도서 원장 테이블
USE book_db;

CREATE TABLE IF NOT EXISTS books (
    no BIGINT NOT NULL AUTO_INCREMENT COMMENT '도서 번호',
    title VARCHAR(255) NOT NULL COMMENT '도서 제목',
    description VARCHAR(255) NOT NULL COMMENT '도서 설명',
    author VARCHAR(255) NOT NULL COMMENT '도서 저자',
    isbn VARCHAR(255) NOT NULL COMMENT '도서 ISBN',
    publication_date DATE NOT NULL COMMENT '도서 출판일',
    source ENUM('DONATION', 'SUPPLY') NOT NULL COMMENT '도서 입수 경로',
    classfication ENUM('ARTS', 'COMPUTER', 'LITERATURE') NOT NULL COMMENT '도서 분류',
    book_status ENUM('ENTERED', 'AVAILABLE', 'UNAVAILABLE') NOT NULL COMMENT '도서 대여 상태',
    location ENUM('JEONGJA', 'PANGYO') NOT NULL COMMENT '도서 소장 지점',
    PRIMARY KEY (no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='도서 서비스 도서 원장';

-- member-service: 회원 원장과 권한 테이블
USE member_db;

CREATE TABLE IF NOT EXISTS members (
    member_no BIGINT NOT NULL AUTO_INCREMENT COMMENT '회원 번호',
    member_id VARCHAR(255) NOT NULL COMMENT '회원 로그인 ID',
    member_name VARCHAR(255) NOT NULL COMMENT '회원 이름',
    password VARCHAR(255) NOT NULL COMMENT '회원 비밀번호',
    email VARCHAR(255) NOT NULL COMMENT '회원 이메일',
    point BIGINT NOT NULL COMMENT '회원 보유 포인트',
    PRIMARY KEY (member_no),
    UNIQUE KEY uk_members_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 서비스 회원 원장';

CREATE TABLE IF NOT EXISTS member_authorities (
    member_no BIGINT NOT NULL COMMENT '회원 번호',
    role ENUM('ADMIN', 'USER') NOT NULL COMMENT '회원 권한',
    KEY idx_member_authorities_member_no (member_no),
    CONSTRAINT fk_member_authorities_member_no
        FOREIGN KEY (member_no) REFERENCES members (member_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 권한 목록';

-- rental-service: 대여카드 aggregate 저장 테이블
USE rental_db;

CREATE TABLE IF NOT EXISTS rental_cards (
    rental_card_no VARCHAR(255) NOT NULL COMMENT '대여카드 번호',
    late_fee_point BIGINT NOT NULL COMMENT '연체료 포인트',
    member_id VARCHAR(255) NOT NULL COMMENT '대여카드 소유 회원 ID',
    member_name VARCHAR(255) NOT NULL COMMENT '대여카드 소유 회원 이름',
    rent_status ENUM('RENT_AVAILABLE', 'RENT_UNAVAILABLE') NOT NULL COMMENT '대여 가능 상태',
    PRIMARY KEY (rental_card_no),
    UNIQUE KEY uk_rental_cards_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='대여 서비스 대여카드 원장';

CREATE TABLE IF NOT EXISTS rental_card_rent_items (
    rental_card_no VARCHAR(255) NOT NULL COMMENT '대여카드 번호',
    item_no BIGINT DEFAULT NULL COMMENT '대여 중인 도서 번호',
    item_title VARCHAR(255) DEFAULT NULL COMMENT '대여 중인 도서 제목',
    overdue_date DATE DEFAULT NULL COMMENT '반납 예정일',
    overdued BIT(1) DEFAULT NULL COMMENT '연체 여부',
    rent_date DATE DEFAULT NULL COMMENT '대여일',
    KEY idx_rental_card_rent_items_card_no (rental_card_no),
    CONSTRAINT fk_rental_card_rent_items_card_no
        FOREIGN KEY (rental_card_no) REFERENCES rental_cards (rental_card_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='대여카드의 대여 중 도서 목록';

CREATE TABLE IF NOT EXISTS rental_card_return_items (
    rental_card_no VARCHAR(255) NOT NULL COMMENT '대여카드 번호',
    item_no BIGINT DEFAULT NULL COMMENT '반납 완료 도서 번호',
    item_title VARCHAR(255) DEFAULT NULL COMMENT '반납 완료 도서 제목',
    overdue_date DATE DEFAULT NULL COMMENT '반납 예정일',
    overdued BIT(1) DEFAULT NULL COMMENT '연체 여부',
    rent_date DATE DEFAULT NULL COMMENT '대여일',
    return_date DATE DEFAULT NULL COMMENT '실제 반납일',
    KEY idx_rental_card_return_items_card_no (rental_card_no),
    CONSTRAINT fk_rental_card_return_items_card_no
        FOREIGN KEY (rental_card_no) REFERENCES rental_cards (rental_card_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='대여카드의 반납 완료 도서 목록';
