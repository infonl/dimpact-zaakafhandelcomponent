/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.policy.PolicyService
import net.atos.zac.search.SearchService
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.app.search.converter.RestZoekParametersConverter
import nl.info.zac.app.search.converter.RestZoekResultaatConverter
import nl.info.zac.app.search.model.AbstractRestZoekObject
import nl.info.zac.app.search.model.RestZoekKoppelenParameters
import nl.info.zac.app.search.model.RestZoekParameters
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.search.model.toZoekParameters
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@Path("zoeken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class SearchRestService @Inject constructor(
    private val searchService: SearchService,
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
        return restZoekZaakParametersConverter.convert(restZoekParameters).let {
            searchService.zoek(it).let {
                restZoekResultaatConverter.convert(it, restZoekParameters)
            }
        }
    }

    @PUT
    @Path("zaken")
    fun listZakenForInformationObjectType(
        @Valid restZoekKoppelenParameters: RestZoekKoppelenParameters,
    ): RestZoekResultaat<out AbstractRestZoekObject?> {
        PolicyService.assertPolicy(policyService.readWerklijstRechten().zakenTaken)

        return searchService.zoek(restZoekKoppelenParameters.toZoekParameters()).let {
            restZoekResultaatConverter.convert(it, restZoekKoppelenParameters.documentTypeUUID)
        }
    }
}
