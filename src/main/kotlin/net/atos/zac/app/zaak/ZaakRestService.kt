/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.generated.BesluitInformatieObject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.AardRelatie
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.HoofdzaakZaakPatch
import net.atos.client.zgw.zrc.model.LocatieZaakPatch
import net.atos.client.zgw.zrc.model.RelevanteZaak
import net.atos.client.zgw.zrc.model.RelevantezaakZaakPatch
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.extensions.isNuGeldig
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ZaakAfzender
import net.atos.zac.admin.model.ZaakAfzender.Speciaal
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.admin.model.ZaakbeeindigParameter
import net.atos.zac.app.admin.converter.RESTZaakAfzenderConverter
import net.atos.zac.app.admin.model.RESTZaakAfzender
import net.atos.zac.app.audit.converter.RESTHistorieRegelConverter
import net.atos.zac.app.audit.model.RESTHistorieRegel
import net.atos.zac.app.bag.converter.RESTBAGConverter
import net.atos.zac.app.besluit.BesluitService
import net.atos.zac.app.klant.KlantRestService
import net.atos.zac.app.klant.model.klant.IdentificatieType
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag
import net.atos.zac.app.zaak.converter.RestBesluitConverter
import net.atos.zac.app.zaak.converter.RestGeometryConverter
import net.atos.zac.app.zaak.converter.RestZaakConverter
import net.atos.zac.app.zaak.converter.RestZaakOverzichtConverter
import net.atos.zac.app.zaak.converter.RestZaaktypeConverter
import net.atos.zac.app.zaak.converter.historie.RESTZaakHistorieRegelConverter
import net.atos.zac.app.zaak.model.RESTDocumentOntkoppelGegevens
import net.atos.zac.app.zaak.model.RESTReden
import net.atos.zac.app.zaak.model.RESTZaakAanmaakGegevens
import net.atos.zac.app.zaak.model.RESTZaakAfbrekenGegevens
import net.atos.zac.app.zaak.model.RESTZaakAfsluitenGegevens
import net.atos.zac.app.zaak.model.RESTZaakBetrokkeneGegevens
import net.atos.zac.app.zaak.model.RESTZaakEditMetRedenGegevens
import net.atos.zac.app.zaak.model.RESTZaakHeropenenGegevens
import net.atos.zac.app.zaak.model.RESTZaakKoppelGegevens
import net.atos.zac.app.zaak.model.RESTZaakLocatieGegevens
import net.atos.zac.app.zaak.model.RESTZaakOntkoppelGegevens
import net.atos.zac.app.zaak.model.RESTZaakOpschortGegevens
import net.atos.zac.app.zaak.model.RESTZaakOpschorting
import net.atos.zac.app.zaak.model.RESTZaakVerlengGegevens
import net.atos.zac.app.zaak.model.RESTZakenVerdeelGegevens
import net.atos.zac.app.zaak.model.RESTZakenVrijgevenGegevens
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.app.zaak.model.RestBesluit
import net.atos.zac.app.zaak.model.RestBesluitIntrekkenGegevens
import net.atos.zac.app.zaak.model.RestBesluitVastleggenGegevens
import net.atos.zac.app.zaak.model.RestBesluitWijzigenGegevens
import net.atos.zac.app.zaak.model.RestBesluittype
import net.atos.zac.app.zaak.model.RestResultaattype
import net.atos.zac.app.zaak.model.RestZaak
import net.atos.zac.app.zaak.model.RestZaakAssignmentData
import net.atos.zac.app.zaak.model.RestZaakAssignmentToLoggedInUserData
import net.atos.zac.app.zaak.model.RestZaakBetrokkene
import net.atos.zac.app.zaak.model.RestZaakOverzicht
import net.atos.zac.app.zaak.model.RestZaaktype
import net.atos.zac.app.zaak.model.toRestBesluittypes
import net.atos.zac.app.zaak.model.toRestResultaatTypes
import net.atos.zac.app.zaak.model.toRestZaakBetrokkenen
import net.atos.zac.app.zaak.model.updateBesluitWithBesluitWijzigenGegevens
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.bpmn.BPMNService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.healthcheck.HealthCheckService
import net.atos.zac.identity.IdentityService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.productaanvraag.ProductaanvraagService
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.util.UriUtil
import net.atos.zac.util.time.LocalDateUtil
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zaak.ZaakService
import net.atos.zac.zoeken.IndexingService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.collections4.CollectionUtils
import java.net.URI
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

