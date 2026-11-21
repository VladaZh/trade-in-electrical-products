package wholesale.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public final class Formats {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private Formats() {
    }

    public static String money(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return format.format(value);
    }

    public static String padRight(String text, int width) {
        return String.format(Locale.ROOT, "%-" + width + "s", text);
    }

    public static String padLeft(String text, int width) {
        return String.format(Locale.ROOT, "%" + width + "s", text);
    }

    public static String nowStamp() {
        return LocalDateTime.now().format(STAMP);
    }

    public static String pyRepr(String text) {
        char quote = (text.indexOf('\'') >= 0 && text.indexOf('"') < 0) ? '"' : '\'';
        StringBuilder sb = new StringBuilder().append(quote);
        for (char ch : text.toCharArray()) {
            if (ch == quote || ch == '\\') {
                sb.append('\\').append(ch);
            } else if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == '\r') {
                sb.append("\\r");
            } else if (ch == '\t') {
                sb.append("\\t");
            } else {
                sb.append(ch);
            }
        }
        return sb.append(quote).toString();
    }

    public static String pyDict(Map<String, String> map) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (Map.Entry<String, String> e : map.entrySet()) {
            joiner.add(pyRepr(e.getKey()) + ": " + pyRepr(e.getValue()));
        }
        return joiner.toString();
    }
}
