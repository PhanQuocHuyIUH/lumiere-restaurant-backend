package iuh.fit.se.catalog.domain;

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
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "tables", schema = "table_mgmt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Column(name = "table_no", nullable = false)
    private Integer tableNo;

    @Column(name = "table_code", insertable = false, updatable = false)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private String tableCode;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "table_status_enum", nullable = false)
    private TableStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = TableStatus.AVAILABLE;
        }
        if (this.capacity == null) {
            this.capacity = 4;
        }
        if (this.floor == null) {
            this.floor = 1;
        }
        if (this.tableNo == null) {
            this.tableNo = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateLayout(Integer capacity, Integer floor, Integer tableNo) {
        this.capacity = capacity;
        this.floor = floor;
        this.tableNo = tableNo;
    }

    public void occupy() {
        RestaurantTableStateMachine.validate(this.status, TableStatus.OCCUPIED);
        this.status = TableStatus.OCCUPIED;
    }

    public void reserve() {
        RestaurantTableStateMachine.validate(this.status, TableStatus.RESERVED);
        this.status = TableStatus.RESERVED;
    }

    public void markCleaning() {
        RestaurantTableStateMachine.validate(this.status, TableStatus.CLEANING);
        this.status = TableStatus.CLEANING;
    }

    public void markAvailable() {
        RestaurantTableStateMachine.validate(this.status, TableStatus.AVAILABLE);
        this.status = TableStatus.AVAILABLE;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            return;
        }
        if (this.status != TableStatus.CLEANING) {
            markCleaning();
        }
        this.deletedAt = Instant.now();
    }

    public boolean isAvailable() {
        return this.status == TableStatus.AVAILABLE && this.deletedAt == null;
    }
}