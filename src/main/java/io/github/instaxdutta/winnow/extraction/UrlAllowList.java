package io.github.instaxdutta.winnow.extraction;

import io.github.instaxdutta.winnow.config.ScraperProperties;
import io.github.instaxdutta.winnow.domain.ValidationException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Decides whether an outbound URL may be fetched.
 *
 * <p>Building the URL of a crawl straight from a spreadsheet cell and fetching it with no
 * check at all lets a cell become a request to any host on the network, including the
 * loopback interface and the link-local metadata address. This is the control that stops
 * it.
 *
 * <p>Three rules apply, in this order:
 *
 * <ol>
 *   <li>the scheme must be {@code https} or {@code http};
 *   <li>the host must be on the configured allow-list, unless arbitrary hosts are allowed;
 *   <li>every address the host resolves to must be a public address.
 * </ol>
 *
 * <p>The third rule resolves the name and checks every answer, so a public name that points
 * at {@code 127.0.0.1} is rejected.
 */
@Component
public class UrlAllowList {

    private static final Logger LOG = LoggerFactory.getLogger(UrlAllowList.class);

    private final ScraperProperties properties;

    public UrlAllowList(ScraperProperties properties) {
        this.properties = properties;
    }

    /**
     * Checks one URL.
     *
     * @throws ValidationException when the URL may not be fetched
     */
    public void check(URL url) {
        if (url == null) {
            throw new ValidationException("A URL is required");
        }
        String scheme = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new ValidationException("Only http and https URLs may be fetched");
        }
        String host = host(url);
        if (host.isEmpty()) {
            throw new ValidationException("The URL does not name a host");
        }
        if (!isHostPermitted(host)) {
            throw new ValidationException("The host '" + host + "' is not on the allow-list");
        }
        if (resolvesToLocalAddress(host)) {
            throw new ValidationException("The host '" + host + "' resolves to a private or local address");
        }
    }

    /**
     * Whether one URL may be fetched. Never throws.
     */
    public boolean isAllowed(URL url) {
        try {
            check(url);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    private String host(URL url) {
        String host = url.getHost();
        if (host == null) {
            return "";
        }
        host = host.toLowerCase(Locale.ROOT);
        // URL keeps the brackets of an IPv6 literal.
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        return host;
    }

    private boolean isHostPermitted(String host) {
        List<String> suffixes = properties.allowedHostSuffixes();
        if (suffixes.isEmpty()) {
            return properties.allowArbitraryHosts();
        }
        for (String suffix : suffixes) {
            String permitted = suffix.toLowerCase(Locale.ROOT);
            if (host.equals(permitted) || host.endsWith("." + permitted)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the host and checks every address it resolves to.
     *
     * <p>A host that does not resolve is treated as local: it cannot be fetched, so there
     * is no reason to let a request through.
     */
    private boolean resolvesToLocalAddress(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            LOG.warn("The host {} could not be resolved", host);
            return true;
        }
        if (addresses.length == 0) {
            return true;
        }
        for (InetAddress address : addresses) {
            if (isLocal(address)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLocal(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isLocalIpv4(bytes);
        }
        if (isIpv4Mapped(bytes)) {
            return isLocalIpv4(Arrays.copyOfRange(bytes, 12, 16));
        }
        if ((bytes[0] & 0xfe) == 0xfc) {
            return true; // fc00::/7, unique local
        }
        if (isAllZero(bytes)) {
            return true; // ::
        }
        if (isLoopbackIpv6(bytes)) {
            return true; // ::1
        }
        if ((bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xc0) == 0x80) {
            return true; // fe80::/10, link local
        }
        return address.isMulticastAddress();
    }

    private static boolean isLocalIpv4(byte[] bytes) {
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        if (first == 0) {
            return true; // 0.0.0.0/8
        }
        if (first == 10) {
            return true; // 10.0.0.0/8
        }
        if (first == 127) {
            return true; // 127.0.0.0/8
        }
        if (first == 172 && second >= 16 && second <= 31) {
            return true; // 172.16.0.0/12
        }
        if (first == 192 && second == 168) {
            return true; // 192.168.0.0/16
        }
        if (first == 169 && second == 254) {
            return true; // 169.254.0.0/16, which includes the metadata address
        }
        return first >= 224 && first <= 239; // 224.0.0.0/4, multicast
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        if (bytes.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return (bytes[10] & 0xff) == 0xff && (bytes[11] & 0xff) == 0xff;
    }

    private static boolean isAllZero(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLoopbackIpv6(byte[] bytes) {
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[bytes.length - 1] == 1;
    }
}
