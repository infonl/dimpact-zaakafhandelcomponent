/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.converter

import jakarta.inject.Inject
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.informatieobjecten.model.RestInformatieobjecttype
import nl.info.zac.app.informatieobjecten.model.toRestInformatieobjecttype
import java.net.URI

class RestInformatieobjecttypeConverter @Inject constructor(
    private val ztcClientService: ZtcClientService
) {
    fun convertFromUris(informatieobjecttypeUris: List<URI>): List<RestInformatieobjecttype> =
        informatieobjecttypeUris
            .map { ztcClientService.readInformatieobjecttype(it) }
            .map { it.toRestInformatieobjecttype() }
}
