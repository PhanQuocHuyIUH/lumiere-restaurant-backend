package iuh.fit.se.support.repository;

import iuh.fit.se.support.domain.SupportRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    List<SupportRequest> findByTableCodeOrderByCreatedAtDesc(String tableCode);
}
