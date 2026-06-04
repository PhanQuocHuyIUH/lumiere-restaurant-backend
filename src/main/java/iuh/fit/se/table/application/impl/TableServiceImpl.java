package iuh.fit.se.table.application.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderRevision;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.ordering.repository.OrderItemRepository;
import iuh.fit.se.ordering.repository.OrderRepository;
import iuh.fit.se.ordering.repository.OrderRevisionRepository;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.storage.ImageStorageService;
import iuh.fit.se.shared.storage.StoredImage;
import iuh.fit.se.table.application.QrSessionToken;
import iuh.fit.se.table.application.TableData;
import iuh.fit.se.table.application.TableGroupData;
import iuh.fit.se.table.application.TableQrCodeData;
import iuh.fit.se.table.application.TableService;
import iuh.fit.se.table.domain.TableGroup;
import iuh.fit.se.table.domain.TableGroupStatus;
import iuh.fit.se.table.repository.TableGroupRepository;
import iuh.fit.se.table.domain.QrSession;
import iuh.fit.se.table.domain.QrSessionStatus;
import iuh.fit.se.table.domain.RestaurantTable;
import iuh.fit.se.table.domain.TableQrCode;
import iuh.fit.se.table.domain.TableQrCodeStatus;
import iuh.fit.se.table.domain.TableStatus;
import iuh.fit.se.table.repository.QrSessionRepository;
import iuh.fit.se.table.repository.RestaurantTableRepository;
import iuh.fit.se.table.repository.TableQrCodeRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TableServiceImpl implements TableService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableServiceImpl.class);
    private static final Pattern TABLE_CODE_PATTERN = Pattern.compile("^(\\d+)-(\\d{1,3})$");
    private static final Pattern QR_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9]{16,120}$");
    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.CREATED,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.SERVED
    );
    private static final int QR_IMAGE_SIZE = 512;
    private static final int QR_IMAGE_MARGIN = 1;

    private final RestaurantTableRepository restaurantTableRepository;
    private final TableQrCodeRepository tableQrCodeRepository;
    private final QrSessionRepository qrSessionRepository;
    private final OrderRepository orderRepository;
    private final OrderRevisionRepository orderRevisionRepository;
    private final OrderItemRepository orderItemRepository;
    private final ImageStorageService imageStorageService;
    private final QrSessionCacheService qrSessionCacheService;
    private final OrderingService orderingService;
    private final KitchenService kitchenService;
    private final TableGroupRepository tableGroupRepository;

    @Value("${app.qr.base-url:http://localhost:8080/tables/qr}")
    private String qrBaseUrl;

    @Value("${app.qr.session-expiration:1800000}")
    private long qrSessionExpirationMs;

    @Value("${app.table.cleaning-timeout-minutes:30}")
    private long cleaningTimeoutMinutes;

    public TableServiceImpl(
            RestaurantTableRepository restaurantTableRepository,
            TableQrCodeRepository tableQrCodeRepository,
            QrSessionRepository qrSessionRepository,
            OrderRepository orderRepository,
            OrderRevisionRepository orderRevisionRepository,
            OrderItemRepository orderItemRepository,
            ImageStorageService imageStorageService,
            QrSessionCacheService qrSessionCacheService,
            @Lazy OrderingService orderingService,
            @Lazy KitchenService kitchenService,
            TableGroupRepository tableGroupRepository
    ) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.tableQrCodeRepository = tableQrCodeRepository;
        this.qrSessionRepository = qrSessionRepository;
        this.orderRepository = orderRepository;
        this.orderRevisionRepository = orderRevisionRepository;
        this.orderItemRepository = orderItemRepository;
        this.imageStorageService = imageStorageService;
        this.qrSessionCacheService = qrSessionCacheService;
        this.orderingService = orderingService;
        this.kitchenService = kitchenService;
        this.tableGroupRepository = tableGroupRepository;
    }

    @Override
    @Cacheable(value = "tables", key = "'all'")
    public List<TableData> getAllTables() {
        return restaurantTableRepository.findAllByDeletedAtIsNullOrderByFloorAscTableNoAsc()
                .stream()
                .map(TableData::from)
                .toList();
    }

    @Override
    public TableData getTableByCode(String tableCode) {
        return TableData.from(getActiveTable(tableCode));
    }

    @Override
    public TableData getTableById(Long tableId) {
        return TableData.from(getActiveTable(tableId));
    }

    @Override
    public TableData getTableByQrKey(String qrKey) {
        return TableData.from(getActiveTableByQrKey(qrKey));
    }

    @Override
    public OrderResponse getCurrentOrderByTableCode(String tableCode) {
        return findCurrentOrderByTableCode(tableCode)
                .orElseThrow(() -> new ResourceNotFoundException("Active order", "tableCode=" + tableCode));
    }

    @Override
    public Optional<OrderResponse> findCurrentOrderByTableCode(String tableCode) {
        RestaurantTable table = getActiveTable(tableCode);
        return orderRepository
                .findTopByTableIdAndStatusInOrderByCreatedAtDesc(table.getId(), ACTIVE_ORDER_STATUSES)
                .map(order -> {
                    OrderRevision latestRevision = orderRevisionRepository
                            .findTopByOrderIdOrderByRevisionNumberDesc(order.getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "OrderRevision", "orderId=" + order.getId()));
                    List<OrderItem> latestItems = orderItemRepository
                            .findAllByRevisionIdOrderByIdAsc(latestRevision.getId());
                    return OrderResponse.from(order, latestRevision.getRevisionNumber(), latestItems);
                });
    }

    @Override
    @Transactional
    public QrSessionToken issueQrSession(String tableCode) {
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
            try { qrSessionCacheService.save(session); } catch (Exception ex) { /* best-effort */ }
        } else {
            if (activeSessionOpt.isPresent()) {
                QrSession staleSession = activeSessionOpt.get();
                staleSession.markExpired();
                qrSessionRepository.save(staleSession);
                try { qrSessionCacheService.delete(staleSession.getSessionId()); } catch (Exception ex) { }
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
            try { qrSessionCacheService.save(session); } catch (Exception ex) { }
        }

        qrCode.markIssuedSession();
        tableQrCodeRepository.save(qrCode);

        return new QrSessionToken(session.getSessionId(), session.getExpiresAt());
    }

    @Override
    @Transactional
    public void validateQrSession(String sessionId, String tableCode) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        RestaurantTable table = getActiveTable(tableCode);

        // Try cache first
        QrSession session = qrSessionCacheService.get(normalizedSessionId)
                .orElseGet(() -> qrSessionRepository.findBySessionId(normalizedSessionId)
                        .orElseThrow(() -> new DomainException("Invalid QR session")));

        if (!session.getTableId().equals(table.getId())) {
            throw new DomainException("QR session does not belong to table: " + tableCode);
        }

        Instant now = Instant.now();
        if (!session.isActive(now)) {
            if (session.getStatus() == QrSessionStatus.ACTIVE) {
                session.markExpired();
                qrSessionRepository.save(session);
                try { qrSessionCacheService.delete(session.getSessionId()); } catch (Exception ex) { }
            }
            throw new DomainException("QR session expired or revoked");
        }

        session.touchAccess();
        qrSessionRepository.save(session);
        try { qrSessionCacheService.save(session); } catch (Exception ex) { }
    }

    @Override
    @Transactional
    public TableQrCodeData getOrCreateTableQrCode(String tableCode) {
        RestaurantTable table = getActiveTable(tableCode);
        TableQrCode qrCode = getOrCreateQrCode(table);
        if (qrCode.getQrImageUrl() == null || qrCode.getQrImageUrl().isBlank()) {
            qrCode = uploadAndPersistQrImage(table, qrCode);
        }
        return toTableQrCodeDto(table, qrCode);
    }

    @Override
    @Transactional
    public TableQrCodeData rotateTableQrCode(String tableCode) {
        RestaurantTable table = getActiveTable(tableCode);
        TableQrCode qrCode = tableQrCodeRepository.findByTableId(table.getId())
                .orElseGet(() -> TableQrCode.create(table.getId(), generateQrKey(), Instant.now().plus(Duration.ofDays(90))));

        String previousPublicId = qrCode.getQrImagePublicId();
        qrCode.rotateTo(generateQrKey(), Instant.now().plus(Duration.ofDays(90)));
        TableQrCode savedQrCode = uploadAndPersistQrImage(table, qrCode);
        deleteObsoleteImage(previousPublicId, savedQrCode.getQrImagePublicId());
        return toTableQrCodeDto(table, savedQrCode);
    }

    @Override
    @Transactional
    public void markTableOccupied(Long tableId) {
        RestaurantTable table = getActiveTable(tableId);
        if (table.getStatus() == TableStatus.OCCUPIED) {
            return;
        }

        table.occupy();
        restaurantTableRepository.save(table);
    }

    @Override
    @Transactional
    public void markTableAvailable(Long tableId) {
        RestaurantTable table = getActiveTable(tableId);
        if (table.getStatus() == TableStatus.AVAILABLE) {
            return;
        }

        table.markAvailable();
        restaurantTableRepository.save(table);
        // Customer may have left without paying (cashier voided the order); make
        // sure their device cannot reuse the QR session against the next guest.
        revokeActiveSessionsForTable(tableId);
    }

    @Override
    @Transactional
    public void markTableCleaning(Long tableId) {
        RestaurantTable table = getActiveTable(tableId);
        if (table.getStatus() == TableStatus.CLEANING) {
            return;
        }

        table.markCleaning();
        restaurantTableRepository.save(table);
        // Payment just completed — kill the QR session so the leaving customer
        // can't keep adding orders to the next party seated at this table.
        revokeActiveSessionsForTable(tableId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void autoCompleteCleaningTables() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(cleaningTimeoutMinutes));
        List<RestaurantTable> tables = restaurantTableRepository
                .findAllByStatusAndDeletedAtIsNullAndUpdatedAtBefore(TableStatus.CLEANING, cutoff);
        if (tables.isEmpty()) return;
        for (RestaurantTable table : tables) {
            try {
                table.markAvailable();
            } catch (Exception ex) {
                LOGGER.warn("Cannot auto-transition table {} CLEANING→AVAILABLE: {}",
                        table.getTableCode(), ex.getMessage());
            }
        }
        restaurantTableRepository.saveAll(tables);
        LOGGER.info("Auto-transitioned {} CLEANING table(s) to AVAILABLE", tables.size());
    }

    @Override
    @Transactional
    public TableData updateTableStatus(String tableCode, TableStatus newStatus) {
        RestaurantTable table = getActiveTable(tableCode);

        switch (newStatus) {
            case AVAILABLE -> table.markAvailable();
            case OCCUPIED -> table.occupy();
            case RESERVED -> table.reserve();
            case CLEANING -> table.markCleaning();
        }

        RestaurantTable saved = restaurantTableRepository.save(table);
        // Manual override by cashier/manager: when they drop the table back to a
        // "freed" state, also revoke any lingering ACTIVE QR session so the
        // previous customer's phone can't keep using its session token.
        if (newStatus == TableStatus.AVAILABLE || newStatus == TableStatus.CLEANING) {
            revokeActiveSessionsForTable(saved.getId());
        }
        return TableData.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public TableData moveTable(String fromCode, String toCode) {
        if (fromCode == null || toCode == null) {
            throw new DomainException("fromCode and toCode are required");
        }
        RestaurantTable from = getActiveTable(fromCode);
        RestaurantTable to = getActiveTable(toCode);
        if (from.getId().equals(to.getId())) {
            throw new DomainException("Bàn nguồn và bàn đích phải khác nhau");
        }
        if (to.getStatus() != TableStatus.AVAILABLE) {
            throw new DomainException("Bàn đích không sẵn sàng (status=" + to.getStatus() + ")");
        }

        Optional<Long> movedOrderId = orderingService.transferActiveOrderToTable(from.getId(), to.getId());
        if (movedOrderId.isEmpty()) {
            throw new DomainException("Bàn nguồn không có order đang phục vụ để chuyển");
        }

        kitchenService.reassignTableForOrder(movedOrderId.get(), to.getId());

        // Source becomes available, destination becomes occupied.
        from.markAvailable();
        to.occupy();
        restaurantTableRepository.saveAll(List.of(from, to));

        // Customer's QR session for the source table must die so their phone can't
        // keep ordering on a now-empty table.
        revokeActiveSessionsForTable(from.getId());

        LOGGER.info("Moved order {} from table {} to table {}",
                movedOrderId.get(), from.getTableCode(), to.getTableCode());
        return TableData.from(to);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public TableGroupData createTableGroup(String masterCode, List<String> memberCodes, String note) {
        if (masterCode == null || memberCodes == null || memberCodes.isEmpty()) {
            throw new DomainException("masterCode và memberCodes là bắt buộc");
        }
        RestaurantTable master = getActiveTable(masterCode);
        List<RestaurantTable> members = new java.util.ArrayList<>();
        members.add(master);
        for (String code : memberCodes) {
            RestaurantTable t = getActiveTable(code);
            if (!t.getId().equals(master.getId())) {
                members.add(t);
            }
        }
        if (members.size() < 2) {
            throw new DomainException("Gộp bàn cần ít nhất 2 bàn khác nhau");
        }
        // Reject if any member is already in an OPEN group.
        for (RestaurantTable t : members) {
            tableGroupRepository.findActiveGroupForTable(t.getId(), TableGroupStatus.OPEN)
                    .ifPresent(g -> {
                        throw new DomainException("Bàn " + t.getTableCode() + " đã thuộc nhóm đang mở (id=" + g.getId() + ")");
                    });
        }

        TableGroup group = TableGroup.builder()
                .masterTableId(master.getId())
                .status(TableGroupStatus.OPEN)
                .note(note)
                .build();
        TableGroup saved = tableGroupRepository.save(group);
        for (RestaurantTable t : members) {
            saved.addMember(t.getId());
            if (t.getStatus() == TableStatus.AVAILABLE) {
                t.occupy();
            }
        }
        restaurantTableRepository.saveAll(members);
        saved = tableGroupRepository.save(saved);

        LOGGER.info("Created table group {} master={} members={}",
                saved.getId(), master.getTableCode(),
                members.stream().map(RestaurantTable::getTableCode).toList());
        return TableGroupData.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public TableGroupData closeTableGroup(Long groupId) {
        TableGroup group = tableGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("TableGroup", groupId));
        if (!group.isOpen()) {
            throw new DomainException("Nhóm bàn đã đóng");
        }

        // Sanity check: there should be no UNPAID orders left under this group.
        List<iuh.fit.se.ordering.domain.Order> linked = orderRepository.findAllByTableGroupId(groupId);
        boolean hasUnpaid = linked.stream().anyMatch(o ->
                o.getStatus() != OrderStatus.PAID && o.getStatus() != OrderStatus.CANCELLED);
        if (hasUnpaid) {
            throw new DomainException("Còn order chưa thanh toán trong nhóm bàn — không thể đóng");
        }

        group.close();
        tableGroupRepository.save(group);

        List<Long> memberIds = group.getMembers().stream().map(m -> m.getTableId()).toList();
        List<RestaurantTable> members = restaurantTableRepository.findAllById(memberIds);
        for (RestaurantTable t : members) {
            if (t.getStatus() == TableStatus.OCCUPIED) {
                try {
                    t.markCleaning();
                } catch (Exception ex) {
                    LOGGER.warn("Cannot transition table {} to CLEANING: {}", t.getTableCode(), ex.getMessage());
                }
            }
            revokeActiveSessionsForTable(t.getId());
        }
        restaurantTableRepository.saveAll(members);

        LOGGER.info("Closed table group {}", groupId);
        return TableGroupData.from(group);
    }

    @Override
    public Optional<TableGroupData> findActiveGroupForTable(Long tableId) {
        return tableGroupRepository.findActiveGroupForTable(tableId, TableGroupStatus.OPEN)
                .map(TableGroupData::from);
    }

    @Override
    public List<TableGroupData> getOpenTableGroups() {
        return tableGroupRepository.findAllByStatus(TableGroupStatus.OPEN)
                .stream().map(TableGroupData::from).toList();
    }

    @Override
    public Optional<TableGroupData> getTableGroupById(Long groupId) {
        return tableGroupRepository.findById(groupId).map(TableGroupData::from);
    }

    /**
     * Revoke every ACTIVE QR session for this table. Mirrors {@code session.revoke()}
     * to the cache so the auth check blocks the next request without waiting for
     * cache TTL. Failures on individual cache deletes are non-fatal.
     */
    private void revokeActiveSessionsForTable(Long tableId) {
        List<QrSession> active = qrSessionRepository.findAllByTableIdAndStatus(
                tableId, QrSessionStatus.ACTIVE);
        if (active.isEmpty()) return;

        Instant now = Instant.now();
        for (QrSession session : active) {
            // Defensive: if the row is already past expiry, mark EXPIRED so we
            // don't overwrite a more accurate terminal state with REVOKED.
            if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
                session.markExpired();
            } else {
                session.revoke();
            }
            try {
                qrSessionCacheService.delete(session.getSessionId());
            } catch (Exception ex) {
                LOGGER.warn("Failed to evict revoked QR session {} from cache: {}",
                        session.getSessionId(), ex.getMessage());
            }
        }
        qrSessionRepository.saveAll(active);
        LOGGER.info("Revoked {} QR session(s) for table {}", active.size(), tableId);
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

    private TableQrCode uploadAndPersistQrImage(RestaurantTable table, TableQrCode qrCode) {
        String qrPayloadUrl = buildQrPayloadUrl(qrCode.getQrKey());
        byte[] qrImageBytes = generateQrCodePng(qrPayloadUrl);

        StoredImage uploadedImage = imageStorageService.uploadTableQrImage(table.getTableCode(), qrImageBytes);
        try {
            qrCode.updateQrImage(uploadedImage.url(), uploadedImage.publicId());
            return tableQrCodeRepository.save(qrCode);
        } catch (RuntimeException ex) {
            safeDelete(uploadedImage.publicId());
            throw ex;
        }
    }

    private TableQrCodeData toTableQrCodeDto(RestaurantTable table, TableQrCode qrCode) {
        return TableQrCodeData.from(qrCode, table.getTableCode(), qrCode.getQrImageUrl());
    }

    private byte[] generateQrCodePng(String qrPayloadUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, QR_IMAGE_MARGIN
            );
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qrPayloadUrl,
                    BarcodeFormat.QR_CODE,
                    QR_IMAGE_SIZE,
                    QR_IMAGE_SIZE,
                    hints
            );

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
                return outputStream.toByteArray();
            }
        } catch (WriterException | IOException ex) {
            throw new DomainException("Failed to generate QR image", ex);
        }
    }

    private String buildQrPayloadUrl(String qrKey) {
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

    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public TableData createTable(int floor, int tableNo, int capacity) {
        RestaurantTable table = RestaurantTable.builder()
                .floor(floor)
                .tableNo(tableNo)
                .capacity(capacity)
                .status(TableStatus.AVAILABLE)
                .build();
        RestaurantTable saved = restaurantTableRepository.save(table);
        return TableData.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public TableData updateTable(String tableCode, int floor, int tableNo, int capacity) {
        RestaurantTable table = getActiveTable(tableCode);
        table.updateLayout(capacity, floor, tableNo);
        RestaurantTable saved = restaurantTableRepository.save(table);
        return TableData.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void deleteTable(String tableCode) {
        RestaurantTable table = getActiveTable(tableCode);
        table.softDelete();
        restaurantTableRepository.save(table);
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
