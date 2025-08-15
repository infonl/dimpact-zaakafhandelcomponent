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
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_GROUP
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_USER
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
import nl.info.client.zgw.zrc.model.DeleteGeoJSONGeometry
import nl.info.client.zgw.zrc.model.NillableHoofdzaakZaakPatch
import nl.info.client.zgw.zrc.model.NillableRelevanteZakenZaakPatch
import nl.info.client.zgw.zrc.model.generated.AardRelatieEnum
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.zrc.model.generated.RelevanteZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.client.zgw.ztc.model.generated.BrondatumArchiefprocedure
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.converter.RestZaaktypeConverter
import nl.info.zac.app.zaak.exception.BetrokkeneNotAllowedException
import nl.info.zac.app.zaak.exception.CommunicationChannelNotFound
import nl.info.zac.app.zaak.exception.ExplanationRequiredException
import nl.info.zac.app.zaak.model.BetrokkeneIdentificatie
import nl.info.zac.app.zaak.model.RESTDocumentOntkoppelGegevens
import nl.info.zac.app.zaak.model.RESTReden
import nl.info.zac.app.zaak.model.RESTZaakAanmaakGegevens
import nl.info.zac.app.zaak.model.RESTZaakAfbrekenGegevens
import nl.info.zac.app.zaak.model.RESTZaakAfsluitenGegevens
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
import nl.info.zac.app.zaak.model.RestZaakBetrokkeneGegevens
import nl.info.zac.app.zaak.model.RestZaakInitiatorGegevens
import nl.info.zac.app.zaak.model.RestZaakLinkData
import nl.info.zac.app.zaak.model.RestZaakLocatieGegevens
import nl.info.zac.app.zaak.model.RestZaakOverzicht
import nl.info.zac.app.zaak.model.RestZaakUnlinkData
import nl.info.zac.app.zaak.model.RestZaaktype
import nl.info.zac.app.zaak.model.toGeoJSONGeometry
import nl.info.zac.app.zaak.model.toPatchZaak
import nl.info.zac.app.zaak.model.toRestDecisionTypes
import nl.info.zac.app.zaak.model.toRestResultaatTypes
import nl.info.zac.app.zaak.model.toRestZaakBetrokkenen
import nl.info.zac.app.zaak.model.toZaak
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.history.ZaakHistoryService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.history.model.HistoryLine
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.getFullName
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.policy.output.ZaakRechten
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import nl.info.zac.zaak.exception.ZaakWithADecisionCannotBeTerminatedException
import org.apache.commons.collections4.CollectionUtils
import java.net.URI
import java.time.LocalDate
import java.util.UUID
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
        const val VESTIGING_IDENTIFICATIE_DELIMITER = "|"
    }

    @GET
    @Path("zaak/{uuid}")
    fun readZaak(@PathParam("uuid") zaakUUID: UUID): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertPolicy(zaakRechten.lezen)
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten).also {
            signaleringService.deleteSignaleringenForZaak(zaak)
        }
    }

    @GET
    @Path("zaak/id/{identificatie}")
    fun readZaakById(@PathParam("identificatie") zaakIdentification: String): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakID(zaakIdentification)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertPolicy(zaakRechten.lezen)
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten).also {
            signaleringService.deleteSignaleringenForZaak(zaak)
        }
    }

    @PATCH
    @Path("initiator")
    fun updateInitiator(restZaakInitiatorGegevens: RestZaakInitiatorGegevens): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakInitiatorGegevens.zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        zgwApiService.findInitiatorRoleForZaak(zaak)?.also {
            requireNotNull(restZaakInitiatorGegevens.toelichting) { throw ExplanationRequiredException() }
            removeInitiator(zaakRechten, it, ROL_VERWIJDER_REDEN)
        }
        val (identificationType, identification) = composeBetrokkeneIdentification(
            restZaakInitiatorGegevens.betrokkeneIdentificatie
        )
        addInitiator(
            identificationType,
            identification,
            zaak,
            zaakRechten,
            restZaakInitiatorGegevens.toelichting
        )
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten)
    }

    @DELETE
    @Path("{uuid}/initiator")
    fun deleteInitiator(@PathParam("uuid") zaakUUID: UUID, reden: RESTReden): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        zgwApiService.findInitiatorRoleForZaak(zaak)?.also {
            removeInitiator(zaakRechten, it, reden.reden)
        }
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten)
    }

    @POST
    @Path("betrokkene")
    fun addBetrokkene(@Valid restZaakBetrokkeneGegevens: RestZaakBetrokkeneGegevens): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakBetrokkeneGegevens.zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        addBetrokkeneToZaak(
            roleTypeUUID = restZaakBetrokkeneGegevens.roltypeUUID,
            betrokkeneIdentificatie = restZaakBetrokkeneGegevens.betrokkeneIdentificatie,
            explanation = restZaakBetrokkeneGegevens.roltoelichting?.ifEmpty { ROL_TOEVOEGEN_REDEN } ?: ROL_TOEVOEGEN_REDEN,
            zaak = zaak,
            zaakRechten = zaakRechten,
        )
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten)
    }

    @DELETE
    @Path("betrokkene/{uuid}")
    fun deleteBetrokkene(
        @PathParam("uuid") betrokkeneUUID: UUID,
        reden: RESTReden
    ): RestZaak {
        val betrokkene = zrcClientService.readRol(betrokkeneUUID)
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakURI(betrokkene.zaak)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        removeBetrokkene(zaakRechten, betrokkene, reden.reden)
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten)
    }

    @Suppress("LongMethod")
    @POST
    @Path("zaak")
    fun createZaak(@Valid restZaakAanmaakGegevens: RESTZaakAanmaakGegevens): RestZaak {
        val restZaak = restZaakAanmaakGegevens.zaak
        val zaaktypeUUID = restZaak.zaaktype.uuid
        val zaakType = zaakService.readZaakTypeByUUID(zaaktypeUUID)
        assertCanAddBetrokkene(restZaak)

        // make sure to use the omschrijving of the zaaktype that was retrieved to perform authorization on zaaktype
        assertPolicy(
            policyService.readOverigeRechten().startenZaak &&
                loggedInUserInstance.get().isAuthorisedForZaaktype(zaakType.omschrijving)
        )
        restZaak.communicatiekanaal?.isNotBlank() == true || throw CommunicationChannelNotFound()
        val bronOrganisatie = configuratieService.readBronOrganisatie()
        val verantwoordelijkeOrganisatie = configuratieService.readVerantwoordelijkeOrganisatie()
        val zaak = restZaak.toZaak(
            zaaktype = zaakType,
            bronOrganisatie = bronOrganisatie,
            verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie
        ).let(zgwApiService::createZaak)
        restZaak.initiatorIdentificatie?.takeIf { it.isNotEmpty() }?.let { initiatorId ->
            restZaak.initiatorIdentificatieType?.let { initiatorType ->
                val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
                val identification = when (initiatorType) {
                    IdentificatieType.VN -> createVestigingIdentificationString(restZaak.kvkNummer, initiatorId)
                    else -> initiatorId
                }
                addInitiator(
                    identificationType = initiatorType,
                    identification = identification,
                    zaak = zaak,
                    zaakRechten = zaakRechten,
                    explanation = AANMAKEN_ZAAK_REDEN
                )
            }
        }

        updateZaakRoles(restZaak, zaak)

        // if BPMN support is enabled and a BPMN process definition is defined for the zaaktype, start a BPMN process;
        // otherwise start a CMMN case
        val processDefinition = bpmnService.findProcessDefinitionForZaaktype(zaaktypeUUID)
        if (configuratieService.featureFlagBpmnSupport() && processDefinition != null) {
            bpmnService.startProcess(
                zaak = zaak,
                zaaktype = zaakType,
                processDefinitionKey = processDefinition.bpmnProcessDefinitionKey,
                zaakData = buildMap {
                    restZaak.groep?.let { put(VAR_ZAAK_GROUP, it.naam) }
                    restZaak.behandelaar?.let { put(VAR_ZAAK_USER, it.naam) }
                }
            )
        } else {
            cmmnService.startCase(
                zaak = zaak,
                zaaktype = zaakType,
                zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                    zaakType.url.extractUuid()
                )
            )
        }
        restZaakAanmaakGegevens.inboxProductaanvraag?.let { koppelInboxProductaanvraag(zaak, it) }
        restZaakAanmaakGegevens.bagObjecten?.forEach {
            zrcClientService.createZaakobject(RestBagConverter.convertToZaakobject(it, zaak))
        }
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten)
    }

    private fun updateZaakRoles(
        restZaak: RestZaak,
        zaak: Zaak
    ) {
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
    }

    @PATCH
    @Path("zaak/{uuid}")
    fun updateZaak(
        @PathParam("uuid") zaakUUID: UUID,
        restZaakEditMetRedenGegevens: RESTZaakEditMetRedenGegevens
    ): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertCanAddBetrokkene(restZaakEditMetRedenGegevens.zaak)
        with(zaakRechten) {
            assertPolicy(wijzigen)
            if (
                // do not compare LocalDate fields using identity-sensitive operators like '!=' because they are value-based
                // see e.g., https://docs.oracle.com/javase/8/docs/api/java/lang/doc-files/ValueBased.html
                restZaakEditMetRedenGegevens.zaak.startdatum?.equals(zaak.startdatum) == false ||
                restZaakEditMetRedenGegevens.zaak.einddatumGepland?.equals(zaak.einddatumGepland) == false ||
                restZaakEditMetRedenGegevens.zaak.uiterlijkeEinddatumAfdoening?.equals(zaak.uiterlijkeEinddatumAfdoening) == false
            ) {
                assertPolicy(verlengenDoorlooptijd)
                assertPolicy(wijzigenDoorlooptijd)
            }
        }
        restZaakEditMetRedenGegevens.zaak.run {
            behandelaar?.id?.let { behandelaarId ->
                groep?.id?.let { groepId ->
                    identityService.validateIfUserIsInGroup(behandelaarId, groepId)
                }
            }
        }
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID,
            restZaakEditMetRedenGegevens.zaak.toPatchZaak(),
            restZaakEditMetRedenGegevens.reden
        )
        restZaakEditMetRedenGegevens.zaak.uiterlijkeEinddatumAfdoening?.let { newFinalDate ->
            if (newFinalDate.isBefore(zaak.uiterlijkeEinddatumAfdoening) && adjustFinalDateForOpenTasks(zaakUUID, newFinalDate) > 0) {
                eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(updatedZaak))
            }
        }
        return restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten)
    }

    @PATCH
    @Path("{uuid}/zaaklocatie")
    fun updateZaakLocatie(
        @PathParam("uuid") zaakUUID: UUID,
        restZaakLocatieGegevens: RestZaakLocatieGegevens
    ): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertPolicy(zaakRechten.wijzigenLocatie)
        val zaakPatch = Zaak().apply {
            zaakgeometrie = restZaakLocatieGegevens.geometrie?.toGeoJSONGeometry()
                ?: DeleteGeoJSONGeometry()
        }
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID = zaakUUID,
            zaak = zaakPatch,
            explanation = restZaakLocatieGegevens.reden
        )
        return restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten)
    }

    @PATCH
    @Path("zaak/{uuid}/opschorting")
    fun opschortenZaak(
        @PathParam("uuid") zaakUUID: UUID,
        opschortGegevens: RESTZaakOpschortGegevens
    ): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        return if (opschortGegevens.indicatieOpschorting) {
            val suspendedZaak = opschortenZaakHelper.suspendZaak(
                zaak = zaak,
                numberOfDays = opschortGegevens.duurDagen,
                suspensionReason = opschortGegevens.redenOpschorting
            )
            restZaakConverter.toRestZaak(suspendedZaak, zaakType, zaakRechten)
        } else {
            val resumedZaak = opschortenZaakHelper.resumeZaak(zaak, opschortGegevens.redenOpschorting)
            restZaakConverter.toRestZaak(resumedZaak, zaakType, zaakRechten)
        }
    }

    @GET
    @Path("zaak/{uuid}/opschorting")
    fun readOpschortingZaak(@PathParam("uuid") zaakUUID: UUID): RESTZaakOpschorting {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertPolicy(zaakRechten.lezen)
        return RESTZaakOpschorting().apply {
            vanafDatumTijd = zaakVariabelenService.findDatumtijdOpgeschort(zaakUUID)
            duurDagen = zaakVariabelenService.findVerwachteDagenOpgeschort(zaakUUID) ?: 0
        }
    }

    @PATCH
    @Path("zaak/{uuid}/verlenging")
    fun verlengenZaak(
        @PathParam("uuid") zaakUUID: UUID,
        restZaakVerlengGegevens: RESTZaakVerlengGegevens
    ): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertPolicy(zaakRechten.verlengen)
        val toelichting = "$VERLENGING: ${restZaakVerlengGegevens.redenVerlenging}"
        val updatedZaak = zrcClientService.patchZaak(
            zaakUUID = zaakUUID,
            zaak = restZaakConverter.convertToPatch(
                zaakUUID,
                restZaakVerlengGegevens
            ),
            explanation = toelichting
        )
        if (restZaakVerlengGegevens.takenVerlengen) {
            val aantalTakenVerlengd = verlengOpenTaken(zaakUUID, restZaakVerlengGegevens.duurDagen.toLong())
            if (aantalTakenVerlengd > 0) {
                eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(updatedZaak))
            }
        }
        return restZaakConverter.toRestZaak(updatedZaak, zaakType, zaakRechten)
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
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaak.uuid)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType).wijzigen)
        restZaak.zaakdata?.let {
            zaakVariabelenService.setZaakdata(restZaak.uuid, it)
        }
        return restZaak
    }

    @PATCH
    @Path("toekennen")
    fun assignZaak(@Valid restZaakAssignmentData: RestZaakAssignmentData): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentData.zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertPolicy(zaakRechten.toekennen)

        restZaakAssignmentData.assigneeUserName?.let {
            identityService.validateIfUserIsInGroup(userId = it, groupId = restZaakAssignmentData.groupId)
        }

        val behandelaar = zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            ?.betrokkeneIdentificatie?.identificatie
        val (user, userUpdated) = assignUser(zaak, restZaakAssignmentData, behandelaar)
        val (group, groupUpdated) = assignGroup(zaak, restZaakAssignmentData.groupId, restZaakAssignmentData.reason)
        changeZaakDataAssignment(zaak.uuid, group, user)
        if (userUpdated || groupUpdated) {
            indexingService.indexeerDirect(zaak.uuid.toString(), ZoekObjectType.ZAAK, false)
        }
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten)
    }

    @PUT
    @Path("lijst/toekennen/mij")
    fun assignZaakToLoggedInUserFromList(
        @Valid restZaakAssignmentToLoggedInUserData: RestZaakAssignmentToLoggedInUserData
    ): RestZaakOverzicht {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentToLoggedInUserData.zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertPolicy(zaak.isOpen() && zaakRechten.toekennen)
        identityService.validateIfUserIsInGroup(
            userId = loggedInUserInstance.get().id,
            groupId = restZaakAssignmentToLoggedInUserData.groupId
        )

        val user = assignLoggedInUserToZaak(
            zaak = zaak,
            reason = restZaakAssignmentToLoggedInUserData.reason,
        )
        val (group) = assignGroup(
            zaak = zaak,
            groupId = restZaakAssignmentToLoggedInUserData.groupId,
            reason = restZaakAssignmentToLoggedInUserData.reason
        )
        changeZaakDataAssignment(zaak.uuid, group, user)
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
        // this can be a long-running operation, so run it asynchronously
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
        // this can be a long-running operation, so run it asynchronously
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
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val statustype = zaak.status?.let {
            ztcClientService.readStatustype(zrcClientService.readStatus(it).statustype)
        }
        assertPolicy(policyService.readZaakRechten(zaak, zaakType).afbreken)
        assertPolicy(zaak.isOpen() && !statustype.isHeropend())
        zaak.resultaat?.run {
            throw ZaakWithADecisionCannotBeTerminatedException(
                "The zaak with UUID '${zaak.uuid}' cannot be terminated because a decision is already added to it."
            )
        }
        zaakService.checkZaakAfsluitbaar(zaak)
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
            zaakType.url.extractUuid()
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
        // Terminate case after the zaak is ended to prevent the EndCaseLifecycleListener from ending the zaak.
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
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        assertPolicy(!zaak.isOpen() && policyService.readZaakRechten(zaak, zaakType).heropenen)
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
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        assertPolicy(zaak.isOpen() && policyService.readZaakRechten(zaak, zaakType).behandelen)

        zaakService.processBrondatumProcedure(
            zaak,
            afsluitenGegevens.resultaattypeUuid,
            BrondatumArchiefprocedure().apply {
                datumkenmerk = afsluitenGegevens.brondatumEigenschap
            }
        )

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
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakLinkData.zaakUuid)
        val (zaakToLinkTo, zaakToLinkToZaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(
            restZaakLinkData.teKoppelenZaakUuid
        )
        assertPolicy(policyService.readZaakRechten(zaak, zaakType).koppelen)
        assertPolicy(policyService.readZaakRechten(zaakToLinkTo, zaakToLinkToZaakType).koppelen)

        when (restZaakLinkData.relatieType) {
            RelatieType.HOOFDZAAK -> koppelHoofdEnDeelzaak(zaakToLinkTo, zaak)
            RelatieType.DEELZAAK -> koppelHoofdEnDeelzaak(zaak, zaakToLinkTo)
            RelatieType.VERVOLG -> koppelRelevanteZaken(zaak, zaakToLinkTo, AardRelatieEnum.VERVOLG)
            RelatieType.ONDERWERP -> koppelRelevanteZaken(zaak, zaakToLinkTo, AardRelatieEnum.ONDERWERP)
            RelatieType.BIJDRAGE -> koppelRelevanteZaken(zaak, zaakToLinkTo, AardRelatieEnum.BIJDRAGE)
            RelatieType.OVERIG -> throw BadRequestException("Relatie type 'OVERIG' is not supported.")
        }
        restZaakLinkData.reverseRelatieType?.let { reverseRelatieType ->
            when (reverseRelatieType) {
                RelatieType.VERVOLG -> koppelRelevanteZaken(zaakToLinkTo, zaak, AardRelatieEnum.VERVOLG)
                RelatieType.ONDERWERP -> koppelRelevanteZaken(zaakToLinkTo, zaak, AardRelatieEnum.ONDERWERP)
                RelatieType.BIJDRAGE -> koppelRelevanteZaken(zaakToLinkTo, zaak, AardRelatieEnum.BIJDRAGE)
                else -> error("Reverse relatie type $reverseRelatieType is not supported")
            }
        }
    }

    @PATCH
    @Path("/zaak/ontkoppel")
    fun unlinkZaak(restZaakUnlinkData: RestZaakUnlinkData) {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakUnlinkData.zaakUuid)
        val (linkedZaak, linkedZaakType) = zaakService.readZaakAndZaakTypeByZaakID(
            restZaakUnlinkData.gekoppeldeZaakIdentificatie
        )
        assertPolicy(policyService.readZaakRechten(zaak, zaakType).wijzigen)
        assertPolicy(policyService.readZaakRechten(linkedZaak, linkedZaakType).wijzigen)

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
            RelatieType.VERVOLG -> ontkoppelRelevantezaken(
                zaak = zaak,
                andereZaak = linkedZaak,
                aardRelatie = AardRelatieEnum.VERVOLG,
                explanation = restZaakUnlinkData.reden
            )
            RelatieType.ONDERWERP -> ontkoppelRelevantezaken(
                zaak = zaak,
                andereZaak = linkedZaak,
                aardRelatie = AardRelatieEnum.ONDERWERP,
                explanation = restZaakUnlinkData.reden
            )
            RelatieType.BIJDRAGE -> ontkoppelRelevantezaken(
                zaak = zaak,
                andereZaak = linkedZaak,
                aardRelatie = AardRelatieEnum.BIJDRAGE,
                explanation = restZaakUnlinkData.reden
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
    ): RestZaak {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentToLoggedInUserData.zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType)
        assertPolicy(zaak.isOpen() && zaakRechten.toekennen)
        identityService.validateIfUserIsInGroup(
            userId = loggedInUserInstance.get().id,
            groupId = restZaakAssignmentToLoggedInUserData.groupId
        )

        val user = assignLoggedInUserToZaak(
            zaak = zaak,
            reason = restZaakAssignmentToLoggedInUserData.reason
        )
        val (group) = assignGroup(
            zaak = zaak,
            groupId = restZaakAssignmentToLoggedInUserData.groupId,
            reason = restZaakAssignmentToLoggedInUserData.reason
        )
        changeZaakDataAssignment(zaak.uuid, group, user)
        return restZaakConverter.toRestZaak(zaak, zaakType, zaakRechten)
    }

    @GET
    @Path("zaak/{uuid}/historie")
    fun listZaakHistory(@PathParam("uuid") zaakUUID: UUID): List<HistoryLine> {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        return zaakHistoryService.getZaakHistory(zaakUUID)
    }

    /**
     * Returns the list of betrokkenen for a given zaak.
     *
     * We do filter out roles that do not have a [Rol.betrokkeneIdentificatie], which is technically possible in the ZGW API
     * but for our purposes are invalid/incomplete roles.
     */
    @GET
    @Path("zaak/{uuid}/betrokkene")
    fun listBetrokkenenVoorZaak(@PathParam("uuid") zaakUUID: UUID): List<RestZaakBetrokkene> {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType).lezen)
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
    fun createBesluit(@Valid besluitToevoegenGegevens: RestDecisionCreateData): RestDecision {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(besluitToevoegenGegevens.zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType).vastleggenBesluit)
        assertPolicy(CollectionUtils.isNotEmpty(zaakType.besluittypen))

        return decisionService.createDecision(zaak, besluitToevoegenGegevens).let {
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
    fun listProcesVariabelen(): List<String> = ZaakVariabelenService.ALL_ZAAK_VARIABLE_NAMES

    private fun createVestigingIdentificationString(kvkNummer: String?, vestigingsnummer: String?): String =
        listOfNotNull(kvkNummer, vestigingsnummer).joinToString(VESTIGING_IDENTIFICATIE_DELIMITER)

    private fun addBetrokkeneToZaak(
        roleTypeUUID: UUID,
        betrokkeneIdentificatie: BetrokkeneIdentificatie,
        explanation: String,
        zaak: Zaak,
        zaakRechten: ZaakRechten
    ) {
        val (identificationType, identification) = composeBetrokkeneIdentification(betrokkeneIdentificatie)
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

    private fun composeBetrokkeneIdentification(
        betrokkeneIdentificatie: BetrokkeneIdentificatie
    ): Pair<IdentificatieType, String> {
        return when (betrokkeneIdentificatie.type) {
            IdentificatieType.BSN -> {
                val bsn = betrokkeneIdentificatie.bsnNummer
                require(!bsn.isNullOrBlank()) { "BSN is required for betrokkene identification type BSN" }
                IdentificatieType.BSN to bsn
            }
            IdentificatieType.VN -> {
                val kvk = betrokkeneIdentificatie.kvkNummer
                val vestiging = betrokkeneIdentificatie.vestigingsnummer
                require(!kvk.isNullOrBlank() && !vestiging.isNullOrBlank()) {
                    "KVK nummer and vestigingsnummer are required for betrokkene identification type VN"
                }
                IdentificatieType.VN to createVestigingIdentificationString(kvk, vestiging)
            }
            IdentificatieType.RSIN -> {
                val rsin = betrokkeneIdentificatie.rsinNummer
                require(!rsin.isNullOrBlank()) { "RSIN is required for type RSIN" }
                IdentificatieType.RSIN to rsin
            }
        }
    }

    private fun addInitiator(
        identificationType: IdentificatieType,
        identification: String,
        zaak: Zaak,
        zaakRechten: ZaakRechten,
        explanation: String? = ROL_TOEVOEGEN_REDEN
    ) {
        when (identificationType) {
            IdentificatieType.BSN -> assertPolicy(zaakRechten.toevoegenInitiatorPersoon)
            IdentificatieType.VN -> assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
            IdentificatieType.RSIN -> assertPolicy(zaakRechten.toevoegenBetrokkeneBedrijf)
        }
        zaakService.addInitiatorToZaak(
            identificationType = identificationType,
            identification = identification,
            zaak = zaak,
            explanation = explanation?.ifEmpty { ROL_TOEVOEGEN_REDEN } ?: ROL_TOEVOEGEN_REDEN
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

    private fun assignUser(
        zaak: Zaak,
        restZaakAssignmentData: RestZaakAssignmentData,
        behandelaar: String?
    ): Pair<User?, Boolean> {
        var user: User? = null
        var userUpdated = false
        restZaakAssignmentData.assigneeUserName?.takeIf { it.isNotEmpty() }?.let {
            user = identityService.readUser(it)
            if (behandelaar != restZaakAssignmentData.assigneeUserName) {
                zrcClientService.updateRol(
                    zaak,
                    zaakService.bepaalRolMedewerker(user, zaak),
                    restZaakAssignmentData.reason
                )
                userUpdated = true
            }
        } ?: zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, restZaakAssignmentData.reason).also {
            userUpdated = true
        }
        return Pair(user, userUpdated)
    }

    private fun assignGroup(
        zaak: Zaak,
        groupId: String,
        reason: String?
    ): Pair<Group, Boolean> {
        var groupUpdated = false
        val group = identityService.readGroup(groupId)
        zgwApiService.findGroepForZaak(zaak)?.betrokkeneIdentificatie?.identificatie.let { currentGroupId ->
            // if the zaak is not already assigned to the requested group, assign it to this group
            if (currentGroupId == null || currentGroupId != groupId) {
                val role = zaakService.bepaalRolGroep(group, zaak)
                zrcClientService.updateRol(zaak, role, reason)
                groupUpdated = true
            }
        }
        return Pair(group, groupUpdated)
    }

    private fun changeZaakDataAssignment(
        zaakUuid: UUID,
        group: Group,
        user: User?
    ) {
        if (bpmnService.isProcessDriven(zaakUuid)) {
            zaakVariabelenService.setGroup(zaakUuid, group.name)
            user?.let {
                zaakVariabelenService.setUser(zaakUuid, it.getFullName())
            } ?: zaakVariabelenService.removeUser(zaakUuid)
        }
    }

    private fun assignLoggedInUserToZaak(zaak: Zaak, reason: String?) =
        identityService.readUser(loggedInUserInstance.get().id).also {
            zrcClientService.updateRol(
                zaak = zaak,
                rol = zaakService.bepaalRolMedewerker(it, zaak),
                toelichting = reason
            )
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
        zrcClientService.patchZaak(
            zaakUUID = deelZaak.uuid,
            zaak = NillableHoofdzaakZaakPatch(hoofdzaak = hoofdZaak.url)
        )
        // Open Zaak only sends a notification for the deelzaak.
        // So we manually send a ScreenEvent for the hoofdzaak.
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

    private fun koppelRelevanteZaken(
        zaak: Zaak,
        andereZaak: Zaak,
        aardRelatie: AardRelatieEnum
    ) {
        zrcClientService.patchZaak(
            zaak.uuid,
            Zaak().apply {
                relevanteAndereZaken = addRelevanteZaak(
                    zaak.relevanteAndereZaken,
                    andereZaak.url,
                    aardRelatie
                )
            }
        )
    }

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

    private fun ontkoppelRelevantezaken(
        zaak: Zaak,
        andereZaak: Zaak,
        aardRelatie: AardRelatieEnum,
        explanation: String
    ) {
        zrcClientService.patchZaak(
            zaakUUID = zaak.uuid,
            zaak = NillableRelevanteZakenZaakPatch(
                relevanteAndereZaken = removeRelevanteZaak(zaak.relevanteAndereZaken, andereZaak.url, aardRelatie)
            ),
            explanation = explanation
        )
    }

    private fun removeBetrokkene(zaakRechten: ZaakRechten, betrokkene: Rol<*>, reden: String) {
        assertPolicy(zaakRechten.verwijderenBetrokkene)
        zrcClientService.deleteRol(betrokkene, reden)
    }

    private fun removeInitiator(zaakRechten: ZaakRechten, initiator: Rol<*>, reden: String) {
        assertPolicy(zaakRechten.verwijderenInitiator)
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

    private fun assertCanAddBetrokkene(restZaak: RestZaak) {
        restZaak.initiatorIdentificatieType?.let {
            val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                restZaak.zaaktype.uuid
            )

            if (it.isKvK && !zaakafhandelParameters.betrokkeneKoppelingen.kvkKoppelen) {
                throw BetrokkeneNotAllowedException()
            }
            if (it.isBsn && !zaakafhandelParameters.betrokkeneKoppelingen.brpKoppelen) {
                throw BetrokkeneNotAllowedException()
            }
        }
    }
}
