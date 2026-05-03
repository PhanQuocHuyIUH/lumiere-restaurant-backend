package iuh.fit.se.menu.infrastructure;

import iuh.fit.se.menu.domain.MenuItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    Optional<MenuItem> findByIdAndDeletedAtIsNull(Long id);

    List<MenuItem> findAllByDeletedAtIsNullOrderByIdAsc();

    List<MenuItem> findAllByCategoryIdAndDeletedAtIsNullOrderByIdAsc(Long categoryId);

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);
}