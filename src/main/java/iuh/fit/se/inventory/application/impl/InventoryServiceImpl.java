package iuh.fit.se.inventory.application.impl;

import iuh.fit.se.inventory.api.dto.AdjustStockRequest;
import iuh.fit.se.inventory.api.dto.CreateIngredientRequest;
import iuh.fit.se.inventory.api.dto.ImportStockRequest;
import iuh.fit.se.inventory.api.dto.IngredientResponse;
import iuh.fit.se.inventory.api.dto.LowStockAlert;
import iuh.fit.se.inventory.api.dto.StockTransactionResponse;
import iuh.fit.se.inventory.api.dto.UpdateIngredientRequest;
import iuh.fit.se.inventory.application.IngredientData;
import iuh.fit.se.inventory.application.InventoryService;
import iuh.fit.se.inventory.domain.Ingredient;
import iuh.fit.se.inventory.domain.StockTransaction;
import iuh.fit.se.inventory.domain.StockTxnType;
import iuh.fit.se.inventory.repository.IngredientRepository;
import iuh.fit.se.inventory.repository.StockTransactionRepository;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final IngredientRepository ingredientRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public InventoryServiceImpl(
            IngredientRepository ingredientRepository,
            StockTransactionRepository stockTransactionRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.ingredientRepository = ingredientRepository;
        this.stockTransactionRepository = stockTransactionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public IngredientResponse createIngredient(CreateIngredientRequest request) {
        Ingredient ingredient = Ingredient.builder()
                .name(request.name().trim())
                .unit(request.unit())
                .lowStockThreshold(request.lowStockThreshold() != null ? request.lowStockThreshold() : BigDecimal.ZERO)
                .imageUrl(normalizeOptionalText(request.imageUrl()))
                .build();
        Ingredient saved = ingredientRepository.save(ingredient);
        return IngredientResponse.from(saved);
    }

    @Override
    public List<IngredientResponse> getAllIngredients() {
        return ingredientRepository.findAllByDeletedAtIsNullOrderByNameAsc().stream()
                .map(IngredientResponse::from)
                .toList();
    }

    @Override
    public IngredientResponse getIngredient(Long id) {
        return IngredientResponse.from(getActiveIngredient(id));
    }

    @Override
    @Transactional
    public IngredientResponse updateIngredient(Long id, UpdateIngredientRequest request) {
        Ingredient ingredient = getActiveIngredient(id);
        ingredient.updateDetails(
                request.name().trim(),
                request.unit(),
                request.lowStockThreshold() != null ? request.lowStockThreshold() : BigDecimal.ZERO,
                normalizeOptionalText(request.imageUrl())
        );
        Ingredient saved = ingredientRepository.save(ingredient);
        checkAndNotifyLowStock(saved);
        return IngredientResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteIngredient(Long id) {
        Ingredient ingredient = getActiveIngredient(id);
        ingredient.softDelete();
        ingredientRepository.save(ingredient);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public IngredientResponse importStock(ImportStockRequest request, Long staffId) {
        Ingredient ingredient = getActiveIngredient(request.ingredientId());
        BigDecimal quantityBefore = ingredient.getCurrentQty();

        ingredient.importStock(request.quantity());
        Ingredient saved = ingredientRepository.save(ingredient);

        StockTransaction txn = StockTransaction.record(
                ingredient.getId(),
                StockTxnType.IMPORT,
                quantityBefore,
                request.quantity(),
                saved.getCurrentQty(),
                request.note(),
                staffId
        );
        stockTransactionRepository.save(txn);

        checkAndNotifyLowStock(saved);
        return IngredientResponse.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public IngredientResponse adjustStock(Long ingredientId, AdjustStockRequest request, Long staffId) {
        Ingredient ingredient = getActiveIngredient(ingredientId);
        BigDecimal quantityBefore = ingredient.getCurrentQty();
        BigDecimal quantityChange = request.newQuantity().subtract(quantityBefore);

        StockTxnType txnType = request.newQuantity().compareTo(BigDecimal.ZERO) == 0
                ? StockTxnType.MANUAL_REPORT
                : StockTxnType.ADJUSTMENT;

        ingredient.adjust(request.newQuantity());
        Ingredient saved = ingredientRepository.save(ingredient);

        StockTransaction txn = StockTransaction.record(
                ingredient.getId(),
                txnType,
                quantityBefore,
                quantityChange,
                saved.getCurrentQty(),
                request.note(),
                staffId
        );
        stockTransactionRepository.save(txn);

        checkAndNotifyLowStock(saved);
        return IngredientResponse.from(saved);
    }

    @Override
    public List<IngredientResponse> getLowStockIngredients() {
        return ingredientRepository.findAllLowStock().stream()
                .map(IngredientResponse::from)
                .toList();
    }

    @Override
    public List<StockTransactionResponse> getStockHistory(Long ingredientId) {
        getActiveIngredient(ingredientId);
        return stockTransactionRepository.findAllByIngredientIdOrderByCreatedAtDesc(ingredientId).stream()
                .map(StockTransactionResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateIngredientExists(Long id) {
        getActiveIngredient(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientData> getIngredientsData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ingredientRepository.findAllById(ids).stream()
                .filter(i -> i.getDeletedAt() == null)
                .map(i -> new IngredientData(i.getId(), i.getName(), i.getUnit(), i.getCurrentQty()))
                .toList();
    }

    private Ingredient getActiveIngredient(Long id) {
        return ingredientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient", id));
    }

    private void checkAndNotifyLowStock(Ingredient ingredient) {
        if (ingredient.isLowStock()) {
            try {
                messagingTemplate.convertAndSend(
                        "/topic/manager/low-stock",
                        LowStockAlert.from(ingredient)
                );
            } catch (Exception ex) {
                LOGGER.warn("Failed to send low-stock alert for ingredient {}", ingredient.getId(), ex);
            }
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
