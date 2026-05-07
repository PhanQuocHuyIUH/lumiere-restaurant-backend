package iuh.fit.se.kitchen.infrastructure;

import iuh.fit.se.kitchen.domain.BatchPerformance;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchPerformanceRepository extends JpaRepository<BatchPerformance, Long> {

        @Query("""
                        select bp
                        from BatchPerformance bp
                            where bp.recordedAt >= :fromTime
                                and bp.recordedAt < :toTime
                        """)
        Page<BatchPerformance> searchForAiExport(
                        @Param("fromTime") Instant fromTime,
                        @Param("toTime") Instant toTime,
                        Pageable pageable
        );

    Optional<BatchPerformance> findByBatchId(Long batchId);
}
