package iuh.fit.se.table.api;

import iuh.fit.se.table.api.dto.CreateTableRequest;
import iuh.fit.se.table.api.dto.TableResponse;
import iuh.fit.se.table.api.dto.UpdateTableRequest;
import iuh.fit.se.table.application.TableDTO;
import iuh.fit.se.table.application.TableService;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tables")
@PreAuthorize("hasRole('MANAGER')")
public class AdminTableController {

    private final TableService tableService;

    public AdminTableController(TableService tableService) {
        this.tableService = tableService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> create(
            @Valid @RequestBody CreateTableRequest request
    ) {
        TableDTO created = tableService.createTable(request.floor(), request.tableNo(), request.capacity());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Table created", TableResponse.from(created)));
    }

    @PutMapping("/{tableCode}")
    public ResponseEntity<ApiResponse<TableResponse>> update(
            @PathVariable("tableCode") String tableCode,
            @Valid @RequestBody UpdateTableRequest request
    ) {
        TableDTO updated = tableService.updateTable(tableCode, request.floor(), request.tableNo(), request.capacity());
        return ResponseEntity.ok(ApiResponse.ok("Table updated", TableResponse.from(updated)));
    }

    @DeleteMapping("/{tableCode}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("tableCode") String tableCode) {
        tableService.deleteTable(tableCode);
        return ResponseEntity.ok(ApiResponse.ok("Table deleted", null));
    }
}
