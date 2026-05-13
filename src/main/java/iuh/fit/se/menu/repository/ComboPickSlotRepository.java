package iuh.fit.se.menu.repository;

import iuh.fit.se.menu.domain.ComboPickSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboPickSlotRepository extends JpaRepository<ComboPickSlot, Long> {
    List<ComboPickSlot> findAllByComboItemIdOrderByDisplayOrderAscIdAsc(Long comboItemId);
    void deleteAllByComboItemId(Long comboItemId);
}

