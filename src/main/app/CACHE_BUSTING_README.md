# Cache Busting Implementation

This document describes the cache busting implementation for the Zaakafhandelcomponent to ensure that translation files and fonts are properly cached and invalidated when they change.

## Overview

The implementation provides automatic cache busting for:

- **Translation files** (`en.json`, `nl.json`) - using content-based hashing
- **Fonts in the app** (Material Symbols, Roboto) - declared with `@font-face` in `src/styles.less`; the Angular production build (`outputHashing: all`) puts a content hash in their file names
- **Fonts in `src/static/smart-documents-result.html`** (Roboto) - this page is not processed by Angular, so it uses the `FONT_HASH` placeholder, based on file size and modification time

## Material Symbols icon font

The full variable font lives in `fonts/MaterialSymbolsOutlined.woff2` and is not shipped. `scripts/generate-icon-font.mjs` writes `src/generated/fonts/material-symbols-outlined.woff2` (gitignored):

- `npm run build` / `build:dev` keep only the icons whose names appear in `src`, with the weight, grade and optical size axes fixed to their defaults (about 30 KB); the FILL axis stays variable because `styles.less` switches between filled and outlined icons with it
- `npm run dev` / `start` pass `--all-icons`: every icon, same axes (about 280 KB), so a new icon works without a restart

The build fails when an icon found in the source is missing from the generated font. An icon name built at runtime (for example by string concatenation) is not found by the scan and shows as plain text in production; write icon names as complete literals.

The generator is tested by `scripts/generate-icon-font.test.mjs` (Node's built-in test runner): `npm run test:scripts`. CI runs it in the frontend unit test job.

## Architecture

### Core Services

1. **`CacheBustingTranslateLoader`** - Custom translate loader with cache busting

### Build Process

1. **Icon Font Generation** - Creates the Material Symbols font from the icons used in the source
2. **Angular Build** - Compiles the application and hashes the fonts referenced from `styles.less`
3. **Hash Generation** - Calculates hashes for translations and the static page fonts
4. **Placeholder Replacement** - Replaces placeholders in JS and HTML files

## Generated Output

### Translation URLs

```
/assets/i18n/en.json?v=HASH
/assets/i18n/nl.json?v=HASH
```

### Font URLs

```
/media/material-symbols-outlined-HASH.woff2
/media/300-HASH.woff2
/media/400-HASH.woff2
/media/500-HASH.woff2
```

The static page uses `/assets/fonts/Roboto/<weight>.woff2?v=HASH`.
