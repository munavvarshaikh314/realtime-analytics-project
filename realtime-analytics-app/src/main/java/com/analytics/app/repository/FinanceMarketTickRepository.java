package com.analytics.app.repository;

import com.analytics.app.model.FinanceMarketTick;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FinanceMarketTickRepository extends JpaRepository<FinanceMarketTick, Long> {

    List<FinanceMarketTick> findTop500ByAssetIdOrderByEventTimestampDesc(String assetId);

    Page<FinanceMarketTick> findBySymbolOrderByEventTimestampDesc(String symbol, Pageable pageable);

    Page<FinanceMarketTick> findAllByOrderByEventTimestampDesc(Pageable pageable);

    FinanceMarketTick findFirstBySymbolOrderByEventTimestampDesc(String symbol);

    long countByEventTimestampAfter(LocalDateTime timestamp);
}
