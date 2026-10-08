/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.app.mail.model.toMailGegevens
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CmmnService
import nl.info.zac.flowable.task.isSendDataSendMail
import nl.info.zac.flowable.task.isZaakOpschorten
import nl.info.zac.flowable.task.readMailAttachments
import nl.info.zac.flowable.task.readMailBody
import nl.info.zac.flowable.task.readMailFrom
import nl.info.zac.flowable.task.readMailReplyTo
import nl.info.zac.flowable.task.readMailTo
import nl.info.zac.flowable.task.readSendDataMail
import nl.info.zac.flowable.task.setMailBody
import nl.info.zac.util.time.convertToDate
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.FormulierDefinitie
import nl.info.zac.admin.model.ZaaktypeCmmnExtension
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import nl.info.zac.app.planitems.converter.RestPlanItemConverter
import nl.info.zac.app.planitems.model.RestHumanTaskData
import nl.info.zac.app.planitems.model.RestPlanItem
import nl.info.zac.app.planitems.model.RestUserEventListenerData
import nl.info.zac.app.planitems.model.UserEventListenerActie
import nl.info.zac.util.toLocalDate
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.mail.MailService
import nl.info.zac.mail.model.MailAdres
import nl.info.zac.mail.model.getBronnenFromZaak
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailGegevens
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.task.TaskHistoryService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.logging.Logger

private val LOG = Logger.getLogger(PlanItemsRestService::class.java.name)

/**
 * Provides REST endpoints for CMMN plan items.
 */
