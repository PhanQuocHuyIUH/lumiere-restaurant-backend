package iuh.fit.se.menu.repository;

import iuh.fit.se.menu.domain.MenuItemIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemIngredientRepository extends JpaRepository<MenuItemIngredient, Long> {

    List<MenuItemIngredient> findAllByMenuItemId(Long menuItemId);

    void deleteAllByMenuItemId(Long menuItemId);
}
