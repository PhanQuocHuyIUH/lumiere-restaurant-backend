package iuh.fit.se.inventory.repository;

import iuh.fit.se.inventory.domain.StockTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findAllByIngredientIdOrderByCreatedAtDesc(Long ingredientId);
}
