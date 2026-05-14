package iuh.fit.se.shared.tax.application;

import iuh.fit.se.shared.domain.TaxMode;
import iuh.fit.se.shared.tax.api.dto.MenuItemPricingPreviewResponse;
import iuh.fit.se.shared.tax.api.dto.TaxConfigResponse;
import java.io.Serializable;

public interface TaxConfigService {

    TaxConfigDto getActive();

    TaxConfigResponse getActiveAsResponse();

    TaxConfigResponse update(TaxMode taxMode, int taxRateBps, Long staffId);

    /**
     * Cập nhật global template VÀ áp dụng ngay xuống tất cả menu items đang active.
     * Trả về số items đã được cập nhật.
     */
    int applyToAllMenuItems(TaxMode taxMode, int taxRateBps, Long staffId);

    MenuItemPricingPreviewResponse previewMenuItemPricing();

    record TaxConfigDto(TaxMode taxMode, int taxRateBps) implements Serializable {}
}
