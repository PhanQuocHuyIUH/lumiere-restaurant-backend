package iuh.fit.se.table.repository;

import iuh.fit.se.table.domain.QrSession;
import iuh.fit.se.table.domain.QrSessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrSessionRepository extends JpaRepository<QrSession, Long> {

    Optional<QrSession> findBySessionId(String sessionId);

    Optional<QrSession> findTopByTableIdAndStatusOrderByIssuedAtDesc(Long tableId, QrSessionStatus status);

    /**
     * All sessions for a table currently in the given status. Used when a table
     * is freed (CLEANING / AVAILABLE) to revoke every lingering ACTIVE session
     * so a stale device can't reuse its X-QR-Session header to keep ordering.
     */
    List<QrSession> findAllByTableIdAndStatus(Long tableId, QrSessionStatus status);
}
