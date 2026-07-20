/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.event.EventingService
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.GerelateerdeZakenZaakPatch
import nl.info.client.zgw.zrc.model.NillableHoofdzaakZaakPatch
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.model.RestFindLinkableZakenRequest
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestZaakLinkData
import nl.info.zac.app.zaak.model.RestZaakUnlinkData
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuration.ConfigurationService
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
import nl.info.zac.zaak.model.ZaakLinkData
import nl.info.zac.zaak.ZaakService
import nl.info.zac.zaak.model.canBeHoofdzaakFor
import nl.info.zac.zaak.model.canBeRelatedTo
import nl.info.zac.zaak.model.canBeUnlinkedFromDeelzaak
import nl.info.zac.zaak.model.canBeUnlinkedFromRelatedZaak
import nl.info.zac.zaak.model.toZaakLinkData
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
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val restZaaktypeConverter: RestZaaktypeConverter,
    private val configurationService: ConfigurationService,
) {
    @GET
    @Path("gekoppelde-zaken/{zaakUuid}/zoek-koppelbare-zaken")
    @Suppress("UnusedParameter")
    fun findLinkableZaken(
        @BeanParam @Valid request: RestFindLinkableZakenRequest
    ): RestZoekResultaat<RestZaakKoppelenZoekObject> {
        val zaak = zrcClientService.readZaak(request.zaakUuid)
        val searchResults = searchService.zoek(
            zoekParameters = buildZoekParameters(zaak, request)
        )
        return filterSearchResults(
            searchResults = searchResults,
            zaak = zaak,
            relationType = request.relationType,
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
                zaak.toZaakLinkData(user, zaakType).canBeRelatedTo(
                    zaakToLinkTo.toZaakLinkData(user, zaakToLinkToZaakType)
                )
            ).also {
                koppelGerelateerdeZaken(zaak, zaakToLinkTo, restZaakLinkData.reden)
            }
            RelatieType.HOOFDZAAK -> assertPolicy(
                zaakToLinkTo
                    .toZaakLinkData(user, zaakToLinkToZaakType)
                    .canBeHoofdzaakFor(
                    zaak.toZaakLinkData(user, zaakType),
                    zaakToLinkToZaakType.getDeelzaaktypenSet()
                )
            ).also {
                koppelHoofdEnDeelzaak(zaakToLinkTo, zaak)
            }
            RelatieType.DEELZAAK -> assertPolicy(
                zaak
                    .toZaakLinkData(user, zaakType)
                    .canBeHoofdzaakFor(
                    zaakToLinkTo.toZaakLinkData(user, zaakToLinkToZaakType),
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
                linkedZaak
                    .toZaakLinkData(loggedInUserInstance.get(), linkedZaakType)
                    .canBeUnlinkedFromDeelzaak(
                        zaak.toZaakLinkData(loggedInUserInstance.get(), zaakType)
                    )
            ).also {
                ontkoppelHoofdEnDeelzaak(
                    hoofdZaak = linkedZaak,
                    deelZaak = zaak,
                    explanation = restZaakUnlinkData.reden
                )
            }
            RelatieType.DEELZAAK -> assertPolicy(
                zaak
                    .toZaakLinkData(loggedInUserInstance.get(), zaakType)
                    .canBeUnlinkedFromDeelzaak(
                        linkedZaak.toZaakLinkData(loggedInUserInstance.get(), linkedZaakType)
                    )
            ).also {
                ontkoppelHoofdEnDeelzaak(
                    hoofdZaak = zaak,
                    deelZaak = linkedZaak,
                    explanation = restZaakUnlinkData.reden
                )
            }
            RelatieType.GERELATEERD -> assertPolicy(
                zaak
                    .toZaakLinkData(loggedInUserInstance.get(), zaakType)
                    .canBeUnlinkedFromRelatedZaak(
                        linkedZaak.toZaakLinkData(loggedInUserInstance.get(), linkedZaakType)
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

    @GET
    @Path("gekoppelde-zaken/zaaktypen")
    fun listZaaktypesToLink(): List<RestZaaktype> =
        ztcClientService.listZaaktypen(configurationService.readDefaultCatalogusURI())
            .asSequence()
            .filter {
                policyService.readOverigeRechten(it.omschrijving).zoeken
            }
            .filter { !it.concept }
            .filter { it.isNuGeldig() }
            .map(restZaaktypeConverter::convert)
            .toList()

    private fun ZaakType.getDeelzaaktypenSet() = this.deelzaaktypen
        .map{ it.extractUuid() }
        .toSet()

    private fun ZaakZoekObject.toZaakLinkData() =
        policyService.readZaakRechtenForZaakZoekObject(this).let{ rechten ->
            ZaakLinkData(
                isOpen = this.archiefNominatie == null,
                isHoofdzaak = this.isIndicatie(HOOFDZAAK),
                isDeelzaak =  this.isIndicatie(DEELZAAK),
                zaaktypeUUID = UUID.fromString(this.zaaktypeUuid),
                lezen = rechten.lezen,
                koppelen = rechten.koppelen
            )
        }

    private fun Zaak.toZaakLinkData(user: LoggedInUser, zaaktype: ZaakType) =
        policyService.readZaakRechten(this, zaaktype, user).let{ rechten -> this.toZaakLinkData(rechten) }

    private fun buildZoekParameters(
        zaak: Zaak,
        request: RestFindLinkableZakenRequest
    ) = ZoekParameters(ZoekObjectType.ZAAK).apply {
        start = request.page * request.rows
        rows = request.rows
        addFilter(FilterVeld.ZAAK_IDENTIFICATIE, FilterParameters(listOf(zaak.identificatie), true))
        request.zoekZaakIdentifier?.takeIf { it.isNotBlank() }?.let {
            addZoekVeld(ZoekVeld.ZAAK_IDENTIFICATIE, it.trim())
        }
        request.zoekZaakOmschrijving?.takeIf { it.isNotBlank() }?.let {
            addZoekVeld(ZoekVeld.ZAAK_OMSCHRIJVING, it.trim())
        }
        request.zoekZaakType?.let {
            addFilter(FilterVeld.ZAAK_ZAAKTYPE_UUID, it.toString())
        }
    }

    private fun filterSearchResults(
        searchResults: ZoekResultaat<out ZoekObject>,
        zaak: Zaak,
        relationType: RelatieType,
        user: LoggedInUser
    ): RestZoekResultaat<RestZaakKoppelenZoekObject> {
        val zaaktype = zaakService.readZaakTypeByZaak(zaak)
        val koppelData = zaak.toZaakLinkData(user, zaaktype)
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
        sourceZaak: ZaakLinkData,
        sourceZaaktype: ZaakType,
        targetZaak: ZaakZoekObject,
        relationType: RelatieType): Boolean =
        when (relationType) {
            // "The case you are searching for here will become the main case"
            RelatieType.HOOFDZAAK -> targetZaak.toZaakLinkData().canBeHoofdzaakFor(
                deelzaak = sourceZaak,
                allowedDeelzaaktypes = ztcClientService
                    .readZaaktype(UUID.fromString(targetZaak.zaaktypeUuid))
                    .getDeelzaaktypenSet()
            )
            RelatieType.DEELZAAK -> sourceZaak.canBeHoofdzaakFor(
                deelzaak = targetZaak.toZaakLinkData(),
                allowedDeelzaaktypes = sourceZaaktype.getDeelzaaktypenSet()
            )
            RelatieType.GERELATEERD -> sourceZaak.canBeRelatedTo(targetZaak.toZaakLinkData())
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
