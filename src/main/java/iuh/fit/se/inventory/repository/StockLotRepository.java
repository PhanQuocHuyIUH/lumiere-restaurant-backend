package iuh.fit.se.inventory.repository;

import iuh.fit.se.inventory.domain.StockLot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockLotRepository extends JpaRepository<StockLot, Long> {

    /** FEFO lookup: lots with remaining stock sorted by earliest expiry. */
    @Query("""
            select l from StockLot l
            where l.ingredientId = :ingredientId
              and l.remainingQty > 0
              and l.deletedAt is null
            order by l.expiryDate asc, l.id asc
            """)
    List<StockLot> findActiveByIngredientOrderByExpiry(@Param("ingredientId") Long ingredientId);

    /** Lots (any ingredient) expiring on or before {@code cutoff} that still have stock. */
    @Query("""
            select l from StockLot l
            where l.expiryDate <= :cutoff
              and l.remainingQty > 0
              and l.deletedAt is null
            order by l.expiryDate asc
            """)
    List<StockLot> findExpiringBefore(@Param("cutoff") LocalDate cutoff);

    Optional<StockLot> findByIdAndDeletedAtIsNull(Long id);
}
