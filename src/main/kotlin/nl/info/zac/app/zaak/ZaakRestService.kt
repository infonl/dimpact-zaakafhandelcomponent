/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.BadRequestException
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.AardRelatie
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.HoofdzaakZaakPatch
import net.atos.client.zgw.zrc.model.RelevantezaakZaakPatch
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.ZaakafhandelParameterService.INADMISSIBLE_TERMINATION_ID
import net.atos.zac.admin.ZaakafhandelParameterService.INADMISSIBLE_TERMINATION_REASON
import net.atos.zac.admin.model.ZaakAfzender.Speciaal
import net.atos.zac.app.admin.converter.RESTZaakAfzenderConverter
import net.atos.zac.app.admin.model.RESTZaakAfzender
import net.atos.zac.app.bag.converter.RestBagConverter
import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.productaanvraag.InboxProductaanvraagService
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.util.time.LocalDateUtil
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.GeoJSONGeometryWithDeletionSupport
import nl.info.client.zgw.zrc.model.generated.AardRelatieEnum
import nl.info.client.zgw.zrc.model.generated.RelevanteZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.exception.BetrokkeneNotAllowed
import nl.info.zac.app.zaak.exception.CommunicationChannelNotFound
import nl.info.zac.app.zaak.model.RESTDocumentOntkoppelGegevens
import nl.info.zac.app.zaak.model.RESTReden
import nl.info.zac.app.zaak.model.RESTZaakAanmaakGegevens
import nl.info.zac.app.zaak.model.RESTZaakAfbrekenGegevens
import nl.info.zac.app.zaak.model.RESTZaakAfsluitenGegevens
import nl.info.zac.app.zaak.model.RESTZaakBetrokkeneGegevens
import nl.info.zac.app.zaak.model.RESTZaakEditMetRedenGegevens
import nl.info.zac.app.zaak.model.RESTZaakHeropenenGegevens
import nl.info.zac.app.zaak.model.RESTZaakOpschortGegevens
import nl.info.zac.app.zaak.model.RESTZaakOpschorting
import nl.info.zac.app.zaak.model.RESTZaakVerlengGegevens
import nl.info.zac.app.zaak.model.RESTZakenVerdeelGegevens
import nl.info.zac.app.zaak.model.RESTZakenVrijgevenGegevens
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.app.zaak.model.RestDecision
import nl.info.zac.app.zaak.model.RestDecisionChangeData
import nl.info.zac.app.zaak.model.RestDecisionCreateData
import nl.info.zac.app.zaak.model.RestDecisionType
import nl.info.zac.app.zaak.model.RestDecisionWithdrawalData
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.app.zaak.model.RestZaak
import nl.info.zac.app.zaak.model.RestZaakAssignmentData
import nl.info.zac.app.zaak.model.RestZaakAssignmentToLoggedInUserData
import nl.info.zac.app.zaak.model.RestZaakBetrokkene
import nl.info.zac.app.zaak.model.RestZaakLinkData
import nl.info.zac.app.zaak.model.RestZaakLocatieGegevens
import nl.info.zac.app.zaak.model.RestZaakOverzicht
import nl.info.zac.app.zaak.model.RestZaakUnlinkData
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.app.zaak.model.toGeoJSONGeometry
import nl.info.zac.app.zaak.model.toRestDecisionTypes
import nl.info.zac.app.zaak.model.toRestResultaatTypes
import nl.info.zac.app.zaak.model.toRestZaakBetrokkenen
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.history.model.HistoryLine
import nl.info.zac.identity.IdentityService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import org.apache.commons.collections4.CollectionUtils
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
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
    private val decisionService: DecisionService,
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
    private val bpmnService: BpmnService,
    private val flowableTaskService: FlowableTaskService,
    private val objectsClientService: ObjectsClientService,
    private val inboxProductaanvraagService: InboxProductaanvraagService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val configuratieService: ConfiguratieService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val restZaakConverter: RestZaakConverter,
    private val restZaaktypeConverter: RestZaaktypeConverter,
    private val restDecisionConverter: RestDecisionConverter,
    private val restZaakOverzichtConverter: RestZaakOverzichtConverter,
    private val zaakHistoryLineConverter: ZaakHistoryLineConverter,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val healthCheckService: HealthCheckService,
    private val opschortenZaakHelper: SuspensionZaakHelper,
    private val zaakService: ZaakService,
    private val zaakHistoryService: ZaakHistoryService,

    /**
     * Declare a Kotlin coroutine dispatcher here so that it can be overridden in unit tests with a test dispatcher
     * while in normal operation it will be injected using [nl.info.zac.util.CoroutineDispatcherProducer].
     */
    private val dispatcher: CoroutineDispatcher
) {
    companion object {
        private const val ROL_VERWIJDER_REDEN = "Verwijderd door de medewerker tijdens het behandelen van de zaak"
        private const val ROL_TOEVOEGEN_REDEN = "Toegekend door de medewerker tijdens het behandelen van de zaak"
        private const val AANMAKEN_ZAAK_REDEN = "Aanmaken zaak"
        private const val VERLENGING = "Verlenging"
        const val AANVULLENDE_INFORMATIE_TASK_NAME = "Aanvullende informatie"
    }

    @GET
    @Path("zaak/{uuid}")
    fun readZaak(@PathParam("uuid") zaakUUID: UUID): RestZaak =
        zrcClientService.readZaak(zaakUUID).let { zaak ->
            restZaakConverter.toRestZaak(zaak).also {
                assertPolicy(it.rechten.lezen)
                signaleringService.deleteSignaleringenForZaak(zaak)
            }
        }

    @GET
    @Path("zaak/id/{identificatie}")
    fun readZaakById(@PathParam("identificatie") identificatie: String): RestZaak =
        zrcClientService.readZaakByID(identificatie).let { zaak ->
            restZaakConverter.toRestZaak(zaak).also {
                assertPolicy(it.rechten.lezen)
                signaleringService.deleteSignaleringenForZaak(zaak)
            }
        }

    @PATCH
    @Path("initiator")
    fun updateInitiator(gegevens: RESTZaakBetrokkeneGegevens): RestZaak {
        val zaak = zrcClientService.readZaak(gegevens.zaakUUID)
        zgwApiService.findInitiatorRoleForZaak(zaak)?.also {
            requireNotNull(gegevens.roltoelichting) { throw BetrokkeneNotAllowed() }
            removeInitiator(zaak, it, ROL_VERWIJDER_REDEN)
        }
        addInitiator(
            gegevens.betrokkeneIdentificatieType,
            gegevens.betrokkeneIdentificatie,
            zaak,
            gegevens.roltoelichting
        )
        return restZaakConverter.toRestZaak(zaak)
    }

    @DELETE
    @Path("{uuid}/initiator")
    fun deleteInitiator(@PathParam("uuid") zaakUUID: UUID, reden: RESTReden): RestZaak {
        val zaak = zrcClientService.readZaak(zaakUUID)
        zgwApiService.findInitiatorRoleForZaak(zaak)?.also {
            removeInitiator(zaak, it, reden.reden)
        }
        return restZaakConverter.toRestZaak(zaak)
    }

    @POST
    @Path("betrokkene")
    fun addBetrokkene(@Valid gegevens: RESTZaakBetrokkeneGegevens): RestZaak {
        val zaak = zrcClientService.readZaak(gegevens.zaakUUID)
        addBetrokkeneToZaak(
            roleTypeUUID = gegevens.roltypeUUID,
            identificationType = gegevens.betrokkeneIdentificatieType,
            identification = gegevens.betrokkeneIdentificatie,
            explanation = gegevens.roltoelichting?.ifEmpty { ROL_TOEVOEGEN_REDEN } ?: ROL_TOEVOEGEN_REDEN,
            zaak
        )
        return restZaakConverter.toRestZaak(zaak)
    }

    @DELETE
    @Path("betrokkene/{uuid}")
    fun deleteBetrokkene(
        @PathParam("uuid") betrokkeneUUID: UUID,
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
        val zaaktypeUUID = restZaak.zaaktype.uuid
        val zaaktype = ztcClientService.readZaaktype(zaaktypeUUID)

        assertCanAddBetrokkene(restZaak)

        // make sure to use the omschrijving of the zaaktype that was retrieved to perform
        // authorisation on zaaktype
        assertPolicy(
            policyService.readOverigeRechten().startenZaak &&
                loggedInUserInstance.get().isAuthorisedForZaaktype(zaaktype.omschrijving)
        )
        restZaak.communicatiekanaal?.isNotBlank() == true || throw CommunicationChannelNotFound()
        val zaak = restZaakConverter.toZaak(restZaak, zaaktype).let(zgwApiService::createZaak)
        restZaak.initiatorIdentificatie?.takeIf { it.isNotEmpty() }?.let {
            addInitiator(
                restZaak.initiatorIdentificatieType!!,
                restZaak.initiatorIdentificatie!!,
                zaak,
                AANMAKEN_ZAAK_REDEN
            )
        }
        restZaak.groep?.let {
            zrcClientService.updateRol(
                zaak,
                zaakService.bepaalRolGroep(identityService.readGroup(it.id), zaak),
                AANMAKEN_ZAAK_REDEN
            )
        }
        restZaak.behandelaar?.let {
            zrcClientService.updateRol(
                zaak,
                zaakService.bepaalRolMedewerker(identityService.readUser(it.id), zaak),
                AANMAKEN_ZAAK_REDEN
            )
        }
        // if BPMN support is enabled and a BPMN process definition is defined for the zaaktype, start a BPMN process;
        // otherwise start a CMMN case
        val processDefinition = bpmnService.findProcessDefinitionForZaaktype(zaaktypeUUID)
        if (configuratieService.featureFlagBpmnSupport() && processDefinition != null) {
            bpmnService.startProcess(
                zaak = zaak,
                zaaktype = zaaktype,
                processDefinitionKey = processDefinition.bpmnProcessDefinitionKey
            )
        } else {
            cmmnService.startCase(
                zaak = zaak,
                zaaktype = zaaktype,
                zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                    zaaktype.url.extractUuid()
                )
            )
        }
        restZaakAanmaakGegevens.inboxProductaanvraag?.let { koppelInboxProductaanvraag(zaak, it) }
        restZaakAanmaakGegevens.bagObjecten?.forEach {
            zrcClientService.createZaakobject(RestBagConverter.convertToZaakobject(it, zaak))
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

        assertCanAddBetrokkene(restZaakEditMetRedenGegevens.zaak)

        with(policyService.readZaakRechten(zaak)) {
            assertPolicy(wijzigen)
            if (
                zaak.startdatum != restZaakEditMetRedenGegevens.zaak.startdatum ||
                zaak.einddatumGepland != restZaakEditMetRedenGegevens.zaak.einddatumGepland ||
                zaak.uiterlijkeEinddatumAfdoening != restZaakEditMetRedenGegevens.zaak.uiterlijkeEinddatumAfdoening
            ) {
                assertPolicy(verlengenDoorlooptijd)
                assertPolicy(wijzigenDoorlooptijd)
            }
        }
        restZaakEditMetRedenGegevens.zaak.run {
            behandelaar?.id?.let { behandelaarId ->
                groep?.id?.let { groepId ->
                    identityService.checkIfUserIsInGroup(behandelaarId, groepId)
                }
            }
        }
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            restZaakConverter.convertToPatch(restZaakEditMetRedenGegevens.zaak),
            restZaakEditMetRedenGegevens.reden
        )
        restZaakEditMetRedenGegevens.zaak.uiterlijkeEinddatumAfdoening?.let { newFinalDate ->
            if (newFinalDate.isBefore(zaak.uiterlijkeEinddatumAfdoening) && adjustFinalDateForOpenTasks(zaakUUID, newFinalDate) > 0) {
                eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(updatedZaak))
            }
        }
        return restZaakConverter.toRestZaak(updatedZaak)
    }

    @PATCH
    @Path("{uuid}/zaaklocatie")
    fun updateZaakLocatie(
        @PathParam("uuid") zaakUUID: UUID,
        restZaakLocatieGegevens: RestZaakLocatieGegevens
    ): RestZaak {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).wijzigenLocatie)
        val zaakPatch = Zaak().apply {
            // TODO: solution for geometry to be deleted
            // zaakgeometrie = restZaakLocatieGegevens.geometrie?.toGeometry() ?: GeometryToBeDeleted()
            zaakgeometrie = restZaakLocatieGegevens.geometrie?.toGeoJSONGeometry()
                ?: GeoJSONGeometryWithDeletionSupport(markGeometryForDeletion = true)
        }
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID = zaakUUID,
            zaak = zaakPatch,
            toelichting = restZaakLocatieGegevens.reden
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
                opschortenZaakHelper.suspendZaak(
                    zaak,
                    opschortGegevens.duurDagen,
                    opschortGegevens.redenOpschorting
                )
            )
        } else {
            restZaakConverter.toRestZaak(
                opschortenZaakHelper.resumeZaak(zaak, opschortGegevens.redenOpschorting)
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
    fun ontkoppelInformatieObject(restDocumentOntkoppelGegevens: RESTDocumentOntkoppelGegevens) {
        val zaak = zrcClientService.readZaak(restDocumentOntkoppelGegevens.zaakUUID)
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(
            restDocumentOntkoppelGegevens.documentUUID
        )
        assertPolicy(policyService.readDocumentRechten(informatieobject, zaak).ontkoppelen)
        val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
            ZaakInformatieobjectListParameters().apply {
                this.informatieobject = informatieobject.url
                this.zaak = zaak.url
            }
        )
        if (zaakInformatieobjecten.isEmpty()) {
            throw NotFoundException(
                "Geen ZaakInformatieobject gevonden voor Zaak: '${restDocumentOntkoppelGegevens.zaakUUID}' " +
                    "en InformatieObject: '${restDocumentOntkoppelGegevens.documentUUID}'"
            )
        }
        zaakInformatieobjecten.forEach {
            zrcClientService.deleteZaakInformatieobject(
                it.uuid,
                restDocumentOntkoppelGegevens.reden,
                "Ontkoppeld"
            )
        }
        // if this informatieobject is not linked to any other zaken, remove it from the
        // Solr index and add it to the list of 'ontkoppelde documenten'
        if (zrcClientService.listZaakinformatieobjecten(informatieobject).isEmpty()) {
            indexingService.removeInformatieobject(informatieobject.url.extractUuid())
            ontkoppeldeDocumentenService.create(informatieobject, zaak, restDocumentOntkoppelGegevens.reden)
        }
    }

    @GET
    @Path("waarschuwing")
    fun listZaakWaarschuwingen(): List<RestZaakOverzicht> {
        val vandaag = LocalDate.now()
        val einddatumGeplandWaarschuwing = mutableMapOf<UUID, LocalDate>()
        val uiterlijkeEinddatumAfdoeningWaarschuwing = mutableMapOf<UUID, LocalDate>()
        zaakafhandelParameterService.listZaakafhandelParameters().forEach {
            if (it.einddatumGeplandWaarschuwing != null) {
                einddatumGeplandWaarschuwing[it.zaakTypeUUID] = datumWaarschuwing(
                    vandaag,
                    it.einddatumGeplandWaarschuwing
                )
            }
            if (it.uiterlijkeEinddatumAfdoeningWaarschuwing != null) {
                uiterlijkeEinddatumAfdoeningWaarschuwing[it.zaakTypeUUID] = datumWaarschuwing(
                    vandaag,
                    it.uiterlijkeEinddatumAfdoeningWaarschuwing
                )
            }
        }
        val zaakListParameters = ZaakListParameters().apply {
            rolBetrokkeneIdentificatieMedewerkerIdentificatie = loggedInUserInstance.get().id
        }
        return zrcClientService.listZaken(zaakListParameters).results
            .filter { it.isOpen() }
            .filter {
                isWaarschuwing(
                    it,
                    vandaag,
                    einddatumGeplandWaarschuwing,
                    uiterlijkeEinddatumAfdoeningWaarschuwing
                )
            }
            .map(restZaakOverzichtConverter::convert)
    }

    @GET
    @Path("zaaktypes")
    fun listZaaktypes(): List<RestZaaktype> =
        ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())
            .filter { loggedInUserInstance.get().isAuthorisedForZaaktype(it.omschrijving) }
            .filter { !it.concept }
            .filter { it.isNuGeldig() }
            .filter {
                // return zaaktypes for which a BPMN process definition key
                // or a valid (CMMN) zaakafhandelparameters has been configured
                (
                    configuratieService.featureFlagBpmnSupport() &&
                        bpmnService.findProcessDefinitionForZaaktype(it.url.extractUuid()) != null
                    ) || healthCheckService.controleerZaaktype(it.url).isValide
            }
            .map(restZaaktypeConverter::convert)

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
    fun assignZaak(@Valid toekennenGegevens: RestZaakAssignmentData): RestZaak {
        val zaak = zrcClientService.readZaak(toekennenGegevens.zaakUUID).also {
            assertPolicy(policyService.readZaakRechten(it).toekennen)
        }
        toekennenGegevens.assigneeUserName?.let { behandelaarId ->
            toekennenGegevens.groupId.let { groepId ->
                identityService.checkIfUserIsInGroup(behandelaarId, groepId)
            }
        }

        val behandelaar = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            ?.betrokkeneIdentificatie?.identificatie
        val isUpdated = AtomicBoolean(false)
        if (behandelaar != toekennenGegevens.assigneeUserName) {
            toekennenGegevens.assigneeUserName?.takeIf { it.isNotEmpty() }?.let {
                val user = identityService.readUser(it)
                zrcClientService.updateRol(zaak, zaakService.bepaalRolMedewerker(user, zaak), toekennenGegevens.reason)
            } ?: zrcClientService.deleteRol(zaak, BetrokkeneType.MEDEWERKER, toekennenGegevens.reason)
            isUpdated.set(true)
        }
        // if the zaak is not already assigned to the requested group, assign it to this group
        zgwApiService.findGroepForZaak(zaak)?.betrokkeneIdentificatie?.identificatie.let { currentGroupId ->
            if (currentGroupId == null || currentGroupId != toekennenGegevens.groupId) {
                val group = identityService.readGroup(toekennenGegevens.groupId)
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
    fun assignZaakToLoggedInUserFromList(
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
        CoroutineScope(dispatcher).launch {
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
    fun releaseZakenFromList(@Valid restZakenVrijgevenGegevens: RESTZakenVrijgevenGegevens) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        CoroutineScope(dispatcher).launch {
            zaakService.releaseZaken(
                zaakUUIDs = restZakenVrijgevenGegevens.uuids,
                explanation = restZakenVrijgevenGegevens.reden,
                screenEventResourceId = restZakenVrijgevenGegevens.screenEventResourceId
            )
        }
    }

    @PATCH
    @Path("/zaak/{uuid}/afbreken")
    fun terminateZaak(
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
        assertPolicy(zaak.isOpen() && !statustype.isHeropend())
        zaakService.checkZaakAfsluitbaar(zaak)
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
            zaak.zaaktype.extractUuid()
        )
        if (afbrekenGegevens.zaakbeeindigRedenId == INADMISSIBLE_TERMINATION_ID) {
            // Use the hardcoded "niet ontvankelijk" zaakbeeindigreden that we don't manage via ZaakafhandelParameters
            terminateZaak(zaak, zaakafhandelParameters.nietOntvankelijkResultaattype, INADMISSIBLE_TERMINATION_REASON)
        } else {
            afbrekenGegevens.zaakbeeindigRedenId.toLong().let { zaakbeeindigRedenId ->
                zaakafhandelParameters.readZaakbeeindigParameter(zaakbeeindigRedenId).let {
                    terminateZaak(zaak, it.resultaattype, it.zaakbeeindigReden.naam)
                }
            }
        }

        // Terminate case after the zaak is ended in order to prevent the EndCaseLifecycleListener from ending the zaak.
        cmmnService.terminateCase(zaakUUID)
    }

    private fun terminateZaak(
        zaak: Zaak,
        resultaattype: UUID,
        zaakbeeindigRedenNaam: String
    ) {
        zgwApiService.createResultaatForZaak(zaak, resultaattype, zaakbeeindigRedenNaam)
        zgwApiService.endZaak(zaak, zaakbeeindigRedenNaam)
    }

    @PATCH
    @Path("/zaak/{uuid}/heropenen")
    fun reopenZaak(
        @PathParam("uuid") zaakUUID: UUID,
        heropenenGegevens: RESTZaakHeropenenGegevens
    ) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(!zaak.isOpen() && policyService.readZaakRechten(zaak).heropenen)
        zgwApiService.createStatusForZaak(
            zaak,
            ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND,
            heropenenGegevens.reden
        )
        zaak.resultaat?.let {
            zrcClientService.deleteResultaat(it.extractUuid())
        }
    }

    @PATCH
    @Path("/zaak/{uuid}/afsluiten")
    fun closeZaak(
        @PathParam("uuid") zaakUUID: UUID,
        afsluitenGegevens: RESTZaakAfsluitenGegevens
    ) {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(zaak.isOpen() && policyService.readZaakRechten(zaak).behandelen)
        zaakService.checkZaakAfsluitbaar(zaak)
        zgwApiService.updateResultaatForZaak(
            zaak,
            afsluitenGegevens.resultaattypeUuid,
            afsluitenGegevens.reden
        )
        zgwApiService.closeZaak(zaak, afsluitenGegevens.reden)
    }

    @PATCH
    @Path("/zaak/koppel")
    fun linkZaak(restZaakLinkData: RestZaakLinkData) {
        val zaak = zrcClientService.readZaak(restZaakLinkData.zaakUuid)
        val zaakToLinkTo = zrcClientService.readZaak(restZaakLinkData.teKoppelenZaakUuid)
        assertPolicy(zaak.isOpen() == zaakToLinkTo.isOpen())
        assertPolicy(policyService.readZaakRechten(zaak).koppelen)
        assertPolicy(policyService.readZaakRechten(zaakToLinkTo).koppelen)

        when (restZaakLinkData.relatieType) {
            RelatieType.HOOFDZAAK -> koppelHoofdEnDeelzaak(zaakToLinkTo, zaak)
            RelatieType.DEELZAAK -> koppelHoofdEnDeelzaak(zaak, zaakToLinkTo)
            RelatieType.VERVOLG -> koppelRelevantezaken(zaak, zaakToLinkTo, AardRelatieEnum.VERVOLG)
            RelatieType.ONDERWERP -> koppelRelevantezaken(zaak, zaakToLinkTo, AardRelatieEnum.ONDERWERP)
            RelatieType.BIJDRAGE -> koppelRelevantezaken(zaak, zaakToLinkTo, AardRelatieEnum.BIJDRAGE)
            RelatieType.OVERIG -> throw BadRequestException("Relatie type 'OVERIG' is not supported.")
        }
        restZaakLinkData.reverseRelatieType?.let { reverseRelatieType ->
            when (reverseRelatieType) {
                RelatieType.VERVOLG -> koppelRelevantezaken(zaakToLinkTo, zaak, AardRelatieEnum.VERVOLG)
                RelatieType.ONDERWERP -> koppelRelevantezaken(zaakToLinkTo, zaak, AardRelatieEnum.ONDERWERP)
                RelatieType.BIJDRAGE -> koppelRelevantezaken(zaakToLinkTo, zaak, AardRelatieEnum.BIJDRAGE)
                else -> error("Reverse relatie type $reverseRelatieType is not supported")
            }
        }
    }

    @PATCH
    @Path("/zaak/ontkoppel")
    fun unlinkZaak(restZaakUnlinkData: RestZaakUnlinkData) {
        val zaak = zrcClientService.readZaak(restZaakUnlinkData.zaakUuid)
        val linkedZaak = zrcClientService.readZaakByID(restZaakUnlinkData.gekoppeldeZaakIdentificatie)
        assertPolicy(policyService.readZaakRechten(zaak).wijzigen)
        assertPolicy(policyService.readZaakRechten(linkedZaak).wijzigen)

        when (restZaakUnlinkData.relatieType) {
            RelatieType.HOOFDZAAK -> ontkoppelHoofdEnDeelzaak(linkedZaak, zaak, restZaakUnlinkData.reden)
            RelatieType.DEELZAAK -> ontkoppelHoofdEnDeelzaak(zaak, linkedZaak, restZaakUnlinkData.reden)
            RelatieType.VERVOLG -> ontkoppelRelevantezaken(
                zaak,
                linkedZaak,
                AardRelatie.VERVOLG,
                restZaakUnlinkData.reden
            )
            RelatieType.ONDERWERP -> ontkoppelRelevantezaken(
                zaak,
                linkedZaak,
                AardRelatie.ONDERWERP,
                restZaakUnlinkData.reden
            )
            RelatieType.BIJDRAGE -> ontkoppelRelevantezaken(
                zaak,
                linkedZaak,
                AardRelatie.BIJDRAGE,
                restZaakUnlinkData.reden
            )
            RelatieType.OVERIG -> {
                throw BadRequestException("Relatie type 'OVERIG' is not supported.")
            }
        }
    }

    @PUT
    @Path("toekennen/mij")
    fun assignZaakToLoggedInUser(
        @Valid restZaakAssignmentToLoggedInUserData: RestZaakAssignmentToLoggedInUserData
    ): RestZaak = assignLoggedInUserToZaak(
        zaakUUID = restZaakAssignmentToLoggedInUserData.zaakUUID,
        reason = restZaakAssignmentToLoggedInUserData.reason
    ).let { restZaakConverter.toRestZaak(it) }

    @GET
    @Path("zaak/{uuid}/historie")
    fun listZaakHistory(@PathParam("uuid") zaakUUID: UUID): List<HistoryLine> {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        return zaakHistoryService.getZaakHistory(zaakUUID)
    }

    @GET
    @Path("zaak/{uuid}/betrokkene")
    fun listBetrokkenenVoorZaak(@PathParam("uuid") zaakUUID: UUID): List<RestZaakBetrokkene> {
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak).lezen)
        return zaakService.listBetrokkenenforZaak(zaak).toRestZaakBetrokkenen()
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
                    zaakafhandelParameterService.readZaakafhandelParameters(zaak.zaaktype.extractUuid()).zaakAfzenders
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
    fun readDefaultAfzenderVoorZaak(@PathParam("uuid") zaakUUID: UUID): RESTZaakAfzender? {
        val zaak = zrcClientService.readZaak(zaakUUID)
        return zaakafhandelParameterService.readZaakafhandelParameters(zaak.zaaktype.extractUuid())
            .zaakAfzenders
            .firstOrNull { it.isDefault }
            ?.let(RESTZaakAfzenderConverter::convertZaakAfzender)
    }

    @GET
    @Path("besluit/zaakUuid/{zaakUuid}")
    fun listBesluitenForZaakUUID(@PathParam("zaakUuid") zaakUuid: UUID): List<RestDecision> =
        zrcClientService.readZaak(zaakUuid)
            .let { brcClientService.listBesluiten(it) }
            .map { restDecisionConverter.convertToRestDecision(it) }

    @POST
    @Path("besluit")
    fun createBesluit(@Valid besluitToevoegenGegevens: RestDecisionCreateData) =
        zrcClientService.readZaak(besluitToevoegenGegevens.zaakUuid).let { zaak ->
            ztcClientService.readZaaktype(zaak.zaaktype).let { zaaktype ->
                assertPolicy(policyService.readZaakRechten(zaak, zaaktype).vastleggenBesluit)
                assertPolicy(CollectionUtils.isNotEmpty(zaaktype.besluittypen))
            }

            decisionService.createDecision(zaak, besluitToevoegenGegevens).let {
                restDecisionConverter.convertToRestDecision(it).also {
                    // This event should result from a ZAAKBESLUIT CREATED notification on the ZAKEN channel
                    // but open_zaak does not send that one, so emulate it here.
                    eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
                }
            }
        }

    @PUT
    @Path("besluit")
    fun updateBesluit(@Valid restDecisionChangeData: RestDecisionChangeData) =
        brcClientService.readBesluit(restDecisionChangeData.besluitUuid).let { besluit ->
            zrcClientService.readZaak(besluit.zaak).let { zaak ->
                assertPolicy(policyService.readZaakRechten(zaak).vastleggenBesluit)

                decisionService.updateDecision(zaak, besluit, restDecisionChangeData).let {
                    restDecisionConverter.convertToRestDecision(besluit).also {
                        // This event should result from a ZAAKBESLUIT CREATED notification on the ZAKEN channel
                        // but open_zaak does not send that one, so emulate it here.
                        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
                    }
                }
            }
        }

    @PUT
    @Path("besluit/intrekken")
    fun intrekkenBesluit(@Valid restDecisionWithdrawalData: RestDecisionWithdrawalData) =
        decisionService.readDecision(restDecisionWithdrawalData).let { besluit ->
            zrcClientService.readZaak(besluit.zaak).let { zaak ->
                assertPolicy(zaak.isOpen() && policyService.readZaakRechten(zaak).behandelen)

                decisionService.withdrawDecision(besluit, restDecisionWithdrawalData.reden).let {
                    restDecisionConverter.convertToRestDecision(it).also {
                        // This event should result from a ZAAKBESLUIT UPDATED notification on the ZAKEN channel
                        // but open_zaak does not send that one, so emulate it here.
                        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
                    }
                }
            }
        }

    @GET
    @Path("besluit/{uuid}/historie")
    fun listBesluitHistorie(@PathParam("uuid") uuid: UUID): List<HistoryLine> =
        brcClientService.listAuditTrail(uuid).let {
            zaakHistoryLineConverter.convert(it)
        }

    @GET
    @Path("besluittypes/{zaaktypeUUID}")
    fun listBesluittypes(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID
    ): List<RestDecisionType> {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        return ztcClientService.readBesluittypen(ztcClientService.readZaaktype(zaaktypeUUID).url)
            .filter { LocalDateUtil.dateNowIsBetween(it) }
            .toRestDecisionTypes()
    }

    @GET
    @Path("resultaattypes/{zaaktypeUUID}")
    fun listResultaattypesForZaaktype(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID
    ): List<RestResultaattype> {
        assertPolicy(
            policyService.readWerklijstRechten().zakenTaken || policyService.readOverigeRechten().beheren
        )
        return ztcClientService.readResultaattypen(
            ztcClientService.readZaaktype(zaaktypeUUID).url
        ).toRestResultaatTypes()
    }

    @GET
    @Path("{uuid}/procesdiagram")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadProcessDiagram(@PathParam("uuid") uuid: UUID): Response =
        Response.ok(bpmnService.getProcessDiagram(uuid))
            .header(
                "Content-Disposition",
                """attachment; filename="procesdiagram.gif"""".trimIndent()
            )
            .build()

    @GET
    @Path("procesvariabelen")
    fun listProcesVariabelen(): List<String> = ZaakVariabelenService.VARS

    private fun addBetrokkeneToZaak(
        roleTypeUUID: UUID,
        identificationType: IdentificatieType,
        identification: String,
        explanation: String,
        zaak: Zaak
    ) {
        val zaakRechten = policyService.readZaakRechten(zaak)
        when (identificationType) {
            IdentificatieType.BSN -> assertPolicy(zaakRechten.toevoegenBetrokkenePersoon)
            IdentificatieType.VN -> assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
            IdentificatieType.RSIN -> assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
        }
        zaakService.addBetrokkeneToZaak(
            roleTypeUUID = roleTypeUUID,
            identificationType = identificationType,
            identification = identification,
            zaak = zaak,
            explanation = explanation
        )
    }

    private fun addInitiator(
        identificationType: IdentificatieType,
        identification: String,
        zaak: Zaak,
        reden: String? = ROL_TOEVOEGEN_REDEN
    ) {
        val zaakRechten = policyService.readZaakRechten(zaak)
        when (identificationType) {
            IdentificatieType.BSN -> assertPolicy(zaakRechten.toevoegenInitiatorPersoon)
            IdentificatieType.VN -> assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
            IdentificatieType.RSIN -> assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
        }
        zaakService.addInitiatorToZaak(
            identificationType = identificationType,
            identification = identification,
            zaak = zaak,
            explanation = reden?.ifEmpty { ROL_TOEVOEGEN_REDEN } ?: ROL_TOEVOEGEN_REDEN
        )
    }

    private fun addRelevanteZaak(
        relevanteZaken: MutableList<RelevanteZaak>?,
        andereZaakURI: URI,
        aardRelatie: AardRelatieEnum
    ): List<RelevanteZaak> {
        val relevanteZaak = RelevanteZaak().apply {
            this.url = andereZaakURI
            this.aardRelatie = aardRelatie
        }
        return relevanteZaken?.apply {
            if (none { it.aardRelatie == aardRelatie && it.url == andereZaakURI }) add(relevanteZaak)
        } ?: listOf(relevanteZaak)
    }

    private fun datumWaarschuwing(vandaag: LocalDate, dagen: Int): LocalDate = vandaag.plusDays(dagen + 1L)

    private fun assignLoggedInUserToZaak(
        zaakUUID: UUID,
        reason: String?
    ): Zaak = zrcClientService.readZaak(zaakUUID).let { zaak ->
        assertPolicy(zaak.isOpen() && policyService.readZaakRechten(zaak).toekennen)
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
        val zaaktypeUUID = zaak.zaaktype.extractUuid()
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
        // TODO: check
        zrcClientService.patchZaak(deelZaak.uuid, HoofdzaakZaakPatch(hoofdZaak.url))
        // Open Zaak only sends a notification for the subcase.
        // So we manually send a ScreenEvent for the main case.
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
        aardRelatie: AardRelatieEnum
    ) {
        // TODO: fix. aardrelatie moet gezet worden
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
        // TODO: fix
        zrcClientService.patchZaak(deelZaak.uuid, hoofdZaak, reden)
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
        // TODO: fix
        zrcClientService.patchZaak(
            zaak.uuid,
            andereZaak,
//            RelevantezaakZaakPatch(
//                removeRelevanteZaak(zaak.relevanteAndereZaken, andereZaak.url, aardRelatie)
//            ),
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
        andereZaakURI: URI,
        aardRelatie: AardRelatieEnum
    ): List<RelevanteZaak>? {
        relevanteZaken?.removeIf { it.aardRelatie == aardRelatie && it.url == andereZaakURI }
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

    private fun speciaalMail(mail: String): Speciaal? = if (!mail.contains("@")) Speciaal.valueOf(mail) else null

    private fun assertCanAddBetrokkene(zaak: RestZaak) {
        zaak.initiatorIdentificatieType?.let {
            val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                zaak.zaaktype.uuid
            )

            if (it.isKvK && !zaakafhandelParameters.betrokkeneKoppelingen.kvkKoppelen) {
                throw BetrokkeneNotAllowed()
            }
            if (it.isBsn && !zaakafhandelParameters.betrokkeneKoppelingen.brpKoppelen) {
                throw BetrokkeneNotAllowed()
            }
        }
    }
}
