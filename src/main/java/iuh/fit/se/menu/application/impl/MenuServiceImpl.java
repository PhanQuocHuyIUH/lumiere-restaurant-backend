package iuh.fit.se.menu.application.impl;

import iuh.fit.se.analytics.application.AnalyticsService;
import iuh.fit.se.inventory.application.IngredientData;
import iuh.fit.se.inventory.application.InventoryService;
import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.kitchen.application.KitchenTaskCookData;
import iuh.fit.se.menu.api.dto.CustomerMenuCategoryResponse;
import iuh.fit.se.menu.api.dto.CustomerMenuItemResponse;
import iuh.fit.se.menu.api.dto.CustomerTrendingResponse;
import iuh.fit.se.menu.api.dto.TrendingMenuItemResponse;
import iuh.fit.se.menu.api.dto.MenuCategorySummaryResponse;
import iuh.fit.se.menu.api.dto.MenuCategoryResponse;
import iuh.fit.se.menu.api.dto.MenuItemResponse;
import iuh.fit.se.menu.api.dto.manager.CookTimeSuggestionResponse;
import iuh.fit.se.menu.api.dto.manager.ManagerMenuCategoryListItemResponse;
import iuh.fit.se.menu.api.dto.manager.CreateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.manager.CreateMenuItemRequest;
import iuh.fit.se.menu.api.dto.manager.MenuCategoryDetailResponse;
import iuh.fit.se.menu.api.dto.manager.MenuItemManagerDetailResponse;
import iuh.fit.se.menu.api.dto.manager.RecipeItemResponse;
import iuh.fit.se.menu.api.dto.manager.UpdateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.manager.UpdateMenuItemRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertFixedComboRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertPickComboRequest;
import iuh.fit.se.menu.api.dto.manager.UpsertRecipeRequest;
import iuh.fit.se.menu.application.MenuItemAvailability;
import iuh.fit.se.menu.application.MenuItemData;
import iuh.fit.se.menu.application.MenuItemPricingData;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.menu.domain.ComboFixedComponent;
import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.ComboPickSlot;
import iuh.fit.se.menu.domain.ComboPickSlotItem;
import iuh.fit.se.menu.domain.MenuCategory;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemIngredient;
import iuh.fit.se.menu.domain.MenuItemType;
import iuh.fit.se.menu.repository.ComboFixedComponentRepository;
import iuh.fit.se.menu.repository.ComboPickSlotItemRepository;
import iuh.fit.se.menu.repository.ComboPickSlotRepository;
import iuh.fit.se.menu.repository.MenuCategoryRepository;
import iuh.fit.se.menu.repository.MenuItemIngredientRepository;
import iuh.fit.se.menu.repository.MenuItemRepository;
import iuh.fit.se.shared.ai.AiClient;
import iuh.fit.se.shared.ai.AiOperation;
import iuh.fit.se.shared.ai.client.dto.ComboGenerateRequest;
import iuh.fit.se.shared.ai.client.dto.ComboGenerateResponse;
import iuh.fit.se.shared.ai.client.dto.SyncMenuRequest;
import iuh.fit.se.shared.ai.client.dto.SyncMenuResponse;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.storage.ImageStorageService;
import iuh.fit.se.shared.storage.StoredImage;
import java.math.BigDecimal;
import iuh.fit.se.shared.domain.Money;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuServiceImpl.class);

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final ComboFixedComponentRepository comboFixedComponentRepository;
    private final ComboPickSlotRepository comboPickSlotRepository;
    private final ComboPickSlotItemRepository comboPickSlotItemRepository;
    private final ImageStorageService imageStorageService;
    private final MenuItemIngredientRepository menuItemIngredientRepository;
    private final InventoryService inventoryService;
    private final AiClient aiClient;
    private final TaskExecutor aiTaskExecutor;
    private final KitchenService kitchenService;
    private final AnalyticsService analyticsService;
    private final StringRedisTemplate stringRedisTemplate;

    public MenuServiceImpl(
            MenuCategoryRepository menuCategoryRepository,
            MenuItemRepository menuItemRepository,
            ComboFixedComponentRepository comboFixedComponentRepository,
            ComboPickSlotRepository comboPickSlotRepository,
            ComboPickSlotItemRepository comboPickSlotItemRepository,
            ImageStorageService imageStorageService,
            MenuItemIngredientRepository menuItemIngredientRepository,
            InventoryService inventoryService,
            AiClient aiClient,
            @Qualifier("aiTaskExecutor") TaskExecutor aiTaskExecutor,
            @Lazy KitchenService kitchenService,
            AnalyticsService analyticsService,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.comboFixedComponentRepository = comboFixedComponentRepository;
        this.comboPickSlotRepository = comboPickSlotRepository;
        this.comboPickSlotItemRepository = comboPickSlotItemRepository;
        this.imageStorageService = imageStorageService;
        this.menuItemIngredientRepository = menuItemIngredientRepository;
        this.inventoryService = inventoryService;
        this.aiClient = aiClient;
        this.aiTaskExecutor = aiTaskExecutor;
        this.kitchenService = kitchenService;
        this.analyticsService = analyticsService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Cacheable(value = "menu", key = "'full'")
    public List<MenuCategoryResponse> getMenu() {
        List<MenuCategory> categories = menuCategoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc();
        List<MenuItem> menuItems = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc();

        Map<Long, List<MenuItemResponse>> itemsByCategoryId = menuItems.stream()
                .collect(Collectors.groupingBy(
                        MenuItem::getCategoryId,
                        LinkedHashMap::new,
                        Collectors.mapping(MenuItemResponse::from, Collectors.toList())
                ));

        return categories.stream()
                .map(category -> MenuCategoryResponse.from(
                        category,
                        itemsByCategoryId.getOrDefault(category.getId(), List.of())
                ))
                .toList();
    }

    @Override
    @Cacheable(value = "menu", key = "'categories'")
    public List<MenuCategorySummaryResponse> getStaffMenuCategories() {
        return menuCategoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc().stream()
                .map(MenuCategorySummaryResponse::from)
                .toList();
    }

    @Override
    @Cacheable(value = "menu", key = "'customer'")
    public List<CustomerMenuCategoryResponse> getCustomerMenu() {
        List<MenuCategory> categories = menuCategoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc();
        List<MenuItem> menuItems = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc();

        Map<Long, List<CustomerMenuItemResponse>> itemsByCategoryId = menuItems.stream()
                .collect(Collectors.groupingBy(
                        MenuItem::getCategoryId,
                        LinkedHashMap::new,
                        Collectors.mapping(item -> {
                            boolean sufficient = checkIngredientAvailability(item.getId(), 1).sufficient();
                            return CustomerMenuItemResponse.from(item, sufficient);
                        }, Collectors.toList())
                ));

        return categories.stream()
                .map(category -> CustomerMenuCategoryResponse.from(
                        category,
                        itemsByCategoryId.getOrDefault(category.getId(), List.of())
                ))
                .toList();
    }

    @Override
    @Cacheable(value = "trending", key = "'items'")
    public CustomerTrendingResponse getTrending() {
        List<MenuCategory> categories = menuCategoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc();
        List<MenuItem> allItems = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc();

        Map<Long, MenuCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(MenuCategory::getId, Function.identity()));

        // Order counts for the last 7 days
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Map<Long, Long> orderCountByItemId = analyticsService.getOrderCountsByItem(from, to);

        // CTR from shared Redis — written by AI service's feedback endpoint
        Map<Long, Double> ctrByItemId = readCtrFromRedis(allItems);

        // Normalize order count and blend: 0.6 × order_count + 0.4 × CTR
        long maxCount = orderCountByItemId.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        final long divisor = maxCount == 0 ? 1L : maxCount;

        List<MenuItem> ranked = allItems.stream()
                .sorted(Comparator.comparingDouble((MenuItem item) -> {
                    double normOrder = (double) orderCountByItemId.getOrDefault(item.getId(), 0L) / divisor;
                    double ctr = ctrByItemId.getOrDefault(item.getId(), 0.0);
                    return 0.6 * normOrder + 0.4 * ctr;
                }).reversed())
                .limit(10)
                .toList();

        // Filter by available + ingredient sufficiency, split by type
        List<TrendingMenuItemResponse> singles = new ArrayList<>();
        List<TrendingMenuItemResponse> combos = new ArrayList<>();

        for (MenuItem item : ranked) {
            if (!item.isAvailable()) continue;
            boolean sufficient = checkIngredientAvailability(item.getId(), 1).sufficient();
            if (!sufficient) continue;

            MenuCategory category = categoryMap.get(item.getCategoryId());
            TrendingMenuItemResponse dto = TrendingMenuItemResponse.from(item, category, true);

            if (item.getItemType() == MenuItemType.COMBO) {
                combos.add(dto);
            } else {
                singles.add(dto);
            }
        }

        TrendingMenuItemResponse hero = singles.isEmpty() ? null : singles.get(0);
        List<TrendingMenuItemResponse> topRanked = singles.stream().limit(3).toList();
        List<TrendingMenuItemResponse> topCombos = combos.stream().limit(3).toList();

        return new CustomerTrendingResponse(hero, topRanked, topCombos);
    }

    private Map<Long, Double> readCtrFromRedis(List<MenuItem> items) {
        if (items.isEmpty()) return Map.of();
        try {
            List<String> fields = items.stream().map(item -> String.valueOf(item.getId())).toList();
            Collection<Object> fieldKeys = new ArrayList<>(fields);
            List<Object> impressions = stringRedisTemplate.opsForHash().multiGet("ai:ctr:imp", fieldKeys);
            List<Object> clicks      = stringRedisTemplate.opsForHash().multiGet("ai:ctr:clk", fieldKeys);

            Map<Long, Double> result = new HashMap<>();
            for (int i = 0; i < items.size(); i++) {
                long imp = parseLong(impressions.get(i));
                long clk = parseLong(clicks.get(i));
                result.put(items.get(i).getId(), imp > 0 ? (double) clk / imp : 0.0);
            }
            return result;
        } catch (Exception ex) {
            LOGGER.warn("Failed to read CTR from Redis, scoring by order count only: {}", ex.getMessage());
            return Map.of();
        }
    }

    private static long parseLong(Object value) {
        if (value == null) return 0L;
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return 0L; }
    }

    @Override
    public List<ManagerMenuCategoryListItemResponse> getAllCategoriesForManager() {
        List<MenuCategory> categories =
                menuCategoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc();
        return categories.stream()
                .map(category -> ManagerMenuCategoryListItemResponse.from(
                        category,
                        menuItemRepository.countByCategoryIdAndDeletedAtIsNull(category.getId())
                ))
                .toList();
    }

    @Override
    public List<MenuItemResponse> getAvailableItemsByCategory(Long categoryId) {
        getActiveCategory(categoryId); // throws ResourceNotFoundException if not found
        return menuItemRepository
                .findAllByCategoryIdAndDeletedAtIsNullOrderByIdAsc(categoryId)
                .stream()
                .filter(MenuItem::isAvailable)
                .map(MenuItemResponse::from)
                .toList();
    }

    @Override
    @Cacheable(value = "menu", key = "'item:' + #id")
    public MenuItemData getItem(Long id) {
        return MenuItemData.from(getActiveMenuItem(id));
    }

    @Override
    public List<MenuItemData> getMenuItemsBulk(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return menuItemRepository.findAllById(ids).stream()
                .map(MenuItemData::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemPricingData> getAllMenuItemsForTaxPreview() {
        List<MenuItem> items = menuItemRepository.findAllByDeletedAtIsNullOrderByIdAsc();
        Map<Long, String> categoryNames = menuCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(MenuCategory::getId, MenuCategory::getName));
        return items.stream()
                .map(item -> new MenuItemPricingData(
                        item.getId(),
                        item.getName(),
                        categoryNames.getOrDefault(item.getCategoryId(), ""),
                        item.getPrice() == null ? BigDecimal.ZERO : item.getPrice().toBigDecimal(),
                        item.getItemTaxMode(),
                        item.getItemTaxRateBps()
                ))
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemManagerDetailResponse updateMenuItemImage(Long id, MultipartFile file) {
        MenuItem menuItem = getActiveMenuItem(id);
        String previousPublicId = menuItem.getImagePublicId();

        StoredImage uploadedImage = imageStorageService.uploadMenuItemImage(menuItem.getId(), file);
        try {
            menuItem.updateImage(uploadedImage.url(), uploadedImage.publicId());
            MenuItem savedMenuItem = menuItemRepository.save(menuItem);
            deleteObsoleteImage(previousPublicId, uploadedImage.publicId());
            return MenuItemManagerDetailResponse.fromBase(savedMenuItem);
        } catch (RuntimeException ex) {
            safeDelete(uploadedImage.publicId());
            throw ex;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemManagerDetailResponse createMenuItem(CreateMenuItemRequest request) {
        MenuCategory category = getActiveCategory(request.categoryId());

        MenuItem item = MenuItem.builder()
                .categoryId(request.categoryId())
                .name(request.name().trim())
                .description(normalizeOptionalText(request.description()))
                .price(Money.of(request.price()))
                .cookTime(request.cookTime())
                .imageUrl(normalizeOptionalText(request.imageUrl()))
                .available(true)
                .itemType(request.itemType() == null ? MenuItemType.SINGLE : request.itemType())
                .comboKind(request.itemType() == MenuItemType.COMBO ? request.comboKind() : null)
                .build();
        item.updateTaxOverride(request.itemTaxMode(), request.itemTaxRateBps());

        MenuItem saved = menuItemRepository.save(item);
        asyncSyncToVectorDb(saved, category.getName());
        return buildManagerDetail(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemManagerDetailResponse updateMenuItem(Long id, UpdateMenuItemRequest request) {
        MenuCategory category = getActiveCategory(request.categoryId());
        MenuItem item = getActiveMenuItem(id);

        item.updateBasics(
                request.categoryId(),
                request.name().trim(),
                normalizeOptionalText(request.description()),
                request.price(),
                request.cookTime(),
                normalizeOptionalText(request.imageUrl())
        );

        item.updateType(request.itemType(), request.comboKind());
        item.updateTaxOverride(request.itemTaxMode(), request.itemTaxRateBps());

        if (request.available() != null) {
            if (Boolean.TRUE.equals(request.available())) {
                item.markAvailable();
            } else {
                item.markUnavailable();
            }
        }

        MenuItem saved = menuItemRepository.save(item);
        asyncSyncToVectorDb(saved, category.getName());
        return buildManagerDetail(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public void deleteMenuItem(Long id) {
        MenuItem item = getActiveMenuItem(id);
        item.softDelete();
        menuItemRepository.save(item);
        asyncDeleteFromVectorDb(id);
    }

    @Override
    public MenuItemManagerDetailResponse getMenuItemManagerDetail(Long id) {
        return buildManagerDetail(getActiveMenuItem(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemManagerDetailResponse upsertFixedComboConfig(Long comboItemId, UpsertFixedComboRequest request) {
        MenuItem combo = getActiveMenuItem(comboItemId);
        ensureComboKind(combo, ComboKind.FIXED);

        comboFixedComponentRepository.deleteAllByComboItemId(comboItemId);
        comboFixedComponentRepository.flush();

        List<ComboFixedComponent> components = request.components().stream()
                .map(componentRequest -> {
                    MenuItem component = getActiveMenuItem(componentRequest.menuItemId());
                    if (component.getItemType() != MenuItemType.SINGLE) {
                        throw new DomainException("Component item must be SINGLE: " + componentRequest.menuItemId());
                    }
                    return ComboFixedComponent.create(comboItemId, componentRequest.menuItemId(), componentRequest.quantity());
                })
                .toList();
        comboFixedComponentRepository.saveAll(components);

        return buildManagerDetail(combo);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemManagerDetailResponse upsertPickComboConfig(Long comboItemId, UpsertPickComboRequest request) {
        MenuItem combo = getActiveMenuItem(comboItemId);
        ensureComboKind(combo, ComboKind.PICK);

        List<ComboPickSlot> existingSlots = comboPickSlotRepository.findAllByComboItemIdOrderByDisplayOrderAscIdAsc(comboItemId);
        if (!existingSlots.isEmpty()) {
            List<Long> slotIds = existingSlots.stream().map(ComboPickSlot::getId).toList();
            comboPickSlotItemRepository.deleteAllBySlotIdIn(slotIds);
            comboPickSlotItemRepository.flush();
            comboPickSlotRepository.deleteAllByComboItemId(comboItemId);
            comboPickSlotRepository.flush();
        }

        List<ComboPickSlot> slots = request.slots().stream()
                .map(slotRequest -> {
                    if (slotRequest.minSelect() > slotRequest.maxSelect()) {
                        throw new DomainException("Slot minSelect must be <= maxSelect");
                    }
                    return ComboPickSlot.create(
                            comboItemId,
                            slotRequest.name().trim(),
                            slotRequest.minSelect(),
                            slotRequest.maxSelect(),
                            slotRequest.displayOrder()
                    );
                })
                .toList();
        List<ComboPickSlot> savedSlots = comboPickSlotRepository.saveAll(slots);

        List<ComboPickSlotItem> itemsToSave = new java.util.ArrayList<>();
        for (int i = 0; i < savedSlots.size(); i++) {
            ComboPickSlot savedSlot = savedSlots.get(i);
            UpsertPickComboRequest.Slot slotRequest = request.slots().get(i);
            for (Long allowedItemId : slotRequest.allowedItemIds()) {
                MenuItem allowed = getActiveMenuItem(allowedItemId);
                if (allowed.getItemType() != MenuItemType.SINGLE) {
                    throw new DomainException("Allowed item must be SINGLE: " + allowedItemId);
                }
                itemsToSave.add(ComboPickSlotItem.create(savedSlot.getId(), allowedItemId));
            }
        }
        comboPickSlotItemRepository.saveAll(itemsToSave);

        return buildManagerDetail(combo);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuCategoryDetailResponse createCategory(CreateMenuCategoryRequest request) {
        MenuCategory category = MenuCategory.builder()
                .name(request.name().trim())
                .description(normalizeOptionalText(request.description()))
                .displayOrder(request.displayOrder())
                .build();
        MenuCategory saved = menuCategoryRepository.save(category);
        return MenuCategoryDetailResponse.from(saved);
    }

    @Override
    public MenuCategoryDetailResponse getCategoryDetail(Long id) {
        return MenuCategoryDetailResponse.from(getActiveCategory(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuCategoryDetailResponse updateCategory(Long id, UpdateMenuCategoryRequest request) {
        MenuCategory category = getActiveCategory(id);
        category.updateDetails(
                request.name().trim(),
                normalizeOptionalText(request.description()),
                request.displayOrder()
        );
        MenuCategory saved = menuCategoryRepository.save(category);
        return MenuCategoryDetailResponse.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public void deleteCategory(Long id) {
        MenuCategory category = getActiveCategory(id);
        category.softDelete();
        menuCategoryRepository.save(category);
    }

    private MenuItem getActiveMenuItem(Long id) {
        return menuItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
    }

    private MenuCategory getActiveCategory(Long id) {
        return menuCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", id));
    }

    private void ensureComboKind(MenuItem combo, ComboKind expected) {
        if (combo.getItemType() != MenuItemType.COMBO) {
            throw new DomainException("Menu item is not a COMBO: " + combo.getId());
        }
        if (combo.getComboKind() != expected) {
            throw new DomainException("Combo kind mismatch. Expected " + expected + " but got " + combo.getComboKind());
        }
    }

    private MenuItemManagerDetailResponse buildManagerDetail(MenuItem item) {
        MenuItemManagerDetailResponse base = MenuItemManagerDetailResponse.fromBase(item);
        if (item.getItemType() != MenuItemType.COMBO || item.getComboKind() == null) {
            return base;
        }

        if (item.getComboKind() == ComboKind.FIXED) {
            List<MenuItemManagerDetailResponse.FixedComponent> components = comboFixedComponentRepository
                    .findAllByComboItemIdOrderByIdAsc(item.getId()).stream()
                    .map(component -> MenuItemManagerDetailResponse.FixedComponent.builder()
                            .menuItemId(component.getComponentItemId())
                            .quantity(component.getQuantity())
                            .build())
                    .toList();
            return MenuItemManagerDetailResponse.builder()
                    .id(base.getId())
                    .categoryId(base.getCategoryId())
                    .name(base.getName())
                    .description(base.getDescription())
                    .price(base.getPrice())
                    .cookTime(base.getCookTime())
                    .imageUrl(base.getImageUrl())
                    .available(base.isAvailable())

                    .createdAt(base.getCreatedAt())
                    .updatedAt(base.getUpdatedAt())
                    .itemType(base.getItemType())
                    .comboKind(base.getComboKind())
                    .fixedCombo(MenuItemManagerDetailResponse.FixedCombo.builder().components(components).build())
                    .build();
        }

        List<ComboPickSlot> slots = comboPickSlotRepository.findAllByComboItemIdOrderByDisplayOrderAscIdAsc(item.getId());
        List<Long> slotIds = slots.stream().map(ComboPickSlot::getId).toList();
        List<ComboPickSlotItem> allowedItems = slotIds.isEmpty() ? List.of() : comboPickSlotItemRepository.findAllBySlotIdIn(slotIds);
        Map<Long, List<Long>> allowedBySlotId = allowedItems.stream()
                .collect(Collectors.groupingBy(
                        ComboPickSlotItem::getSlotId,
                        LinkedHashMap::new,
                        Collectors.mapping(ComboPickSlotItem::getMenuItemId, Collectors.toList())
                ));

        List<MenuItemManagerDetailResponse.PickSlot> slotResponses = slots.stream()
                .map(slot -> MenuItemManagerDetailResponse.PickSlot.builder()
                        .id(slot.getId())
                        .name(slot.getName())
                        .minSelect(slot.getMinSelect())
                        .maxSelect(slot.getMaxSelect())
                        .displayOrder(slot.getDisplayOrder())
                        .allowedItemIds(allowedBySlotId.getOrDefault(slot.getId(), List.of()))
                        .build())
                .toList();

        return MenuItemManagerDetailResponse.builder()
                .id(base.getId())
                .categoryId(base.getCategoryId())
                .name(base.getName())
                .description(base.getDescription())
                .price(base.getPrice())
                .cookTime(base.getCookTime())
                .imageUrl(base.getImageUrl())
                .available(base.isAvailable())

                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .itemType(base.getItemType())
                .comboKind(base.getComboKind())
                .pickCombo(MenuItemManagerDetailResponse.PickCombo.builder().slots(slotResponses).build())
                .build();
    }

    private void deleteObsoleteImage(String previousPublicId, String currentPublicId) {
        if (previousPublicId == null || previousPublicId.isBlank()) {
            return;
        }
        if (previousPublicId.equals(currentPublicId)) {
            return;
        }
        safeDelete(previousPublicId);
    }

    private void safeDelete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            imageStorageService.deleteImage(publicId);
        } catch (DomainException ex) {
            LOGGER.warn("Unable to delete obsolete image asset {}", publicId, ex);
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ComboGenerateRequest normalizeComboGenerateRequest(ComboGenerateRequest request) {
        if (request == null) {
            throw new DomainException("Combo generate request is required");
        }

        if (request.analyzeDays() <= 0) {
            throw new DomainException("analyzeDays must be greater than 0");
        }
        if (request.minSupport() <= 0 || request.minSupport() > 1) {
            throw new DomainException("minSupport must be in range (0, 1]");
        }
        if (request.minConfidence() <= 0 || request.minConfidence() > 1) {
            throw new DomainException("minConfidence must be in range (0, 1]");
        }

        return request;
    }

    private void asyncSyncToVectorDb(MenuItem item, String categoryName) {
        CompletableFuture.runAsync(() -> {
            try {
                List<String> tags = new ArrayList<>();
                tags.add(item.getItemType().name());
                if (item.getComboKind() != null) {
                    tags.add(item.getComboKind().name());
                }
                if (!item.isAvailable()) {
                    tags.add("UNAVAILABLE");
                }
                SyncMenuRequest req = new SyncMenuRequest(
                        item.getId().intValue(),
                        item.getName(),
                        item.getDescription(),
                        item.getPrice() != null ? item.getPrice().toBigDecimal() : BigDecimal.ZERO,
                        categoryName,
                        tags
                );
                aiClient.post("/ai/sync-menu", req, SyncMenuResponse.class, AiOperation.SYNC_MENU);
            } catch (Exception ex) {
                LOGGER.warn("AI sync-menu failed for item {}: {}", item.getId(), ex.getMessage());
            }
        }, aiTaskExecutor);
    }

    private void asyncDeleteFromVectorDb(Long itemId) {
        CompletableFuture.runAsync(() -> {
            try {
                aiClient.delete("/ai/sync-menu/" + itemId, SyncMenuResponse.class, AiOperation.DELETE_MENU);
            } catch (Exception ex) {
                LOGGER.warn("AI delete-menu failed for item {}: {}", itemId, ex.getMessage());
            }
        }, aiTaskExecutor);
    }

    // ========================== Recipe (Định lượng) ==========================

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public List<RecipeItemResponse> upsertRecipe(Long menuItemId, UpsertRecipeRequest request) {
        getActiveMenuItem(menuItemId);

        menuItemIngredientRepository.deleteAllByMenuItemId(menuItemId);
        menuItemIngredientRepository.flush();

        List<MenuItemIngredient> entries = request.items().stream()
                .map(item -> {
                    inventoryService.validateIngredientExists(item.ingredientId());
                    return MenuItemIngredient.create(menuItemId, item.ingredientId(), item.quantity());
                })
                .toList();
        menuItemIngredientRepository.saveAll(entries);

        return getRecipe(menuItemId);
    }

    @Override
    public List<RecipeItemResponse> getRecipe(Long menuItemId) {
        getActiveMenuItem(menuItemId);

        List<MenuItemIngredient> entries = menuItemIngredientRepository.findAllByMenuItemId(menuItemId);
        if (entries.isEmpty()) {
            return List.of();
        }

        List<Long> ingredientIds = entries.stream().map(MenuItemIngredient::getIngredientId).toList();
        Map<Long, IngredientData> ingredientMap = inventoryService.getIngredientsData(ingredientIds).stream()
                .collect(Collectors.toMap(IngredientData::id, Function.identity()));

        return entries.stream()
                .map(entry -> {
                    IngredientData ingredient = ingredientMap.get(entry.getIngredientId());
                    return RecipeItemResponse.builder()
                            .ingredientId(entry.getIngredientId())
                            .ingredientName(ingredient != null ? ingredient.name() : null)
                            .unit(ingredient != null ? ingredient.unit() : null)
                            .quantity(entry.getQuantity())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public void deleteRecipe(Long menuItemId) {
        getActiveMenuItem(menuItemId);
        menuItemIngredientRepository.deleteAllByMenuItemId(menuItemId);
    }

    @Override
    public ComboGenerateResponse generateComboSuggestions(ComboGenerateRequest request) {
        ComboGenerateRequest safeRequest = normalizeComboGenerateRequest(request);
        return aiClient.post("/ai/combo-generate", safeRequest, ComboGenerateResponse.class, AiOperation.COMBO_GENERATE)
                .orElseGet(() -> new ComboGenerateResponse(false, List.of()));
    }

    // ========================== Ingredient Availability ==========================

    @Override
    public MenuItemAvailability checkIngredientAvailability(Long menuItemId, int quantity) {
        List<MenuItemIngredient> recipe = menuItemIngredientRepository.findAllByMenuItemId(menuItemId);

        if (recipe.isEmpty()) {
            return MenuItemAvailability.available(menuItemId);
        }

        List<Long> ingredientIds = recipe.stream().map(MenuItemIngredient::getIngredientId).toList();
        Map<Long, IngredientData> ingredientMap = inventoryService.getIngredientsData(ingredientIds).stream()
                .collect(Collectors.toMap(IngredientData::id, Function.identity()));

        List<String> shortages = new ArrayList<>();
        for (MenuItemIngredient r : recipe) {
            IngredientData ing = ingredientMap.get(r.getIngredientId());
            if (ing == null) {
                shortages.add("Unknown ingredient #" + r.getIngredientId());
                continue;
            }
            BigDecimal needed = r.getQuantity().multiply(BigDecimal.valueOf(quantity));
            if (ing.currentQty().compareTo(needed) < 0) {
                shortages.add(ing.name());
            }
        }

        if (shortages.isEmpty()) {
            return MenuItemAvailability.available(menuItemId);
        }
        return MenuItemAvailability.insufficient(menuItemId, shortages);
    }

    // ========================== Cook Time Suggestion ==========================

    @Override
    public CookTimeSuggestionResponse getSuggestedCookTime(Long menuItemId) {
        MenuItem menuItem = getActiveMenuItem(menuItemId);

        List<KitchenTaskCookData> recentTasks = kitchenService.getRecentCompletedTasksForMenuItem(menuItemId, 10);

        List<Integer> actualSeconds = recentTasks.stream()
                .map(KitchenTaskCookData::actualCookSeconds)
                .filter(seconds -> seconds != null && seconds > 0)
                .sorted()
                .toList();

        if (actualSeconds.isEmpty()) {
            return new CookTimeSuggestionResponse(
                    menuItemId,
                    menuItem.getCookTime(),
                    null,
                    0,
                    java.time.Instant.now()
            );
        }

        // Remove outliers if size >= 5 (remove top 1 and bottom 1)
        List<Integer> filteredSeconds = actualSeconds;
        if (actualSeconds.size() >= 5) {
            filteredSeconds = actualSeconds.subList(1, actualSeconds.size() - 1);
        }

        double avgSeconds = filteredSeconds.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int suggestedMinutes = Math.max(1, (int) Math.round(avgSeconds / 60.0));

        return new CookTimeSuggestionResponse(
                menuItemId,
                menuItem.getCookTime(),
                suggestedMinutes,
                actualSeconds.size(),
                java.time.Instant.now()
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public void updateCookTime(Long menuItemId, int newCookTimeMinutes) {
        if (newCookTimeMinutes <= 0) {
            throw new DomainException("Cook time must be positive");
        }
        MenuItem menuItem = getActiveMenuItem(menuItemId);
        menuItem.updateCookTime(newCookTimeMinutes);
        menuItemRepository.save(menuItem);
    }
}
