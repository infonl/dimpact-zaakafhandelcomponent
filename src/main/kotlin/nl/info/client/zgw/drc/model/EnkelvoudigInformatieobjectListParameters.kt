/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.drc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.shared.model.AbstractListParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
class EnkelvoudigInformatieobjectListParameters : AbstractListParameters() {

    /**
     * Een binnen een gegeven context ondubbelzinnige referentie naar het INFORMATIEOBJECT.
     */
    @QueryParam("identificatie")
    var identificatie: String? = null

    /**
     * het RSIN van de Niet-natuurlijk persoon zijnde de organisatie die het informatieobject heeft gecreëerd of heeft ontvangen
     * en als eerste in een samenwerkingsketen heeft vastgelegd.
     */
    @QueryParam("bronorganisatie")
    var bronorganisatie: String? = null
}
