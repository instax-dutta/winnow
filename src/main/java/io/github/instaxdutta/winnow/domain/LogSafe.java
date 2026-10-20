package io.github.instaxdutta.winnow.domain;

/**
 * Makes a value safe to write into a log line.
 *
 * <p>A newline in a value the operator supplied would split one log entry into two, and a
 * terminal escape sequence could make a log viewer do something unexpected. Every value
 * that reaches a log from outside the system goes through here first.
 *
 * <p>The value is reduced to printable characters and cut to a length a human can read.
 * Nothing that matters for debugging is lost: the shape of a domain or a keyword survives.
 */
public final class LogSafe {

    /** The longest value written to a log. */
    private static final int MAX_LENGTH = 120;

    private LogSafe() {}

    /**
     * Returns a version of the value that cannot forge a log entry.
     *
     * @param value the value, may be null
     * @return a printable value, never null
     */
    public static String text(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(Math.min(value.length(), MAX_LENGTH));
        for (int i = 0; i < value.length() && out.length() < MAX_LENGTH; i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                out.append(' ');
            } else if (Character.isISOControl(c)) {
                out.append('?');
            } else {
                out.append(c);
            }
        }
        if (value.length() > MAX_LENGTH) {
            out.append("...");
        }
        return out.toString();
    }
}
