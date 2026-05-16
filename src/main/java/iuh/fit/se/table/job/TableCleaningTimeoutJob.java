package iuh.fit.se.table.job;

import iuh.fit.se.table.application.TableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TableCleaningTimeoutJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableCleaningTimeoutJob.class);

    private final TableService tableService;

    public TableCleaningTimeoutJob(TableService tableService) {
        this.tableService = tableService;
    }

    @Scheduled(fixedDelayString = "${app.table.cleaning-check-interval-ms:120000}")
    public void checkCleaningTables() {
        try {
            tableService.autoCompleteCleaningTables();
        } catch (Exception ex) {
            LOGGER.error("Error during cleaning table timeout check", ex);
        }
    }
}
