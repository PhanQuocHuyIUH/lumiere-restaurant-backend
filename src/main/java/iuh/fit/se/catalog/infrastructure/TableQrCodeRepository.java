package iuh.fit.se.catalog.infrastructure;

import iuh.fit.se.catalog.domain.TableQrCode;
import iuh.fit.se.catalog.domain.TableQrCodeStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableQrCodeRepository extends JpaRepository<TableQrCode, Long> {

    Optional<TableQrCode> findByTableId(Long tableId);

    Optional<TableQrCode> findByQrKeyAndStatus(String qrKey, TableQrCodeStatus status);
}
