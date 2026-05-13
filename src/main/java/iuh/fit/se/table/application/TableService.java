package iuh.fit.se.table.application;

import iuh.fit.se.table.domain.TableStatus;
import java.util.List;

public interface TableService {

    List<TableData> getAllTables();

    TableData getTableByCode(String tableCode);

    TableData getTableById(Long tableId);

    TableData getTableByQrKey(String qrKey);

    QrSessionToken issueQrSession(String tableCode);

    void validateQrSession(String sessionId, String tableCode);

    TableQrCodeData getOrCreateTableQrCode(String tableCode);

    TableQrCodeData rotateTableQrCode(String tableCode);

    void markTableOccupied(Long tableId);

    void markTableAvailable(Long tableId);

    void markTableCleaning(Long tableId);

    TableData updateTableStatus(String tableCode, TableStatus newStatus);

    TableData createTable(int floor, int tableNo, int capacity);

    TableData updateTable(String tableCode, int floor, int tableNo, int capacity);

    void deleteTable(String tableCode);
}
