package iuh.fit.se.catalog.infrastructure;

import iuh.fit.se.catalog.domain.RestaurantTable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    Optional<RestaurantTable> findByIdAndDeletedAtIsNull(Long id);

    Optional<RestaurantTable> findByTableCodeAndDeletedAtIsNull(String tableCode);
}