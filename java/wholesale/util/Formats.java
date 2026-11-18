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

/** Форматирование, совпадающее с f-строками Python ({x:,.2f}, {s:<10}, {n:>8}, repr(dict)). */
public final class Formats {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private Formats() {
    }

    /** Аналог Python f"{value:,.2f}". */
    public static String money(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return format.format(value);
    }

    /** Аналог {s:<width}. */
    public static String padRight(String text, int width) {
        return String.format(Locale.ROOT, "%-" + width + "s", text);
    }

    /** Аналог {s:>width}. */
    public static String padLeft(String text, int width) {
        return String.format(Locale.ROOT, "%" + width + "s", text);
    }

    /** Метка времени для истории и сообщений: "дд.мм чч:мм". */
    public static String nowStamp() {
        return LocalDateTime.now().format(STAMP);
    }

    /** Аналог Python repr() для строки. */
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

    /** Аналог str(dict) для словаря строка -> строка: {'phone': '+7', 'address': 'x'}. */
    public static String pyDict(Map<String, String> map) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (Map.Entry<String, String> e : map.entrySet()) {
            joiner.add(pyRepr(e.getKey()) + ": " + pyRepr(e.getValue()));
        }
        return joiner.toString();
    }
}
