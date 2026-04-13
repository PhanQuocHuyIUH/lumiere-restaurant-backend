package iuh.fit.se.catalog.infrastructure;

import iuh.fit.se.catalog.domain.QrSession;
import iuh.fit.se.catalog.domain.QrSessionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrSessionRepository extends JpaRepository<QrSession, Long> {

    Optional<QrSession> findBySessionId(String sessionId);

    Optional<QrSession> findTopByTableIdAndStatusOrderByIssuedAtDesc(Long tableId, QrSessionStatus status);
}
