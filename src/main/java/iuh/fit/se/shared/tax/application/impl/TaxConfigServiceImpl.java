package iuh.fit.se.shared.tax.application.impl;

import iuh.fit.se.menu.application.MenuItemPricingData;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.shared.domain.Money;
import iuh.fit.se.shared.domain.PricingSnapshot;
import iuh.fit.se.shared.domain.TaxMode;
import iuh.fit.se.shared.settings.domain.SystemSetting;
import iuh.fit.se.shared.settings.repository.SystemSettingRepository;
import iuh.fit.se.shared.tax.api.dto.MenuItemPricingPreviewResponse;
import iuh.fit.se.shared.tax.api.dto.TaxConfigResponse;
import iuh.fit.se.shared.tax.application.TaxConfigService;
import iuh.fit.se.shared.util.PricingEngine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaxConfigServiceImpl implements TaxConfigService {

    static final String KEY_TAX_MODE = "tax.mode";
    static final String KEY_TAX_RATE = "tax.rate-bps";
    static final String CACHE_NAME = "tax-config";
    static final String CACHE_KEY = "active";

    private final SystemSettingRepository settingRepo;
    private final MenuService menuService;
    private final PricingEngine pricingEngine;

    public TaxConfigServiceImpl(
            SystemSettingRepository settingRepo,
            MenuService menuService,
            PricingEngine pricingEngine
    ) {
        this.settingRepo = settingRepo;
        this.menuService = menuService;
        this.pricingEngine = pricingEngine;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'" + CACHE_KEY + "'")
    public TaxConfigDto getActive() {
        TaxMode mode = parseTaxMode(settingRepo.findValueByKey(KEY_TAX_MODE).orElse("INCLUSIVE"));
        int rateBps = parseInt(settingRepo.findValueByKey(KEY_TAX_RATE).orElse("800"));
        return new TaxConfigDto(mode, rateBps);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxConfigResponse getActiveAsResponse() {
        SystemSetting modeSetting = settingRepo.findById(KEY_TAX_MODE).orElse(null);
        SystemSetting rateSetting = settingRepo.findById(KEY_TAX_RATE).orElse(null);

        TaxMode mode = parseTaxMode(modeSetting != null ? modeSetting.getValue() : "INCLUSIVE");
        int rateBps = parseInt(rateSetting != null ? rateSetting.getValue() : "1000");
        Instant updatedAt = modeSetting != null ? modeSetting.getUpdatedAt() : null;
        Long updatedBy = modeSetting != null ? modeSetting.getUpdatedBy() : null;

        return TaxConfigResponse.of(mode, rateBps, updatedAt, updatedBy);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "'" + CACHE_KEY + "'")
    public TaxConfigResponse update(TaxMode taxMode, int taxRateBps, Long staffId) {
        upsertSetting(KEY_TAX_MODE, taxMode.name(), staffId);
        upsertSetting(KEY_TAX_RATE, String.valueOf(taxRateBps), staffId);
        return TaxConfigResponse.of(taxMode, taxRateBps, Instant.now(), staffId);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemPricingPreviewResponse previewMenuItemPricing() {
        TaxConfigDto config = getActive();

        List<MenuItemPricingData> items = menuService.getAllMenuItemsForTaxPreview();

        List<MenuItemPricingPreviewResponse.ItemRow> rows = new ArrayList<>();
        for (MenuItemPricingData item : items) {
            TaxMode effectiveMode = item.itemTaxMode() != null ? item.itemTaxMode() : config.taxMode();
            int effectiveRate = item.itemTaxRateBps() != null ? item.itemTaxRateBps() : config.taxRateBps();

            PricingSnapshot snap = pricingEngine.snapshot(Money.of(item.price()), effectiveMode, effectiveRate);

            rows.add(new MenuItemPricingPreviewResponse.ItemRow(
                    item.id(),
                    item.name(),
                    item.categoryName(),
                    effectiveMode,
                    effectiveRate,
                    snap.totalAmount().toBigDecimal(),
                    snap.subtotalAmount().toBigDecimal(),
                    snap.taxAmount().toBigDecimal()
            ));
        }

        return new MenuItemPricingPreviewResponse(config.taxMode(), config.taxRateBps(), rows);
    }

    private void upsertSetting(String key, String value, Long staffId) {
        SystemSetting setting = settingRepo.findById(key)
                .orElseGet(() -> SystemSetting.create(key));
        setting.setValue(value, staffId);
        settingRepo.save(setting);
    }

    private TaxMode parseTaxMode(String value) {
        try {
            return TaxMode.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return TaxMode.INCLUSIVE;
        }
    }

    private int parseInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (Exception e) {
            return 800;
        }
    }
}
