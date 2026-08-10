/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.shared.model.AbstractListParameters
import java.net.URI

class ZaakInformatieobjectListParameters : AbstractListParameters() {
    /**
     * URL-referentie naar de ZAAK.
     */
    @QueryParam("zaak")
    var zaak: URI? = null

    /**
     * URL-referentie naar het INFORMATIEOBJECT (in de Documenten API), waar ook de relatieinformatie opgevraagd kan worden.
     */
    @QueryParam("informatieobject")
    var informatieobject: URI? = null
}
