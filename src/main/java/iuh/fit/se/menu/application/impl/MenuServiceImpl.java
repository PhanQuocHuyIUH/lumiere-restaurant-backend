package iuh.fit.se.menu.application.impl;

import iuh.fit.se.inventory.domain.Ingredient;
import iuh.fit.se.inventory.infrastructure.IngredientRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenTaskRepository;
import iuh.fit.se.menu.api.dto.CustomerMenuCategoryResponse;
import iuh.fit.se.menu.api.dto.CustomerMenuItemResponse;
import iuh.fit.se.menu.api.dto.MenuCategorySummaryResponse;
import iuh.fit.se.menu.api.dto.MenuCategoryResponse;
import iuh.fit.se.menu.api.dto.MenuItemResponse;
import iuh.fit.se.menu.api.dto.admin.AdminMenuCategoryListItemResponse;
import iuh.fit.se.menu.api.dto.admin.CreateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.admin.CreateMenuItemRequest;
import iuh.fit.se.menu.api.dto.admin.MenuCategoryDetailResponse;
import iuh.fit.se.menu.api.dto.admin.MenuItemAdminDetailResponse;
import iuh.fit.se.menu.api.dto.admin.RecipeItemResponse;
import iuh.fit.se.menu.api.dto.admin.UpdateMenuCategoryRequest;
import iuh.fit.se.menu.api.dto.admin.UpdateMenuItemRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertFixedComboRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertPickComboRequest;
import iuh.fit.se.menu.api.dto.admin.UpsertRecipeRequest;
import iuh.fit.se.menu.application.MenuItemAvailabilityDTO;
import iuh.fit.se.menu.application.MenuItemDTO;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.menu.domain.ComboFixedComponent;
import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.ComboPickSlot;
import iuh.fit.se.menu.domain.ComboPickSlotItem;
import iuh.fit.se.menu.domain.MenuCategory;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemIngredient;
import iuh.fit.se.menu.domain.MenuItemType;
import iuh.fit.se.menu.infrastructure.ComboFixedComponentRepository;
import iuh.fit.se.menu.infrastructure.ComboPickSlotItemRepository;
import iuh.fit.se.menu.infrastructure.ComboPickSlotRepository;
import iuh.fit.se.menu.infrastructure.MenuCategoryRepository;
import iuh.fit.se.menu.infrastructure.MenuItemIngredientRepository;
import iuh.fit.se.menu.infrastructure.MenuItemRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
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
    private final IngredientRepository ingredientRepository;
    private final AiClient aiClient;
    private final TaskExecutor aiTaskExecutor;
    private final KitchenTaskRepository kitchenTaskRepository;

    public MenuServiceImpl(
            MenuCategoryRepository menuCategoryRepository,
            MenuItemRepository menuItemRepository,
            ComboFixedComponentRepository comboFixedComponentRepository,
            ComboPickSlotRepository comboPickSlotRepository,
            ComboPickSlotItemRepository comboPickSlotItemRepository,
            ImageStorageService imageStorageService,
            MenuItemIngredientRepository menuItemIngredientRepository,
            IngredientRepository ingredientRepository,
            AiClient aiClient,
            @Qualifier("aiTaskExecutor") TaskExecutor aiTaskExecutor,
            KitchenTaskRepository kitchenTaskRepository
    ) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.comboFixedComponentRepository = comboFixedComponentRepository;
        this.comboPickSlotRepository = comboPickSlotRepository;
        this.comboPickSlotItemRepository = comboPickSlotItemRepository;
        this.imageStorageService = imageStorageService;
        this.menuItemIngredientRepository = menuItemIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.aiClient = aiClient;
        this.aiTaskExecutor = aiTaskExecutor;
        this.kitchenTaskRepository = kitchenTaskRepository;
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
    public List<AdminMenuCategoryListItemResponse> getAllCategoriesForAdmin() {
        List<MenuCategory> categories =
                menuCategoryRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc();
        return categories.stream()
                .map(category -> AdminMenuCategoryListItemResponse.from(
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
    public MenuItemDTO getItem(Long id) {
        return MenuItemDTO.from(getActiveMenuItem(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemDTO updateMenuItemImage(Long id, MultipartFile file) {
        MenuItem menuItem = getActiveMenuItem(id);
        String previousPublicId = menuItem.getImagePublicId();

        StoredImage uploadedImage = imageStorageService.uploadMenuItemImage(menuItem.getId(), file);
        try {
            menuItem.updateImage(uploadedImage.url(), uploadedImage.publicId());
            MenuItem savedMenuItem = menuItemRepository.save(menuItem);
            deleteObsoleteImage(previousPublicId, uploadedImage.publicId());
            return MenuItemDTO.from(savedMenuItem);
        } catch (RuntimeException ex) {
            safeDelete(uploadedImage.publicId());
            throw ex;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemAdminDetailResponse createMenuItem(CreateMenuItemRequest request) {
        MenuCategory category = getActiveCategory(request.categoryId());

        MenuItem item = MenuItem.builder()
                .categoryId(request.categoryId())
                .name(request.name().trim())
                .description(normalizeOptionalText(request.description()))
                .price(request.price())
                .cookTime(request.cookTime())
                .imageUrl(normalizeOptionalText(request.imageUrl()))

                .available(true)
                .itemType(request.itemType() == null ? MenuItemType.SINGLE : request.itemType())
                .comboKind(request.itemType() == MenuItemType.COMBO ? request.comboKind() : null)
                .build();

        MenuItem saved = menuItemRepository.save(item);
        asyncSyncToVectorDb(saved, category.getName());
        return buildAdminDetail(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemAdminDetailResponse updateMenuItem(Long id, UpdateMenuItemRequest request) {
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

        if (request.available() != null) {
            if (Boolean.TRUE.equals(request.available())) {
                item.markAvailable();
            } else {
                item.markUnavailable();
            }
        }

        MenuItem saved = menuItemRepository.save(item);
        asyncSyncToVectorDb(saved, category.getName());
        return buildAdminDetail(saved);
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
    public MenuItemAdminDetailResponse getMenuItemAdminDetail(Long id) {
        return buildAdminDetail(getActiveMenuItem(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemAdminDetailResponse upsertFixedComboConfig(Long comboItemId, UpsertFixedComboRequest request) {
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

        return buildAdminDetail(combo);
    }

    @Override
    @Transactional
    @CacheEvict(value = "menu", allEntries = true)
    public MenuItemAdminDetailResponse upsertPickComboConfig(Long comboItemId, UpsertPickComboRequest request) {
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

        return buildAdminDetail(combo);
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

    private MenuItemAdminDetailResponse buildAdminDetail(MenuItem item) {
        MenuItemAdminDetailResponse base = MenuItemAdminDetailResponse.fromBase(item);
        if (item.getItemType() != MenuItemType.COMBO || item.getComboKind() == null) {
            return base;
        }

        if (item.getComboKind() == ComboKind.FIXED) {
            List<MenuItemAdminDetailResponse.FixedComponent> components = comboFixedComponentRepository
                    .findAllByComboItemIdOrderByIdAsc(item.getId()).stream()
                    .map(component -> MenuItemAdminDetailResponse.FixedComponent.builder()
                            .menuItemId(component.getComponentItemId())
                            .quantity(component.getQuantity())
                            .build())
                    .toList();
            return MenuItemAdminDetailResponse.builder()
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
                    .fixedCombo(MenuItemAdminDetailResponse.FixedCombo.builder().components(components).build())
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

        List<MenuItemAdminDetailResponse.PickSlot> slotResponses = slots.stream()
                .map(slot -> MenuItemAdminDetailResponse.PickSlot.builder()
                        .id(slot.getId())
                        .name(slot.getName())
                        .minSelect(slot.getMinSelect())
                        .maxSelect(slot.getMaxSelect())
                        .displayOrder(slot.getDisplayOrder())
                        .allowedItemIds(allowedBySlotId.getOrDefault(slot.getId(), List.of()))
                        .build())
                .toList();

        return MenuItemAdminDetailResponse.builder()
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
                .pickCombo(MenuItemAdminDetailResponse.PickCombo.builder().slots(slotResponses).build())
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
                        item.getPrice() != null ? item.getPrice().doubleValue() : 0.0,
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
                    ingredientRepository.findByIdAndDeletedAtIsNull(item.ingredientId())
                            .orElseThrow(() -> new ResourceNotFoundException("Ingredient", item.ingredientId()));
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
        Map<Long, Ingredient> ingredientMap = ingredientRepository.findAllById(ingredientIds).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        return entries.stream()
                .map(entry -> {
                    Ingredient ingredient = ingredientMap.get(entry.getIngredientId());
                    return RecipeItemResponse.builder()
                            .ingredientId(entry.getIngredientId())
                            .ingredientName(ingredient != null ? ingredient.getName() : null)
                            .unit(ingredient != null ? ingredient.getUnit() : null)
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
    public MenuItemAvailabilityDTO checkIngredientAvailability(Long menuItemId, int quantity) {
        List<MenuItemIngredient> recipe = menuItemIngredientRepository.findAllByMenuItemId(menuItemId);

        if (recipe.isEmpty()) {
            return MenuItemAvailabilityDTO.available(menuItemId);
        }

        List<Long> ingredientIds = recipe.stream().map(MenuItemIngredient::getIngredientId).toList();
        Map<Long, Ingredient> ingredientMap = ingredientRepository.findAllById(ingredientIds).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        List<String> shortages = new ArrayList<>();
        for (MenuItemIngredient r : recipe) {
            Ingredient ing = ingredientMap.get(r.getIngredientId());
            if (ing == null || ing.getDeletedAt() != null) {
                shortages.add("Unknown ingredient #" + r.getIngredientId());
                continue;
            }
            BigDecimal needed = r.getQuantity().multiply(BigDecimal.valueOf(quantity));
            if (ing.getCurrentQty().compareTo(needed) < 0) {
                shortages.add(ing.getName());
            }
        }

        if (shortages.isEmpty()) {
            return MenuItemAvailabilityDTO.available(menuItemId);
        }
        return MenuItemAvailabilityDTO.insufficient(menuItemId, shortages);
    }

    // ========================== Cook Time Suggestion ==========================

    @Override
    public iuh.fit.se.menu.api.dto.admin.CookTimeSuggestionResponse getSuggestedCookTime(Long menuItemId) {
        MenuItem menuItem = getActiveMenuItem(menuItemId);

        List<iuh.fit.se.kitchen.domain.KitchenTask> recentTasks = kitchenTaskRepository
                .findTop10ByMenuItemIdAndStatusOrderByCompletedAtDesc(menuItemId, iuh.fit.se.kitchen.domain.KitchenTaskStatus.DONE);

        List<Integer> actualSeconds = recentTasks.stream()
                .map(iuh.fit.se.kitchen.domain.KitchenTask::getActualCookSeconds)
                .filter(seconds -> seconds != null && seconds > 0)
                .sorted()
                .toList();

        if (actualSeconds.isEmpty()) {
            return new iuh.fit.se.menu.api.dto.admin.CookTimeSuggestionResponse(
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

        return new iuh.fit.se.menu.api.dto.admin.CookTimeSuggestionResponse(
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
