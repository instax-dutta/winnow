package com.pg.seo.extraction;

/**
 * One product the Amazon sidecar returned.
 *
 * <p>There is no price as a number: the sidecar returns whatever it read from the page, and
 * a numeric price and a formatted price are both legitimate answers.
 */
public record ScrapedProduct(
        String title,
        String url,
        String image,
        String price,
        String soldBy,
        String fulfilledBy) {}
