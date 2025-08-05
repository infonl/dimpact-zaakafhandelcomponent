/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems

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
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.HumanTaskParameters
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.app.mail.converter.RESTMailGegevensConverter
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.planitems.converter.RESTPlanItemConverter
import nl.info.zac.app.planitems.model.RESTHumanTaskData
import nl.info.zac.app.planitems.model.RESTPlanItem
import nl.info.zac.app.planitems.model.RESTProcessTaskData
import nl.info.zac.app.planitems.model.RESTUserEventListenerData
import nl.info.zac.app.planitems.model.UserEventListenerActie
import nl.info.zac.configuratie.ConfiguratieService
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
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID

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
    private var zaakVariabelenService: ZaakVariabelenService,
    private var cmmnService: CMMNService,
    private var zrcClientService: ZrcClientService,
    private var brcClientService: BrcClientService,
    private var zaakafhandelParameterService: ZaakafhandelParameterService,
    private var planItemConverter: RESTPlanItemConverter,
    private var zgwApiService: ZGWApiService,
    private var indexingService: IndexingService,
    private var mailService: MailService,
    private var configuratieService: ConfiguratieService,
    private var mailTemplateService: MailTemplateService,
    private var policyService: PolicyService,
    private var suspensionZaakHelper: SuspensionZaakHelper,
    private var restMailGegevensConverter: RESTMailGegevensConverter,
    private var zaakService: ZaakService
) {
    companion object {
        private const val REDEN_OPSCHORTING = "Aanvullende informatie opgevraagd"
        private const val REDEN_PAST_FATALE_DATUM = "Aanvullende informatie opgevraagd"
    }

    @GET
    @Path("zaak/{uuid}/humanTaskPlanItems")
    fun listHumanTaskPlanItems(@PathParam("uuid") zaakUUID: UUID): List<RESTPlanItem> =
        cmmnService.listHumanTaskPlanItems(zaakUUID).let { humanTaskPlanItems ->
            zrcClientService.readZaak(zaakUUID).let { zaak ->
                planItemConverter.convertPlanItems(humanTaskPlanItems, zaak).filter { it.actief }
            }
        }

    @GET
    @Path("zaak/{uuid}/processTaskPlanItems")
    fun listProcessTaskPlanItems(@PathParam("uuid") zaakUUID: UUID): List<RESTPlanItem> =
        cmmnService.listProcessTaskPlanItems(zaakUUID).let { processTaskPlanItems ->
            zrcClientService.readZaak(zaakUUID).let { zaak ->
                planItemConverter.convertPlanItems(processTaskPlanItems, zaak)
            }
        }

    @GET
    @Path("zaak/{uuid}/userEventListenerPlanItems")
    fun listUserEventListenerPlanItems(@PathParam("uuid") zaakUUID: UUID): List<RESTPlanItem> =
        cmmnService.listUserEventListenerPlanItems(zaakUUID).let { userEventListenerPlanItems ->
            zrcClientService.readZaak(zaakUUID).let { zaak ->
                planItemConverter.convertPlanItems(userEventListenerPlanItems, zaak)
            }
        }

    @GET
    @Path("humanTaskPlanItem/{id}")
    fun readHumanTaskPlanItem(@PathParam("id") planItemId: String): RESTPlanItem =
        convertPlanItem(planItemId)

    @GET
    @Path("processTaskPlanItem/{id}")
    fun readProcessTaskPlanItem(@PathParam("id") planItemId: String): RESTPlanItem =
        convertPlanItem(planItemId)

    @Suppress("NestedBlockDepth")
    private fun convertPlanItem(planItemId: String): RESTPlanItem =
        cmmnService.readOpenPlanItem(planItemId).let { planItemInstance ->
            zaakVariabelenService.readZaakUUID(planItemInstance).let { zaakUUID ->
                zaakVariabelenService.readZaaktypeUUID(planItemInstance).let { zaaktypeUUID ->
                    zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUUID).let { zaps ->
                        planItemConverter.convertPlanItem(planItemInstance, zaakUUID, zaps)
                    }
                }
            }
        }

    @POST
    @Path("doHumanTaskPlanItem")
    @Suppress("LongMethod")
    fun doHumanTaskplanItem(@Valid humanTaskData: RESTHumanTaskData) {
        val planItem = cmmnService.readOpenPlanItem(humanTaskData.planItemInstanceId)
        val zaakUUID = zaakVariabelenService.readZaakUUID(planItem)
        val zaak = zrcClientService.readZaak(zaakUUID)
        val taakdata = humanTaskData.taakdata
        assertPolicy(policyService.readZaakRechten(zaak).startenTaak)
        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
            zaak.zaaktype.extractUuid()
        )

        val fatalDate = calculateFatalDate(humanTaskData, zaakafhandelParameters, planItem, zaak)?.also {
            if (TaakVariabelenService.isZaakOpschorten(taakdata)) {
                val numberOfDays = ChronoUnit.DAYS.between(LocalDate.now(), it)
                suspensionZaakHelper.suspendZaak(zaak, numberOfDays, REDEN_OPSCHORTING)
            } else if (it.isAfter(zaak.uiterlijkeEinddatumAfdoening)) {
                val numberOfDays = ChronoUnit.DAYS.between(zaak.uiterlijkeEinddatumAfdoening, it)
                suspensionZaakHelper.extendZaakFatalDate(zaak, numberOfDays, REDEN_PAST_FATALE_DATUM)
            }
        }

        if (humanTaskData.taakStuurGegevens.sendMail && humanTaskData.taakStuurGegevens.mail != null) {
            val mail = Mail.valueOf(humanTaskData.taakStuurGegevens.mail as String)

            val mailTemplate = zaakafhandelParameters.mailtemplateKoppelingen
                .map { it.mailTemplate }
                .firstOrNull { it.mail == mail }
                ?: mailTemplateService.readMailtemplate(mail)

            val afzender = configuratieService.readGemeenteNaam()
            TaakVariabelenService.setMailBody(
                taakdata,
                mailService.sendMail(
                    MailGegevens(
                        TaakVariabelenService.readMailFrom(taakdata)
                            .map { MailAdres(it, afzender) }
                            .orElseGet { mailService.getGemeenteMailAdres() },
                        TaakVariabelenService.readMailTo(taakdata)
                            .map { MailAdres(it, null) }
                            .orElse(null),
                        TaakVariabelenService.readMailReplyTo(taakdata)
                            .map { MailAdres(it, afzender) }
                            .orElse(null),
                        mailTemplate.onderwerp,
                        TaakVariabelenService.readMailBody(taakdata).orElse(null),
                        TaakVariabelenService.readMailAttachments(taakdata).orElse(null),
                        true
                    ),
                    zaak.getBronnenFromZaak()
                )
            )
        }

        cmmnService.startHumanTaskPlanItem(
            planItemInstanceId = humanTaskData.planItemInstanceId,
            groupId = humanTaskData.groep.id,
            assignee = humanTaskData.medewerker?.id.takeIf { !it.isNullOrBlank() },
            dueDate = DateTimeConverterUtil.convertToDate(fatalDate),
            description = humanTaskData.toelichting,
            taakdata = taakdata,
            zaakUUID = zaakUUID
        )
        indexingService.addOrUpdateZaak(zaakUUID, false)
    }

    @POST
    @Path("doProcessTaskPlanItem")
    fun doProcessTaskplanItem(processTaskData: RESTProcessTaskData) =
        cmmnService.startProcessTaskPlanItem(processTaskData.planItemInstanceId, processTaskData.data)

    @POST
    @Path("doUserEventListenerPlanItem")
    fun doUserEventListenerPlanItem(userEventListenerData: RESTUserEventListenerData) {
        val zaak = zrcClientService.readZaak(userEventListenerData.zaakUuid)
        val zaakRechten = policyService.readZaakRechten(zaak)
        assertPolicy(zaakRechten.startenTaak)
        if (userEventListenerData.restMailGegevens != null) {
            assertPolicy(zaakRechten.versturenEmail)
        }
        when (userEventListenerData.actie) {
            UserEventListenerActie.INTAKE_AFRONDEN -> {
                userEventListenerData.planItemInstanceId?.let {
                    val planItemInstance = cmmnService.readOpenPlanItem(it)
                    zaakVariabelenService.setOntvankelijk(planItemInstance, userEventListenerData.zaakOntvankelijk)
                }
                if (!userEventListenerData.zaakOntvankelijk) {
                    zaakService.checkZaakAfsluitbaar(zaak)
                    val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(
                        zaak.zaaktype.extractUuid()
                    )
                    zgwApiService.createResultaatForZaak(
                        zaak,
                        zaakafhandelParameters.nietOntvankelijkResultaattype,
                        userEventListenerData.resultaatToelichting
                    )
                }
            }

            UserEventListenerActie.ZAAK_AFHANDELEN -> {
                zaakService.checkZaakAfsluitbaar(zaak)
                if (!brcClientService.listBesluiten(zaak).isEmpty()) {
                    val resultaat = zrcClientService.readResultaat(zaak.resultaat)
                    resultaat.toelichting = userEventListenerData.resultaatToelichting
                    zrcClientService.updateResultaat(resultaat)
                } else {
                    zgwApiService.createResultaatForZaak(
                        zaak,
                        userEventListenerData.resultaattypeUuid ?: throw InputValidationFailedException(
                            errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
                            message = "Resultaattype UUID moet gevuld zijn bij het afhandelen van een zaak."
                        ),
                        userEventListenerData.resultaatToelichting
                    )
                }
            }
        }
        userEventListenerData.planItemInstanceId?.let {
            cmmnService.startUserEventListenerPlanItem(it)
        }
        if (userEventListenerData.restMailGegevens != null) {
            mailService.sendMail(
                restMailGegevensConverter.convert(userEventListenerData.restMailGegevens),
                zaak.getBronnenFromZaak()
            )
        }
    }

    private fun calculateFatalDate(
        humanTaskData: RESTHumanTaskData,
        zaakafhandelParameters: ZaakafhandelParameters,
        planItem: PlanItemInstance,
        zaak: Zaak
    ): LocalDate? {
        val humanTaskParameters = zaakafhandelParameters.findHumanTaskParameter(planItem.planItemDefinitionId)
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
        humanTaskParameters: Optional<HumanTaskParameters>,
        zaakFatalDate: LocalDate?
    ): LocalDate? {
        if (humanTaskParameters.isPresent && humanTaskParameters.get().doorlooptijd != null) {
            var calculatedFinalDate = LocalDate.now().plusDays(humanTaskParameters.get().doorlooptijd.toLong())
            if (calculatedFinalDate.isAfter(zaakFatalDate)) {
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
