/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.GerelateerdeZakenZaakPatch
import nl.info.client.zgw.zrc.model.NillableHoofdzaakZaakPatch
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isDeelzaak
import nl.info.client.zgw.zrc.util.isHoofdzaak
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestZaakLinkData
import nl.info.zac.app.zaak.model.RestZaakUnlinkData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.search.IndexingService
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
import nl.info.zac.zaak.ZaakService
import java.net.URI
import java.util.UUID

@Path("zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class ZaakKoppelenRestService @Inject constructor(
    private val eventingService: EventingService,
    private val indexingService: IndexingService,
    private val policyService: PolicyService,
    private val searchService: SearchService,
    private val zaakService: ZaakService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    @GET
    @Path("gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken")
    @Suppress("UnusedParameter")
    fun findLinkableZaken(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @QueryParam("zoekZaakIdentifier") zoekZaakIdentifier: String,
        @QueryParam("relationType") relationType: RelatieType,
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("rows") @DefaultValue("10") rows: Int
    ): RestZoekResultaat<RestZaakKoppelenZoekObject> {
        val loggedInUser = loggedInUserInstance.get()
        val zaak = zrcClientService.readZaak(zaakUuid)
        val searchResults = searchService.zoek(
            zoekParameters = buildZoekParameters(zaak, zoekZaakIdentifier, page, rows)
        )
        return filterSearchResults(
            searchResults = searchResults,
            zaak = zaak,
            relationType = relationType,
            loggedInUser = loggedInUser
        )
    }

    @PATCH
    @Path("/zaak/koppel")
    fun linkZaak(restZaakLinkData: RestZaakLinkData) {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid)
        val (zaakToLinkTo, zaakToLinkToZaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(
            restZaakLinkData.teKoppelenZaakUuid
        )
        assertPolicy(policyService.readZaakRechten(zaak, zaakType, loggedInUserInstance.get()).koppelen)
        if (restZaakLinkData.relatieType == RelatieType.GERELATEERD) {
            assertPolicy(
                policyService.readZaakRechten(zaakToLinkTo, zaakToLinkToZaakType, loggedInUserInstance.get()).lezen
            )
        } else {
            assertPolicy(
                policyService.readZaakRechten(zaakToLinkTo, zaakToLinkToZaakType, loggedInUserInstance.get()).koppelen
            )
        }

        when (restZaakLinkData.relatieType) {
            RelatieType.HOOFDZAAK -> koppelHoofdEnDeelzaak(zaakToLinkTo, zaak)
            RelatieType.DEELZAAK -> koppelHoofdEnDeelzaak(zaak, zaakToLinkTo)
            RelatieType.GERELATEERD -> koppelGerelateerdeZaken(zaak, zaakToLinkTo, restZaakLinkData.reden)
            else -> throw IllegalArgumentException(
                "RelatieType ${restZaakLinkData.relatieType} cannot be used for linking zaken"
            )
        }
    }

    @PATCH
    @Path("/zaak/ontkoppel")
    fun unlinkZaak(restZaakUnlinkData: RestZaakUnlinkData) {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakUnlinkData.zaakUuid)
        val (linkedZaak, linkedZaakType) = zaakService.readZaakAndZaakTypeByZaakID(
            restZaakUnlinkData.gekoppeldeZaakIdentificatie
        )
        assertPolicy(policyService.readZaakRechten(zaak, zaakType, loggedInUserInstance.get()).wijzigen)
        assertPolicy(policyService.readZaakRechten(linkedZaak, linkedZaakType, loggedInUserInstance.get()).wijzigen)

        when (restZaakUnlinkData.relatieType) {
            RelatieType.HOOFDZAAK -> ontkoppelHoofdEnDeelzaak(
                hoofdZaak = linkedZaak,
                deelZaak = zaak,
                explanation = restZaakUnlinkData.reden
            )
            RelatieType.DEELZAAK -> ontkoppelHoofdEnDeelzaak(
                hoofdZaak = zaak,
                deelZaak = linkedZaak,
                explanation = restZaakUnlinkData.reden
            )
            RelatieType.GERELATEERD -> ontkoppelGerelateerdeZaken(
                zaak = zaak,
                andereZaak = linkedZaak,
                explanation = restZaakUnlinkData.reden
            )
            else -> throw IllegalArgumentException(
                "RelatieType ${restZaakUnlinkData.relatieType} cannot be used for unlinking zaken"
            )
        }
    }

    private fun areBothClosed(sourceZaak: Zaak, targetZaak: ZaakZoekObject) =
        !sourceZaak.isOpen() && targetZaak.archiefNominatie != null

    private fun areBothOpen(sourceZaak: Zaak, targetZaak: ZaakZoekObject) =
        sourceZaak.isOpen() && targetZaak.archiefNominatie == null

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

    private fun filterSearchResults(
        searchResults: ZoekResultaat<out ZoekObject>,
        zaak: Zaak,
        relationType: RelatieType,
        loggedInUser: LoggedInUser
    ): RestZoekResultaat<RestZaakKoppelenZoekObject> =
        RestZoekResultaat(
            searchResults.items.map {
                val zaakZoekObject = it as ZaakZoekObject
                zaakZoekObject.toRestZaakKoppelenZoekObject(
                    isLinkable(
                        sourceZaak = zaak,
                        targetZaak = zaakZoekObject,
                        relationType = relationType,
                        loggedInUser = loggedInUser
                    )
                )
            },
            searchResults.count
        )

    private fun ZaakZoekObject.hasLinkRights() = policyService.readZaakRechtenForZaakZoekObject(this).koppelen

    private fun Zaak.hasLinkRights(loggedInUser: LoggedInUser) = policyService.readZaakRechten(
        this,
        loggedInUser
    ).koppelen

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
                ztcClientService.readZaaktype(sourceZaak.zaaktype).deelzaaktypen.any {
                    it.toString().contains(this.zaaktypeUuid)
                }
            RelatieType.GERELATEERD ->
                true
            else -> throw IllegalArgumentException(
                "RelatieType $relationType cannot be used for matching zaaktype"
            )
        }

    private fun isLinkable(
        sourceZaak: Zaak,
        targetZaak: ZaakZoekObject,
        relationType: RelatieType,
        loggedInUser: LoggedInUser
    ) =
        (areBothOpen(sourceZaak, targetZaak) || areBothClosed(sourceZaak, targetZaak)) &&
            sourceZaak.hasLinkRights(loggedInUser) &&
            targetZaak.hasLinkRights() &&
            sourceZaak.isLinkableTo(targetZaak, relationType) &&
            targetZaak.hasMatchingZaaktypeWith(sourceZaak, relationType)

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
            RelatieType.GERELATEERD ->
                true
            else -> throw IllegalArgumentException(
                "RelatieType $relationType cannot be used for linking zaken"
            )
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

    private fun addGerelateerdeZaak(
        gerelateerdeZaken: MutableList<GerelateerdeZaak>?,
        andereZaakURI: URI
    ): List<GerelateerdeZaak> {
        val gerelateerdeZaak = GerelateerdeZaak().apply { url = andereZaakURI }
        return gerelateerdeZaken?.apply {
            if (none { it.url == andereZaakURI }) add(gerelateerdeZaak)
        } ?: listOf(gerelateerdeZaak)
    }

    private fun koppelGerelateerdeZaken(
        zaak: Zaak,
        otherZaak: Zaak,
        explanation: String?
    ) {
        zrcClientService.patchZaak(
            zaakUUID = zaak.uuid,
            zaak = GerelateerdeZakenZaakPatch(
                gerelateerdeZaken = addGerelateerdeZaak(zaak.gerelateerdeZaken, otherZaak.url)
            ),
            explanation = explanation
        )
    }

    private fun koppelHoofdEnDeelzaak(hoofdZaak: Zaak, deelZaak: Zaak) {
        zrcClientService.patchZaak(
            zaakUUID = deelZaak.uuid,
            zaak = NillableHoofdzaakZaakPatch(hoofdzaak = hoofdZaak.url)
        )
        // Open Zaak only sends a notification for the deelzaak.
        // So we manually send a ScreenEvent for the hoofdzaak.
        indexingService.addOrUpdateZaak(hoofdZaak.uuid, false)
        eventingService.send(ScreenEventType.ZAAK.updated(hoofdZaak.uuid))
    }

    private fun ontkoppelGerelateerdeZaken(
        zaak: Zaak,
        andereZaak: Zaak,
        explanation: String
    ) = zrcClientService.patchZaak(
        zaakUUID = zaak.uuid,
        zaak = GerelateerdeZakenZaakPatch(
            gerelateerdeZaken = removeGerelateerdeZaak(zaak.gerelateerdeZaken, andereZaak.url)
        ),
        explanation = explanation
    )

    private fun ontkoppelHoofdEnDeelzaak(
        hoofdZaak: Zaak,
        deelZaak: Zaak,
        explanation: String
    ) {
        zrcClientService.patchZaak(
            zaakUUID = deelZaak.uuid,
            zaak = NillableHoofdzaakZaakPatch(hoofdzaak = null),
            explanation = explanation
        )
        // Hiervoor wordt door open zaak alleen voor de deelzaak een notificatie verstuurd.
        // Dus zelf het ScreenEvent versturen voor de hoofdzaak!
        indexingService.addOrUpdateZaak(hoofdZaak.uuid, false)
        eventingService.send(ScreenEventType.ZAAK.updated(hoofdZaak.uuid))
    }

    private fun removeGerelateerdeZaak(
        gerelateerdeZaken: MutableList<GerelateerdeZaak>?,
        andereZaakURI: URI
    ): List<GerelateerdeZaak> {
        gerelateerdeZaken?.removeIf { it.url == andereZaakURI }
        return gerelateerdeZaken ?: emptyList()
    }
}
