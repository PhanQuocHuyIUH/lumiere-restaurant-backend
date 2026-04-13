package iuh.fit.se.catalog.application;

import iuh.fit.se.catalog.api.dto.MenuCategoryResponse;
import java.util.List;

public interface CatalogService {

    List<MenuCategoryResponse> getMenu();

    MenuItemDTO getItem(Long id);

    TableDTO getTableByCode(String tableCode);

    TableDTO getTableById(Long tableId);

    TableDTO getTableByQrKey(String qrKey);

    boolean isItemAvailable(Long id);

    QrSessionTokenDTO issueQrSession(String tableCode);

    void validateQrSession(String sessionId, String tableCode);

    TableQrCodeDTO getOrCreateTableQrCode(String tableCode);

    TableQrCodeDTO rotateTableQrCode(String tableCode);
}