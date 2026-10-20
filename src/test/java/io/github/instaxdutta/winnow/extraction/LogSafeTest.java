package io.github.instaxdutta.winnow.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A value that reaches a log from outside the system must not be able to forge an entry.
 */
class LogSafeTest {

    @Test
    @DisplayName("a newline cannot split one log entry into two")
    void aNewlineCannotSplitTheEntry() {
        String safe = LogSafe.text("Guest\nUser: admin");

        assertThat(safe).doesNotContain("\n").doesNotContain("\r");
        assertThat(safe).contains("Guest User: admin");
    }

    @Test
    @DisplayName("a carriage return cannot split the entry either")
    void aCarriageReturnCannotSplitTheEntry() {
        assertThat(LogSafe.text("one\rtwo")).doesNotContain("\r").contains("one two");
    }

    @Test
    @DisplayName("a control character is replaced rather than written")
    void aControlCharacterIsReplaced() {
        assertThat(LogSafe.text("a\u0007b")).doesNotContain("\u0007").contains("a?b");
    }

    @Test
    @DisplayName("an escape sequence cannot reach the log raw")
    void anEscapeSequenceIsNeutralised() {
        String safe = LogSafe.text("\u001b[31mred\u001b[0m");

        assertThat(safe).doesNotContain("\u001b");
    }

    @Test
    @DisplayName("a long value is cut to a length a human can read")
    void aLongValueIsCut() {
        String safe = LogSafe.text("x".repeat(500));

        assertThat(safe.length()).isLessThan(200);
        assertThat(safe).endsWith("...");
    }

    @Test
    @DisplayName("a normal value is left readable")
    void aNormalValueIsLeftReadable() {
        assertThat(LogSafe.text("www.amazon.in")).isEqualTo("www.amazon.in");
    }

    @Test
    @DisplayName("a null value is empty rather than a failure")
    void aNullValueIsEmpty() {
        assertThat(LogSafe.text(null)).isEmpty();
    }
}
