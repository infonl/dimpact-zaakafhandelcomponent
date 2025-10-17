# Font Caching Procedure

## Overview
Zac implements font caching strategy to enhance performance and user experience. By caching fonts, we reduce load times, ensure consistency in font rendering, and prevent users from seeing fallback text for icons.

## Caching Strategy
- all .woff2 files on *.dimpact.lifely.nl are cached for 30 days
- backend Expires/Cache-Control replies are overridden and expires is set to 30 days
- the first hit once the local cache expires returns the old cache entry and the server refreshes the cached file from the backend, all other requests after this wait until the refresh has finished
- the files are cached locally in the nginx container in /tmp and do not persist on restarts

### Material Symbols Font
- The Material Symbols font, which is relatively large (~3MB), is also cached.
- Caching this font is crucial as it prevents the display of fallback text for Material icons.
- We also prefetch the font so it's impossible for the user to experience a scenario where the icons font is not loaded in time and shows the fallback text.
  - in index.html there will be a line that adds this functionality, something like: `<link rel="prefetch" href="assets/fonts/material-symbols.woff2" as="font" type="font/woff2" crossorigin>`]
  - Due to the cache invalidation, a user might experience a slightly longer white screen when loading the app because of the prefetch. This will only occur if the cache has expired and the fonts need to be re-fetched.

