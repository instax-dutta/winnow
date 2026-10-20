package io.github.instaxdutta.winnow.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The template body is rendered without escaping, so it has to be cleaned on write.
 *
 * <p>Without this, anybody who can edit a template can put a script tag into the preview
 * of the approvals page and run it in the reviewing administrator's browser, on the same
 * origin as the application.
 */
class HtmlSanitiserTest {

    private final HtmlSanitiser sanitiser = new HtmlSanitiser();

    @Test
    @DisplayName("a script tag is removed")
    void aScriptTagIsRemoved() {
        String cleaned = sanitiser.clean("<p>Hello</p><script>alert(1)</script>");

        assertThat(cleaned).doesNotContain("script").doesNotContain("alert(1)");
        assertThat(cleaned).contains("Hello");
    }

    @Test
    @DisplayName("an event handler attribute is removed")
    void anEventHandlerIsRemoved() {
        String cleaned = sanitiser.clean("<div onclick=\"steal()\">text</div>");

        assertThat(cleaned).doesNotContain("onclick").doesNotContain("steal()");
    }

    @Test
    @DisplayName("a javascript link is removed")
    void aJavascriptLinkIsRemoved() {
        String cleaned = sanitiser.clean("<a href=\"javascript:alert(1)\">click</a>");

        assertThat(cleaned).doesNotContain("javascript:");
    }

    @Test
    @DisplayName("an iframe is removed")
    void anIframeIsRemoved() {
        String cleaned = sanitiser.clean("<iframe src=\"https://evil.example/\"></iframe>");

        assertThat(cleaned).doesNotContain("iframe");
    }

    @Test
    @DisplayName("the formatting an email body needs is kept")
    void theFormattingIsKept() {
        String body = """
                <h2>Subject line</h2>
                <p>Hello <strong>there</strong>, here is <em>the detail</em>.</p>
                <ul><li>One</li><li>Two</li></ul>
                <a href="https://example.invalid/thing">a link</a>
                <img src="https://example.invalid/a.png" alt="an image">
                <table><tr><td>A cell</td></tr></table>
                """;

        String cleaned = sanitiser.clean(body);

        assertThat(cleaned)
                .contains("<strong>")
                .contains("<em>")
                .contains("<li>")
                .contains("https://example.invalid/thing")
                .contains("<img")
                .contains("<table");
    }

    @Test
    @DisplayName("an empty body is empty")
    void anEmptyBodyIsEmpty() {
        assertThat(sanitiser.clean(null)).isEmpty();
        assertThat(sanitiser.clean("   ")).isEmpty();
    }
}
