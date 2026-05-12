package iuh.fit.se.shared.runner;

import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.table.application.TableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CacheWarmupRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheWarmupRunner.class);
    private final MenuService menuService;
    private final TableService tableService;

    public CacheWarmupRunner(MenuService menuService, TableService tableService) {
        this.menuService = menuService;
        this.tableService = tableService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            LOGGER.info("Starting cache warmup: menu and tables");
            menuService.getMenu();
            tableService.getAllTables();
            LOGGER.info("Cache warmup completed");
        } catch (Exception ex) {
            LOGGER.warn("Cache warmup failed (non-fatal): {}", ex.getMessage());
        }
    }
}
