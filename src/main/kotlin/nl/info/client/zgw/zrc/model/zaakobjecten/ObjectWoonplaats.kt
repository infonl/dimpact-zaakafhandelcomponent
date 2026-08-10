/*
 * SPDX-FileCopyrightText: 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.zac.util.NoArgConstructor

/**
 * ObjectWoonplaats
 */
@NoArgConstructor
data class ObjectWoonplaats(
    override var identificatie: String? = null,
    /**
     * De door het bevoegde gemeentelijke orgaan aan een WOONPLAATS toegekende benaming.
     * - maxLength: 80
     * - required
     */
    var woonplaatsNaam: String? = null
) : ObjectBagObject(identificatie)
