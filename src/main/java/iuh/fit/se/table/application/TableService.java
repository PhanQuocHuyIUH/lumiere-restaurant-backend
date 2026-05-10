package iuh.fit.se.table.application;

import iuh.fit.se.table.domain.TableStatus;
import java.util.List;

public interface TableService {

    List<TableDTO> getAllTables();

    TableDTO getTableByCode(String tableCode);

    TableDTO getTableById(Long tableId);

    TableDTO getTableByQrKey(String qrKey);

    QrSessionTokenDTO issueQrSession(String tableCode);

    void validateQrSession(String sessionId, String tableCode);

    TableQrCodeDTO getOrCreateTableQrCode(String tableCode);

    TableQrCodeDTO rotateTableQrCode(String tableCode);

    void markTableOccupied(Long tableId);

    void markTableAvailable(Long tableId);

    void markTableCleaning(Long tableId);

    TableDTO updateTableStatus(String tableCode, TableStatus newStatus);

    TableDTO createTable(int floor, int tableNo, int capacity);

    TableDTO updateTable(String tableCode, int floor, int tableNo, int capacity);

    void deleteTable(String tableCode);
}
