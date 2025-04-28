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
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
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
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService
) {

    @GET
    @Path("{zaakUuid}/zoek-koppelbare-zaken")
    fun findLinkableZaken(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @QueryParam("zoekZaakIdentifier") zoekZaakIdentifier: String,
        @QueryParam("linkType") linkType: String,
        @QueryParam("page") page: Int = 0,
        @QueryParam("rows") rows: Int = 10
    ) = zrcClientService.readZaak(zaakUuid)
        .also { PolicyService.assertPolicy(policyService.readZaakRechten(it).koppelen) }
        .let { Pair(it, buildZoekParameters(it, zoekZaakIdentifier, linkType, page, rows)) }
        .let { Pair(it.first, searchService.zoek(it.second)) }
        .let { pair ->
            val fromZaakAndZaaktype = Pair(pair.first, ztcClientService.readZaaktype(pair.first.zaaktype))
            pair.second.items
                .map {
                    val zaakZoekObject = it as ZaakZoekObject
                    zaakZoekObject.toRestZaakKoppelenZoekObject(isLinkable(fromZaakAndZaaktype, zaakZoekObject))
                }
                .let { RestZoekResultaat(it, pair.second.count) }
        }

    private fun isLinkable(
        fromZaakAndZaaktype: Pair<Zaak, ZaakType>,
        zaakZoekObject: ZaakZoekObject
    ): Boolean = fromZaakAndZaaktype.second.deelzaaktypen.any { uri ->
        uri.toString().contains(zaakZoekObject.zaaktypeUuid!!)
    }

    @Suppress("UnusedParameter")
    private fun buildZoekParameters(
        zaak: Zaak,
        zoekZaakIdentifier: String,
        linkType: String,
        pageNo: Int,
        rowsNo: Int
    ) = ZoekParameters(ZoekObjectType.ZAAK).apply {
        start = pageNo * rowsNo
        rows = rowsNo
        addZoekVeld(ZoekVeld.ZAAK_IDENTIFICATIE, zoekZaakIdentifier)
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
