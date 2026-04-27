package iuh.fit.se.table.infrastructure;

import iuh.fit.se.table.domain.TableQrCode;
import iuh.fit.se.table.domain.TableQrCodeStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableQrCodeRepository extends JpaRepository<TableQrCode, Long> {

    Optional<TableQrCode> findByTableId(Long tableId);

    Optional<TableQrCode> findByQrKeyAndStatus(String qrKey, TableQrCodeStatus status);
}