@Singleton
@Path("planitems")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
@Suppress("LongParameterList", "TooManyFunctions")
class PlanItemsRestService @Inject constructor(
    private val zaakVariabelenService: ZaakVariabelenService,
    private val cmmnService: CmmnService,
    private val zrcClientService: ZrcClientService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val resultaattypeReferenceService: ResultaattypeReferenceService,
    private val planItemConverter: RestPlanItemConverter,
    private val zgwApiService: ZgwApiService,
    private val indexingService: IndexingService,
    private val mailService: MailService,
    private val configurationService: ConfigurationService,
    private val mailTemplateService: MailTemplateService,
    private val policyService: PolicyService,
    private val suspensionZaakHelper: SuspensionZaakHelper,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val zaakspecifiekeAutorisatieService: ZaakspecifiekeAutorisatieService,
    private val taskHistoryService: TaskHistoryService
) {
    companion object {
        private const val REDEN_OPSCHORTING = "Aanvullende informatie opgevraagd"
        private const val REDEN_PAST_FATALE_DATUM = "Aanvullende informatie opgevraagd"
    }

    @GET
    @Path("zaak/{uuid}/humanTaskPlanItems")
    fun listHumanTaskPlanItems(@PathParam("uuid") zaakUUID: UUID): List<RestPlanItem> =
        cmmnService.listHumanTaskPlanItems(zaakUUID).let { humanTaskPlanItems ->
            zrcClientService.readZaak(zaakUUID).let { zaak ->
                planItemConverter.convertPlanItems(humanTaskPlanItems, zaak).filter { it.isActief }
            }
        }

    @GET
    @Path("zaak/{uuid}/userEventListenerPlanItems")
    fun listUserEventListenerPlanItems(@PathParam("uuid") zaakUUID: UUID): List<RestPlanItem> =
        cmmnService.listUserEventListenerPlanItems(zaakUUID).let { userEventListenerPlanItems ->
            zrcClientService.readZaak(zaakUUID).let { zaak ->
                planItemConverter.convertPlanItems(userEventListenerPlanItems, zaak)
            }
        }

    @GET
    @Path("humanTaskPlanItem/{id}")
    fun readHumanTaskPlanItem(@PathParam("id") planItemId: String): RestPlanItem =
        convertPlanItem(planItemId)

    @Suppress("NestedBlockDepth")
    private fun convertPlanItem(planItemId: String): RestPlanItem =
        cmmnService.readOpenPlanItem(planItemId).let { planItemInstance ->
            zaakVariabelenService.readZaakUUID(planItemInstance).let { zaakUUID ->
                zaakVariabelenService.readZaaktypeUUID(planItemInstance).let { zaaktypeUUID ->
                    planItemConverter.convertPlanItem(
                        planItemInstance,
                        zaakUUID,
                        zaaktypeConfigurationService.findConfiguration(zaaktypeUUID)?.cmmnExtension
                    )
                }
            }
        }

    @POST
    @Path("doHumanTaskPlanItem")
    @Suppress("LongMethod")
    fun doHumanTaskplanItem(@Valid humanTaskData: RestHumanTaskData) {
        val planItem = cmmnService.readOpenPlanItem(humanTaskData.planItemInstanceId)
        val zaakUUID = zaakVariabelenService.readZaakUUID(planItem)
        val zaak = zrcClientService.readZaak(zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak, loggedInUserInstance.get()).canStartenTaak)
        val taakdata = checkNotNull(humanTaskData.taakdata) { "No task data found for plan item '${humanTaskData.planItemInstanceId}'" }
        val assignee = humanTaskData.medewerker?.id?.takeIf { it.isNotBlank() }
        val zaakspecifiekGeautoriseerdeTaakbehandelaar = assignee?.takeIf {
            zaakspecifiekeAutorisatieService.grantZaakspecifiekeAutorisatieToTaakbehandelaar(zaak, it)
        }
        val zaaktypeConfiguration = zaaktypeConfigurationService.findConfiguration(zaak.zaaktype.extractUuid())

        val fatalDate = calculateFatalDate(
            humanTaskData = humanTaskData,
            zaaktypeCmmnExtension = zaaktypeConfiguration?.cmmnExtension,
            planItem = planItem,
            zaak = zaak
        )?.also {
            if (isZaakOpschorten(taakdata)) {
                val numberOfDays = ChronoUnit.DAYS.between(LocalDate.now(), it)
                suspensionZaakHelper.suspendZaak(zaak, numberOfDays, REDEN_OPSCHORTING)
            } else if (it.isAfter(zaak.uiterlijkeEinddatumAfdoening)) {
                val numberOfDays = ChronoUnit.DAYS.between(zaak.uiterlijkeEinddatumAfdoening, it)
                suspensionZaakHelper.extendZaakFatalDate(zaak, numberOfDays, REDEN_PAST_FATALE_DATUM)
            }
        }

        val shouldSendMail = isSendDataSendMail(taakdata)
        val sendDataMail = readSendDataMail(taakdata)
        if (shouldSendMail && sendDataMail != null) {
            val mail = Mail.valueOf(sendDataMail)

            val mailTemplate = zaaktypeConfiguration?.getMailtemplateKoppelingen().orEmpty()
                .map { it.mailTemplate }
                .firstOrNull { it?.mail == mail }
                ?: mailTemplateService.readDefaultMailTemplate(mail)

            val afzender = configurationService.readGemeenteNaam()
            setMailBody(
                taakdata,
                mailService.sendMail(
                    MailGegevens(
                        from = readMailFrom(taakdata)?.let { MailAdres(it, afzender) }
                            ?: mailService.getGemeenteMailAdres(),
                        to = MailAdres(checkNotNull(readMailTo(taakdata)) { "No mail recipient found in task data" }, null),
                        replyTo = readMailReplyTo(taakdata)?.let { MailAdres(it, afzender) },
                        subject = mailTemplate.onderwerp,
                        body = checkNotNull(readMailBody(taakdata)) { "No mail body found in task data" },
                        attachments = readMailAttachments(taakdata),
                        isCreateDocumentFromMail = true,
                        vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR
                    ),
                    zaak.getBronnenFromZaak()
                )
            )
        }

        cmmnService.startHumanTaskPlanItem(
            planItemInstanceId = humanTaskData.planItemInstanceId,
            groupId = humanTaskData.groep.id,
            assignee = assignee,
            dueDate = fatalDate?.let(::convertToDate),
            description = humanTaskData.toelichting,
            taakdata = taakdata,
            zaakUUID = zaakUUID
        )
        zaakspecifiekGeautoriseerdeTaakbehandelaar?.let {
            taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(
                task = cmmnService.readOpenTaskForPlanItem(humanTaskData.planItemInstanceId),
                zaak = zaak,
                medewerkerId = it
            )
        }
        indexingService.addOrUpdateZaakOrThrow(zaakUUID, false)
    }

    @POST
    @Path("doUserEventListenerPlanItem")
    fun doUserEventListenerPlanItem(userEventListenerData: RestUserEventListenerData) {
        val zaak = zrcClientService.readZaak(userEventListenerData.zaakUuid)
        val zaakRechten = policyService.readZaakRechten(zaak, loggedInUserInstance.get())
        when (userEventListenerData.actie) {
            UserEventListenerActie.BRONDATUM_ZETTEN -> assertPolicy(zaakRechten.canBrondatumZetten)
            else -> assertPolicy(zaakRechten.canStartenTaak)
        }
        userEventListenerData.restMailGegevens?.run {
            assertPolicy(zaakRechten.canVersturenEmail)
        }

        when (userEventListenerData.actie) {
            UserEventListenerActie.INTAKE_AFRONDEN -> handleIntakeAfronden(zaak, userEventListenerData)
            UserEventListenerActie.ZAAK_AFHANDELEN -> handleZaakAfhandelen(zaak, userEventListenerData)
            UserEventListenerActie.BRONDATUM_ZETTEN -> handleBrondatumZetten(zaak, userEventListenerData)
        }

        userEventListenerData.planItemInstanceId?.let {
            cmmnService.startUserEventListenerPlanItem(it)
        }
        userEventListenerData.restMailGegevens?.let {
            mailService.sendMail(
                it.toMailGegevens(configurationService.readGemeenteNaam()),
                zaak.getBronnenFromZaak()
            )
        }
    }

    private fun handleIntakeAfronden(
        zaak: Zaak,
        userEventListenerData: RestUserEventListenerData
    ) {
        userEventListenerData.planItemInstanceId?.let {
            val planItemInstance = cmmnService.readOpenPlanItem(it)
            zaakVariabelenService.setOntvankelijk(planItemInstance, userEventListenerData.isZaakOntvankelijk)
        }

        if (userEventListenerData.isZaakOntvankelijk) return

        val zaaktypeConfiguration = zaaktypeConfigurationService.readConfiguration(zaak.zaaktype.extractUuid())
        zaaktypeConfiguration.nietOntvankelijkResultaattypeOmschrijving?.let { omschrijving ->
            zgwApiService.closeZaak(
                zaak = zaak,
                resultaatTypeUUID = resultaattypeReferenceService.readNietOntvankelijkResultaattype(
                    zaaktypeConfiguration.zaaktypeUuid,
                    omschrijving
                ).url.extractUuid(),
                description = userEventListenerData.resultaatToelichting
            )
        }
    }

    private fun handleZaakAfhandelen(zaak: Zaak, userEventListenerData: RestUserEventListenerData) {
        userEventListenerData.resultaattypeUuid?.let { resultaattypeUUID ->
            zgwApiService.closeZaak(
                zaak = zaak,
                resultaatTypeUUID = resultaattypeUUID,
                description = userEventListenerData.resultaatToelichting,
                brondatum = userEventListenerData.brondatum?.let(String::toLocalDate)
            )
        } ?: throw InputValidationFailedException(
            errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
            message = "Resultaattype UUID moet gevuld zijn bij het afhandelen van een zaak."
        )
    }

    private fun handleBrondatumZetten(zaak: Zaak, userEventListenerData: RestUserEventListenerData) {
        userEventListenerData.brondatum?.let {
            val brondatum = it.let(String::toLocalDate)
            LOG.info { "Set brondatum to $brondatum for ${zaak.identificatie}" }
            zgwApiService.setBrondatum(zaak, brondatum)
        } ?: throw InputValidationFailedException(
            errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
            message = "Brondatum moet gevuld zijn bij het zetten van de brondatum van een zaak."
        )
    }

    private fun calculateFatalDate(
        humanTaskData: RestHumanTaskData,
        zaaktypeCmmnExtension: ZaaktypeCmmnExtension?,
        planItem: PlanItemInstance,
        zaak: Zaak
    ): LocalDate? {
        val humanTaskParameters = zaaktypeCmmnExtension?.findHumanTaskParameter(planItem.planItemDefinitionId)
        val zaakFatalDate = zaak.uiterlijkeEinddatumAfdoening

        humanTaskData.fataledatum?.let {
            if (!isAanvullendeInformatieTask(planItem)) {
                validateFatalDate(humanTaskData.fataledatum, zaakFatalDate)
            }

            return humanTaskData.fataledatum
        }

        return calculateFatalDateFromLeadTime(humanTaskParameters, zaakFatalDate)
    }

    private fun calculateFatalDateFromLeadTime(
        zaaktypeCmmnHumantaskParameters: ZaaktypeCmmnHumantaskParameters?,
        zaakFatalDate: LocalDate?
    ): LocalDate? {
        zaaktypeCmmnHumantaskParameters?.doorlooptijd?.let { days ->
            var calculatedFinalDate = LocalDate.now().plusDays(days.toLong())
            if (zaakFatalDate != null && calculatedFinalDate.isAfter(zaakFatalDate)) {
                calculatedFinalDate = zaakFatalDate
            }
            return calculatedFinalDate
        }
        return null
    }

    private fun isAanvullendeInformatieTask(planItem: PlanItemInstance): Boolean =
        FormulierDefinitie.AANVULLENDE_INFORMATIE.toString() == planItem.planItemDefinitionId

    private fun validateFatalDate(taskFatalDate: LocalDate?, zaakFatalDate: LocalDate) {
        if (taskFatalDate != null && taskFatalDate.isAfter(zaakFatalDate)) {
            throw InputValidationFailedException(
                message = "Fatal date of a task ($taskFatalDate) cannot be later than the fatal date of the zaak ($zaakFatalDate)"
            )
        }
    }
}
