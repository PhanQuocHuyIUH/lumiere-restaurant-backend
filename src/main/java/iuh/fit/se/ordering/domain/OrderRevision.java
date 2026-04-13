package iuh.fit.se.ordering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "order_revisions", schema = "ordering")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "revision_source", columnDefinition = "revision_source_enum", nullable = false)
    private RevisionSource revisionSource;

    @Column(name = "created_by_staff")
    private Long createdByStaffId;

    @Column(name = "qr_session_id")
    private String qrSessionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public static OrderRevision create(
            Long orderId,
            Integer revisionNumber,
            RevisionSource revisionSource,
            Long createdByStaffId,
            String qrSessionId
    ) {
        return OrderRevision.builder()
                .orderId(orderId)
                .revisionNumber(revisionNumber)
                .revisionSource(revisionSource)
                .createdByStaffId(createdByStaffId)
                .qrSessionId(qrSessionId)
                .build();
    }
}
