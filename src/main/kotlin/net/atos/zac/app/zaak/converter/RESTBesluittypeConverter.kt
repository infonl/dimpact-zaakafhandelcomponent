/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import jakarta.inject.Inject
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.zac.app.zaak.model.RESTBesluittype
import net.atos.zac.util.UriUtil
import java.net.URI

class RESTBesluittypeConverter {
    @Inject
    private lateinit var ztcClientService: ZtcClientService

    fun convertToRESTBesluittype(besluittypeURI: URI): RESTBesluittype {
        return convertToRESTBesluittype(ztcClientService.readBesluittype(besluittypeURI))
    }

    fun convertToRESTBesluittypes(besluittypes: List<BesluitType>): List<RESTBesluittype> {
        return besluittypes.stream()
            .map { this.convertToRESTBesluittype(it) }
            .toList()
    }

    private fun convertToRESTBesluittype(besluittype: BesluitType) = RESTBesluittype(
        id = UriUtil.uuidFromURI(besluittype.url),
        naam = besluittype.omschrijving,
        toelichting = besluittype.toelichting,
        informatieobjecttypen = besluittype.informatieobjecttypen.stream().toList()
    )
}
