/*
 *
 *  * SPDX-FileCopyrightText: 2025 INFO.nl
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
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.search.converter.RestZoekParametersConverter
import nl.info.zac.app.search.converter.RestZoekResultaatConverter
import nl.info.zac.app.search.model.AbstractRestZoekObject
import nl.info.zac.app.search.model.RestZoekKoppelenParameters
import nl.info.zac.app.search.model.RestZoekParameters
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.search.model.toZoekParameters
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

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
    private val policyService: PolicyService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
) {
    @PUT
    @Path("list")
    fun listSearchResults(
        @Valid restZoekParameters: RestZoekParameters
    ): RestZoekResultaat<out AbstractRestZoekObject> {
        when (restZoekParameters.type) {
            ZoekObjectType.ZAAK, ZoekObjectType.TAAK -> assertPolicy(
                policyService.readWerklijstRechten().zakenTaken
            )
            else -> assertPolicy(policyService.readOverigeRechten().zoeken)
        }
        return restZoekZaakParametersConverter.convert(restZoekParameters).let {
            searchService.zoek(it).let {
                restZoekResultaatConverter.convert(it, restZoekParameters)
            }
        }
    }

    @PUT
    @Path("zaken")
    fun listZakenForInformationObjectType(@Valid restZoekKoppelenParameters: RestZoekKoppelenParameters) =
        assertPolicy(policyService.readWerklijstRechten().zakenTaken).run {
            searchService.zoek(restZoekKoppelenParameters.toZoekParameters()).let {
                restZoekResultaatConverter.convert(it, buildDocumentsLinkableList(it, restZoekKoppelenParameters))
            }
        }

    private fun buildDocumentsLinkableList(
        resultaat: ZoekResultaat<out ZoekObject>,
        restZoekKoppelenParameters: RestZoekKoppelenParameters
    ) = resultaat.items.map {
        isDocumentLinkable(
            (it as ZaakZoekObject).identificatie,
            restZoekKoppelenParameters.informationObjectTypeUuid
        )
    }

    private fun isDocumentLinkable(zaakIdentification: String, informationObjectTypeUuid: UUID) =
        zrcClientService.readZaakByID(zaakIdentification).zaaktype.extractUuid().let {
            ztcClientService.readZaaktype(it).informatieobjecttypen.any {
                it.extractUuid() == informationObjectTypeUuid
            }
        }
}
