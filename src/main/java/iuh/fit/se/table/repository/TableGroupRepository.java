package iuh.fit.se.table.repository;

import iuh.fit.se.table.domain.TableGroup;
import iuh.fit.se.table.domain.TableGroupStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TableGroupRepository extends JpaRepository<TableGroup, Long> {

    @Query("""
            select g from TableGroup g
            join g.members m
            where m.tableId = :tableId and g.status = :status
            """)
    Optional<TableGroup> findActiveGroupForTable(@Param("tableId") Long tableId, @Param("status") TableGroupStatus status);

    List<TableGroup> findAllByStatus(TableGroupStatus status);
}
