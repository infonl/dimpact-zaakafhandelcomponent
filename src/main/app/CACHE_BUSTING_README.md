# Cache Busting Implementation

This document describes the cache busting implementation for the Zaakafhandelcomponent to ensure that translation files and fonts are properly cached and invalidated when they change.

## Overview

The implementation provides automatic cache busting for:

- **Translation files** (`en.json`, `nl.json`) - using content-based hashing
- **Font files** (Material Symbols, Roboto) - using file size and modification time hashing
- **Dynamic font injection** - both preload links and `@font-face` rules

## Architecture

### Core Services

1. **`CacheBustingTranslateLoader`** - Custom translate loader with cache busting
2. **`FontCacheBustingService`** - Provides cache-busted font URLs
3. **`FontLoaderService`** - Dynamically loads fonts with cache busting
4. **`FontPreloadInjectorService`** - Injects font preload links dynamically

### Build Process

1. **Angular Build** - Compiles the application
2. **Hash Generation** - Calculates hashes for translations and fonts
3. **Placeholder Replacement** - Replaces placeholders in JS and HTML files
4. **Dynamic Injection** - Fonts are injected at runtime with cache busting

## Generated Output

### Translation URLs

```
/assets/i18n/en.json?v=HASH
/assets/i18n/nl.json?v=HASH
```

### Font URLs

```
/assets/fonts/Roboto/300.woff2?v=HASH
/assets/fonts/Roboto/400.woff2?v=HASH
/assets/fonts/Roboto/500.woff2?v=HASH
/assets/MaterialSymbolsOutlined.woff2?v=HASH
```

### Injected Font Preloads

```html
<link
  rel="preload"
  href="/assets/fonts/Roboto/300.woff2?v=HASH"
  as="font"
  type="font/woff2"
  crossorigin=""
/>
```

### Injected Font-Face Rules

```css
@font-face {
  font-family: "Roboto";
  font-style: normal;
  font-weight: 400;
  font-display: swap;
  src: url("/assets/fonts/Roboto/400.woff2?v=HASH") format("woff2");
}
```
