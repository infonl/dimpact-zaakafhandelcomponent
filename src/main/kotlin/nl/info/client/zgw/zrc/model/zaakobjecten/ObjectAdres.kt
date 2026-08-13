/*
 * SPDX-FileCopyrightText: 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.zac.util.NoArgConstructor

/**
 * ObjectAdres
 */
@NoArgConstructor
data class ObjectAdres(
    override var identificatie: String? = null,
    /**
     * Woonplaats naam
     * - maxLength: 100
     * - required
     */
    var wplWoonplaatsNaam: String? = null,
    /**
     * Een door het bevoegde gemeentelijke orgaan aan een OPENBARE RUIMTE toegekende benaming
     * - maxLength: 100
     * - required
     */
    var gorOpenbareRuimteNaam: String? = null,
    /**
     * Huisnummer
     * - maxSize: 99999
     * - required
     */
    var huisnummer: Int = 0,
    /**
     * Huisletter
     * - maxLength: 1
     */
    var huisletter: String? = null,
    /**
     * Huisnummertoevoeging
     * - maxLength: 4
     */
    var huisnummertoevoeging: String? = null,
    /**
     * Postcode
     * - maxLength: 7
     */
    var postcode: String? = null
) : ObjectBagObject(identificatie)
