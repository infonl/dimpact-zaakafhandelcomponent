# Font Caching Procedure

## Overview
Zac implements font caching strategy to enhance performance and user experience. By caching fonts, we reduce load times, ensure consistency in font rendering, and prevent users from seeing fallback text for icons.

## Caching Strategy
- all .woff2 files on *.dimpact.lifely.nl are cached for 30 days
- backend Expires/Cache-Control replies are overridden and expires is set to 30 days
- the first hit once the local cache expires returns the old cache entry and the server refreshes the cached file from the backend, all other requests after this wait until the refresh has finished
- the files are cached locally in the nginx container in /tmp and do not persist on restarts

### Material Symbols Font
- The full Material Symbols font (~3MB) lives in `src/main/app/fonts/`. The production build (`npm run build`) runs `scripts/generate-icon-font.mjs`, which keeps only the icons whose names appear in `src/` (~30KB) and writes them to `src/generated/fonts/material-symbols-outlined.woff2`. The dev server keeps all icons, so a new icon works without a restart.
- `styles.less` loads the font with `font-display: block`, so icons never show their name as fallback text while the font loads.
- An icon name that never appears literally in `src/` (e.g. one rendered by a library in `node_modules`) must be added to `libraryIconNames` in the generator script, or it renders as plain text in production.

