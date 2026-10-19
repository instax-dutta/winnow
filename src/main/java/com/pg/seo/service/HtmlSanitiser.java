package com.pg.seo.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Removes the active content from operator and inbound HTML.
 *
 * <p>Two fields in this application carry HTML that is rendered without escaping: the body
 * of an email template and the body of an inbound message. Both are sanitised on write, so
 * that a script tag in either of them cannot reach the browser of a reviewer.
 *
 * <p>The safelist keeps formatting and links, and drops scripts, styles, event handlers,
 * frames, objects and embedded content.
 */
@Component
public class HtmlSanitiser {

    /**
     * The allowed markup.
     *
     * <p>{@code Safelist.basic()} keeps the usual formatting tags and links, and adds the
     * images and the tables that an email body needs. Everything else is dropped.
     */
    private static final Safelist ALLOWED = Safelist.basic()
            .addTags("img", "table", "thead", "tbody", "tr", "td", "th", "div", "span", "hr", "br")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("td", "colspan", "rowspan")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https", "mailto")
            .preserveRelativeLinks(true);

    /**
     * Returns the HTML with everything active removed.
     *
     * @param html the HTML to clean, may be null
     * @return the cleaned HTML, never null
     */
    public String clean(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, "", ALLOWED, new OutputSettings().prettyPrint(false));
    }
}
