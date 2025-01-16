/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.zoeken.converter.RestZoekParametersConverter
import net.atos.zac.app.zoeken.converter.RestZoekResultaatConverter
import net.atos.zac.app.zoeken.model.AbstractRestZoekObject
import net.atos.zac.app.zoeken.model.RestZoekParameters
import net.atos.zac.app.zoeken.model.RestZoekResultaat
import net.atos.zac.policy.PolicyService
import net.atos.zac.zoeken.ZoekenService
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Path("zoeken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class ZoekenRestService @Inject constructor(
    private val zoekenService: ZoekenService,
    private val restZoekZaakParametersConverter: RestZoekParametersConverter,
    private val restZoekResultaatConverter: RestZoekResultaatConverter,
    private val policyService: PolicyService
) {
    @PUT
    @Path("list")
    fun list(restZoekParameters: RestZoekParameters): RestZoekResultaat<out AbstractRestZoekObject?> {
        when (restZoekParameters.type) {
            ZoekObjectType.ZAAK, ZoekObjectType.TAAK -> PolicyService.assertPolicy(
                policyService.readWerklijstRechten().zakenTaken
            )
            else -> PolicyService.assertPolicy(policyService.readOverigeRechten().zoeken)
        }
        val zoekParameters = restZoekZaakParametersConverter.convert(restZoekParameters)
        val zoekResultaat = zoekenService.zoek(zoekParameters)
        return restZoekResultaatConverter.convert(zoekResultaat, restZoekParameters)
    }
}
