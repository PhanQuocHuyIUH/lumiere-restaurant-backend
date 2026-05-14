package iuh.fit.se.shared.domain;

public enum TaxMode {
    NO_TAX,
    EXCLUSIVE,
    INCLUSIVE,
    /** Sentinel: order chứa items có nhiều tax mode khác nhau. Amounts vẫn chính xác — xem order_items. */
    MIXED
}
