/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.converter

import jakarta.inject.Inject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import nl.info.zac.app.informatieobjecten.model.RestInformatieobjecttype
import java.net.URI

class RestInformatieobjecttypeConverter @Inject constructor(
    private val ztcClientService: ZtcClientService
) {
    companion object {
        fun convert(type: InformatieObjectType) = RestInformatieobjecttype().apply {
            uuid = type.url.extractUuid()
            concept = type.concept
            omschrijving = type.omschrijving
            // we use the uppercase version of this enum in the ZAC backend API
            vertrouwelijkheidaanduiding = type.vertrouwelijkheidaanduiding.name
        }

        fun convert(informatieobjecttypen: List<InformatieObjectType>): List<RestInformatieobjecttype> =
            informatieobjecttypen.map { convert(it) }
    }

    fun convertFromUris(informatieobjecttypeUris: List<URI>): List<RestInformatieobjecttype> =
        informatieobjecttypeUris
            .map { ztcClientService.readInformatieobjecttype(it) }
            .map { convert(it) }
}
