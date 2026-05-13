package iuh.fit.se.menu.repository;

import iuh.fit.se.menu.domain.ComboPickSlotItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboPickSlotItemRepository extends JpaRepository<ComboPickSlotItem, Long> {
    List<ComboPickSlotItem> findAllBySlotIdOrderByIdAsc(Long slotId);
    List<ComboPickSlotItem> findAllBySlotIdIn(List<Long> slotIds);
    void deleteAllBySlotIdIn(List<Long> slotIds);
}

