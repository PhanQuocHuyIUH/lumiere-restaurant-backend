package iuh.fit.se.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Encodes a VietQR string per EMVCo QR Code Specification 1.5.2 + NAPAS overlay (Tag 38 GUID A000000727).
 * The output is a plain string that any Vietnamese banking app can scan to pre-fill a NAPAS 247 transfer.
 */
public final class VietQrCodec {

    private static final String NAPAS_GUID = "A000000727";
    public static final String SERVICE_TO_ACCOUNT = "QRIBFTTA";
    public static final String SERVICE_TO_CARD = "QRIBFTTC";

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private VietQrCodec() {
    }

    public static String build(VietQrInput input) {
        StringBuilder sb = new StringBuilder();
        // Tag 00 — Payload Format Indicator, fixed "01"
        sb.append(field("00", "01"));
        // Tag 01 — Point of Initiation Method: "11" static, "12" dynamic (with amount)
        sb.append(field("01", "12"));
        // Tag 38 — Merchant Account Information (NAPAS overlay)
        sb.append(buildMerchantAccountInfo(input));
        // Tag 53 — Transaction Currency: 704 = VND
        sb.append(field("53", "704"));
        // Tag 54 — Transaction Amount (integer VND, no decimals)
        sb.append(field("54", formatAmount(input.amount())));
        // Tag 58 — Country Code
        sb.append(field("58", "VN"));
        // Tag 59 — Merchant Name (optional, ASCII, ≤25 chars)
        if (hasText(input.merchantName())) {
            sb.append(field("59", clean(input.merchantName(), 25)));
        }
        // Tag 60 — Merchant City (optional, ASCII, ≤15 chars)
        if (hasText(input.merchantCity())) {
            sb.append(field("60", clean(input.merchantCity(), 15)));
        }
        // Tag 62 — Additional Data Field Template, sub-tag 08 = Purpose of Transaction (memo)
        if (hasText(input.purpose())) {
            sb.append(field("62", field("08", clean(input.purpose(), 25))));
        }
        // Tag 63 — CRC, computed over the entire string including "6304"
        String preCrc = sb.toString() + "6304";
        sb.append("6304").append(crc16Ccitt(preCrc));
        return sb.toString();
    }

    private static String buildMerchantAccountInfo(VietQrInput input) {
        // Sub-tag 00 — GUID (NAPAS)
        String guid = field("00", NAPAS_GUID);
        // Sub-tag 01 — Beneficiary Organization: BIN + Account
        String beneficiary = field("01",
                field("00", input.bankBin())
                        + field("01", input.accountNumber()));
        // Sub-tag 02 — Service Code
        String service = field("02", input.serviceCode() == null ? SERVICE_TO_ACCOUNT : input.serviceCode());
        return field("38", guid + beneficiary + service);
    }

    private static String formatAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String field(String tag, String value) {
        return tag + String.format("%02d", value.length()) + value;
    }

    /**
     * CRC-16/CCITT-FALSE: poly 0x1021, init 0xFFFF, no reflection, no XOR-out.
     * Result is 4 uppercase hex chars.
     */
    static String crc16Ccitt(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }

    /** Strip Vietnamese diacritics → ASCII, then truncate. Banking apps render Tag 59/60/62 as plain text. */
    private static String clean(String value, int maxLength) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD);
        ascii = DIACRITICS.matcher(ascii).replaceAll("");
        ascii = ascii.replace('Đ', 'D').replace('đ', 'd');
        return ascii.length() > maxLength ? ascii.substring(0, maxLength) : ascii;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record VietQrInput(
            String bankBin,
            String accountNumber,
            String serviceCode,
            BigDecimal amount,
            String merchantName,
            String merchantCity,
            String purpose
    ) {
    }
}
