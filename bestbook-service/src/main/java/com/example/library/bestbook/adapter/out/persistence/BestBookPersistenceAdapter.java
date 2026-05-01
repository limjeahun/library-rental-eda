package com.example.library.bestbook.adapter.out.persistence;

import com.example.library.bestbook.adapter.out.persistence.mapper.BestBookPersistenceMapper;
import com.example.library.bestbook.adapter.out.persistence.repository.BestBookMongoRepository;
import com.example.library.bestbook.application.port.out.BestBookPort;
import com.example.library.bestbook.domain.model.BestBook;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 인기 도서 도메인 모델을 MongoDB document로 저장하고 document ID 또는 도서 번호로 복원하는 영속성 컴포넌트입니다.
 */
@Repository
@RequiredArgsConstructor
public class BestBookPersistenceAdapter implements BestBookPort {
    private final BestBookMongoRepository repository;
    private final BestBookPersistenceMapper mapper;

    /**
     * 모든 MongoDB document를 조회해 도메인 모델로 변환합니다.
     *
     * @return MongoDB read model에 저장된 인기 도서 목록을 반환합니다.
     */
    @Override
    public List<BestBook> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    /**
     * document 식별자로 인기 도서 read model을 조회합니다.
     *
     * @param id 조회하거나 저장할 인기 도서 read model 식별자입니다.
     * @return 인기 도서 read model 식별자에 해당하는 결과를 담은 Optional을 반환합니다.
     */
    @Override
    public Optional<BestBook> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    /**
     * 도서 번호로 인기 도서 read model을 조회합니다.
     *
     * @param itemNo 인기 도서 집계 대상 도서 번호입니다.
     * @return 도서 번호에 해당하는 인기 도서 read model을 담은 Optional을 반환합니다.
     */
    @Override
    public Optional<BestBook> findByItemNo(Long itemNo) {
        return repository.findByItemNo(itemNo).map(mapper::toDomain);
    }

    /**
     * 인기 도서 도메인 모델을 MongoDB document로 변환해 저장한 뒤 다시 도메인 모델로 반환합니다.
     *
     * @param bestBook 저장하거나 응답 DTO로 변환할 인기 도서 도메인 모델입니다.
     * @return 저장 후 MongoDB document ID가 반영된 인기 도서 도메인 모델을 반환합니다.
     */
    @Override
    public BestBook save(BestBook bestBook) {
        return mapper.toDomain(repository.save(mapper.toDocument(bestBook)));
    }
}
