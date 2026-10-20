package io.github.instaxdutta.winnow.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.instaxdutta.winnow.config.ScraperProperties;
import io.github.instaxdutta.winnow.domain.ValidationException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The control that decides whether an outbound URL may be fetched.
 *
 * <p>This is the control that stops a spreadsheet cell from becoming a request to an
 * internal service, so the cases below are the address ranges that must never be
 * reachable and the host rules that must hold.
 *
 * <p>Every host in this test is either an address literal, which is resolved without a
 * name service, or {@code localhost}, which the resolver answers from the hosts file.
 * No test in this file reaches the network.
 */
class UrlAllowListTest {

    /**
     * A public address literal. An address literal is not a name, so it is resolved
     * without a name service and the test stays hermetic.
     */
    private static final String PUBLIC_ADDRESS = "93.184.216.34";

    /** A public address literal in IPv6. */
    private static final String PUBLIC_IPV6_ADDRESS = "[2606:4700:4700::1111]";

    /** The allow-list that permits any public host. */
    private static UrlAllowList anyPublicHost() {
        return new UrlAllowList(new ScraperProperties(
                "http://127.0.0.1:3000",
                Duration.ofSeconds(5),
                Duration.ofSeconds(15),
                true,
                List.of()));
    }

    /** The allow-list that permits only the hosts under one suffix. */
    private static UrlAllowList onlySuffix(String suffix) {
        return new UrlAllowList(new ScraperProperties(
                "http://127.0.0.1:3000",
                Duration.ofSeconds(5),
                Duration.ofSeconds(15),
                false,
                List.of(suffix)));
    }

    private static URL url(String value) {
        try {
            return new URL(value);
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Not a URL: " + value, e);
        }
    }

    @Test
    void aLoopbackAddressIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(url("http://127.0.0.1/secret")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("private or local address");
    }

    @Test
    void localhostIsRejectedBecauseItResolvesToTheLoopbackAddress() {
        assertThatThrownBy(() -> anyPublicHost().check(url("https://localhost/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("private or local address");
    }

    @Test
    void aPrivateAddressIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(url("http://10.0.0.1/")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> anyPublicHost().check(url("http://192.168.1.1/")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> anyPublicHost().check(url("http://172.16.0.1/")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void theLinkLocalMetadataAddressIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(url("http://169.254.169.254/latest/meta-data/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("private or local address");
    }

    @Test
    void aUniqueLocalIpv6AddressIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(url("http://[fd00::1]/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("private or local address");
    }

    @Test
    void aLinkLocalIpv6AddressIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(url("http://[fe80::1]/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("private or local address");
    }

    @Test
    void theIpv6LoopbackAddressIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(url("http://[::1]/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("private or local address");
    }

    @Test
    void anIpv4MappedLoopbackAddressIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(url("http://[::ffff:127.0.0.1]/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("private or local address");
    }

    @Test
    void aSchemeOtherThanHttpOrHttpsIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(url("file:///etc/passwd")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only http and https");
        assertThatThrownBy(() -> anyPublicHost().check(url("ftp://" + PUBLIC_ADDRESS + "/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only http and https");
    }

    @Test
    void aPublicHostIsAllowed() {
        assertThat(anyPublicHost().isAllowed(url("http://" + PUBLIC_ADDRESS + "/contact"))).isTrue();
        assertThat(anyPublicHost().isAllowed(url("https://" + PUBLIC_IPV6_ADDRESS + "/contact")))
                .isTrue();
    }

    @Test
    void aHostOutsideTheSuffixListIsRejected() {
        assertThatThrownBy(() -> onlySuffix("example.com").check(url("https://not-example.com/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not on the allow-list");
    }

    @Test
    void aPublicAddressIsRejectedWhenTheSuffixListIsInForce() {
        assertThatThrownBy(() -> onlySuffix("example.com").check(url("http://" + PUBLIC_ADDRESS + "/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not on the allow-list");
    }

    @Test
    void aHostOnTheSuffixListIsStillRejectedWhenItResolvesToALoopbackAddress() {
        // The suffix list is an operator restriction on top of the private range block.
        // The private range block is unconditional, so a permitted host that points at the
        // loopback interface is still refused.
        assertThatThrownBy(() -> onlySuffix("localhost").check(url("https://localhost/")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("private or local address");
    }

    @Test
    void aMissingUrlIsRejected() {
        assertThatThrownBy(() -> anyPublicHost().check(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void isAllowedNeverThrows() {
        assertThat(anyPublicHost().isAllowed(null)).isFalse();
        assertThat(anyPublicHost().isAllowed(url("http://127.0.0.1/"))).isFalse();
        assertThat(anyPublicHost().isAllowed(url("http://" + PUBLIC_ADDRESS + "/"))).isTrue();
    }
}
