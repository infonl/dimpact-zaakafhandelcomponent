/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
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
    @param:JsonbProperty("aardRelatie") val aardRelatie: AardRelatie
) {
    fun `is`(url: URI, aardRelatie: AardRelatie): Boolean = this.aardRelatie == aardRelatie && this.url == url
}
