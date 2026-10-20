# syntax=docker/dockerfile:1
#
# The Amazon scraper sidecar.
#
# Build stage: the dependencies and the browser.
# Runtime stage: the service, the browser and the shared libraries the browser links
# against. Nothing else is carried over.
#
# The image has no display and needs none: the browser runs headless.

FROM node:20-bookworm-slim AS build
WORKDIR /app

# The browser lives here, and the runtime stage looks in the same place.
ENV PUPPETEER_CACHE_DIR=/opt/puppeteer

RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl unzip \
 && rm -rf /var/lib/apt/lists/*

COPY package.json package-lock.json ./

# The install script of puppeteer is not allowed to fetch the browser: the unzip library
# it uses leaves a browser that cannot start.
RUN PUPPETEER_SKIP_DOWNLOAD=true npm ci --omit=dev

# The browser is fetched and unpacked here, and the version is the one puppeteer pins, so
# the two can never drift apart. The build fails if the browser is not there.
RUN set -eux; \
    version="$(node -p "require('puppeteer').PUPPETEER_REVISIONS.chrome")"; \
    curl -fsSL -o /tmp/chrome.zip \
      "https://storage.googleapis.com/chrome-for-testing-public/${version}/linux64/chrome-linux64.zip"; \
    mkdir -p "${PUPPETEER_CACHE_DIR}/chrome/linux-${version}"; \
    unzip -q /tmp/chrome.zip -d "${PUPPETEER_CACHE_DIR}/chrome/linux-${version}"; \
    rm -f /tmp/chrome.zip; \
    test -x "${PUPPETEER_CACHE_DIR}/chrome/linux-${version}/chrome-linux64/chrome"; \
    rm -rf /root/.npm/_cacache

FROM node:20-bookworm-slim AS runtime

ENV PUPPETEER_CACHE_DIR=/opt/puppeteer \
    NODE_ENV=production \
    HOME=/home/scrape \
    HOST=127.0.0.1 \
    PORT=3000

# The libraries chromium links against. The list is the one the Puppeteer project
# publishes for a Debian container.
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      ca-certificates \
      fonts-liberation \
      libasound2 \
      libatk-bridge2.0-0 \
      libatk1.0-0 \
      libc6 \
      libcairo2 \
      libcups2 \
      libdbus-1-3 \
      libexpat1 \
      libfontconfig1 \
      libgbm1 \
      libgcc1 \
      libglib2.0-0 \
      libgtk-3-0 \
      libnspr4 \
      libnss3 \
      libpango-1.0-0 \
      libpangocairo-1.0-0 \
      libstdc++6 \
      libx11-6 \
      libx11-xcb1 \
      libxcb1 \
      libxcomposite1 \
      libxcursor1 \
      libxdamage1 \
      libxext6 \
      libxfixes3 \
      libxi6 \
      libxrandr2 \
      libxrender1 \
      libxss1 \
      libxtst6 \
      xdg-utils \
 && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 1001 scrape \
 && useradd --system --uid 1001 --gid scrape --home-dir /home/scrape --shell /usr/sbin/nologin scrape \
 && mkdir -p /home/scrape \
 && chown scrape:scrape /home/scrape

WORKDIR /app
COPY --from=build --chown=scrape:scrape /app/node_modules ./node_modules
COPY --from=build --chown=scrape:scrape /opt/puppeteer /opt/puppeteer
COPY --chown=scrape:scrape package.json server.js scrape.js config.js verify.js ./

USER scrape
EXPOSE 3000

# The health check asks the service about itself. It never starts a browser.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD ["node", "-e", "fetch('http://127.0.0.1:'+(process.env.PORT||3000)+'/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"]

CMD ["node", "server.js"]
