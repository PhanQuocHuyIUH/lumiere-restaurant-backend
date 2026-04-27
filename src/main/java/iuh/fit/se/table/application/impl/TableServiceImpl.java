package iuh.fit.se.table.application.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.storage.ImageStorageService;
import iuh.fit.se.shared.storage.StoredImage;
import iuh.fit.se.table.application.QrSessionTokenDTO;
import iuh.fit.se.table.application.TableDTO;
import iuh.fit.se.table.application.TableQrCodeDTO;
import iuh.fit.se.table.application.TableService;
import iuh.fit.se.table.domain.QrSession;
import iuh.fit.se.table.domain.QrSessionStatus;
import iuh.fit.se.table.domain.RestaurantTable;
import iuh.fit.se.table.domain.TableQrCode;
import iuh.fit.se.table.domain.TableQrCodeStatus;
import iuh.fit.se.table.domain.TableStatus;
import iuh.fit.se.table.infrastructure.QrSessionRepository;
import iuh.fit.se.table.infrastructure.RestaurantTableRepository;
import iuh.fit.se.table.infrastructure.TableQrCodeRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TableServiceImpl implements TableService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableServiceImpl.class);
    private static final Pattern TABLE_CODE_PATTERN = Pattern.compile("^(\\d+)-(\\d{1,3})$");
    private static final Pattern QR_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9]{16,120}$");
    private static final int QR_IMAGE_SIZE = 512;
    private static final int QR_IMAGE_MARGIN = 1;

    private final RestaurantTableRepository restaurantTableRepository;
    private final TableQrCodeRepository tableQrCodeRepository;
    private final QrSessionRepository qrSessionRepository;
    private final ImageStorageService imageStorageService;

    @Value("${app.qr.base-url:http://localhost:8080/tables/qr}")
    private String qrBaseUrl;

    @Value("${app.qr.session-expiration:1800000}")
    private long qrSessionExpirationMs;

    public TableServiceImpl(
            RestaurantTableRepository restaurantTableRepository,
            TableQrCodeRepository tableQrCodeRepository,
            QrSessionRepository qrSessionRepository,
            ImageStorageService imageStorageService
    ) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.tableQrCodeRepository = tableQrCodeRepository;
        this.qrSessionRepository = qrSessionRepository;
        this.imageStorageService = imageStorageService;
    }

    @Override
    public List<TableDTO> getAllTables() {
        return restaurantTableRepository.findAllByDeletedAtIsNullOrderByFloorAscTableNoAsc()
                .stream()
                .map(TableDTO::from)
                .toList();
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
        if (qrCode.getQrImageUrl() == null || qrCode.getQrImageUrl().isBlank()) {
            qrCode = uploadAndPersistQrImage(table, qrCode);
        }
        return toTableQrCodeDto(table, qrCode);
    }

    @Override
    @Transactional
    public TableQrCodeDTO rotateTableQrCode(String tableCode) {
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
    }

    @Override
    @Transactional
    public TableDTO updateTableStatus(String tableCode, TableStatus newStatus) {
        RestaurantTable table = getActiveTable(tableCode);

        switch (newStatus) {
            case AVAILABLE -> table.markAvailable();
            case OCCUPIED -> table.occupy();
            case RESERVED -> table.reserve();
            case CLEANING -> table.markCleaning();
        }

        RestaurantTable saved = restaurantTableRepository.save(table);
        return TableDTO.from(saved);
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

    private TableQrCodeDTO toTableQrCodeDto(RestaurantTable table, TableQrCode qrCode) {
        return TableQrCodeDTO.from(qrCode, table.getTableCode(), qrCode.getQrImageUrl());
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
    public TableDTO createTable(int floor, int tableNo, int capacity) {
        RestaurantTable table = RestaurantTable.builder()
                .floor(floor)
                .tableNo(tableNo)
                .capacity(capacity)
                .status(TableStatus.AVAILABLE)
                .build();
        RestaurantTable saved = restaurantTableRepository.save(table);
        return TableDTO.from(saved);
    }

    @Override
    @Transactional
    public TableDTO updateTable(String tableCode, int floor, int tableNo, int capacity) {
        RestaurantTable table = getActiveTable(tableCode);
        table.updateLayout(capacity, floor, tableNo);
        RestaurantTable saved = restaurantTableRepository.save(table);
        return TableDTO.from(saved);
    }

    @Override
    @Transactional
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
