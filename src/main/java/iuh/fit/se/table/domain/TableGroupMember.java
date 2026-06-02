package iuh.fit.se.table.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "table_group_members", schema = "table_mgmt")
@IdClass(TableGroupMember.PK.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableGroupMember {

    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Id
    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Long groupId;
        private Long tableId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(groupId, pk.groupId) && Objects.equals(tableId, pk.tableId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, tableId);
        }
    }
}
