package iuh.fit.se.inventory.infrastructure;

import iuh.fit.se.inventory.domain.Ingredient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    Optional<Ingredient> findByIdAndDeletedAtIsNull(Long id);

    List<Ingredient> findAllByDeletedAtIsNullOrderByNameAsc();

    @Query("SELECT i FROM Ingredient i WHERE i.deletedAt IS NULL AND i.currentQty <= i.lowStockThreshold")
    List<Ingredient> findAllLowStock();
}
