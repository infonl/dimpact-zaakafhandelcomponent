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
import nl.info.client.zgw.ztc.model.generated.ZaakType
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
import nl.info.zac.zaak.model.ZaakKoppelenData
import nl.info.zac.zaak.ZaakService
import nl.info.zac.zaak.canBeHoofdAndDeelzaak
import nl.info.zac.zaak.canBeRelated
import nl.info.zac.zaak.hoofdAndDeelzaakCanBeOntkoppeld
import nl.info.zac.zaak.model.toKoppelData
import nl.info.zac.zaak.relatedZakenCanBeOntkoppeld
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
        val zaak = zrcClientService.readZaak(zaakUuid)
        val searchResults = searchService.zoek(
            zoekParameters = buildZoekParameters(zaak, zoekZaakIdentifier, page, rows)
        )
        return filterSearchResults(
            searchResults = searchResults,
            zaak = zaak,
            relationType = relationType,
            user = loggedInUserInstance.get()
        )
    }

    @PATCH
    @Path("/zaak/koppel")
    fun linkZaak(restZaakLinkData: RestZaakLinkData) {
        val user = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid)
        val (zaakToLinkTo, zaakToLinkToZaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(
            restZaakLinkData.teKoppelenZaakUuid
        )
        when (restZaakLinkData.relatieType) {
            RelatieType.GERELATEERD -> assertPolicy(
                canBeRelated(
                    zaak.toKoppelData(user, zaakType),
                    zaakToLinkTo.toKoppelData(user, zaakToLinkToZaakType)
                )
            ).also {
                koppelGerelateerdeZaken(zaak, zaakToLinkTo, restZaakLinkData.reden)
            }
            RelatieType.HOOFDZAAK -> assertPolicy(
                canBeHoofdAndDeelzaak(
                    zaakToLinkTo.toKoppelData(user, zaakToLinkToZaakType),
                    zaak.toKoppelData(user, zaakType),
                    zaakToLinkToZaakType.getDeelzaaktypenSet()
                )
            ).also {
                koppelHoofdEnDeelzaak(zaakToLinkTo, zaak)
            }
            RelatieType.DEELZAAK -> assertPolicy(
                canBeHoofdAndDeelzaak(
                    zaak.toKoppelData(user, zaakType),
                    zaakToLinkTo.toKoppelData(user, zaakToLinkToZaakType),
                    zaakType.getDeelzaaktypenSet()

                )
            ).also {
                koppelHoofdEnDeelzaak(zaak, zaakToLinkTo)
            }
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

        when (restZaakUnlinkData.relatieType) {
            RelatieType.HOOFDZAAK -> assertPolicy(
                hoofdAndDeelzaakCanBeOntkoppeld(
                    linkedZaak.toKoppelData(loggedInUserInstance.get(), linkedZaakType),
                    zaak.toKoppelData(loggedInUserInstance.get(), zaakType)
                )
            ).also {
                ontkoppelHoofdEnDeelzaak(
                    hoofdZaak = linkedZaak,
                    deelZaak = zaak,
                    explanation = restZaakUnlinkData.reden
                )
            }
            RelatieType.DEELZAAK -> assertPolicy(
                hoofdAndDeelzaakCanBeOntkoppeld(
                    zaak.toKoppelData(loggedInUserInstance.get(), zaakType),
                    linkedZaak.toKoppelData(loggedInUserInstance.get(), linkedZaakType)
                )
            ).also {
                ontkoppelHoofdEnDeelzaak(
                    hoofdZaak = zaak,
                    deelZaak = linkedZaak,
                    explanation = restZaakUnlinkData.reden
                )
            }
            RelatieType.GERELATEERD -> assertPolicy(
                relatedZakenCanBeOntkoppeld(
                    zaak.toKoppelData(loggedInUserInstance.get(), zaakType),
                    linkedZaak.toKoppelData(loggedInUserInstance.get(), linkedZaakType)
                )
            ).also {
                ontkoppelGerelateerdeZaken(
                    zaak = zaak,
                    andereZaak = linkedZaak,
                    explanation = restZaakUnlinkData.reden
                )
            }
            else -> throw IllegalArgumentException(
                "RelatieType ${restZaakUnlinkData.relatieType} cannot be used for unlinking zaken"
            )
        }
    }

    private fun ZaakType.getDeelzaaktypenSet() = this.deelzaaktypen
        .map{ it.extractUuid() }
        .toSet()

    private fun ZaakZoekObject.toKoppelData() =
        policyService.readZaakRechtenForZaakZoekObject(this).let{ rechten ->
            ZaakKoppelenData(
                isOpen = this.archiefNominatie == null,
                isHoofdzaak = this.isIndicatie(HOOFDZAAK),
                isDeelzaak =  this.isIndicatie(DEELZAAK),
                zaaktypeUUID = UUID.fromString(this.zaaktypeUuid),
                lezen = rechten.lezen,
                koppelen = rechten.koppelen
            )
        }

    private fun Zaak.toKoppelData(user: LoggedInUser, zaaktype: ZaakType) =
        policyService.readZaakRechten(this, zaaktype, user).let{ rechten -> this.toKoppelData(rechten) }

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
        user: LoggedInUser
    ): RestZoekResultaat<RestZaakKoppelenZoekObject> {
        val zaaktype = zaakService.readZaakTypeByZaak(zaak)
        val koppelData = zaak.toKoppelData(user, zaaktype)
        return RestZoekResultaat(
            searchResults.items.map {
                val zaakZoekObject = it as ZaakZoekObject
                zaakZoekObject.toRestZaakKoppelenZoekObject(
                    isLinkableTo(koppelData, zaaktype, zaakZoekObject, relationType),
                )
            },
            searchResults.count
        )
    }




    private fun isLinkableTo(
        sourceZaak: ZaakKoppelenData,
        sourceZaaktype: ZaakType,
        targetZaak: ZaakZoekObject,
        relationType: RelatieType): Boolean =
        when (relationType) {
            // "The case you are searching for here will become the main case"
            RelatieType.HOOFDZAAK -> canBeHoofdAndDeelzaak(
                hoofdzaak = targetZaak.toKoppelData(),
                deelzaak = sourceZaak,
                allowedDeelzaaktypes = ztcClientService
                    .readZaaktype(UUID.fromString(targetZaak.zaaktypeUuid))
                    .getDeelzaaktypenSet()
            )
            RelatieType.DEELZAAK -> canBeHoofdAndDeelzaak(
                hoofdzaak = sourceZaak,
                deelzaak = targetZaak.toKoppelData(),
                allowedDeelzaaktypes = sourceZaaktype.getDeelzaaktypenSet()
                )
            RelatieType.GERELATEERD -> canBeRelated(sourceZaak, targetZaak.toKoppelData())
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
