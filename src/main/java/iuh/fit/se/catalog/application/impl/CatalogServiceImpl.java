package iuh.fit.se.catalog.application.impl;

import iuh.fit.se.catalog.api.dto.MenuCategoryResponse;
import iuh.fit.se.catalog.api.dto.MenuItemResponse;
import iuh.fit.se.catalog.application.CatalogService;
import iuh.fit.se.catalog.application.MenuItemDTO;
import iuh.fit.se.catalog.application.QrSessionTokenDTO;
import iuh.fit.se.catalog.application.TableDTO;
import iuh.fit.se.catalog.application.TableQrCodeDTO;
import iuh.fit.se.catalog.domain.QrSession;
import iuh.fit.se.catalog.domain.QrSessionStatus;
import iuh.fit.se.catalog.domain.MenuCategory;
import iuh.fit.se.catalog.domain.MenuItem;
import iuh.fit.se.catalog.domain.RestaurantTable;
import iuh.fit.se.catalog.domain.TableQrCode;
import iuh.fit.se.catalog.domain.TableQrCodeStatus;
import iuh.fit.se.catalog.infrastructure.QrSessionRepository;
import iuh.fit.se.catalog.infrastructure.MenuCategoryRepository;
import iuh.fit.se.catalog.infrastructure.MenuItemRepository;
import iuh.fit.se.catalog.infrastructure.RestaurantTableRepository;
import iuh.fit.se.catalog.infrastructure.TableQrCodeRepository;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private static final Pattern TABLE_CODE_PATTERN = Pattern.compile("^(\\d+)-(\\d{1,3})$");
    private static final Pattern QR_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9]{16,120}$");

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final TableQrCodeRepository tableQrCodeRepository;
    private final QrSessionRepository qrSessionRepository;

    @Value("${app.qr.base-url:http://localhost:8080/menu/tables/qr}")
    private String qrBaseUrl;

    @Value("${app.qr.session-expiration:1800000}")
    private long qrSessionExpirationMs;

    public CatalogServiceImpl(
            MenuCategoryRepository menuCategoryRepository,
            MenuItemRepository menuItemRepository,
            RestaurantTableRepository restaurantTableRepository,
            TableQrCodeRepository tableQrCodeRepository,
            QrSessionRepository qrSessionRepository
    ) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.tableQrCodeRepository = tableQrCodeRepository;
        this.qrSessionRepository = qrSessionRepository;
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
    public MenuItemDTO getItem(Long id) {
        return MenuItemDTO.from(getActiveMenuItem(id));
    }

    @Override
    public TableDTO getTableByCode(String tableCode) {
        return TableDTO.from(getActiveTable(tableCode));
    }

    @Override
    public TableDTO getTableById(Long tableId) {
        return TableDTO.from(getActiveTable(tableId));
    }

    @Override
    public TableDTO getTableByQrKey(String qrKey) {
        return TableDTO.from(getActiveTableByQrKey(qrKey));
    }

    @Override
    public boolean isItemAvailable(Long id) {
        return getActiveMenuItem(id).isAvailable();
    }

    @Override
    @Transactional
    public QrSessionTokenDTO issueQrSession(String tableCode) {
        RestaurantTable table = getActiveTable(tableCode);
        TableQrCode qrCode = getOrCreateQrCode(table);

        if (!qrCode.isActive()) {
            throw new DomainException("QR code is not active for table: " + table.getTableCode());
        }

        Instant now = Instant.now();
        Optional<QrSession> activeSessionOpt = qrSessionRepository.findTopByTableIdAndStatusOrderByIssuedAtDesc(
            table.getId(),
            QrSessionStatus.ACTIVE
        );

        QrSession session;
        if (activeSessionOpt.isPresent() && activeSessionOpt.get().isActive(now)) {
            session = activeSessionOpt.get();
            session.touchAccess();
            qrSessionRepository.save(session);
        } else {
            if (activeSessionOpt.isPresent()) {
            QrSession staleSession = activeSessionOpt.get();
            staleSession.markExpired();
            qrSessionRepository.save(staleSession);
            }

            String sessionId = UUID.randomUUID().toString();
            Instant expiresAt = now.plusMillis(qrSessionExpirationMs);

            session = QrSession.issue(
                sessionId,
                qrCode.getId(),
                table.getId(),
                expiresAt,
                "customer"
            );
            session = qrSessionRepository.save(session);
        }

        qrCode.markIssuedSession();
        tableQrCodeRepository.save(qrCode);

        return new QrSessionTokenDTO(session.getSessionId(), session.getExpiresAt());
    }

    @Override
    @Transactional
    public void validateQrSession(String sessionId, String tableCode) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        RestaurantTable table = getActiveTable(tableCode);

        QrSession session = qrSessionRepository.findBySessionId(normalizedSessionId)
                .orElseThrow(() -> new DomainException("Invalid QR session"));

        if (!session.getTableId().equals(table.getId())) {
            throw new DomainException("QR session does not belong to table: " + tableCode);
        }

        Instant now = Instant.now();
        if (!session.isActive(now)) {
            if (session.getStatus() == QrSessionStatus.ACTIVE) {
                session.markExpired();
                qrSessionRepository.save(session);
            }
            throw new DomainException("QR session expired or revoked");
        }

        session.touchAccess();
        qrSessionRepository.save(session);
    }

    @Override
    @Transactional
    public TableQrCodeDTO getOrCreateTableQrCode(String tableCode) {
        RestaurantTable table = getActiveTable(tableCode);
        TableQrCode qrCode = getOrCreateQrCode(table);
        return toTableQrCodeDto(table, qrCode);
    }

    @Override
    @Transactional
    public TableQrCodeDTO rotateTableQrCode(String tableCode) {
        RestaurantTable table = getActiveTable(tableCode);
        TableQrCode qrCode = tableQrCodeRepository.findByTableId(table.getId())
                .orElseGet(() -> TableQrCode.create(table.getId(), generateQrKey(), Instant.now().plus(Duration.ofDays(90))));

        qrCode.rotateTo(generateQrKey(), Instant.now().plus(Duration.ofDays(90)));
        TableQrCode savedQrCode = tableQrCodeRepository.save(qrCode);
        return toTableQrCodeDto(table, savedQrCode);
    }

    private MenuItem getActiveMenuItem(Long id) {
        return menuItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
    }

    private RestaurantTable getActiveTableByQrKey(String qrKey) {
        String normalizedQrKey = normalizeQrKey(qrKey);

        TableQrCode qrCode = tableQrCodeRepository.findByQrKeyAndStatus(normalizedQrKey, TableQrCodeStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("TableQrCode", normalizedQrKey));

        return getActiveTable(qrCode.getTableId());
    }

    private RestaurantTable getActiveTable(String tableCode) {
        String normalizedCode = normalizeTableCode(tableCode);
        return restaurantTableRepository.findByTableCodeAndDeletedAtIsNull(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", normalizedCode));
    }

    private RestaurantTable getActiveTable(Long tableId) {
        return restaurantTableRepository.findByIdAndDeletedAtIsNull(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", tableId));
    }

    private TableQrCode getOrCreateQrCode(RestaurantTable table) {
        return tableQrCodeRepository.findByTableId(table.getId())
                .orElseGet(() -> tableQrCodeRepository.save(
                        TableQrCode.create(table.getId(), generateQrKey(), Instant.now().plus(Duration.ofDays(90)))
                ));
    }

    private TableQrCodeDTO toTableQrCodeDto(RestaurantTable table, TableQrCode qrCode) {
        String qrUrl = buildQrUrl(qrCode.getQrKey());
        return TableQrCodeDTO.from(qrCode, table.getTableCode(), qrUrl);
    }

    private String buildQrUrl(String qrKey) {
        String normalizedBaseUrl = qrBaseUrl.endsWith("/")
                ? qrBaseUrl.substring(0, qrBaseUrl.length() - 1)
                : qrBaseUrl;
        return normalizedBaseUrl + "/" + qrKey;
    }

    private String normalizeQrKey(String qrKey) {
        if (qrKey == null || qrKey.isBlank()) {
            throw new DomainException("QR key is required");
        }

        String normalized = qrKey.trim();
        if (!QR_KEY_PATTERN.matcher(normalized).matches()) {
            throw new DomainException("Invalid QR key format");
        }

        return normalized;
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new DomainException("QR session id is required");
        }

        return sessionId.trim();
    }

    private String generateQrKey() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeTableCode(String tableCode) {
        if (tableCode == null || tableCode.isBlank()) {
            throw new DomainException("Table code is required");
        }

        Matcher matcher = TABLE_CODE_PATTERN.matcher(tableCode.trim());
        if (!matcher.matches()) {
            throw new DomainException("Invalid table code format. Expected format: floor-number, e.g. 1-001");
        }

        int floor = Integer.parseInt(matcher.group(1));
        int tableNo = Integer.parseInt(matcher.group(2));
        return "%d-%03d".formatted(floor, tableNo);
    }
}