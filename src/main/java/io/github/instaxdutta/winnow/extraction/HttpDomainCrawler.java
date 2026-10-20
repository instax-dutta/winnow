package io.github.instaxdutta.winnow.extraction;

import io.github.instaxdutta.winnow.config.ExtractionProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fetches a domain and reads the addresses it publishes.
 *
 * <p>The request is built from the domain alone, it goes through {@link UrlAllowList} before
 * it is sent, and it has an explicit connect timeout, an explicit read timeout and a cap on
 * the number of bytes it reads. Redirects are followed by this class and not by the HTTP
 * client, so that every hop is checked against the same rules.
 *
 * <p>A domain that cannot be fetched yields an empty list. It never raises: one broken
 * domain must not fail a whole run.
 */
@Component
public class HttpDomainCrawler implements DomainCrawler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpDomainCrawler.class);

    /** A conservative address pattern. It never matches a name with a space or a quote. */
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(?:\\.[A-Za-z0-9\\-]+)+");

    /** The most addresses one domain may contribute. */
    private static final int MAX_ADDRESSES_PER_DOMAIN = 500;

    private final UrlAllowList allowList;
    private final ExtractionProperties properties;
    private final HttpClient client;

    public HttpDomainCrawler(UrlAllowList allowList, ExtractionProperties properties) {
        this.allowList = allowList;
        this.properties = properties;
        this.client = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public List<String> findEmails(String domain) {
        String host = normalise(domain);
        if (host == null) {
            return List.of();
        }
        try {
            String body = fetch(host);
            return extract(host, body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("The crawl of {} was interrupted", LogSafe.text(host));
            return List.of();
        } catch (IOException | RuntimeException e) {
            LOG.warn("The crawl of {} failed ({})", LogSafe.text(host), e.getClass().getSimpleName());
            return List.of();
        }
    }

    /**
     * Fetches the page and follows the redirects by hand, checking every hop.
     */
    private String fetch(String host) throws IOException, InterruptedException {
        URI target = URI.create("https://" + host + "/");
        int redirects = 0;
        while (true) {
            allowList.check(target.toURL());
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(properties.readTimeout())
                    .header("User-Agent", "seo-extraction/1.0")
                    .header("Accept", "text/html,text/plain;q=0.9")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("Location").orElse(null);
                response.body().close();
                if (location == null || location.isBlank()) {
                    throw new IOException("the redirect carried no location");
                }
                if (redirects >= properties.maxRedirects()) {
                    throw new IOException("the redirect chain was longer than " + properties.maxRedirects());
                }
                redirects++;
                target = resolve(target, location);
                continue;
            }
            try (InputStream body = response.body()) {
                byte[] bytes = readAtMost(body, properties.maxResponseBytes());
                if (status >= 400) {
                    return "";
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }

    private URI resolve(URI base, String location) throws IOException {
        try {
            return base.resolve(location);
        } catch (IllegalArgumentException e) {
            throw new IOException("the redirect carried an unusable location");
        }
    }

    /**
     * Reads at most {@code limit} bytes. The response is never read into memory whole.
     */
    private static byte[] readAtMost(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(Math.max(limit, 1024), 64 * 1024));
        byte[] buffer = new byte[8192];
        int total = 0;
        while (total < limit) {
            int read = in.read(buffer, 0, Math.min(buffer.length, limit - total));
            if (read < 0) {
                break;
            }
            out.write(buffer, 0, read);
            total += read;
        }
        return out.toByteArray();
    }

    /**
     * Reads the addresses out of the page and keeps only the ones that belong to the domain
     * that was crawled.
     */
    private static List<String> extract(String host, String body) {
        if (body == null || body.isEmpty()) {
            return List.of();
        }
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = EMAIL.matcher(body);
        while (matcher.find()) {
            String address = matcher.group().toLowerCase(Locale.ROOT);
            if (address.length() > 254) {
                continue;
            }
            int at = address.lastIndexOf('@');
            if (at < 0) {
                continue;
            }
            String mailDomain = address.substring(at + 1);
            if (!mailDomain.equals(host) && !mailDomain.endsWith("." + host)) {
                continue;
            }
            found.add(address);
            if (found.size() >= MAX_ADDRESSES_PER_DOMAIN) {
                break;
            }
        }
        return List.copyOf(found);
    }

    /**
     * Reduces a cell value to a bare host. A value that is not a host yields null.
     */
    private static String normalise(String value) {
        if (value == null) {
            return null;
        }
        String host = value.trim().toLowerCase(Locale.ROOT);
        int scheme = host.indexOf("://");
        if (scheme >= 0) {
            host = host.substring(scheme + 3);
        }
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.isEmpty() || !host.matches("[a-z0-9]([a-z0-9\\-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9\\-]*[a-z0-9])?)+")) {
            return null;
        }
        return host;
    }
}
