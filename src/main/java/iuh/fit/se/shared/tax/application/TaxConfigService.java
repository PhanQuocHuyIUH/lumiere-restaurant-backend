package iuh.fit.se.shared.tax.application;

import iuh.fit.se.shared.domain.TaxMode;
import iuh.fit.se.shared.tax.api.dto.MenuItemPricingPreviewResponse;
import iuh.fit.se.shared.tax.api.dto.TaxConfigResponse;
import java.io.Serializable;

public interface TaxConfigService {

    TaxConfigDto getActive();

    TaxConfigResponse getActiveAsResponse();

    TaxConfigResponse update(TaxMode taxMode, int taxRateBps, Long staffId);

    MenuItemPricingPreviewResponse previewMenuItemPricing();

    record TaxConfigDto(TaxMode taxMode, int taxRateBps) implements Serializable {}
}
