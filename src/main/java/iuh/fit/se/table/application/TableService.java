package iuh.fit.se.table.application;

import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.table.domain.TableStatus;
import java.util.List;
import java.util.Optional;

public interface TableService {

    List<TableData> getAllTables();

    TableData getTableByCode(String tableCode);

    TableData getTableById(Long tableId);

    TableData getTableByQrKey(String qrKey);

    OrderResponse getCurrentOrderByTableCode(String tableCode);

    /**
     * Non-throwing variant — returns Optional.empty() when no active order exists.
     * Use this in callers that legitimately handle the "no order" case without
     * needing the rollback semantics of a RuntimeException.
     */
    Optional<OrderResponse> findCurrentOrderByTableCode(String tableCode);

    QrSessionToken issueQrSession(String tableCode);

    void validateQrSession(String sessionId, String tableCode);

    TableQrCodeData getOrCreateTableQrCode(String tableCode);

    TableQrCodeData rotateTableQrCode(String tableCode);

    void markTableOccupied(Long tableId);

    void markTableAvailable(Long tableId);

    void markTableCleaning(Long tableId);

    void autoCompleteCleaningTables();

    TableData updateTableStatus(String tableCode, TableStatus newStatus);

    TableData createTable(int floor, int tableNo, int capacity);

    TableData updateTable(String tableCode, int floor, int tableNo, int capacity);

    void deleteTable(String tableCode);
}
