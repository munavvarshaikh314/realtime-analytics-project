package com.analytics.app.repository;

import com.analytics.app.model.FinancePrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FinancePredictionRepository extends JpaRepository<FinancePrediction, Long> {

    FinancePrediction findFirstBySymbolOrderByCreatedAtDesc(String symbol);

    Page<FinancePrediction> findBySymbolOrderByCreatedAtDesc(String symbol, Pageable pageable);

    Page<FinancePrediction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<FinancePrediction> findTop20ByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(LocalDateTime timestamp);
}
