package iuh.fit.se.table.repository;

import iuh.fit.se.table.domain.QrSession;
import iuh.fit.se.table.domain.QrSessionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrSessionRepository extends JpaRepository<QrSession, Long> {

    Optional<QrSession> findBySessionId(String sessionId);

    Optional<QrSession> findTopByTableIdAndStatusOrderByIssuedAtDesc(Long tableId, QrSessionStatus status);
}
