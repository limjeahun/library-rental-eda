package com.example.library.bestbook.domain.model;

import com.example.library.common.vo.Item;

/**
 * 대여 이벤트를 기반으로 누적되는 인기 도서 read model 도메인 모델입니다.
 */
public class BestBook {
    private Long id;
    private Long itemNo;
    private String itemTitle;
    private long rentCount;

    /**
     * 저장된 인기 도서 상태를 도메인 모델로 복원할 때 사용합니다.
     *
     * @param id 조회하거나 저장할 인기 도서 read model 식별자입니다.
     * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
     * @param itemTitle 인기 도서 집계에 표시할 도서 제목입니다.
     * @param rentCount 저장하거나 응답할 누적 대여 횟수입니다.
     */
    public BestBook(Long id, Long itemNo, String itemTitle, long rentCount) {
        this.id = id;
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentCount = rentCount;
    }

    /**
     * 공통 도서 값으로 첫 대여가 기록된 인기 도서 모델을 생성합니다.
     *
     * @param item 업무 대상 도서의 번호와 제목입니다.
     * @return 첫 대여 횟수 1회가 기록된 인기 도서 도메인 모델을 반환합니다.
     */
    public static BestBook registerBestBook(Item item) {
        return new BestBook(null, item.no(), item.title(), 1L);
    }

    /**
     * 도서 번호와 제목으로 첫 대여가 기록된 인기 도서 모델을 생성합니다.
     *
     * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
     * @param itemTitle 인기 도서 집계에 표시할 도서 제목입니다.
     * @return 첫 대여 횟수 1회가 기록된 인기 도서 도메인 모델을 반환합니다.
     */
    public static BestBook registerBestBook(Long itemNo, String itemTitle) {
        return new BestBook(null, itemNo, itemTitle, 1L);
    }

    /**
     * 기존 오탈자 메서드명을 유지하면서 실제 누적 대여 횟수 증가 규칙을 실행합니다.
     *
     * @return 증가 후 누적 대여 횟수를 반환합니다.
     * @deprecated use {@link #increaseBestBookCount()}.
     */
    @Deprecated
    public long increseBestBookCount() {
        return increaseBestBookCount();
    }

    /**
     * 인기 도서의 누적 대여 횟수를 1 증가시키고 변경된 값을 반환합니다.
     *
     * @return 증가 후 누적 대여 횟수를 반환합니다.
     */
    public long increaseBestBookCount() {
        this.rentCount += 1;
        return this.rentCount;
    }

    /**
     * 인기 도서 read model 식별자를 반환합니다.
     *
     * @return 인기 도서 read model 식별자를 반환합니다.
     */
    public Long getId() {
        return id;
    }

    /**
     * 도서 번호를 반환합니다.
     *
     * @return 인기 도서로 집계된 도서 번호를 반환합니다.
     */
    public Long getItemNo() {
        return itemNo;
    }

    /**
     * 도서 제목을 반환합니다.
     *
     * @return 인기 도서로 집계된 도서 제목을 반환합니다.
     */
    public String getItemTitle() {
        return itemTitle;
    }

    /**
     * 누적 대여 횟수를 반환합니다.
     *
     * @return 증가 후 누적 대여 횟수를 반환합니다.
     */
    public long getRentCount() {
        return rentCount;
    }
}
