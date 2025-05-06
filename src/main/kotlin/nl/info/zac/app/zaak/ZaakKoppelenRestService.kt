/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.policy.PolicyService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.lang.UnsupportedOperationException
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
    private val ztcClientService: ZtcClientService,
) {

    @GET
    @Path("{zaakUuid}/zoek-koppelbare-zaken")
    @Suppress("UnusedParameter")
    fun findLinkableZaken(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @QueryParam("zoekZaakIdentifier") zoekZaakIdentifier: String,
        @QueryParam("relationType") relationType: RelatieType,
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("rows") @DefaultValue("10") rows: Int
    ) =
        zrcClientService.readZaak(zaakUuid).let { zaak ->
            filterSearchResults(
                searchService.zoek(
                    buildZoekParameters(zaak, zoekZaakIdentifier, page, rows)
                ),
                zaak,
                relationType
            )
        }

    private fun buildZoekParameters(
        zaak: Zaak,
        zoekZaakIdentifier: String,
        pageNo: Int,
        rowsNo: Int
    ) = ZoekParameters(ZoekObjectType.ZAAK).apply {
        start = pageNo * rowsNo
        rows = rowsNo
        addZoekVeld(ZoekVeld.ZAAK_IDENTIFICATIE, zoekZaakIdentifier)
        addFilter(FilterVeld.ZAAK_IDENTIFICATIE, FilterParameters(listOf(zaak.identificatie), true))
    }

    private fun ZaakZoekObject.toRestZaakKoppelenZoekObject(linkable: Boolean) =
        RestZaakKoppelenZoekObject(
            id = getObjectId(),
            type = ZoekObjectType.ZAAK,
            identificatie = identificatie,
            omschrijving = omschrijving,
            zaaktypeOmschrijving = zaaktypeOmschrijving,
            statustypeOmschrijving = statustypeOmschrijving,
            isKoppelbaar = linkable,
        )

    private fun filterSearchResults(
        searchResults: ZoekResultaat<out ZoekObject>,
        zaak: Zaak,
        relationType: RelatieType
    ): RestZoekResultaat<RestZaakKoppelenZoekObject> =
        RestZoekResultaat(
            searchResults.items.map {
                val zaakZoekObject = it as ZaakZoekObject
                zaakZoekObject.toRestZaakKoppelenZoekObject(
                    isLinkable(sourceZaak = zaak, targetZaak = zaakZoekObject, relationType = relationType)
                )
            },
            searchResults.count
        )

    private fun isLinkable(
        sourceZaak: Zaak,
        targetZaak: ZaakZoekObject,
        relationType: RelatieType
    ) =
        (areBothOpen(sourceZaak, targetZaak) || areBothClosed(sourceZaak, targetZaak)) &&
            sourceZaak.hasLinkRights() &&
            targetZaak.hasLinkRights() &&
            targetZaak.isLinkableTo(sourceZaak, relationType)

    private fun areBothOpen(sourceZaak: Zaak, targetZaak: ZaakZoekObject) =
        sourceZaak.isOpen && targetZaak.archiefNominatie == null

    private fun areBothClosed(sourceZaak: Zaak, targetZaak: ZaakZoekObject) =
        !sourceZaak.isOpen && targetZaak.archiefNominatie != null

    private fun ZaakZoekObject.hasLinkRights() = policyService.readZaakRechten(this).koppelen

    private fun Zaak.hasLinkRights() = policyService.readZaakRechten(this).koppelen

    private fun ZaakZoekObject.isLinkableTo(sourceZaak: Zaak, relationType: RelatieType) =
        when (relationType) {
            RelatieType.HOOFDZAAK -> this.zaaktypeUuid?.let { uuid ->
                ztcClientService.readZaaktype(sourceZaak.zaaktype).deelzaaktypen.any {
                    it.toString().contains(uuid)
                }
            } ?: false
            RelatieType.DEELZAAK -> sourceZaak.zaaktype.extractUuid().toString().let { uuid ->
                ztcClientService.readZaaktype(UUID.fromString(this.zaaktypeUuid)).deelzaaktypen.any {
                    it.toString().contains(uuid)
                }
            }
            else -> throw UnsupportedOperationException("Unsupported link type: $relationType")
        }
}
