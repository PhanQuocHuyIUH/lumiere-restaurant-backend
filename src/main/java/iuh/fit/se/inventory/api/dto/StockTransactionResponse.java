package iuh.fit.se.inventory.api.dto;

import iuh.fit.se.inventory.domain.StockTransaction;
import iuh.fit.se.inventory.domain.StockTxnType;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockTransactionResponse {

    private Long id;
    private StockTxnType txnType;
    private BigDecimal quantityBefore;
    private BigDecimal quantityChange;
    private BigDecimal quantityAfter;
    private String note;
    private Long performedBy;
    private Instant createdAt;

    public static StockTransactionResponse from(StockTransaction txn) {
        return StockTransactionResponse.builder()
                .id(txn.getId())
                .txnType(txn.getTxnType())
                .quantityBefore(txn.getQuantityBefore())
                .quantityChange(txn.getQuantityChange())
                .quantityAfter(txn.getQuantityAfter())
                .note(txn.getNote())
                .performedBy(txn.getPerformedBy())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
