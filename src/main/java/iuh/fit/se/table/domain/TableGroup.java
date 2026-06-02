package iuh.fit.se.table.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "table_groups", schema = "table_mgmt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_table_id", nullable = false)
    private Long masterTableId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "table_group_status_enum", nullable = false)
    @Builder.Default
    private TableGroupStatus status = TableGroupStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "note")
    private String note;

    // Unidirectional @OneToMany. group_id is part of TableGroupMember's composite PK and is
    // populated by the application in addMember(), so the join column is read-only here —
    // letting Hibernate manage it would try to UPDATE a primary-key column.
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    @Builder.Default
    private List<TableGroupMember> members = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = TableGroupStatus.OPEN;
        }
    }

    public void addMember(Long tableId) {
        members.add(TableGroupMember.builder()
                .groupId(this.id)
                .tableId(tableId)
                .joinedAt(Instant.now())
                .build());
    }

    public void close() {
        if (this.status == TableGroupStatus.CLOSED) {
            return;
        }
        this.status = TableGroupStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public boolean isOpen() {
        return this.status == TableGroupStatus.OPEN;
    }
}
