/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.zgw.zrc.model.generated.AardRelatieEnum
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI

@NoArgConstructor
@AllOpen
data class RelevanteZaak @JsonbCreator constructor(
    /**
     * URL-referentie naar de ZAAK.
     */
    @param:JsonbProperty("url") val url: URI,

    /**
     * 'Benamingen van de aard van de relaties van andere zaken tot (onderhanden) zaken.'
     */
    @param:JsonbProperty("aardRelatie") val aardRelatie: AardRelatieEnum
)
