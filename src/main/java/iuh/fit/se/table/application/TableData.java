package iuh.fit.se.table.application;

import iuh.fit.se.table.domain.RestaurantTable;
import iuh.fit.se.table.domain.TableStatus;

public record TableData(
        Long id,
    String tableCode,
    Integer floor,
    Integer tableNo,
        Integer capacity,
    TableStatus status
) {

    public static TableData from(RestaurantTable restaurantTable) {
        return new TableData(
                restaurantTable.getId(),
                restaurantTable.getTableCode(),
                restaurantTable.getFloor(),
                restaurantTable.getTableNo(),
                restaurantTable.getCapacity(),
                restaurantTable.getStatus()
        );
    }
}
