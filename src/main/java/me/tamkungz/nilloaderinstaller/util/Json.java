package me.tamkungz.nilloaderinstaller.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small dependency-free JSON reader/writer for launcher configuration files. */
public final class Json {
    private Json() {}

    public static Object read(Path path) throws IOException {
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    public static Object parse(String input) {
        return new Parser(input).parse();
    }

    public static void write(Path path, Object value) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, stringify(value) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        return (List<Object>) list;
    }

    public static String string(Object value, String fallback) {
        return value instanceof String s ? s : fallback;
    }

    public static Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), deepCopy(e.getValue()));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object item : list) out.add(deepCopy(item));
            return out;
        }
        return value;
    }

    private static void writeValue(StringBuilder sb, Object value, int depth) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            if (!map.isEmpty()) sb.append('\n');
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                indent(sb, depth + 1);
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(": ");
                writeValue(sb, e.getValue(), depth + 1);
                if (++i < map.size()) sb.append(',');
                sb.append('\n');
            }
            if (!map.isEmpty()) indent(sb, depth);
            sb.append('}');
        } else if (value instanceof List<?> list) {
            sb.append('[');
            if (!list.isEmpty()) sb.append('\n');
            for (int i = 0; i < list.size(); i++) {
                indent(sb, depth + 1);
                writeValue(sb, list.get(i), depth + 1);
                if (i + 1 < list.size()) sb.append(',');
                sb.append('\n');
            }
            if (!list.isEmpty()) indent(sb, depth);
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
        }
    }

    private static void indent(StringBuilder sb, int depth) {
        sb.append("  ".repeat(Math.max(0, depth)));
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) { this.s = s; }

        Object parse() {
            skipWs();
            Object value = readValue();
            skipWs();
            if (pos != s.length()) error("Trailing characters");
            return value;
        }

        private Object readValue() {
            skipWs();
            if (pos >= s.length()) error("Unexpected end of input");
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> { expect("true"); yield Boolean.TRUE; }
                case 'f' -> { expect("false"); yield Boolean.FALSE; }
                case 'n' -> { expect("null"); yield null; }
                default -> {
                    if (c == '-' || Character.isDigit(c)) yield readNumber();
                    error("Unexpected character '" + c + "'");
                    yield null;
                }
            };
        }

        private Map<String, Object> readObject() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            pos++;
            skipWs();
            if (peek('}')) { pos++; return map; }
            while (true) {
                skipWs();
                if (!peek('"')) error("Expected string key");
                String key = readString();
                skipWs();
                require(':');
                Object value = readValue();
                map.put(key, value);
                skipWs();
                if (peek('}')) { pos++; return map; }
                require(',');
            }
        }

        private List<Object> readArray() {
            ArrayList<Object> list = new ArrayList<>();
            pos++;
            skipWs();
            if (peek(']')) { pos++; return list; }
            while (true) {
                list.add(readValue());
                skipWs();
                if (peek(']')) { pos++; return list; }
                require(',');
            }
        }

        private String readString() {
            require('"');
            StringBuilder out = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') return out.toString();
                if (c == '\\') {
                    if (pos >= s.length()) error("Unterminated escape");
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (pos + 4 > s.length()) error("Bad unicode escape");
                            String hex = s.substring(pos, pos + 4);
                            try { out.append((char) Integer.parseInt(hex, 16)); }
                            catch (NumberFormatException ex) { error("Bad unicode escape"); }
                            pos += 4;
                        }
                        default -> error("Bad escape: \\" + e);
                    }
                } else {
                    out.append(c);
                }
            }
            error("Unterminated string");
            return null;
        }

        private Number readNumber() {
            int start = pos;
            if (peek('-')) pos++;
            readDigits();
            boolean floating = false;
            if (peek('.')) {
                floating = true;
                pos++;
                readDigits();
            }
            if (peek('e') || peek('E')) {
                floating = true;
                pos++;
                if (peek('+') || peek('-')) pos++;
                readDigits();
            }
            String raw = s.substring(start, pos);
            try {
                if (floating) return Double.parseDouble(raw);
                long n = Long.parseLong(raw);
                return n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE ? (int) n : n;
            } catch (NumberFormatException e) {
                error("Bad number: " + raw);
                return null;
            }
        }

        private void readDigits() {
            int start = pos;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
            if (start == pos) error("Expected digit");
        }

        private void expect(String literal) {
            if (!s.startsWith(literal, pos)) error("Expected " + literal);
            pos += literal.length();
        }

        private void require(char c) {
            skipWs();
            if (!peek(c)) error("Expected '" + c + "'");
            pos++;
        }

        private boolean peek(char c) {
            return pos < s.length() && s.charAt(pos) == c;
        }

        private void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        private void error(String message) {
            throw new IllegalArgumentException(message + " at offset " + pos);
        }
    }
}
