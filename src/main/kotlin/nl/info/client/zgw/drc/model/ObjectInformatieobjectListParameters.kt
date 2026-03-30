/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.drc.model

import jakarta.ws.rs.QueryParam
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI

@NoArgConstructor
@AllOpen
class ObjectInformatieobjectListParameters {

    /**
     * URL-referentie naar het gerelateerde OBJECT (in deze of een andere API).
     */
    @QueryParam("object")
    var objectUri: URI? = null

    /**
     * URL-referentie naar het INFORMATIEOBJECT.
     */
    @QueryParam("informatieobject")
    var informatieobject: URI? = null
}
