/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
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
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.SearchService
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.ZaakIndicatie.DEELZAAK
import nl.info.zac.search.model.ZaakIndicatie.HOOFDZAAK
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
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isDeelzaak
import nl.info.client.zgw.zrc.util.isHoofdzaak
import nl.info.client.zgw.zrc.util.isOpen

@Path("zaken/gekoppelde-zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
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
        addZoekVeld(ZoekVeld.ZAAK_IDENTIFICATIE, zoekZaakIdentifier.trim())
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
            sourceZaak.isLinkableTo(targetZaak, relationType) &&
            targetZaak.hasMatchingZaaktypeWith(sourceZaak, relationType)

    private fun areBothOpen(sourceZaak: Zaak, targetZaak: ZaakZoekObject) =
        sourceZaak.isOpen() && targetZaak.archiefNominatie == null

    private fun areBothClosed(sourceZaak: Zaak, targetZaak: ZaakZoekObject) =
        !sourceZaak.isOpen() && targetZaak.archiefNominatie != null

    private fun ZaakZoekObject.hasLinkRights() = policyService.readZaakRechtenForZaakZoekObject(this).koppelen

    private fun Zaak.hasLinkRights() = policyService.readZaakRechten(this).koppelen

    private fun Zaak.isLinkableTo(targetZaak: ZaakZoekObject, relationType: RelatieType): Boolean =
        when (relationType) {
            // "The case you are searching for here will become the main case"
            RelatieType.HOOFDZAAK ->
                // hoofdzaak to hoofdzaak link not allowed
                !this.isHoofdzaak() && !targetZaak.isIndicatie(HOOFDZAAK) &&
                    // a zaak cannot have two hoofdzaken
                    !this.isDeelzaak() && !targetZaak.isIndicatie(DEELZAAK)
            // "The case you are searching for here will become the subcase"
            RelatieType.DEELZAAK ->
                // As per https://vng-realisatie.github.io/gemma-zaken/standaard/zaken
                // "deelzaken van deelzaken zijn NIET toegestaan"
                !this.isDeelzaak() && !targetZaak.isIndicatie(DEELZAAK) &&
                    // a hoofdzaak cannot be both a hoofdzaak and a deelzaak
                    !targetZaak.isIndicatie(HOOFDZAAK)
            else -> throw UnsupportedOperationException(
                "Unsupported link type: $relationType for ${this.identificatie} -> ${targetZaak.identificatie}"
            )
        }

    private fun ZaakZoekObject.hasMatchingZaaktypeWith(sourceZaak: Zaak, relationType: RelatieType): Boolean =
        when (relationType) {
            // "The case you are searching for here will become the main case"
            RelatieType.HOOFDZAAK ->
                // source zaak's zaaktype is allowed in target zaak as deelzaak
                sourceZaak.zaaktype.extractUuid().toString().let { uuid ->
                    ztcClientService.readZaaktype(UUID.fromString(this.zaaktypeUuid)).deelzaaktypen.any {
                        it.toString().contains(uuid)
                    }
                }
            // "The case you are searching for here will become the subcase"
            RelatieType.DEELZAAK ->
                // target zaak's zaaktype is allowed in source zaak as deelzaak
                this.zaaktypeUuid?.let { uuid ->
                    ztcClientService.readZaaktype(sourceZaak.zaaktype).deelzaaktypen.any {
                        it.toString().contains(uuid)
                    }
                } ?: false
            else -> throw UnsupportedOperationException(
                "Unsupported link type: $relationType for ${sourceZaak.identificatie} -> ${this.identificatie}"
            )
        }
}
