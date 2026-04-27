package iuh.fit.se.table.api.dto;

import iuh.fit.se.table.application.TableDTO;
import iuh.fit.se.table.domain.RestaurantTable;
import iuh.fit.se.table.domain.TableStatus;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TableResponse {

    private Long id;
    private String tableCode;
    private Integer floor;
    private Integer tableNo;
    private Integer capacity;
    private TableStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static TableResponse from(RestaurantTable restaurantTable) {
        return TableResponse.builder()
                .id(restaurantTable.getId())
            .tableCode(restaurantTable.getTableCode())
            .floor(restaurantTable.getFloor())
            .tableNo(restaurantTable.getTableNo())
                .capacity(restaurantTable.getCapacity())
                .status(restaurantTable.getStatus())
                .createdAt(restaurantTable.getCreatedAt())
                .updatedAt(restaurantTable.getUpdatedAt())
                .build();
    }

    public static TableResponse from(TableDTO table) {
        return TableResponse.builder()
                .id(table.id())
            .tableCode(table.tableCode())
            .floor(table.floor())
            .tableNo(table.tableNo())
                .capacity(table.capacity())
                .status(table.status())
                .build();
    }
}