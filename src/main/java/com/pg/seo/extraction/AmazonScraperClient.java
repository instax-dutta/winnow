package com.pg.seo.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.seo.config.ScraperProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Talks to the Node sidecar that renders the Amazon search page.
 *
 * <p>The request always goes to the configured base URL. The keyword travels in a query
 * parameter, never in the host or the path, so a keyword can never move the request to
 * another host. The original bound the sidecar to every interface with a wildcard CORS
 * header and no authentication, which turned it into an open proxy; that is a property of
 * the sidecar and is not repeated here.
 *
 * <p>The sidecar answers with either a JSON array of products or an object with a
 * {@code products} array. Each product carries a title, a link, an image, a price and the
 * two seller fields.
 */
@Component
public class AmazonScraperClient {

    private static final Logger LOG = LoggerFactory.getLogger(AmazonScraperClient.class);

    /** The most products one page may contribute. */
    private static final int MAX_PRODUCTS = 200;

    private final ScraperProperties properties;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public AmazonScraperClient(ScraperProperties properties) {
        this.properties = properties;
        this.client = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Reads one page of results.
     *
     * @param keyword what to search for
     * @param page    the page number, one based
     * @return the products, an empty list when the sidecar could not be reached
     */
    public List<ScrapedProduct> scrape(String keyword, int page) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        URI target;
        try {
            target = target(keyword, page);
        } catch (RuntimeException e) {
            LOG.error("The configured scraper base URL is not usable ({})", e.getClass().getSimpleName());
            return List.of();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(properties.readTimeout())
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                LOG.warn("The scraper sidecar answered {}", response.statusCode());
                return List.of();
            }
            return parse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("The scrape of '{}' was interrupted", keyword);
            return List.of();
        } catch (IOException | RuntimeException e) {
            LOG.warn("The scrape of '{}' failed ({})", keyword, e.getClass().getSimpleName());
            return List.of();
        }
    }

    /**
     * Builds the request URI.
     *
     * <p>The scheme, the host, the port and the path come from the configuration. Only the
     * query is built from the caller's input, and it is encoded.
     */
    private URI target(String keyword, int page) {
        URI base = URI.create(properties.baseUrl());
        String scheme = base.getScheme() == null ? "" : base.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalStateException("the base URL is not an http or https URL");
        }
        if (base.getHost() == null || base.getHost().isBlank()) {
            throw new IllegalStateException("the base URL has no host");
        }
        if (base.getUserInfo() != null) {
            throw new IllegalStateException("the base URL must not carry credentials");
        }
        String authority = base.getHost() + (base.getPort() < 0 ? "" : ":" + base.getPort());
        String path = base.getPath() == null ? "" : base.getPath();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        String query = "keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&page=" + Math.max(page, 1);
        return URI.create(scheme + "://" + authority + path + "scrape?" + query);
    }

    private List<ScrapedProduct> parse(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode array = root.isArray() ? root : root.path("products");
            if (!array.isArray()) {
                return List.of();
            }
            List<ScrapedProduct> products = new ArrayList<>();
            for (JsonNode node : array) {
                if (products.size() >= MAX_PRODUCTS) {
                    break;
                }
                products.add(new ScrapedProduct(
                        text(node, "title"),
                        text(node, "url", "link"),
                        text(node, "image", "imageUrl", "image_url"),
                        text(node, "price"),
                        text(node, "soldBy", "sold_by"),
                        text(node, "fulfilledBy", "fulfilled_by")));
            }
            return List.copyOf(products);
        } catch (IOException e) {
            LOG.warn("The scraper sidecar answered something that is not JSON");
            return List.of();
        }
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }
}
