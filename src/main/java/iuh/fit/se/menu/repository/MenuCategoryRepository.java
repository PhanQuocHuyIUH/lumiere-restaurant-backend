package iuh.fit.se.menu.repository;

import iuh.fit.se.menu.domain.MenuCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findAllByDeletedAtIsNullOrderByDisplayOrderAscIdAsc();

    Optional<MenuCategory> findByIdAndDeletedAtIsNull(Long id);
}