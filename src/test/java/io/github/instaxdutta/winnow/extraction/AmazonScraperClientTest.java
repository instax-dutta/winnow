package io.github.instaxdutta.winnow.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.instaxdutta.winnow.config.ScraperProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Java half of the sidecar contract.
 *
 * <p>The sidecar is a separate service, so the two halves can drift apart. This test fixes
 * the shape the Java side expects: a JSON object with a {@code products} array, each
 * product carrying the canonical field names, and the alternative names the sidecar may
 * also use.
 *
 * <p>It also proves the two things that matter for safety. The keyword travels as a query
 * parameter and never becomes part of the host or the path, and the base URL is the only
 * thing that decides where the request goes.
 */
class AmazonScraperClientTest {

    private HttpServer server;
    private AmazonScraperClient client;
    private AtomicReference<String> lastQuery;

    @BeforeEach
    void startServer() throws IOException {
        lastQuery = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/scrape", exchange -> {
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            String body = """
                    {
                      "keyword": "usb c hub",
                      "page": 1,
                      "productCount": 2,
                      "products": [
                        {
                          "title": "A hub",
                          "url": "https://www.amazon.in/dp/B000000001",
                          "image": "https://example.invalid/a.jpg",
                          "price": "499",
                          "soldBy": "Sold by Example",
                          "fulfilledBy": "Amazon"
                        },
                        {
                          "title": "Another hub",
                          "link": "https://www.amazon.in/dp/B000000002",
                          "imageUrl": "https://example.invalid/b.jpg",
                          "sold_by": "Sold by Another"
                        }
                      ]
                    }
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        client = new AmazonScraperClient(
                new ScraperProperties("http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2), Duration.ofSeconds(5), false, List.of()));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("the products the sidecar returned are read into the model")
    void theProductsAreRead() {
        List<ScrapedProduct> products = client.scrape("usb c hub", 1);

        assertThat(products).hasSize(2);
        assertThat(products.get(0).title()).isEqualTo("A hub");
        assertThat(products.get(0).url()).isEqualTo("https://www.amazon.in/dp/B000000001");
        assertThat(products.get(0).image()).isEqualTo("https://example.invalid/a.jpg");
        assertThat(products.get(0).price()).isEqualTo("499");
        assertThat(products.get(0).soldBy()).isEqualTo("Sold by Example");
        assertThat(products.get(0).fulfilledBy()).isEqualTo("Amazon");
    }

    @Test
    @DisplayName("the alternative field names the sidecar may use are also read")
    void theAlternativeFieldNamesAreRead() {
        List<ScrapedProduct> products = client.scrape("usb c hub", 1);

        assertThat(products.get(1).url()).isEqualTo("https://www.amazon.in/dp/B000000002");
        assertThat(products.get(1).image()).isEqualTo("https://example.invalid/b.jpg");
        assertThat(products.get(1).soldBy()).isEqualTo("Sold by Another");
    }

    @Test
    @DisplayName("a field the sidecar did not send is null rather than an error")
    void aMissingFieldIsNull() {
        List<ScrapedProduct> products = client.scrape("usb c hub", 1);

        assertThat(products.get(1).price()).isNull();
        assertThat(products.get(1).fulfilledBy()).isNull();
    }

    @Test
    @DisplayName("the keyword travels as a query parameter and never as a host or a path")
    void theKeywordIsOnlyEverAQueryParameter() {
        client.scrape("usb c hub", 3);

        String query = lastQuery.get();
        assertThat(query).isNotNull();
        assertThat(query).contains("keyword=usb+c+hub").contains("page=3");
        assertThat(query).doesNotContain("http").doesNotContain("/");
    }

    @Test
    @DisplayName("a keyword that looks like a URL is still only a search term")
    void aKeywordThatLooksLikeAUrlIsStillASearchTerm() {
        client.scrape("https://evil.example/", 1);

        assertThat(lastQuery.get()).contains("keyword=https%3A%2F%2Fevil.example%2F");
    }

    @Test
    @DisplayName("a blank keyword is not sent at all")
    void aBlankKeywordIsNotSent() {
        assertThat(client.scrape("  ", 1)).isEmpty();
        assertThat(client.scrape(null, 1)).isEmpty();
        assertThat(lastQuery.get()).isNull();
    }
}
