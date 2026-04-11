package iuh.fit.se.identity.infrastructure;

import iuh.fit.se.identity.domain.Staff;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByIdAndDeletedAtIsNull(Long id);

    Optional<Staff> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNullAndIdNot(String username, Long id);

    List<Staff> findAllByDeletedAtIsNullOrderByIdAsc();
}
