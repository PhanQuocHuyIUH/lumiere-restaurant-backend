package iuh.fit.se.menu.repository;

import iuh.fit.se.menu.domain.ComboFixedComponent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboFixedComponentRepository extends JpaRepository<ComboFixedComponent, Long> {
    List<ComboFixedComponent> findAllByComboItemIdOrderByIdAsc(Long comboItemId);
    void deleteAllByComboItemId(Long comboItemId);
}

