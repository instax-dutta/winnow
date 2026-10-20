package io.github.instaxdutta.winnow.mail;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Renders a template body for one recipient.
 *
 * <p>A placeholder is written as <code>{{Name}}</code>. Every substituted value is HTML
 * escaped. There is no raw mode: rendering operator-authored HTML raw, with
 * {@code th:utext}, lets a value in a spreadsheet inject markup into every message.
 */
@Component
public class MailTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    /**
     * Replaces every placeholder in {@code contentHtml} with the matching value.
     *
     * <p>A placeholder that has no value becomes an empty string. A dotted name is looked up
     * as the whole key first and then as a path into a nested map.
     *
     * @param contentHtml the template body
     * @param variables   the values for this recipient, may be null
     */
    public String render(String contentHtml, Map<String, String> variables) {
        if (contentHtml == null || contentHtml.isEmpty()) {
            return "";
        }
        Map<String, String> values = variables == null ? Map.of() : variables;
        Matcher matcher = PLACEHOLDER.matcher(contentHtml);
        StringBuilder rendered = new StringBuilder(contentHtml.length() + 64);
        int copied = 0;
        while (matcher.find()) {
            rendered.append(contentHtml, copied, matcher.start());
            rendered.append(escape(resolve(values, matcher.group(1))));
            copied = matcher.end();
        }
        rendered.append(contentHtml, copied, contentHtml.length());
        return rendered.toString();
    }

    /**
     * Escapes the five characters that can change the meaning of HTML.
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private String resolve(Map<String, String> values, String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        int dot = name.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        Object nested = values.get(name.substring(0, dot));
        if (!(nested instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = lookup(map, name.substring(dot + 1));
        return value == null ? null : String.valueOf(value);
    }

    private Object lookup(Map<?, ?> map, String path) {
        Object current = map;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(segment);
        }
        return current;
    }
}
