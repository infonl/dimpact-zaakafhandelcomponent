/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

enum class RelatieType {
    HOOFDZAAK,
    DEELZAAK,
    GERELATEERD,

    // These values map to AardRelatieEnum from the ZTC API via RelatieType.valueOf() in RestZaaktypeRelatie.
    // They are used for display purposes only and cannot be used for linking or unlinking zaken.
    // Do not remove or rename these values as it will cause runtime failures when ZTC API responses are converted.
    VERVOLG,
    ONDERWERP,
    BIJDRAGE,
    OVERIG
}
