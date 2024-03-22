/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.zac.app.zaken.model.RESTBesluittype
import net.atos.zac.util.UriUtil
import java.net.URI

class RESTBesluittypeConverter {
    @Inject
    private lateinit var ztcClientService: ZTCClientService

    fun convertToRESTBesluittype(besluittypeURI: URI): RESTBesluittype {
        return convertToRESTBesluittype(ztcClientService.readBesluittype(besluittypeURI))
    }

    fun convertToRESTBesluittypes(besluittypes: List<BesluitType>): List<RESTBesluittype> {
        return besluittypes.stream()
            .map { besluittype -> this.convertToRESTBesluittype(besluittype) }
            .toList()
    }

    private fun convertToRESTBesluittype(besluittype: BesluitType) = RESTBesluittype(
        id = UriUtil.uuidFromURI(besluittype.url),
        naam = besluittype.omschrijving,
        toelichting = besluittype.toelichting,
        informatieobjecttypen = besluittype.informatieobjecttypen.stream().toList()
    )
}
