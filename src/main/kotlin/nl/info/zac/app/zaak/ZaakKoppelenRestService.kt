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
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
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
    private val ztcClientService: ZtcClientService,
) {

    @GET
    @Path("{zaakUuid}/zoek-koppelbare-zaken")
    @Suppress("UnusedParameter")
    fun findLinkableZaken(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @QueryParam("zoekZaakIdentifier") zoekZaakIdentifier: String,
        @QueryParam("linkType") linkType: String,
        @QueryParam("page") page: Int = 0,
        @QueryParam("rows") rows: Int = 10
    ): RestZoekResultaat<RestZaakKoppelenZoekObject> {
        val zaak = zrcClientService.readZaak(zaakUuid)
        val zaakType = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakLinkRight = policyService.readZaakRechten(zaak).koppelen
        val searchParameters = buildZoekParameters(zaak, zoekZaakIdentifier, page, rows)

        return filterSearchResults(
            searchService.zoek(searchParameters),
            zaakLinkRight,
            zaak,
            zaakType,
            linkType
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
            statustypeOmschrijving = statustypeOmschrijving,
            isKoppelbaar = linkable,
        )

    private fun filterSearchResults(
        searchResults: ZoekResultaat<out ZoekObject>,
        zaakLinkRight: Boolean,
        zaak: Zaak,
        zaakType: ZaakType,
        linkType: String
    ): RestZoekResultaat<RestZaakKoppelenZoekObject> =
        RestZoekResultaat(
            searchResults.items.map {
                val zaakZoekObject = it as ZaakZoekObject
                zaakZoekObject.toRestZaakKoppelenZoekObject(
                    isLinkable(
                        zaakLinkRight = zaakLinkRight,
                        sourceZaak = zaak,
                        sourceZaakType = zaakType,
                        targetZaak = zaakZoekObject,
                        linkType = linkType
                    )
                )
            },
            searchResults.count
        )

    private fun isLinkable(
        zaakLinkRight: Boolean,
        sourceZaak: Zaak,
        sourceZaakType: ZaakType,
        targetZaak: ZaakZoekObject,
        linkType: String
    ) = zaakLinkRight &&
        sourceZaak.isOpen &&
        targetZaak.isOpen() &&
        targetZaak.hasLinkRights() &&
        targetZaak.hasEqualZaakType(sourceZaak) &&
        targetZaak.isLinkableTo(sourceZaakType, linkType)

    private fun ZaakZoekObject.isOpen() =
        this.archiefNominatie == null

    private fun ZaakZoekObject.hasLinkRights() =
        policyService.readZaakRechten(this).koppelen

    private fun ZaakZoekObject.hasEqualZaakType(zaak: Zaak) =
        zaak.zaaktype.extractUuid() == UUID.fromString(this.zaaktypeUuid)

    private fun ZaakZoekObject.isLinkableTo(sourceZaakType: ZaakType, linkType: String): Boolean {
        val wantedLinkType = ZaakIndicatie.valueOf(linkType)
        val sourceDeelzaaktypen = sourceZaakType.deelzaaktypen

        return when (wantedLinkType) {
            ZaakIndicatie.HOOFDZAAK -> {
                !this.isIndicatie(ZaakIndicatie.HOOFDZAAK)
            }
            ZaakIndicatie.DEELZAAK -> {
                this.zaaktypeUuid?.let { uuid ->
                    sourceDeelzaaktypen.any { it.toString().contains(uuid) }
                } ?: false
            }
            else -> throw IllegalArgumentException("Invalid link type: $wantedLinkType")
        }
    }
}
