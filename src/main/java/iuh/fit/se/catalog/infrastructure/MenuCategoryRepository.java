package iuh.fit.se.catalog.infrastructure;

import iuh.fit.se.catalog.domain.MenuCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc();
}