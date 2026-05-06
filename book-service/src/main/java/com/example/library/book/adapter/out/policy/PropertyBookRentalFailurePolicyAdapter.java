package com.example.library.book.adapter.out.policy;

import com.example.library.book.application.port.out.BookRentalFailurePolicyPort;
import com.example.library.book.config.BookFailureProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Spring configuration properties를 application failure policy port로 변환하는 adapter입니다.
 */
@Component
@RequiredArgsConstructor
public class PropertyBookRentalFailurePolicyAdapter implements BookRentalFailurePolicyPort {
    private final BookFailureProperties properties;

    @Override
    public boolean shouldFailRent() {
        return properties.forceRentFail();
    }

    @Override
    public boolean shouldFailReturn() {
        return properties.forceReturnFail();
    }
}
