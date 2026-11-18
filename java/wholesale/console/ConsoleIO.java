package wholesale.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;
import wholesale.util.Out;

public class ConsoleIO {
    private static final Set<String> YES_WORDS = Set.of("да", "д", "y", "yes");
    private static final Set<String> NO_WORDS = Set.of("нет", "н", "n", "no");

    private final BufferedReader reader;

    public ConsoleIO() {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
    }

    public ConsoleIO(BufferedReader reader) {
        this.reader = reader;
    }

    public String readLine(String prompt) {
        Out.print(prompt);
        Out.flush();
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new EndOfInputException();
            }
            return line;
        } catch (IOException e) {
            throw new EndOfInputException();
        }
    }

    public static boolean isDecimal(String text) {
        return !text.isEmpty() && text.chars().allMatch(Character::isDigit);
    }

    public String askText(String prompt) {
        return readLine(prompt).strip();
    }

    public String askNonEmpty(String prompt) {
        while (true) {
            String text = readLine(prompt).strip();
            if (!text.isEmpty()) {
                return text;
            }
            Out.println("Значение не может быть пустым.");
        }
    }

    public boolean askYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (да/нет): ").strip().toLowerCase(Locale.ROOT);
            if (YES_WORDS.contains(answer)) {
                return true;
            }
            if (NO_WORDS.contains(answer)) {
                return false;
            }
            Out.println("Введите «да» или «нет».");
        }
    }

    public int askInt(String prompt, int low, int high) {
        while (true) {
            String text = readLine(prompt).strip();
            if (isDecimal(text)) {
                BigInteger value = new BigInteger(text);
                if (value.compareTo(BigInteger.valueOf(low)) >= 0 && value.compareTo(BigInteger.valueOf(high)) <= 0) {
                    return value.intValue();
                }
            }
            Out.println("Введите число от " + low + " до " + high + ".");
        }
    }

    public int askFromIds(String prompt, Set<Integer> validIds) {
        while (true) {
            String text = readLine(prompt).strip();
            if (isDecimal(text)) {
                BigInteger value = new BigInteger(text);
                if (value.bitLength() < 31 && validIds.contains(value.intValue())) {
                    return value.intValue();
                }
            }
            Out.println("Нет такого номера, попробуйте ещё раз.");
        }
    }

    public String askDate(String prompt, LocalDate minDate) {
        while (true) {
            String text = readLine(prompt).strip();
            LocalDate value;
            try {
                value = LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                Out.println("Формат даты: ГГГГ-ММ-ДД, например 2026-11-20.");
                continue;
            }
            if (value.isBefore(minDate)) {
                Out.println("Дата не может быть раньше " + minDate + ".");
                continue;
            }
            return value.toString();
        }
    }
}
