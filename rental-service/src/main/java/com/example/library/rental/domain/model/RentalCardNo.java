package com.example.library.rental.domain.model;

import java.time.Year;
import java.util.UUID;

/**
 * 연도와 UUID로 구성된 대여카드 번호 값 객체입니다.
 */
public class RentalCardNo {
    private String no;

    /**
     * 프레임워크 바인딩을 위한 기본 생성자입니다.
     */
    public RentalCardNo() {
    }

    /**
     * 지정한 문자열 번호를 가진 대여카드 번호를 생성합니다.
     *
     * @param no 연도와 UUID로 구성된 대여카드 번호 문자열입니다.
     */
    public RentalCardNo(String no) {
        this.no = no;
    }

    /**
     * 현재 연도와 UUID를 조합해 새 대여카드 번호를 생성합니다.
     *
     * @return 현재 연도와 UUID로 구성된 새 대여카드 번호를 반환합니다.
     */
    public static RentalCardNo createRentalCardNo() {
        return new RentalCardNo(Year.now().getValue() + "-" + UUID.randomUUID());
    }

    /**
     * 대여카드 번호 문자열을 반환합니다.
     *
     * @return 대여카드 번호 문자열을 반환합니다.
     */
    public String getNo() {
        return no;
    }

    /**
     * 대여카드 번호 문자열을 설정합니다.
     *
     * @param no 연도와 UUID로 구성된 대여카드 번호 문자열입니다.
     */
    public void setNo(String no) {
        this.no = no;
    }
}
