"use strict";

/**
 * The Amazon search results reader.
 *
 * The reader is handed a browser and a keyword, and returns the products it found. It
 * never builds a URL from anything the caller sent: the search URL is built here, from a
 * fixed origin and the keyword, so a caller cannot make the browser visit anywhere it
 * chooses.
 *
 * Amazon changes its markup without notice, so every field is read on its own. A title
 * that cannot be read costs the title, not the product.
 */

/**
 * Builds the search URL for one page of results.
 *
 * The origin is the configured one. The keyword only ever lands in the query string,
 * percent-encoded, so a keyword that looks like a URL is still a search term.
 *
 * @param {string} base the configured Amazon origin
 * @param {string} keyword the search term
 * @param {number} page the page number, one based
 * @returns {string} the URL to visit
 */
function searchUrl(base, keyword, page) {
  const url = new URL("/s", base.endsWith("/") ? base : `${base}/`);
  url.searchParams.set("k", keyword);
  url.searchParams.set("page", String(page));
  return url.toString();
}

/**
 * Reads the products off the page. Runs inside the browser, so it carries everything it
 * needs and reaches for nothing outside.
 *
 * @param {number} limit the most products to return
 * @returns {Array<object>} the raw fields of every result on the page
 */
function readResults(limit) {
  const clean = (value) => {
    if (typeof value !== "string") return null;
    const trimmed = value.replace(/\s+/g, " ").trim();
    return trimmed === "" ? null : trimmed;
  };

  const firstText = (root, selectors) => {
    for (const selector of selectors) {
      try {
        const element = root.querySelector(selector);
        if (!element) continue;
        const value = clean(element.textContent);
        if (value) return value;
      } catch (error) {
        // A selector Amazon has changed must not cost the whole product.
      }
    }
    return null;
  };

  const firstAttribute = (root, selectors, attribute) => {
    for (const selector of selectors) {
      try {
        const element = root.querySelector(selector);
        if (!element) continue;
        const value = clean(element.getAttribute(attribute));
        if (value) return value;
      } catch (error) {
        // As above.
      }
    }
    return null;
  };

  // The seller sits either in the same element as the word "Sold by" or in the element
  // right after it. The innermost elements are tried first, so a wrapper that carries
  // more text does not win.
  const sellerOf = (root) => {
    const candidates = [
      ...root.querySelectorAll("span"),
      ...root.querySelectorAll("a"),
      ...root.querySelectorAll("div")
    ];
    for (const node of candidates) {
      const own = clean(node.textContent);
      if (!own || !/^sold by\b/i.test(own)) continue;
      const inline = /^sold by\s+(.+)$/i.exec(own);
      if (inline) return clean(inline[1]);
      const sibling = node.nextElementSibling;
      if (sibling) {
        const value = clean(sibling.textContent);
        if (value) return value;
      }
    }
    return null;
  };

  const priceOf = (root) => {
    const whole = firstText(root, ["span.a-price-whole"]);
    const offscreen = firstText(root, ["span.a-price span.a-offscreen"]);
    if (offscreen) return offscreen;
    if (!whole) return null;
    const symbol = firstText(root, ["span.a-price-symbol"]);
    const fraction = firstText(root, ["span.a-price-fraction"]);
    return [symbol, whole, fraction ? `.${fraction}` : null].filter(Boolean).join("");
  };

  // Only the containers that carry the search result marker. A page holds other elements
  // with a data-asin as well: the ad slot, the sponsored carousel and the face out that
  // holds the add to cart button. They are not results.
  const containers = Array.from(
    document.querySelectorAll(
      'div[data-component-type="s-search-result"][data-asin], div.s-result-item.s-asin[data-asin]'
    )
  );

  const products = [];
  for (const container of containers) {
    if (products.length >= limit) break;
    try {
      const text = container.textContent || "";
      products.push({
        asin: clean(container.getAttribute("data-asin")),
        title: firstText(container, [
          "h2 span",
          "h2 a span",
          "h2",
          "[data-cy=title-recipe] h2 span",
          "span.a-size-medium.a-color-base.a-text-normal",
          "span.a-size-base-plus.a-color-base.a-text-normal"
        ]),
        href: firstAttribute(
          container,
          ["h2 a", "a.a-link-normal.s-no-outline", "a.a-link-normal"],
          "href"
        ),
        image:
          firstAttribute(container, ["img.s-image", "img.a-dynamic-image", "img"], "src") ||
          firstAttribute(container, ["img.s-image", "img.a-dynamic-image", "img"], "data-src"),
        price: priceOf(container),
        soldBy: sellerOf(container),
        fulfilledBy: /fulfilled by amazon/i.test(text) ? "Amazon" : null
      });
    } catch (error) {
      // One broken result must not cost the page.
    }
  }
  return products;
}

/**
 * Builds the link to a product. It stays on the origin that was scraped.
 *
 * @param {object} item the raw fields of one result
 * @param {object} config the configuration
 * @returns {string|null} the product URL, or null when there is none to give
 */
function productUrl(item, config) {
  const base = config.amazonBaseUrl;
  if (item.asin && /^[A-Z0-9]{10}$/.test(item.asin)) return `${base}/dp/${item.asin}`;
  if (!item.href) return null;
  try {
    const resolved = new URL(item.href, `${base}/`);
    if (resolved.origin !== new URL(base).origin) return null;
    return `${resolved.origin}${resolved.pathname}`;
  } catch (error) {
    return null;
  }
}

/**
 * Turns the raw fields into the shape the caller receives.
 *
 * @param {Array<object>} raw the raw fields of every result on the page
 * @param {object} config the configuration
 * @returns {Array<object>} the products
 */
function normalise(raw, config) {
  const products = [];
  for (const item of raw || []) {
    const product = {
      title: item.title || null,
      url: productUrl(item, config),
      image: item.image || null,
      price: item.price || null,
      soldBy: item.soldBy || null,
      fulfilledBy: item.fulfilledBy || null
    };
    // A container that Amazon left empty carries nothing but its address. It is not a
    // product, and a link on its own is not worth passing on.
    if (!product.title && !product.image && !product.price) continue;
    products.push(product);
    if (products.length >= config.maxProducts) break;
  }
  return products;
}

/**
 * Reads one page of Amazon search results.
 *
 * @param {object} request the request
 * @param {string} request.keyword the search term
 * @param {number} request.page the page number, one based
 * @param {object} request.browser the browser to use
 * @param {object} request.config the configuration
 * @returns {Promise<Array<object>>} the products on that page
 */
async function scrape({ keyword, page, browser, config }) {
  const url = searchUrl(config.amazonBaseUrl, keyword, page);
  const tab = await browser.newPage();
  try {
    await tab.setViewport({ width: 1536, height: 864, deviceScaleFactor: 1 });
    await tab.setExtraHTTPHeaders({ "Accept-Language": "en-IN,en;q=0.9" });
    // The browser's own agent, without the word "Headless". A pinned agent that disagrees
    // with the engine is worse than no override at all.
    const agent = config.userAgent || (await browser.userAgent()).replace("HeadlessChrome", "Chrome");
    await tab.setUserAgent(agent);

    await tab.goto(url, { waitUntil: "domcontentloaded", timeout: config.navigationTimeoutMs });
    await tab
      .waitForSelector('div[data-component-type="s-search-result"], div[data-asin]', { timeout: 5000 })
      .catch(() => null);

    const raw = await tab.evaluate(readResults, config.maxProducts);
    return normalise(raw, config);
  } finally {
    await tab.close().catch(() => null);
  }
}

module.exports = { scrape, searchUrl, normalise, readResults };
