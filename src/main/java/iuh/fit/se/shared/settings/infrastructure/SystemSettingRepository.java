package iuh.fit.se.shared.settings.infrastructure;

import iuh.fit.se.shared.settings.domain.SystemSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {

    @Query("select s.value from SystemSetting s where s.key = :key")
    Optional<String> findValueByKey(@Param("key") String key);
}
