package iuh.fit.se.table.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "table_qr_codes", schema = "table_mgmt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableQrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false, unique = true)
    private Long tableId;

    @Column(name = "qr_key", nullable = false, unique = true, length = 120)
    private String qrKey;

    @Column(name = "qr_image_url")
    private String qrImageUrl;

    @Column(name = "qr_image_public_id")
    private String qrImagePublicId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "qr_code_status_enum", nullable = false)
    @Builder.Default
    private TableQrCodeStatus status = TableQrCodeStatus.ACTIVE;

    @Column(name = "rotate_after")
    private Instant rotateAfter;

    @Column(name = "last_issued_session_at")
    private Instant lastIssuedSessionAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = TableQrCodeStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public static TableQrCode create(Long tableId, String qrKey, Instant rotateAfter) {
        return TableQrCode.builder()
                .tableId(tableId)
                .qrKey(qrKey)
                .status(TableQrCodeStatus.ACTIVE)
                .rotateAfter(rotateAfter)
                .build();
    }

    public void rotateTo(String newQrKey, Instant rotateAfter) {
        this.qrKey = newQrKey;
        this.rotateAfter = rotateAfter;
        this.status = TableQrCodeStatus.ACTIVE;
        this.disabledAt = null;
        this.qrImageUrl = null;
        this.qrImagePublicId = null;
    }

    public void updateQrImage(String qrImageUrl, String qrImagePublicId) {
        this.qrImageUrl = qrImageUrl;
        this.qrImagePublicId = qrImagePublicId;
    }

    public void markRotating() {
        this.status = TableQrCodeStatus.ROTATING;
    }

    public void disable() {
        this.status = TableQrCodeStatus.DISABLED;
        this.disabledAt = Instant.now();
    }

    public void markIssuedSession() {
        this.lastIssuedSessionAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == TableQrCodeStatus.ACTIVE;
    }
}
