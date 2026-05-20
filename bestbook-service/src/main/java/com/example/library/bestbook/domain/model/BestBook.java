package com.example.library.bestbook.domain.model;

import com.example.library.bestbook.domain.vo.BestBookItem;

/**
 * 대여 이벤트를 기반으로 누적되는 인기 도서 read model 도메인 모델입니다.
 */
public class BestBook {
    /**
     *  인기 도서 read model 식별자.
     */
    private final Long id;
    /**
     *  도서 번호.
     */
    private final Long itemNo;
    /**
     *  도서 제목.
     */
    private final String itemTitle;
    /**
     *  누적 대여 횟수.
     */
    private long rentCount;

    /**
     * factory와 저장소 복원에서만 인기 도서 상태를 초기화합니다.
     *
     * @param id 인기 도서 read model 식별자.
     * @param itemNo 인기 도서 집계 대상 도서 번호.
     * @param itemTitle 인기 도서 집계에 표시할 도서 제목.
     * @param rentCount 누적 대여 횟수.
     */
    private BestBook(Long id, Long itemNo, String itemTitle, long rentCount) {
        this.id = id;
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentCount = rentCount;
    }

    /**
     * 첫 대여가 기록된 인기 도서 모델을 생성합니다.
     *
     * @param item 업무 대상 도서의 번호와 제목.
     * @return 첫 대여 횟수 1회가 기록된 인기 도서 도메인 모델.
     */
    public static BestBook registerBestBook(BestBookItem item) {
        return new BestBook(null, item.no(), item.title(), 1L);
    }

    /**
     * 첫 대여가 기록된 인기 도서 모델을 생성합니다.
     *
     * @param itemNo 인기 도서 집계 대상 도서 번호.
     * @param itemTitle 인기 도서 집계에 표시할 도서 제목.
     * @return 첫 대여 횟수 1회가 기록된 인기 도서 도메인 모델.
     */
    public static BestBook registerBestBook(Long itemNo, String itemTitle) {
        return new BestBook(null, itemNo, itemTitle, 1L);
    }

    /**
     * 저장소 상태로 인기 도서 모델을 복원합니다.
     *
     * @param id 인기 도서 read model 식별자.
     * @param itemNo 인기 도서 집계 대상 도서 번호.
     * @param itemTitle 인기 도서 집계에 표시할 도서 제목.
     * @param rentCount 저장된 누적 대여 횟수.
     * @return 저장소 상태에서 복원한 인기 도서 도메인 모델.
     */
    public static BestBook reconstitute(Long id, Long itemNo, String itemTitle, long rentCount) {
        return new BestBook(id, itemNo, itemTitle, rentCount);
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
     * 보상 완료 이벤트에 따라 누적 대여 횟수를 1 감소시키되 0 아래로 내려가지 않게 합니다.
     *
     * @return 감소 후 누적 대여 횟수를 반환합니다.
     */
    public long decreaseBestBookCount() {
        if (this.rentCount > 0) {
            this.rentCount -= 1;
        }
        return this.rentCount;
    }

    public Long id() {
        return id;
    }

    public Long itemNo() {
        return itemNo;
    }

    public String itemTitle() {
        return itemTitle;
    }

    public long rentCount() {
        return rentCount;
    }

}
