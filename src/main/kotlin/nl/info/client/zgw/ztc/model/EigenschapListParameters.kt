/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

enum class EigenschapListParametersStatus(val value: String) {
    concept("concept"),
    definitief("definitief"),
    alles("alles")
}

class EigenschapListParameters {
    @field:QueryParam("zaaktype")
    var zaaktype: URI? = null

    @field:QueryParam("zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String? = null

    @field:QueryParam("status")
    var status: EigenschapListParametersStatus? = null
}
