package io.github.instaxdutta.winnow.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The template renderer.
 *
 * <p>Every substituted value is HTML escaped, because rendering the operator authored HTML
 * raw lets a value that came from a spreadsheet inject markup into every message that goes
 * out.
 */
class MailTemplateRendererTest {

    private final MailTemplateRenderer renderer = new MailTemplateRenderer();

    @Test
    void aPlaceholderIsReplacedByItsValue() {
        String rendered = renderer.render("<p>Hello {{Name}}</p>", Map.of("Name", "Ada"));

        assertThat(rendered).isEqualTo("<p>Hello Ada</p>");
    }

    @Test
    void aValueThatCarriesMarkupIsEscaped() {
        String rendered = renderer.render(
                "<p>Hello {{Name}}</p>",
                Map.of("Name", "<script>alert('x')</script>"));

        assertThat(rendered).doesNotContain("<script>");
        assertThat(rendered).contains("&lt;script&gt;");
        assertThat(rendered).isEqualTo("<p>Hello &lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;</p>");
    }

    @Test
    void aValueThatIsNotAPlaceholderIsLeftAlone() {
        String rendered = renderer.render(
                "<p>Hello {{Name}}, this text is not a placeholder</p>",
                Map.of("Name", "Ada"));

        assertThat(rendered).isEqualTo("<p>Hello Ada, this text is not a placeholder</p>");
    }

    @Test
    void theFiveHtmlCharactersOfAValueAreEscaped() {
        String rendered = renderer.render("{{Value}}", Map.of("Value", "&<>\"'"));

        assertThat(rendered).isEqualTo("&amp;&lt;&gt;&quot;&#39;");
    }

    @Test
    void aPlaceholderWithoutAValueBecomesAnEmptyString() {
        assertThat(renderer.render("[{{Missing}}]", Map.of())).isEqualTo("[]");
    }

    @Test
    void aPlaceholderWithSpacesAroundTheNameIsFilled() {
        assertThat(renderer.render("[{{ Name }}]", Map.of("Name", "Ada"))).isEqualTo("[Ada]");
    }

    @Test
    void aDottedPlaceholderWhosePrefixIsNotAMapIsEmpty() {
        // The renderer looks a dotted name up in a nested map. The value map is typed as
        // Map<String, String>, so a dotted name whose prefix is a plain value resolves to
        // nothing.
        assertThat(renderer.render("{{company.name}}", Map.of("company", "Example Ltd"))).isEmpty();
    }

    @Test
    void aPlaceholderIsMatchedCaseSensitively() {
        assertThat(renderer.render("{{Name}}", Map.of("name", "Ada"))).isEmpty();
    }

    @Test
    void anEmptyTemplateRendersAsAnEmptyString() {
        assertThat(renderer.render("", Map.of("Name", "Ada"))).isEmpty();
        assertThat(renderer.render(null, Map.of("Name", "Ada"))).isEmpty();
    }

    @Test
    void aMissingValueMapRendersThePlaceholdersAsEmptyStrings() {
        assertThat(renderer.render("[{{Name}}]", null)).isEqualTo("[]");
    }

    @Test
    void theEscaperLeavesOrdinaryTextAlone() {
        assertThat(MailTemplateRenderer.escape("Plain text 123")).isEqualTo("Plain text 123");
        assertThat(MailTemplateRenderer.escape(null)).isEmpty();
    }
}
