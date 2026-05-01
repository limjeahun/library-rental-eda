package com.example.library.common.vo;

import java.util.Objects;

/**
 * 도서 서비스와 대여 서비스가 메시지로 공유하는 도서 식별 값 객체입니다.
 */
public class Item {
    private Long no;
    private String title;

    /**
     * JSON 역직렬화와 프레임워크 바인딩을 위한 기본 생성자입니다.
     */
    public Item() {
    }

    /**
     * 도서 번호와 제목을 가진 공유 도서 값을 생성합니다.
     *
     * @param no 도서 번호입니다.
     * @param title 등록하거나 응답할 도서 제목입니다.
     */
    public Item(Long no, String title) {
        this.no = no;
        this.title = title;
    }

    /**
     * 도서 번호를 반환합니다.
     *
     * @return 도서 번호를 반환합니다.
     */
    public Long getNo() {
        return no;
    }

    /**
     * 메시지 역직렬화 시 도서 번호를 설정합니다.
     *
     * @param no 도서 번호입니다.
     */
    public void setNo(Long no) {
        this.no = no;
    }

    /**
     * 도서 제목을 반환합니다.
     *
     * @return 도서 제목을 반환합니다.
     */
    public String getTitle() {
        return title;
    }

    /**
     * 메시지 역직렬화 시 도서 제목을 설정합니다.
     *
     * @param title 등록하거나 응답할 도서 제목입니다.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 같은 도서 값인지 번호와 제목 기준으로 비교합니다.
     *
     * @param o 동등성 비교 대상 객체입니다.
     * @return 도서 번호와 제목이 모두 같으면 true, 하나라도 다르면 false를 반환합니다.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item item)) {
            return false;
        }
        return Objects.equals(no, item.no) && Objects.equals(title, item.title);
    }

    /**
     * 도서 번호와 제목을 기준으로 값 객체 해시를 계산합니다.
     *
     * @return 계산된 정수 값을 반환합니다.
     */
    @Override
    public int hashCode() {
        return Objects.hash(no, title);
    }
}
