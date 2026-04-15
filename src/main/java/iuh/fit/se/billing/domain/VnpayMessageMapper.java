package iuh.fit.se.billing.domain;

import java.util.Map;

public final class VnpayMessageMapper {

    private static final String DEFAULT_RESPONSE_CODE = "99";
    private static final String DEFAULT_TRANSACTION_STATUS = "99";

    private static final Map<String, String> RESPONSE_CODE_MESSAGES = Map.ofEntries(
            Map.entry("00", "Giao dịch thành công"),
            Map.entry("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)."),
            Map.entry("09", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng."),
            Map.entry("10", "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần"),
            Map.entry("11", "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch."),
            Map.entry("12", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa."),
            Map.entry("13", "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP). Xin quý khách vui lòng thực hiện lại giao dịch."),
            Map.entry("24", "Giao dịch không thành công do: Khách hàng hủy giao dịch"),
            Map.entry("51", "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch."),
            Map.entry("65", "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày."),
            Map.entry("75", "Ngân hàng thanh toán đang bảo trì."),
            Map.entry("79", "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định. Xin quý khách vui lòng thực hiện lại giao dịch"),
            Map.entry("99", "Các lỗi khác (lỗi còn lại, không có trong danh sách mã lỗi đã liệt kê)")
    );

    private static final Map<String, String> TRANSACTION_STATUS_MESSAGES = Map.ofEntries(
            Map.entry("00", "Giao dịch thành công"),
            Map.entry("01", "Giao dịch chưa hoàn tất"),
            Map.entry("02", "Giao dịch bị lỗi"),
            Map.entry("04", "Giao dịch đảo (Khách hàng đã bị trừ tiền tại Ngân hàng nhưng GD chưa thành công ở VNPAY)"),
            Map.entry("05", "VNPAY đang xử lý giao dịch này (GD hoàn tiền)"),
            Map.entry("06", "VNPAY đã gửi yêu cầu hoàn tiền sang Ngân hàng (GD hoàn tiền)"),
            Map.entry("07", "Giao dịch bị nghi ngờ gian lận"),
            Map.entry("09", "GD Hoàn trả bị từ chối")
    );

    private static final Map<String, String> IPN_RESPONSE_MESSAGES = Map.ofEntries(
            Map.entry("00", "Xác nhận thành công"),
            Map.entry("01", "Không tìm thấy đơn hàng"),
            Map.entry("02", "Đơn hàng đã được xác nhận"),
            Map.entry("04", "Số tiền không hợp lệ"),
            Map.entry("97", "Chữ ký không hợp lệ"),
            Map.entry("99", "Lỗi không xác định")
    );

    private VnpayMessageMapper() {
    }

    public static String normalizeResponseCode(String responseCode) {
        if (!hasText(responseCode)) {
            return DEFAULT_RESPONSE_CODE;
        }
        String normalized = responseCode.trim();
        return RESPONSE_CODE_MESSAGES.containsKey(normalized) ? normalized : DEFAULT_RESPONSE_CODE;
    }

    public static String normalizeTransactionStatus(String status) {
        if (!hasText(status)) {
            return DEFAULT_TRANSACTION_STATUS;
        }
        String normalized = status.trim();
        return TRANSACTION_STATUS_MESSAGES.containsKey(normalized) ? normalized : DEFAULT_TRANSACTION_STATUS;
    }

    public static String responseCodeMessage(String responseCode) {
        return RESPONSE_CODE_MESSAGES.get(normalizeResponseCode(responseCode));
    }

    public static String transactionStatusMessage(String transactionStatus) {
        String normalized = normalizeTransactionStatus(transactionStatus);
        return TRANSACTION_STATUS_MESSAGES.getOrDefault(normalized, "Trạng thái giao dịch không hợp lệ");
    }

    public static String ipnResponseMessage(String responseCode) {
        if (!hasText(responseCode)) {
            return IPN_RESPONSE_MESSAGES.get("99");
        }
        return IPN_RESPONSE_MESSAGES.getOrDefault(responseCode.trim(), IPN_RESPONSE_MESSAGES.get("99"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}