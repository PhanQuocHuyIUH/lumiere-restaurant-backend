package iuh.fit.se.table.application;

import iuh.fit.se.table.domain.TableGroup;
import iuh.fit.se.table.domain.TableGroupStatus;
import java.time.Instant;
import java.util.List;

public record TableGroupData(
        Long id,
        Long masterTableId,
        TableGroupStatus status,
        Instant createdAt,
        Instant closedAt,
        String note,
        List<Long> memberTableIds
) {

    public static TableGroupData from(TableGroup group) {
        return new TableGroupData(
                group.getId(),
                group.getMasterTableId(),
                group.getStatus(),
                group.getCreatedAt(),
                group.getClosedAt(),
                group.getNote(),
                group.getMembers().stream().map(m -> m.getTableId()).toList()
        );
    }
}
