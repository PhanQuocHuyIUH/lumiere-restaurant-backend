package iuh.fit.se.menu.repository;

import iuh.fit.se.menu.domain.MenuItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    Optional<MenuItem> findByIdAndDeletedAtIsNull(Long id);

    Page<MenuItem> findAllByDeletedAtIsNull(Pageable pageable);

    List<MenuItem> findAllByDeletedAtIsNullOrderByIdAsc();

    List<MenuItem> findAllByCategoryIdAndDeletedAtIsNullOrderByIdAsc(Long categoryId);

    long countByCategoryIdAndDeletedAtIsNull(Long categoryId);
}