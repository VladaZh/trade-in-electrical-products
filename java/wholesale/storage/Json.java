package wholesale.storage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw new JsonException("Лишние символы после JSON в позиции " + parser.pos);
        }
        return value;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        Object value() {
            skipWhitespace();
            if (pos >= s.length()) {
                throw new JsonException("Неожиданный конец JSON");
            }
            char c = s.charAt(pos);
            switch (c) {
                case '{':
                    return object();
                case '[':
                    return array();
                case '"':
                    return string();
                case 't':
                    return literal("true", Boolean.TRUE);
                case 'f':
                    return literal("false", Boolean.FALSE);
                case 'n':
                    return literal("null", null);
                default:
                    return number();
            }
        }

        private Object literal(String word, Object result) {
            if (!s.startsWith(word, pos)) {
                throw new JsonException("Ожидалось " + word + " в позиции " + pos);
            }
            pos += word.length();
            return result;
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = string();
                skipWhitespace();
                expect(':');
                map.put(key, value());
                skipWhitespace();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new JsonException("Ожидалась запятая в позиции " + (pos - 1));
                }
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(value());
                skipWhitespace();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new JsonException("Ожидалась запятая в позиции " + (pos - 1));
                }
            }
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = next();
                switch (e) {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(e);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (pos + 4 > s.length()) {
                            throw new JsonException("Обрезанная escape-последовательность");
                        }
                        sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default:
                        throw new JsonException("Неизвестный escape \\" + e);
                }
            }
        }

        private Object number() {
            int start = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            String text = s.substring(start, pos);
            if (text.isEmpty()) {
                throw new JsonException("Неожиданный символ '" + s.charAt(pos) + "' в позиции " + pos);
            }
            try {
                if (text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0) {
                    return new BigDecimal(text);
                }
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw new JsonException("Некорректное число: " + text);
            }
        }

        private char peek() {
            if (pos >= s.length()) {
                throw new JsonException("Неожиданный конец JSON");
            }
            return s.charAt(pos);
        }

        private char next() {
            char c = peek();
            pos++;
            return c;
        }

        private void expect(char c) {
            if (next() != c) {
                throw new JsonException("Ожидался символ '" + c + "' в позиции " + (pos - 1));
            }
        }
    }


    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb, 0);
        return sb.toString();
    }

    private static void writeValue(Object value, StringBuilder sb, int level) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            quote((String) value, sb);
        } else if (value instanceof Boolean || value instanceof Long || value instanceof Integer) {
            sb.append(value);
        } else if (value instanceof BigDecimal) {
            String plain = ((BigDecimal) value).toPlainString();
            sb.append(plain.indexOf('.') >= 0 ? plain : plain + ".0");
        } else if (value instanceof Map) {
            writeMap(obj(value), sb, level);
        } else if (value instanceof List) {
            writeList(arr(value), sb, level);
        } else {
            throw new JsonException("Неподдерживаемый тип: " + value.getClass());
        }
    }

    private static void writeMap(Map<String, Object> map, StringBuilder sb, int level) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            indent(sb, level + 1);
            quote(e.getKey(), sb);
            sb.append(": ");
            writeValue(e.getValue(), sb, level + 1);
        }
        sb.append('\n');
        indent(sb, level);
        sb.append('}');
    }

    private static void writeList(List<Object> list, StringBuilder sb, int level) {
        if (list.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[\n");
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            indent(sb, level + 1);
            writeValue(item, sb, level + 1);
        }
        sb.append('\n');
        indent(sb, level);
        sb.append(']');
    }

    private static void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level * 2; i++) {
            sb.append(' ');
        }
    }

    private static void quote(String text, StringBuilder sb) {
        sb.append('"');
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) {
        return (List<Object>) o;
    }

    public static String str(Object o) {
        return o == null ? null : (String) o;
    }

    public static long lng(Object o) {
        if (o instanceof Long) {
            return (Long) o;
        }
        return ((BigDecimal) o).longValue();
    }

    public static int integer(Object o) {
        return (int) lng(o);
    }

    public static Integer integerOrNull(Object o) {
        return o == null ? null : integer(o);
    }

    public static boolean bool(Object o) {
        return (Boolean) o;
    }

    public static BigDecimal decimal(Object o) {
        if (o instanceof Long) {
            return BigDecimal.valueOf((Long) o);
        }
        return (BigDecimal) o;
    }
}