@Path("zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
@NoArgConstructor
@AllOpen
class ZaakRestService @Inject constructor(
    private val zgwApiService: ZGWApiService,
    private val productaanvraagService: ProductaanvraagService,
    private val besluitService: BesluitService,
    private val brcClientService: BrcClientService,
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val eventingService: EventingService,
    private val identityService: IdentityService,
    private val signaleringService: SignaleringService,
    private val ontkoppeldeDocumentenService: OntkoppeldeDocumentenService,
    private val indexingService: IndexingService,
    private val policyService: PolicyService,
    private val cmmnService: CMMNService,
    private val bpmnService: BPMNService,
    private val flowableTaskService: FlowableTaskService,
    private val objectsClientService: ObjectsClientService,
    private val inboxProductaanvraagService: InboxProductaanvraagService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val configuratieService: ConfiguratieService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val restZaakConverter: RestZaakConverter,
    private val restZaaktypeConverter: RestZaaktypeConverter,
    private val restBesluitConverter: RestBesluitConverter,
    private val restZaakOverzichtConverter: RestZaakOverzichtConverter,
    private val restBAGConverter: RESTBAGConverter,
    private val restHistorieRegelConverter: RESTHistorieRegelConverter,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val restGeometryConverter: RestGeometryConverter,
    private val healthCheckService: HealthCheckService,
    private val opschortenZaakHelper: OpschortenZaakHelper,
    private val zaakService: ZaakService,
    private val restZaakHistorieRegelConverter: RESTZaakHistorieRegelConverter
) {
    companion object {
        private const val ROL_VERWIJDER_REDEN = "Verwijderd door de medewerker tijdens het behandelen van de zaak"
        private const val ROL_TOEVOEGEN_REDEN = "Toegekend door de medewerker tijdens het behandelen van de zaak"
        private const val AANMAKEN_ZAAK_REDEN = "Aanmaken zaak"
        private const val VERLENGING = "Verlenging"
        private const val AANMAKEN_BESLUIT_TOELICHTING = "Aanmaken besluit"
        const val AANVULLENDE_INFORMATIE_TASK_NAME = "Aanvullende informatie"
    }

    @GET
    @Path("zaak/{uuid}")
    fun readZaak(@PathParam("uuid") zaakUUID: UUID): RestZaak =
        zrcClientService.readZaak(zaakUUID).let { zaak ->
            restZaakConverter.toRestZaak(zaak).also {
                assertPolicy(it.rechten.lezen)
                deleteSignaleringen(zaak)
            }
        }

    @GET
    @Path("zaak/id/{identificatie}")
    fun readZaakById(@PathParam("identificatie") identificatie: String): RestZaak =
        zrcClientService.readZaakByID(identificatie).let { zaak ->
            restZaakConverter.toRestZaak(zaak).also {
                assertPolicy(it.rechten.lezen)
                deleteSignaleringen(zaak)
            }
        }

    @PUT
    @Path("initiator")
    fun updateInitiator(gegevens: RESTZaakBetrokkeneGegevens): RestZaak {
        val zaak = zrcClientService.readZaak(gegevens.zaakUUID)
        zgwApiService.findInitiatorRoleForZaak(zaak)
            .ifPresent { removeInitiator(zaak, it, ROL_VERWIJDER_REDEN) }
        addInitiator(gegevens.betrokkeneIdentificatieType, gegevens.betrokkeneIdentificatie, zaak)
        return restZaakConverter.toRestZaak(zaak)
    }

    @DELETE
    @Path("{uuid}/initiator")
    fun deleteInitiator(@PathParam("uuid") zaakUUID: UUID, reden: RESTReden): RestZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        zgwApiService.findInitiatorRoleForZaak(zaak)
            .ifPresent { removeInitiator(zaak, it, reden.reden) }
        return restZaakConverter.toRestZaak(zaak)
    }

    @POST
    @Path("betrokkene")
    fun createBetrokken(@Valid gegevens: RESTZaakBetrokkeneGegevens): RestZaak {
        val zaak = zrcClientService.readZaak(gegevens.zaakUUID)
        addBetrokkene(
            gegevens.roltypeUUID,
            gegevens.betrokkeneIdentificatieType,
            gegevens.betrokkeneIdentificatie,
            toelichting = gegevens.roltoelichting?.ifEmpty { ROL_TOEVOEGEN_REDEN } ?: ROL_TOEVOEGEN_REDEN,
            zaak
        )
        return restZaakConverter.toRestZaak(zaak)
    }

    @DELETE
    @Path("betrokkene/{uuid}")
    fun deleteBetrokkene(
        @PathParam("uuid") betrokkeneUUID: UUID?,
        reden: RESTReden
    ): RestZaak {
        val betrokkene = zrcClientService.readRol(betrokkeneUUID)
        val zaak = zrcClientService.readZaak(betrokkene.zaak)
        removeBetrokkene(zaak, betrokkene, reden.reden)
        return restZaakConverter.toRestZaak(zaak)
    }

    @POST
    @Path("zaak")
    fun createZaak(@Valid restZaakAanmaakGegevens: RESTZaakAanmaakGegevens): RestZaak {
        val restZaak = restZaakAanmaakGegevens.zaak
        val zaaktype = ztcClientService.readZaaktype(restZaak.zaaktype.uuid)
        // make sure to use the omschrijving of the zaaktype that was retrieved to perform
        // authorisation on zaaktype
        assertPolicy(
            policyService.readOverigeRechten().startenZaak &&
                loggedInUserInstance.get().isAuthorisedForZaaktype(zaaktype.omschrijving)
        )
        val zaak = restZaakConverter.toZaak(restZaak, zaaktype)
            .let(zgwApiService::createZaak)
        restZaak.initiatorIdentificatie?.takeIf { it.isNotEmpty() }?.let {
            addInitiator(
                restZaak.initiatorIdentificatieType!!,
                restZaak.initiatorIdentificatie!!,
                zaak
            )
        }
        restZaak.groep?.let {
            val group = identityService.readGroup(it.id)
            zrcClientService.updateRol(zaak, zaakService.bepaalRolGroep(group, zaak), AANMAKEN_ZAAK_REDEN)
        }
        restZaak.behandelaar?.let {
            val user = identityService.readUser(it.id)
            zrcClientService.updateRol(zaak, zaakService.bepaalRolMedewerker(user, zaak), AANMAKEN_ZAAK_REDEN)
        }
        if (!bpmnService.startProcess(zaak, zaaktype, null)) {
            cmmnService.startCase(
                zaak,
                zaaktype,
                zaakafhandelParameterService.readZaakafhandelParameters(
                    URIUtil.parseUUIDFromResourceURI(zaaktype.url)
                ),
                null
            )
        }
        restZaakAanmaakGegevens.inboxProductaanvraag?.let { inboxProductaanvraag ->
            koppelInboxProductaanvraag(zaak, inboxProductaanvraag)
        }
        restZaakAanmaakGegevens.bagObjecten?.let { restbagObjecten ->
            for (restbagObject in restbagObjecten) {
                val zaakobject: Zaakobject = restBAGConverter.convertToZaakobject(restbagObject, zaak)
                zrcClientService.createZaakobject(zaakobject)
            }
        }
        return restZaakConverter.toRestZaak(zaak)
    }

    @PATCH
    @Path("zaak/{uuid}")
    fun updateZaak(
        @PathParam("uuid") zaakUUID: UUID,
        restZaakEditMetRedenGegevens: RESTZaakEditMetRedenGegevens
    ): RestZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        with(policyService.readZaakRechten(zaak)) {
            assertPolicy(wijzigen && verlengenDoorlooptijd)
        }

        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            restZaakConverter.convertToPatch(restZaakEditMetRedenGegevens.zaak),
            restZaakEditMetRedenGegevens.reden
        )

        val oldFinalDate = zaak.uiterlijkeEinddatumAfdoening
        restZaakEditMetRedenGegevens.zaak.uiterlijkeEinddatumAfdoening?.let { newFinalDate ->
            if (newFinalDate.isBefore(oldFinalDate) && adjustFinalDateForOpenTasks(zaakUUID, newFinalDate) > 0) {
                eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(updatedZaak))
            }
        }

        return restZaakConverter.toRestZaak(updatedZaak)
    }

    @PATCH
    @Path("{uuid}/zaaklocatie")
    fun updateZaakLocatie(
        @PathParam("uuid") zaakUUID: UUID?,
        locatieGegevens: RESTZaakLocatieGegevens
    ): RestZaak {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).wijzigen)
        val locatieZaakPatch = LocatieZaakPatch(
            locatieGegevens.geometrie?.let {
                restGeometryConverter.convert(it)
            }
        )
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            locatieZaakPatch,
            locatieGegevens.reden
        )
        return restZaakConverter.toRestZaak(updatedZaak)
    }

    @PATCH
    @Path("zaak/{uuid}/opschorting")
    fun opschortenZaak(
        @PathParam("uuid") zaakUUID: UUID,
        opschortGegevens: RESTZaakOpschortGegevens
    ): RestZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        return if (opschortGegevens.indicatieOpschorting) {
            restZaakConverter.toRestZaak(
                opschortenZaakHelper.opschortenZaak(
                    zaak,
                    opschortGegevens.duurDagen,
                    opschortGegevens.redenOpschorting
                )
            )
        } else {
            restZaakConverter.toRestZaak(
                opschortenZaakHelper.hervattenZaak(zaak, opschortGegevens.redenOpschorting)
            )
        }
    }

    @GET
    @Path("zaak/{uuid}/opschorting")
    fun readOpschortingZaak(@PathParam("uuid") zaakUUID: UUID): RESTZaakOpschorting {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        val zaakOpschorting = RESTZaakOpschorting()
        zaakVariabelenService.findDatumtijdOpgeschort(zaakUUID)
            .ifPresent { datumtijdOpgeschort -> zaakOpschorting.vanafDatumTijd = datumtijdOpgeschort }
        zaakVariabelenService.findVerwachteDagenOpgeschort(zaakUUID)
            .ifPresent { verwachteDagenOpgeschort -> zaakOpschorting.duurDagen = verwachteDagenOpgeschort }
        return zaakOpschorting
    }

    @PATCH
    @Path("zaak/{uuid}/verlenging")
    fun verlengenZaak(
        @PathParam("uuid") zaakUUID: UUID,
        restZaakVerlengGegevens: RESTZaakVerlengGegevens
    ): RestZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak).verlengen)
        val toelichting = "$VERLENGING: ${restZaakVerlengGegevens.redenVerlenging}"
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            restZaakConverter.convertToPatch(
                zaakUUID,
                restZaakVerlengGegevens
            ),
            toelichting
        )
        if (restZaakVerlengGegevens.takenVerlengen) {
            val aantalTakenVerlengd = verlengOpenTaken(zaakUUID, restZaakVerlengGegevens.duurDagen.toLong())
            if (aantalTakenVerlengd > 0) {
                eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(updatedZaak))
            }
        }
        val status = if (zaak.status != null) {
            zrcClientService.readStatus(zaak.status)
        } else {
            null
        }
        val statustype = if (status != null) {
            ztcClientService.readStatustype(status.statustype)
        } else {
            null
        }
        return restZaakConverter.toRestZaak(updatedZaak, status, statustype)
    }

    @PUT
    @Path("zaakinformatieobjecten/ontkoppel")
    fun ontkoppelInformatieObject(ontkoppelGegevens: RESTDocumentOntkoppelGegevens) {
        val zaak: Zaak = zrcClientService.readZaak(ontkoppelGegevens.zaakUUID)
        val informatieobject: EnkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(
            ontkoppelGegevens.documentUUID
        )
        assertPolicy(policyService.readDocumentRechten(informatieobject, zaak).ontkoppelen)
        val parameters = ZaakInformatieobjectListParameters()
        parameters.informatieobject = informatieobject.url
        parameters.zaak = zaak.url
        val zaakInformatieobjecten: List<ZaakInformatieobject> = zrcClientService.listZaakinformatieobjecten(
            parameters
        )
        if (zaakInformatieobjecten.isEmpty()) {
            throw NotFoundException(
                "Geen ZaakInformatieobject gevonden voor Zaak: '${ontkoppelGegevens.zaakUUID}' " +
                    "en InformatieObject: '${ontkoppelGegevens.documentUUID}'"
            )
        }
        zaakInformatieobjecten.forEach(
            Consumer { zaakInformatieobject: ZaakInformatieobject ->
                zrcClientService.deleteZaakInformatieobject(
                    zaakInformatieobject.uuid,
                    ontkoppelGegevens.reden,
                    "Ontkoppeld"
                )
            }
        )
        if (zrcClientService.listZaakinformatieobjecten(informatieobject).isEmpty()) {
            indexingService.removeInformatieobject(URIUtil.parseUUIDFromResourceURI(informatieobject.url))
            ontkoppeldeDocumentenService.create(informatieobject, zaak, ontkoppelGegevens.reden)
        }
    }

    @GET
    @Path("waarschuwing")
    fun listZaakWaarschuwingen(): List<RestZaakOverzicht> {
        val vandaag = LocalDate.now()
        val einddatumGeplandWaarschuwing = mutableMapOf<UUID, LocalDate>()
        val uiterlijkeEinddatumAfdoeningWaarschuwing = mutableMapOf<UUID, LocalDate>()
        zaakafhandelParameterService.listZaakafhandelParameters().forEach(
            Consumer { parameters: ZaakafhandelParameters ->
                if (parameters.einddatumGeplandWaarschuwing != null) {
                    einddatumGeplandWaarschuwing[parameters.zaakTypeUUID] = datumWaarschuwing(
                        vandaag,
                        parameters.einddatumGeplandWaarschuwing
                    )
                }
                if (parameters.uiterlijkeEinddatumAfdoeningWaarschuwing != null) {
                    uiterlijkeEinddatumAfdoeningWaarschuwing[parameters.zaakTypeUUID] = datumWaarschuwing(
                        vandaag,
                        parameters.uiterlijkeEinddatumAfdoeningWaarschuwing
                    )
                }
            }
        )
        val zaakListParameters = ZaakListParameters()
        zaakListParameters.rolBetrokkeneIdentificatieMedewerkerIdentificatie = loggedInUserInstance.get().id
        return zrcClientService.listZaken(zaakListParameters).results
            .filter { obj -> obj.isOpen }
            .filter { zaak ->
                isWaarschuwing(
                    zaak,
                    vandaag,
                    einddatumGeplandWaarschuwing,
                    uiterlijkeEinddatumAfdoeningWaarschuwing
                )
            }
            .map { zaak -> restZaakOverzichtConverter.convert(zaak) }
            .toList()
    }

    @GET
    @Path("zaaktypes")
    fun listZaaktypes(): List<RestZaaktype> =
        ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())
            .asSequence()
            .filter { loggedInUserInstance.get().isAuthorisedForZaaktype(it.omschrijving) }
            .filter { !it.concept }
            .filter { it.isNuGeldig() }
            .filter {
                (configuratieService.featureFlagBpmnSupport() && it.referentieproces?.naam != null) ||
                    healthCheckService.controleerZaaktype(it.url).isValide
            }
            .map { restZaaktypeConverter.convert(it) }
            .toList()

    @PUT
    @Path("zaakdata")
    fun updateZaakdata(restZaak: RestZaak): RestZaak {
        val zaak = zrcClientService.readZaak(restZaak.uuid)
        assertPolicy(policyService.readZaakRechten(zaak).wijzigen)

        zaakVariabelenService.setZaakdata(restZaak.uuid, restZaak.zaakdata)
        return restZaak
    }

    @PATCH
    @Path("toekennen")
    fun assign(@Valid toekennenGegevens: RestZaakAssignmentData): RestZaak {
        val zaak = zrcClientService.readZaak(toekennenGegevens.zaakUUID).also {
            assertPolicy(policyService.readZaakRechten(it).toekennen)
        }
        val behandelaar = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            .map { it.betrokkeneIdentificatie.identificatie }
            .orElse(null)
        val isUpdated = AtomicBoolean(false)
        if (behandelaar != toekennenGegevens.assigneeUserName) {
            toekennenGegevens.assigneeUserName?.takeIf { it.isNotEmpty() }?.let {
                val user = identityService.readUser(it)
                zrcClientService.updateRol(zaak, zaakService.bepaalRolMedewerker(user, zaak), toekennenGegevens.reason)
            }
                // no, or an empty behandelaarGebruikersnaam means the zaak should be unassigned
                ?: zrcClientService.deleteRol(zaak, BetrokkeneType.MEDEWERKER, toekennenGegevens.reason)
            isUpdated.set(true)
        }
        zgwApiService.findGroepForZaak(zaak).ifPresent {
            val groupId = toekennenGegevens.groupId
            if (it.betrokkeneIdentificatie.identificatie != groupId) {
                val group = identityService.readGroup(groupId)
                val role = zaakService.bepaalRolGroep(group, zaak)
                zrcClientService.updateRol(zaak, role, toekennenGegevens.reason)
                isUpdated.set(true)
            }
        }
        if (isUpdated.get()) {
            indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
        }
        return restZaakConverter.toRestZaak(zaak)
    }

    @PUT
    @Path("lijst/toekennen/mij")
    fun assignToLoggedInUserFromList(
        @Valid restZaakAssignmentToLoggedInUserData: RestZaakAssignmentToLoggedInUserData
    ): RestZaakOverzicht {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        val zaak = assignLoggedInUserToZaak(
            zaakUUID = restZaakAssignmentToLoggedInUserData.zaakUUID,
            reason = restZaakAssignmentToLoggedInUserData.reason,
        )
        indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
        return restZaakOverzichtConverter.convert(zaak)
    }

    /**
     * Assign one or multiple zaken in a batch operation.
     * This can be a long-running operation, so it is run asynchronously.
     */
    @PUT
    @Path("lijst/verdelen")
    fun assignFromList(@Valid restZakenVerdeelGegevens: RESTZakenVerdeelGegevens) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            zaakService.assignZaken(
                zaakUUIDs = restZakenVerdeelGegevens.uuids,
                explanation = restZakenVerdeelGegevens.reden,
                group = restZakenVerdeelGegevens.groepId.let {
                    identityService.readGroup(
                        restZakenVerdeelGegevens.groepId
                    )
                },
                user = restZakenVerdeelGegevens.behandelaarGebruikersnaam?.let {
                    identityService.readUser(it)
                },
                screenEventResourceId = restZakenVerdeelGegevens.screenEventResourceId
            )
        }
    }

    /**
     * Release one or multiple zaken in a batch operation.
     * This can be a long-running operation, so it is run asynchronously.
     */
    @PUT
    @Path("lijst/vrijgeven")
    fun releaseFromList(@Valid restZakenVrijgevenGegevens: RESTZakenVrijgevenGegevens) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            zaakService.releaseZaken(
                zaakUUIDs = restZakenVrijgevenGegevens.uuids,
                explanation = restZakenVrijgevenGegevens.reden,
                screenEventResourceId = restZakenVrijgevenGegevens.screenEventResourceId
            )
        }
    }

    @PATCH
    @Path("/zaak/{uuid}/afbreken")
    fun afbreken(
        @PathParam("uuid") zaakUUID: UUID,
        afbrekenGegevens: RESTZaakAfbrekenGegevens
    ) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        val statustype = if (zaak.status != null) {
            ztcClientService.readStatustype(
                zrcClientService.readStatus(zaak.status).statustype
            )
        } else {
            null
        }
        assertPolicy(policyService.readZaakRechten(zaak).afbreken)
        assertPolicy(zaak.isOpen && !StatusTypeUtil.isHeropend(statustype))
        policyService.checkZaakAfsluitbaar(zaak)
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
            UriUtil.uuidFromURI(zaak.zaaktype)
        )
        val zaakbeeindigParameter: ZaakbeeindigParameter = zaakafhandelParameters.readZaakbeeindigParameter(
            afbrekenGegevens.zaakbeeindigRedenId
        )
        zgwApiService.createResultaatForZaak(
            zaak,
            zaakbeeindigParameter.resultaattype,
            zaakbeeindigParameter.zaakbeeindigReden.naam
        )
        zgwApiService.endZaak(zaak, zaakbeeindigParameter.zaakbeeindigReden.naam)
        // Terminate case after the zaak is ended in order to prevent the EndCaseLifecycleListener from ending the zaak.
        cmmnService.terminateCase(zaakUUID)
    }

    @PATCH
    @Path("/zaak/{uuid}/heropenen")
    fun heropenen(
        @PathParam("uuid") zaakUUID: UUID?,
        heropenenGegevens: RESTZaakHeropenenGegevens
    ) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(!zaak.isOpen && policyService.readZaakRechten(zaak).heropenen)
        zgwApiService.createStatusForZaak(
            zaak,
            ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND,
            heropenenGegevens.reden
        )
        zaak.resultaat?.let {
            zrcClientService.deleteResultaat(UriUtil.uuidFromURI(it))
        }
    }

    @PATCH
    @Path("/zaak/{uuid}/afsluiten")
    fun afsluiten(
        @PathParam("uuid") zaakUUID: UUID?,
        afsluitenGegevens: RESTZaakAfsluitenGegevens
    ) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(zaak.isOpen && policyService.readZaakRechten(zaak).behandelen)
        policyService.checkZaakAfsluitbaar(zaak)
        zgwApiService.updateResultaatForZaak(
            zaak,
            afsluitenGegevens.resultaattypeUuid,
            afsluitenGegevens.reden
        )
        zgwApiService.closeZaak(zaak, afsluitenGegevens.reden)
    }

    @PATCH
    @Path("/zaak/koppel")
    fun koppelZaak(gegevens: RESTZaakKoppelGegevens) {
        val zaak: Zaak = zrcClientService.readZaak(gegevens.zaakUuid)
        val teKoppelenZaak: Zaak = zrcClientService.readZaak(gegevens.teKoppelenZaakUuid)
        assertPolicy(zaak.isOpen == teKoppelenZaak.isOpen)
        assertPolicy(policyService.readZaakRechten(zaak).koppelen)
        assertPolicy(policyService.readZaakRechten(teKoppelenZaak).koppelen)

        when (gegevens.relatieType) {
            RelatieType.HOOFDZAAK -> koppelHoofdEnDeelzaak(teKoppelenZaak, zaak)
            RelatieType.DEELZAAK -> koppelHoofdEnDeelzaak(zaak, teKoppelenZaak)
            RelatieType.VERVOLG -> koppelRelevantezaken(zaak, teKoppelenZaak, AardRelatie.VERVOLG)
            RelatieType.ONDERWERP -> koppelRelevantezaken(zaak, teKoppelenZaak, AardRelatie.ONDERWERP)
            RelatieType.BIJDRAGE -> koppelRelevantezaken(zaak, teKoppelenZaak, AardRelatie.BIJDRAGE)
        }
        gegevens.reverseRelatieType?.let { reverseRelatieType ->
            when (reverseRelatieType) {
                RelatieType.VERVOLG -> koppelRelevantezaken(teKoppelenZaak, zaak, AardRelatie.VERVOLG)
                RelatieType.ONDERWERP -> koppelRelevantezaken(teKoppelenZaak, zaak, AardRelatie.ONDERWERP)
                RelatieType.BIJDRAGE -> koppelRelevantezaken(teKoppelenZaak, zaak, AardRelatie.BIJDRAGE)
                else -> error("Reverse relatie type $reverseRelatieType is not supported")
            }
        }
    }

    @PATCH
    @Path("/zaak/ontkoppel")
    fun ontkoppelZaak(gegevens: RESTZaakOntkoppelGegevens) {
        val zaak: Zaak = zrcClientService.readZaak(gegevens.zaakUuid)
        val gekoppeldeZaak = zrcClientService.readZaakByID(gegevens.gekoppeldeZaakIdentificatie)
        assertPolicy(policyService.readZaakRechten(zaak).wijzigen)
        assertPolicy(policyService.readZaakRechten(gekoppeldeZaak).wijzigen)

        when (gegevens.relatietype) {
            RelatieType.HOOFDZAAK -> ontkoppelHoofdEnDeelzaak(gekoppeldeZaak, zaak, gegevens.reden)
            RelatieType.DEELZAAK -> ontkoppelHoofdEnDeelzaak(zaak, gekoppeldeZaak, gegevens.reden)
            RelatieType.VERVOLG -> ontkoppelRelevantezaken(
                zaak,
                gekoppeldeZaak,
                AardRelatie.VERVOLG,
                gegevens.reden
            )

            RelatieType.ONDERWERP -> ontkoppelRelevantezaken(
                zaak,
                gekoppeldeZaak,
                AardRelatie.ONDERWERP,
                gegevens.reden
            )

            RelatieType.BIJDRAGE -> ontkoppelRelevantezaken(
                zaak,
                gekoppeldeZaak,
                AardRelatie.BIJDRAGE,
                gegevens.reden
            )
        }
    }

    @PUT
    @Path("toekennen/mij")
    fun toekennenAanIngelogdeMedewerker(
        @Valid restZaakAssignmentToLoggedInUserData: RestZaakAssignmentToLoggedInUserData
    ): RestZaak = assignLoggedInUserToZaak(
        zaakUUID = restZaakAssignmentToLoggedInUserData.zaakUUID,
        reason = restZaakAssignmentToLoggedInUserData.reason
    ).let { restZaakConverter.toRestZaak(it) }

    @GET
    @Path("zaak/{uuid}/historie")
    fun listHistorie(@PathParam("uuid") zaakUUID: UUID): List<RESTHistorieRegel> {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        val auditTrail = zrcClientService.listAuditTrail(zaakUUID)
        return auditTrail
            .flatMap(restZaakHistorieRegelConverter::convertZaakRESTHistorieRegel)
            .sortedByDescending { it.datumTijd }
    }

    @GET
    @Path("zaak/{uuid}/betrokkene")
    fun listBetrokkenenVoorZaak(@PathParam("uuid") zaakUUID: UUID): List<RestZaakBetrokkene> {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak).lezen)
        return zrcClientService.listRollen(zaak)
            .filter { rol ->
                KlantRestService.betrokkenen.contains(
                    OmschrijvingGeneriekEnum.valueOf(
                        rol.omschrijvingGeneriek.uppercase(Locale.getDefault())
                    )
                )
            }
            .toRestZaakBetrokkenen()
    }

    /**
     * Retrieve all possible afzenders for a zaak
     *
     * @param zaakUUID the id of the zaak
     * @return list of afzenders
     */
    @GET
    @Path("zaak/{uuid}/afzender")
    fun listAfzendersVoorZaak(@PathParam("uuid") zaakUUID: UUID): List<RESTZaakAfzender> {
        val zaak = zrcClientService.readZaak(zaakUUID)
        return sortAndRemoveDuplicateAfzenders(
            resolveZaakAfzenderMail(
                RESTZaakAfzenderConverter.convertZaakAfzenders(
                    zaakafhandelParameterService.readZaakafhandelParameters(
                        UriUtil.uuidFromURI(zaak.zaaktype)
                    ).zaakAfzenders
                ).stream()
            )
        )
    }

    /**
     * Retrieve the default afzender for a zaak
     *
     * @param zaakUUID the id of the zaak
     * @return the default zaakafzender or null if no default is available
     */
    @GET
    @Path("zaak/{uuid}/afzender/default")
    fun readDefaultAfzenderVoorZaak(@PathParam("uuid") zaakUUID: UUID?): RESTZaakAfzender? {
        val zaak = zrcClientService.readZaak(zaakUUID)
        return resolveZaakAfzenderMail(
            zaakafhandelParameterService.readZaakafhandelParameters(
                UriUtil.uuidFromURI(zaak.zaaktype)
            ).zaakAfzenders
                .filter { it.isDefault }
                .map(RESTZaakAfzenderConverter::convertZaakAfzender)
                .stream()
        )
            .findAny()
            .orElse(null)
    }

    @GET
    @Path("besluit/zaakUuid/{zaakUuid}")
    fun listBesluitenForZaakUUID(@PathParam("zaakUuid") zaakUuid: UUID): List<RestBesluit> =
        zrcClientService.readZaak(zaakUuid)
            .let { brcClientService.listBesluiten(it) }
            .map { restBesluitConverter.convertToRestBesluit(it) }

    @POST
    @Path("besluit")
    fun createBesluit(@Valid besluitToevoegenGegevens: RestBesluitVastleggenGegevens): RestBesluit {
        val zaak = zrcClientService.readZaak(besluitToevoegenGegevens.zaakUuid)
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        assertPolicy(policyService.readZaakRechten(zaak, zaaktype).vastleggenBesluit)
        assertPolicy(CollectionUtils.isNotEmpty(zaaktype.besluittypen))

        val besluit = restBesluitConverter.convertToBesluit(zaak, besluitToevoegenGegevens)
        zaak.resultaat?.let {
            zgwApiService.updateResultaatForZaak(zaak, besluitToevoegenGegevens.resultaattypeUuid, null)
        } ?: run {
            zgwApiService.createResultaatForZaak(zaak, besluitToevoegenGegevens.resultaattypeUuid, null)
        }
        val restBesluit = brcClientService.createBesluit(besluit).let { restBesluitConverter.convertToRestBesluit(it) }
        besluitToevoegenGegevens.informatieobjecten?.forEach { informatieobjectUuid ->
            drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid).let { informatieobject ->
                BesluitInformatieObject().apply {
                    this.informatieobject = informatieobject.url
                    this.besluit = restBesluit.url
                }.let {
                    brcClientService.createBesluitInformatieobject(
                        it,
                        AANMAKEN_BESLUIT_TOELICHTING
                    )
                }
            }
        }

        // This event should result from a ZAAKBESLUIT CREATED notification on the ZAKEN channel
        // but open_zaak does not send that one, so emulate it here.
        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
        return restBesluit
    }

    @PUT
    @Path("besluit")
    fun updateBesluit(
        @Valid restBesluitWijzigenGegevens: RestBesluitWijzigenGegevens
    ): RestBesluit {
        val besluit = brcClientService.readBesluit(restBesluitWijzigenGegevens.besluitUuid)
        val zaak = zrcClientService.readZaak(besluit.zaak).also {
            assertPolicy(policyService.readZaakRechten(it).vastleggenBesluit)
        }
        besluit.updateBesluitWithBesluitWijzigenGegevens(restBesluitWijzigenGegevens).also {
            brcClientService.updateBesluit(it, restBesluitWijzigenGegevens.reden)
        }
        zaak.resultaat?.let {
            zrcClientService.readResultaat(it).let { zaakresultaat ->
                val resultaattype = ztcClientService.readResultaattype(restBesluitWijzigenGegevens.resultaattypeUuid)
                if (!UriUtil.isEqual(zaakresultaat.resultaattype, resultaattype.url)) {
                    zrcClientService.deleteResultaat(zaakresultaat.uuid)
                    zgwApiService.createResultaatForZaak(
                        zaak,
                        restBesluitWijzigenGegevens.resultaattypeUuid,
                        null
                    )
                }
            }
        }
        restBesluitWijzigenGegevens.informatieobjecten?.let {
            besluitService.updateBesluitInformatieobjecten(besluit, it)
        }
        // This event should result from a ZAAKBESLUIT CREATED notification on the ZAKEN channel
        // but open_zaak does not send that one, so emulate it here.
        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
        return restBesluitConverter.convertToRestBesluit(besluit)
    }

    @PUT
    @Path("besluit/intrekken")
    fun intrekkenBesluit(
        @Valid restBesluitIntrekkenGegevens: RestBesluitIntrekkenGegevens
    ): RestBesluit {
        val besluit = besluitService.readBesluit(restBesluitIntrekkenGegevens)
        val zaak = zrcClientService.readZaak(besluit.zaak).also {
            assertPolicy(it.isOpen && policyService.readZaakRechten(it).behandelen)
        }
        return besluitService.withdrawBesluit(besluit, restBesluitIntrekkenGegevens.reden).also {
            // This event should result from a ZAAKBESLUIT UPDATED notification on the ZAKEN channel
            // but open_zaak does not send that one, so emulate it here.
            eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
        }.let {
            restBesluitConverter.convertToRestBesluit(it)
        }
    }

    @GET
    @Path("besluit/{uuid}/historie")
    fun listBesluitHistorie(@PathParam("uuid") uuid: UUID): List<RESTHistorieRegel> =
        brcClientService.listAuditTrail(uuid).let {
            restHistorieRegelConverter.convert(it)
        }

    @GET
    @Path("besluittypes/{zaaktypeUUID}")
    fun listBesluittypes(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID
    ): List<RestBesluittype> {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        return ztcClientService.readBesluittypen(ztcClientService.readZaaktype(zaaktypeUUID).url)
            .filter { LocalDateUtil.dateNowIsBetween(it) }
            .toRestBesluittypes()
    }

    @GET
    @Path("resultaattypes/{zaaktypeUUID}")
    fun listResultaattypes(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID
    ): List<RestResultaattype> {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        return ztcClientService.readResultaattypen(
            ztcClientService.readZaaktype(zaaktypeUUID).url
        ).toRestResultaatTypes()
    }

    @GET
    @Path("{uuid}/procesdiagram")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadProcessDiagram(@PathParam("uuid") uuid: UUID?): Response {
        return Response.ok(bpmnService.getProcessDiagram(uuid))
            .header(
                "Content-Disposition",
                "attachment; filename=\"procesdiagram.gif\""
            )
            .build()
    }

    @GET
    @Path("procesvariabelen")
    fun listProcesVariabelen(): List<String> = ZaakVariabelenService.VARS

    private fun addBetrokkene(
        roltype: UUID,
        identificatieType: IdentificatieType,
        identificatie: String,
        toelichting: String,
        zaak: Zaak
    ) {
        val zaakRechten = policyService.readZaakRechten(zaak)
        val betrokkene = ztcClientService.readRoltype(roltype)
        when (identificatieType) {
            IdentificatieType.BSN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkenePersoon)
                zaakService.addBetrokkenNatuurlijkPersoon(betrokkene, identificatie, zaak, toelichting)
            }

            IdentificatieType.VN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
                zaakService.addBetrokkenVestiging(betrokkene, identificatie, zaak, toelichting)
            }

            IdentificatieType.RSIN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
                zaakService.addBetrokkenNietNatuurlijkPersoon(betrokkene, identificatie, zaak, toelichting)
            }
        }
    }

    private fun addInitiator(
        identificatieType: IdentificatieType,
        identificatie: String,
        zaak: Zaak
    ) {
        val initiator = ztcClientService.readRoltype(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
        val zaakRechten = policyService.readZaakRechten(zaak)
        when (identificatieType) {
            IdentificatieType.BSN -> {
                assertPolicy(zaakRechten.toevoegenInitiatorPersoon)
                zaakService.addBetrokkenNatuurlijkPersoon(initiator, identificatie, zaak, ROL_TOEVOEGEN_REDEN)
            }

            IdentificatieType.VN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
                zaakService.addBetrokkenVestiging(initiator, identificatie, zaak, ROL_TOEVOEGEN_REDEN)
            }

            IdentificatieType.RSIN -> {
                assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
                zaakService.addBetrokkenNietNatuurlijkPersoon(initiator, identificatie, zaak, ROL_TOEVOEGEN_REDEN)
            }
        }
    }

    private fun addRelevanteZaak(
        relevanteZaken: MutableList<RelevanteZaak>?,
        andereZaak: URI,
        aardRelatie: AardRelatie
    ): List<RelevanteZaak> {
        val relevanteZaak = RelevanteZaak(andereZaak, aardRelatie)
        return relevanteZaken?.apply {
            if (none { it.`is`(andereZaak, aardRelatie) }) add(relevanteZaak)
        } ?: listOf(relevanteZaak)
    }

    private fun datumWaarschuwing(vandaag: LocalDate, dagen: Int): LocalDate = vandaag.plusDays(dagen + 1L)

    private fun deleteSignaleringen(zaak: Zaak) {
        signaleringService.deleteSignaleringen(
            SignaleringZoekParameters(loggedInUserInstance.get())
                .types(SignaleringType.Type.ZAAK_OP_NAAM, SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD)
                .subject(zaak)
        )
    }

    private fun assignLoggedInUserToZaak(
        zaakUUID: UUID,
        reason: String?
    ): Zaak = zrcClientService.readZaak(zaakUUID).let { zaak ->
        assertPolicy(zaak.isOpen && policyService.readZaakRechten(zaak).toekennen)
        identityService.readUser(loggedInUserInstance.get().id).let { user ->
            zrcClientService.updateRol(
                zaak,
                zaakService.bepaalRolMedewerker(user, zaak),
                reason
            )
        }
        zaak
    }

    private fun isWaarschuwing(
        zaak: Zaak,
        vandaag: LocalDate,
        einddatumGeplandWaarschuwing: Map<UUID, LocalDate>,
        uiterlijkeEinddatumAfdoeningWaarschuwing: Map<UUID, LocalDate>
    ): Boolean {
        val zaaktypeUUID = UriUtil.uuidFromURI(zaak.zaaktype)
        return (
            zaak.einddatumGepland != null &&
                isWaarschuwing(
                    vandaag,
                    zaak.einddatumGepland,
                    einddatumGeplandWaarschuwing[zaaktypeUUID]
                )
            ) ||
            isWaarschuwing(
                vandaag,
                zaak.uiterlijkeEinddatumAfdoening,
                uiterlijkeEinddatumAfdoeningWaarschuwing[zaaktypeUUID]
            )
    }

    private fun isWaarschuwing(
        vandaag: LocalDate,
        datum: LocalDate,
        datumWaarschuwing: LocalDate?
    ) = datumWaarschuwing != null &&
        !datum.isBefore(vandaag) &&
        datum.isBefore(datumWaarschuwing)

    private fun koppelHoofdEnDeelzaak(hoofdZaak: Zaak, deelZaak: Zaak) {
        zrcClientService.patchZaak(deelZaak.uuid, HoofdzaakZaakPatch(hoofdZaak.url))
        // Hiervoor wordt door open zaak alleen voor de deelzaak een notificatie verstuurd.
        // Dus zelf het ScreenEvent versturen voor de hoofdzaak!
        indexingService.addOrUpdateZaak(hoofdZaak.uuid, false)
        eventingService.send(ScreenEventType.ZAAK.updated(hoofdZaak.uuid))
    }

    private fun koppelInboxProductaanvraag(
        zaak: Zaak,
        inboxProductaanvraag: RESTInboxProductaanvraag
    ) {
        val productaanvraagObject = objectsClientService.readObject(
            inboxProductaanvraag.productaanvraagObjectUUID
        )
        val productaanvraag = productaanvraagService.getProductaanvraag(
            productaanvraagObject
        )
        productaanvraagService.pairProductaanvraagWithZaak(productaanvraagObject, zaak.url)
        productaanvraagService.pairAanvraagPDFWithZaak(productaanvraag, zaak.url)
        productaanvraagService.pairBijlagenWithZaak(
            productaanvraag.bijlagen,
            zaak.url
        )

        // verwijder het verwerkte inbox productaanvraag item
        inboxProductaanvraagService.delete(inboxProductaanvraag.id)
        zaakVariabelenService.setZaakdata(
            zaak.uuid,
            productaanvraagService.getAanvraaggegevens(productaanvraagObject)
        )
    }

    private fun koppelRelevantezaken(
        zaak: Zaak,
        andereZaak: Zaak,
        aardRelatie: AardRelatie
    ) {
        zrcClientService.patchZaak(
            zaak.uuid,
            RelevantezaakZaakPatch(
                addRelevanteZaak(
                    zaak.relevanteAndereZaken,
                    andereZaak.url,
                    aardRelatie
                )
            )
        )
    }

    private fun ontkoppelHoofdEnDeelzaak(
        hoofdZaak: Zaak,
        deelZaak: Zaak,
        reden: String
    ) {
        zrcClientService.patchZaak(deelZaak.uuid, HoofdzaakZaakPatch(null), reden)
        // Hiervoor wordt door open zaak alleen voor de deelzaak een notificatie verstuurd.
        // Dus zelf het ScreenEvent versturen voor de hoofdzaak!
        indexingService.addOrUpdateZaak(hoofdZaak.uuid, false)
        eventingService.send(ScreenEventType.ZAAK.updated(hoofdZaak.uuid))
    }

    private fun ontkoppelRelevantezaken(
        zaak: Zaak,
        andereZaak: Zaak,
        aardRelatie: AardRelatie,
        reden: String
    ) {
        zrcClientService.patchZaak(
            zaak.uuid,
            RelevantezaakZaakPatch(
                removeRelevanteZaak(zaak.relevanteAndereZaken, andereZaak.url, aardRelatie)
            ),
            reden
        )
    }

    private fun removeBetrokkene(zaak: Zaak, betrokkene: Rol<*>, reden: String) {
        assertPolicy(policyService.readZaakRechten(zaak).verwijderenBetrokkene)
        zrcClientService.deleteRol(betrokkene, reden)
    }

    private fun removeInitiator(zaak: Zaak, initiator: Rol<*>, reden: String) {
        assertPolicy(policyService.readZaakRechten(zaak).verwijderenInitiator)
        zrcClientService.deleteRol(initiator, reden)
    }

    private fun resolveZaakAfzenderMail(
        afzenders: Stream<RESTZaakAfzender>
    ): Stream<RESTZaakAfzender> {
        return afzenders.peek { afzender ->
            speciaalMail(afzender.mail)?.let { speciaal ->
                afzender.suffix = "gegevens.mail.afzender.$speciaal"
                afzender.mail = resolveMail(speciaal)
            }
            afzender.replyTo = afzender.replyTo?.let { replyTo ->
                speciaalMail(replyTo)?.let { resolveMail(it) } ?: replyTo
            }
        }.filter { it.mail != null }
    }

    private fun resolveMail(speciaal: Speciaal) =
        when (speciaal) {
            Speciaal.GEMEENTE -> configuratieService.readGemeenteMail()
            Speciaal.MEDEWERKER -> loggedInUserInstance.get().email
        }

    private fun verlengOpenTaken(zaakUUID: UUID, durationDays: Long): Int =
        flowableTaskService.listOpenTasksForZaak(zaakUUID)
            .filter { it.dueDate != null }
            .run {
                forEach { task ->
                    task.dueDate = DateTimeConverterUtil.convertToDate(
                        DateTimeConverterUtil.convertToLocalDate(task.dueDate).plusDays(durationDays)
                    )
                    flowableTaskService.updateTask(task)
                    eventingService.send(ScreenEventType.TAAK.updated(task))
                }
                count()
            }

    private fun adjustFinalDateForOpenTasks(zaakUUID: UUID, zaakFatalDate: LocalDate): Int =
        flowableTaskService.listOpenTasksForZaak(zaakUUID)
            .filter { if (it.name != null) it.name != AANVULLENDE_INFORMATIE_TASK_NAME else true }
            .filter { it.dueDate != null }
            .filter { DateTimeConverterUtil.convertToLocalDate(it.dueDate).isAfter(zaakFatalDate) }
            .run {
                forEach { task ->
                    task.dueDate = DateTimeConverterUtil.convertToDate(zaakFatalDate)
                    flowableTaskService.updateTask(task)
                    eventingService.send(ScreenEventType.TAAK.updated(task))
                }
                count()
            }

    private fun removeRelevanteZaak(
        relevanteZaken: MutableList<RelevanteZaak>?,
        andereZaak: URI,
        aardRelatie: AardRelatie
    ): List<RelevanteZaak>? {
        relevanteZaken?.removeAll(
            relevanteZaken.filter { it.`is`(andereZaak, aardRelatie) }
        )
        return relevanteZaken
    }

    private fun sortAndRemoveDuplicateAfzenders(
        afzenders: Stream<RESTZaakAfzender>
    ): List<RESTZaakAfzender> {
        val list = afzenders.sorted { a, b ->
            val result: Int = a.mail.compareTo(b.mail)
            if (result == 0) if (a.defaultMail) -1 else 0 else result
        }.collect(Collectors.toList())
        val i = list.iterator()
        var previous: String? = null
        while (i.hasNext()) {
            val afzender: RESTZaakAfzender = i.next()
            if (afzender.mail == previous) {
                i.remove()
            } else {
                previous = afzender.mail
            }
        }
        return list
    }

    private fun speciaalMail(mail: String): Speciaal? {
        if (!mail.contains("@")) {
            return ZaakAfzender.Speciaal.valueOf(mail)
        }
        return null
    }
}
