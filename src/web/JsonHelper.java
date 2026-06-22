package asrama.web;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonHelper {

    private JsonHelper() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    public static String object(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue() == null ? "" : entry.getValue())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return map;
        }

        String body = json.trim();
        if (!body.startsWith("{") || !body.endsWith("}")) {
            return map;
        }
        body = body.substring(1, body.length() - 1);

        int i = 0;
        while (i < body.length()) {
            while (i < body.length() && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) {
                i++;
            }
            if (i >= body.length()) {
                break;
            }
            if (body.charAt(i) != '"') {
                break;
            }

            i++;
            int keyStart = i;
            while (i < body.length() && body.charAt(i) != '"') {
                i++;
            }
            String key = body.substring(keyStart, i);
            i++;

            while (i < body.length() && body.charAt(i) != ':') {
                i++;
            }
            i++;
            while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
                i++;
            }

            if (i < body.length() && body.charAt(i) == '"') {
                i++;
                StringBuilder valueBuilder = new StringBuilder();
                while (i < body.length()) {
                    char c = body.charAt(i);
                    if (c == '\\' && i + 1 < body.length()) {
                        valueBuilder.append(body.charAt(++i));
                    } else if (c == '"') {
                        i++;
                        break;
                    } else {
                        valueBuilder.append(c);
                    }
                    i++;
                }
                map.put(key, valueBuilder.toString());
            } else {
                int valueStart = i;
                while (i < body.length() && body.charAt(i) != ',') {
                    i++;
                }
                map.put(key, body.substring(valueStart, i).trim());
            }
        }
        return map;
    }

    public static String get(Map<String, String> map, String key) {
        return map.getOrDefault(key, "");
    }
}
