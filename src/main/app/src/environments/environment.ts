/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  LOCATION_SERVER_API_URL: "https://api.pdok.nl/bzk/locatieserver/search/v3_1",
  BACKGROUND_MAP_API_URL:
    "https://service.pdok.nl/brt/achtergrondkaart/wmts/v2_0",
  GOOGLE_API_URL: "https://maps.googleapis.com/maps/api/js",
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
