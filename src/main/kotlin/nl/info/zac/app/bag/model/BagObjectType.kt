/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

enum class BagObjectType(val expand: String) {
    ADRES("adressen"),
    WOONPLAATS("woonplaats"),
    PAND("panden"),
    OPENBARE_RUIMTE("openbareRuimte"),
    NUMMERAANDUIDING("nummeraanduiding"),
    ADRESSEERBAAR_OBJECT("adreseerbaarObject")
}
