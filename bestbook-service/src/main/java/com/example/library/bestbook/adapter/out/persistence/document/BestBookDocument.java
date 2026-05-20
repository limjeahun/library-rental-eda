package com.example.library.bestbook.adapter.out.persistence.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 인기 도서 read model을 MongoDB에 저장하기 위한 document 모델입니다.
 */
@Document(collection = "best_books")
public class BestBookDocument {
    @Id
    private Long id;

    @Indexed(unique = true)
    private Long itemNo;

    private String itemTitle;

    private long rentCount;

    /**
     * MongoDB mapping이 document를 생성할 때 사용하는 기본 생성자입니다.
     */
    protected BestBookDocument() {
    }

    /**
     * 도메인 모델의 현재 상태를 MongoDB document로 옮길 때 사용합니다.
     *
     * @param id 인기 도서 read model 식별자.
     * @param itemNo 인기 도서 집계 대상 도서 번호.
     * @param itemTitle 인기 도서 집계에 표시할 도서 제목.
     * @param rentCount 누적 대여 횟수.
     */
    public BestBookDocument(Long id, Long itemNo, String itemTitle, long rentCount) {
        this.id = id;
        this.itemNo = itemNo;
        this.itemTitle = itemTitle;
        this.rentCount = rentCount;
    }

    /**
     * MongoDB document 식별자를 반환합니다.
     *
     * @return MongoDB 인기 도서 document 식별자를 반환합니다.
     */
    public Long getId() {
        return id;
    }

    /**
     * 도서 번호를 반환합니다.
     *
     * @return MongoDB document에 저장된 도서 번호를 반환합니다.
     */
    public Long getItemNo() {
        return itemNo;
    }

    /**
     * 도서 제목을 반환합니다.
     *
     * @return 도서 제목을 반환합니다.
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
