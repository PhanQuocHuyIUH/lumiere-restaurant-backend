package iuh.fit.se.shared.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.table.domain.RestaurantTable;
import iuh.fit.se.table.domain.TableStatus;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExportTableContext(
        Long tableId,
        String tableCode,
        Integer floor,
        Integer tableNo,
        Integer capacity,
        TableStatus tableStatus
) {

    public static ExportTableContext from(RestaurantTable table) {
        return new ExportTableContext(
                table.getId(),
                table.getTableCode(),
                table.getFloor(),
                table.getTableNo(),
                table.getCapacity(),
                table.getStatus()
        );
    }
}
