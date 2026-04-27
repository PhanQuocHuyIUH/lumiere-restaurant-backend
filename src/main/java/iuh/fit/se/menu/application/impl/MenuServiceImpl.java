package iuh.fit.se.menu.application.impl;

import iuh.fit.se.inventory.domain.Ingredient;
import iuh.fit.se.inventory.infrastructure.IngredientRepository;
import iuh.fit.se.menu.api.dto.CustomerMenuCategoryResponse;
import iuh.fit.se.menu.api.dto.CustomerMenuItemResponse;
import iuh.fit.se.menu.api.dto.MenuCategoryResponse;
import iuh.fit.se.menu.api.dto.MenuItemResponse;
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
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.storage.ImageStorageService;
import iuh.fit.se.shared.storage.StoredImage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    public MenuServiceImpl(
            MenuCategoryRepository menuCategoryRepository,
            MenuItemRepository menuItemRepository,
            ComboFixedComponentRepository comboFixedComponentRepository,
            ComboPickSlotRepository comboPickSlotRepository,
            ComboPickSlotItemRepository comboPickSlotItemRepository,
            ImageStorageService imageStorageService,
            MenuItemIngredientRepository menuItemIngredientRepository,
            IngredientRepository ingredientRepository
    ) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.comboFixedComponentRepository = comboFixedComponentRepository;
        this.comboPickSlotRepository = comboPickSlotRepository;
        this.comboPickSlotItemRepository = comboPickSlotItemRepository;
        this.imageStorageService = imageStorageService;
        this.menuItemIngredientRepository = menuItemIngredientRepository;
        this.ingredientRepository = ingredientRepository;
    }

    @Override
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
    public MenuItemDTO getItem(Long id) {
        return MenuItemDTO.from(getActiveMenuItem(id));
    }

    @Override
    @Transactional
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
    public MenuItemAdminDetailResponse createMenuItem(CreateMenuItemRequest request) {
        getActiveCategory(request.categoryId());

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
        return buildAdminDetail(saved);
    }

    @Override
    @Transactional
    public MenuItemAdminDetailResponse updateMenuItem(Long id, UpdateMenuItemRequest request) {
        getActiveCategory(request.categoryId());
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
        return buildAdminDetail(saved);
    }

    @Override
    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem item = getActiveMenuItem(id);
        item.softDelete();
        menuItemRepository.save(item);
    }

    @Override
    public MenuItemAdminDetailResponse getMenuItemAdminDetail(Long id) {
        return buildAdminDetail(getActiveMenuItem(id));
    }

    @Override
    @Transactional
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

    // ========================== Recipe (Định lượng) ==========================

    @Override
    @Transactional
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
    public void deleteRecipe(Long menuItemId) {
        getActiveMenuItem(menuItemId);
        menuItemIngredientRepository.deleteAllByMenuItemId(menuItemId);
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
}
