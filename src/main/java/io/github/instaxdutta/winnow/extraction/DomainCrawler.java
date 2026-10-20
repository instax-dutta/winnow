package io.github.instaxdutta.winnow.extraction;

import java.util.List;

/**
 * Finds the addresses that are published on one domain.
 *
 * <p>The extraction package implements this. The service layer only knows the contract, so
 * the crawler owns the outbound request, the allow-list of hosts and the timeout.
 */
public interface DomainCrawler {

    /**
     * The addresses found on one domain.
     *
     * @param domain the domain, without a scheme and without a path
     * @return the addresses, an empty list when the domain publishes none
     */
    List<String> findEmails(String domain);
}
