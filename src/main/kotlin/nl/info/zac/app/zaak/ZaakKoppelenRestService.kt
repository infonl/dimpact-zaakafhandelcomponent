/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.policy.PolicyService
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@Path("zaken/gekoppelde-zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
class ZaakKoppelenRestService @Inject constructor(
    private val policyService: PolicyService,
    private val searchService: SearchService,
    private val zrcClientService: ZrcClientService
) {

    @GET
    @Path("{zaakUuid}/zoek-koppelbare-zaken")
    fun findLinkableZaken(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @QueryParam("zaakIdentifier") zaakIdentifier: String,
        @QueryParam("linkType") linkType: String,
        @QueryParam("page") page: Int = 0,
        @QueryParam("rows") rows: Int = 10
    ) = zrcClientService.readZaak(zaakUuid)
        .also { PolicyService.assertPolicy(policyService.readZaakRechten(it).koppelen()) }
        .let { buildZoekParameters(it, zaakIdentifier, linkType, page, rows) }
        .let(searchService::zoek)
        .let { zoekResultaat ->
            zoekResultaat.items
                .map { (it as ZaakZoekObject).toRestZaakKoppelenZoekObject(true) }
                .let { RestZoekResultaat(it, zoekResultaat.count) }
        }

    @Suppress("UnusedParameter")
    private fun buildZoekParameters(
        zaak: Zaak,
        zaakIdentifier: String,
        linkType: String,
        pageNo: Int,
        rowsNo: Int
    ) = ZoekParameters(ZoekObjectType.ZAAK).apply {
        start = pageNo * rowsNo
        rows = rowsNo
        addZoekVeld(ZoekVeld.ZAAK_IDENTIFICATIE, zaakIdentifier)
        addFilter(FilterVeld.ZAAK_IDENTIFICATIE, FilterParameters(listOf(zaak.identificatie), true))
    }
}

private fun ZaakZoekObject.toRestZaakKoppelenZoekObject(linkable: Boolean) =
    RestZaakKoppelenZoekObject(
        id = identificatie,
        type = ZoekObjectType.ZAAK,
        identificatie = identificatie,
        omschrijving = omschrijving,
        statustypeOmschrijving = statustypeOmschrijving,
        documentKoppelbaar = linkable,
    )
