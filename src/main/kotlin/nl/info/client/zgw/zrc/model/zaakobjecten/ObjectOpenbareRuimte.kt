/*
 * SPDX-FileCopyrightText: 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.zac.util.NoArgConstructor

/**
 * ObjectOpenbareRuimte
 */
@NoArgConstructor
data class ObjectOpenbareRuimte(
    override var identificatie: String? = null,
    /**
     * Een door het bevoegde gemeentelijke orgaan aan een OPENBARE RUIMTE toegekende benaming
     * - maxLength: 80
     * - required
     */
    var gorOpenbareRuimteNaam: String? = null,
    /**
     * De door het bevoegde gemeentelijke orgaan aan een WOONPLAATS toegekende benaming.
     * - maxLength: 80
     * - required
     */
    var wplWoonplaatsNaam: String? = null
) : ObjectBagObject(identificatie)
