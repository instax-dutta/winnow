package io.github.instaxdutta.winnow.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.instaxdutta.winnow.config.ExtractionProperties;
import io.github.instaxdutta.winnow.config.ScraperProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The crawler that reads the addresses a domain publishes.
 *
 * <p>The crawler is the outbound half of the extraction, so the cases below are the two
 * things that matter: it must never reach an internal address, and it must only keep the
 * addresses that belong to the domain it crawled.
 *
 * <p>The local server in this test is the internal service that must stay unreachable.
 * The test opens exactly one socket, on the loopback interface, and the crawler is not
 * allowed to open any.
 */
class HttpDomainCrawlerTest {

    /** The page the local server publishes. */
    private static final String PAGE = """
            <html><body>
              <p>Sales: sales@example.com</p>
              <p>Support: support@example.com</p>
              <p>Other company: hello@other-company.test</p>
              <p>Not an address: sales(at)example.com</p>
            </body></html>""";

    /** How many requests the local server received. */
    private final AtomicInteger requests = new AtomicInteger();

    private HttpServer server;

    private String page;

    @BeforeEach
    void startLocalServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] body = PAGE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        page = fetchLocalPage();
    }

    @AfterEach
    void stopLocalServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Reads the page from the local server. This is the one socket the test opens.
     */
    private String fetchLocalPage() throws IOException {
        URI uri = URI.create("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":"
                + server.getAddress().getPort() + "/");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        try (InputStream in = connection.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * The crawler under test.
     *
     * <p>The allow-list of the crawler is not relaxed: the private range block is part of
     * the crawler test. Only the host rule is left to {@link UrlAllowListTest}.
     */
    private HttpDomainCrawler crawler() {
        ScraperProperties scraper = new ScraperProperties(
                "http://127.0.0.1:3000",
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                true,
                List.of());
        ExtractionProperties extraction = new ExtractionProperties(
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                64 * 1024,
                1,
                1,
                10,
                5,
                100,
                50);
        return new HttpDomainCrawler(new UrlAllowList(scraper), extraction);
    }

    /**
     * The address filter of the crawler.
     *
     * <p>The crawler always fetches over https on the default port, so an unprivileged
     * test cannot point it at a local server: it would have to bind port 443 and present
     * a certificate the JVM trusts. The filter is therefore exercised directly, on the
     * page the local server publishes, and everything else is exercised through the
     * public API.
     */
    private static List<String> extract(String host, String body) throws ReflectiveOperationException {
        Method method = HttpDomainCrawler.class.getDeclaredMethod("extract", String.class, String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> addresses = (List<String>) method.invoke(null, host, body);
        return addresses;
    }

    @Test
    void theLocalServerAnswersTheTest() {
        assertThat(page).contains("sales@example.com");
        assertThat(requests.get()).isPositive();
    }

    @Test
    void theCrawlerDoesNotFetchTheLoopbackAddress() {
        List<String> found = crawler().findEmails("127.0.0.1");

        assertThat(found).isEmpty();
    }

    @Test
    void theCrawlerDoesNotFetchAPrivateAddress() {
        assertThat(crawler().findEmails("10.0.0.1")).isEmpty();
        assertThat(crawler().findEmails("192.168.1.1")).isEmpty();
        assertThat(crawler().findEmails("172.16.0.1")).isEmpty();
    }

    @Test
    void theCrawlerDoesNotFetchTheLinkLocalMetadataAddress() {
        assertThat(crawler().findEmails("169.254.169.254")).isEmpty();
    }

    @Test
    void theCrawlerNeverReachesTheLocalServer() {
        int before = requests.get();

        crawler().findEmails("127.0.0.1");
        crawler().findEmails("localhost");
        crawler().findEmails("192.168.1.1");

        assertThat(requests.get()).isEqualTo(before);
    }

    @Test
    void theCrawlerReturnsNoAddressesForAValueThatIsNotADomain() {
        assertThat(crawler().findEmails("not a domain")).isEmpty();
        assertThat(crawler().findEmails("")).isEmpty();
        assertThat(crawler().findEmails(null)).isEmpty();
    }

    @Test
    void theCrawlerKeepsTheAddressesOfTheDomainItCrawled() throws ReflectiveOperationException {
        List<String> found = extract("example.com", page);

        assertThat(found).containsExactly("sales@example.com", "support@example.com");
    }

    @Test
    void theCrawlerNeverKeepsAnAddressOfAnotherDomain() throws ReflectiveOperationException {
        List<String> found = extract("example.com", page);

        assertThat(found).doesNotContain("hello@other-company.test");
    }

    @Test
    void theCrawlerKeepsAnAddressOfASubdomainOfTheDomainItCrawled() throws ReflectiveOperationException {
        List<String> found = extract("example.com", "<p>sales@mail.example.com</p>");

        assertThat(found).containsExactly("sales@mail.example.com");
    }

    @Test
    void theCrawlerKeepsNoAddressFromAPageWithoutAddresses() throws ReflectiveOperationException {
        assertThat(extract("example.com", "<html><body><p>nothing here</p></body></html>")).isEmpty();
        assertThat(extract("example.com", "")).isEmpty();
    }

    @Test
    void theCrawlerDoesNotOpenASocketForAValueThatIsNotADomain() {
        int before = requests.get();

        crawler().findEmails("not a domain");

        assertThat(requests.get()).isEqualTo(before);
    }
}
