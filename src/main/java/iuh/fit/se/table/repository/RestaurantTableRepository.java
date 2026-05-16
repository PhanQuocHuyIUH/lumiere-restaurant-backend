package iuh.fit.se.table.repository;

import iuh.fit.se.table.domain.RestaurantTable;
import iuh.fit.se.table.domain.TableStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    List<RestaurantTable> findAllByDeletedAtIsNullOrderByFloorAscTableNoAsc();

    List<RestaurantTable> findAllByIdInAndDeletedAtIsNull(Collection<Long> ids);

    Optional<RestaurantTable> findByIdAndDeletedAtIsNull(Long id);

    Optional<RestaurantTable> findByTableCodeAndDeletedAtIsNull(String tableCode);

    List<RestaurantTable> findAllByStatusAndDeletedAtIsNullAndUpdatedAtBefore(TableStatus status, Instant before);
}