/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems

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
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.MailtemplateKoppeling
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.app.exception.InputValidationFailedException
import net.atos.zac.app.mail.converter.RESTMailGegevensConverter
import net.atos.zac.app.planitems.converter.RESTPlanItemConverter
import net.atos.zac.app.planitems.model.RESTHumanTaskData
import net.atos.zac.app.planitems.model.RESTPlanItem
import net.atos.zac.app.planitems.model.RESTProcessTaskData
import net.atos.zac.app.planitems.model.RESTUserEventListenerData
import net.atos.zac.app.planitems.model.UserEventListenerActie
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.mail.MailService
import net.atos.zac.mail.model.MailAdres
import net.atos.zac.mail.model.getBronnenFromZaak
import net.atos.zac.mailtemplates.MailTemplateService
import net.atos.zac.mailtemplates.model.Mail
import net.atos.zac.mailtemplates.model.MailGegevens
import net.atos.zac.mailtemplates.model.MailTemplate
import net.atos.zac.policy.PolicyService
import net.atos.zac.shared.helper.SuspensionZaakHelper
import net.atos.zac.util.UriUtil
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.zoeken.IndexingService
import org.flowable.cmmn.api.runtime.PlanItemInstance
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Provides REST endpoints for CMMN plan items.
 */
@Singleton
@Path("planitems")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class PlanItemsRESTService {
    private var zaakVariabelenService: ZaakVariabelenService? = null
    private var cmmnService: CMMNService? = null
    private var zrcClientService: ZrcClientService? = null
    private var brcClientService: BrcClientService? = null
    private var zaakafhandelParameterService: ZaakafhandelParameterService? = null
    private var planItemConverter: RESTPlanItemConverter? = null
    private var zgwApiService: ZGWApiService? = null
    private var indexingService: IndexingService? = null
    private var mailService: MailService? = null
    private var configuratieService: ConfiguratieService? = null
    private var mailTemplateService: MailTemplateService? = null
    private var policyService: PolicyService? = null
    private var suspensionZaakHelper: SuspensionZaakHelper? = null
    private var restMailGegevensConverter: RESTMailGegevensConverter? = null

    /**
     * Default no-arg constructor, required by Weld.
     */
    constructor()

    @Inject
    constructor(
        zaakVariabelenService: ZaakVariabelenService?,
        cmmnService: CMMNService?,
        zrcClientService: ZrcClientService?,
        brcClientService: BrcClientService?,
        zaakafhandelParameterService: ZaakafhandelParameterService?,
        planItemConverter: RESTPlanItemConverter?,
        zgwApiService: ZGWApiService?,
        indexingService: IndexingService?,
        mailService: MailService?,
        configuratieService: ConfiguratieService?,
        mailTemplateService: MailTemplateService?,
        policyService: PolicyService?,
        suspensionZaakHelper: SuspensionZaakHelper?,
        restMailGegevensConverter: RESTMailGegevensConverter?
    ) {
        this.zaakVariabelenService = zaakVariabelenService
        this.cmmnService = cmmnService
        this.zrcClientService = zrcClientService
        this.brcClientService = brcClientService
        this.zaakafhandelParameterService = zaakafhandelParameterService
        this.planItemConverter = planItemConverter
        this.zgwApiService = zgwApiService
        this.indexingService = indexingService
        this.mailService = mailService
        this.configuratieService = configuratieService
        this.mailTemplateService = mailTemplateService
        this.policyService = policyService
        this.suspensionZaakHelper = suspensionZaakHelper
        this.restMailGegevensConverter = restMailGegevensConverter
    }

    @GET
    @Path("zaak/{uuid}/humanTaskPlanItems")
    fun listHumanTaskPlanItems(@PathParam("uuid") zaakUUID: UUID?): List<RESTPlanItem?> {
        val humanTaskPlanItems = cmmnService!!.listHumanTaskPlanItems(zaakUUID)
        val zaak = zrcClientService!!.readZaak(zaakUUID)
        return planItemConverter!!.convertPlanItems(humanTaskPlanItems, zaak).stream()
            .filter { restPlanItem: RESTPlanItem? -> restPlanItem!!.actief }
            .toList()
    }

    @GET
    @Path("zaak/{uuid}/processTaskPlanItems")
    fun listProcessTaskPlanItems(@PathParam("uuid") zaakUUID: UUID?): List<RESTPlanItem?> {
        val processTaskPlanItems = cmmnService!!.listProcessTaskPlanItems(zaakUUID)
        val zaak = zrcClientService!!.readZaak(zaakUUID)
        return planItemConverter!!.convertPlanItems(processTaskPlanItems, zaak)
    }

    @GET
    @Path("zaak/{uuid}/userEventListenerPlanItems")
    fun listUserEventListenerPlanItems(@PathParam("uuid") zaakUUID: UUID?): List<RESTPlanItem?> {
        val userEventListenerPlanItems = cmmnService!!.listUserEventListenerPlanItems(zaakUUID)
        val zaak = zrcClientService!!.readZaak(zaakUUID)
        return planItemConverter!!.convertPlanItems(userEventListenerPlanItems, zaak)
    }

    @GET
    @Path("humanTaskPlanItem/{id}")
    fun readHumanTaskPlanItem(@PathParam("id") planItemId: String): RESTPlanItem {
        return convertPlanItem(planItemId)
    }

    @GET
    @Path("processTaskPlanItem/{id}")
    fun readProcessTaskPlanItem(@PathParam("id") planItemId: String): RESTPlanItem {
        return convertPlanItem(planItemId)
    }

    private fun convertPlanItem(planItemId: String): RESTPlanItem {
        val planItemInstance = cmmnService!!.readOpenPlanItem(planItemId)
        val zaakUUID = zaakVariabelenService!!.readZaakUUID(planItemInstance)
        val zaaktypeUUID = zaakVariabelenService!!.readZaaktypeUUID(planItemInstance)
        val zaakafhandelParameters = zaakafhandelParameterService!!.readZaakafhandelParameters(
            zaaktypeUUID
        )
        return planItemConverter!!.convertPlanItem(planItemInstance, zaakUUID, zaakafhandelParameters)
    }

    @POST
    @Path("doHumanTaskPlanItem")
    fun doHumanTaskplanItem(humanTaskData: @Valid RESTHumanTaskData) {
        val planItem = cmmnService!!.readOpenPlanItem(humanTaskData.planItemInstanceId)
        val zaakUUID = zaakVariabelenService!!.readZaakUUID(planItem)
        val zaak = zrcClientService!!.readZaak(zaakUUID)
        val taakdata = humanTaskData.taakdata
        PolicyService.assertPolicy(policyService!!.readZaakRechten(zaak).startenTaak)
        val zaakafhandelParameters = zaakafhandelParameterService!!.readZaakafhandelParameters(
            UriUtil.uuidFromURI(zaak.zaaktype)
        )

        val fatalDate = calculateFatalDate(humanTaskData, zaakafhandelParameters, planItem, zaak)
        if (fatalDate != null) {
            if (TaakVariabelenService.isZaakOpschorten(taakdata)) {
                val numberOfDays = ChronoUnit.DAYS.between(LocalDate.now(), fatalDate)
                suspensionZaakHelper!!.suspendZaak(zaak, numberOfDays, REDEN_OPSCHORTING)
            } else if (fatalDate.isAfter(zaak.uiterlijkeEinddatumAfdoening)) {
                val numberOfDays = ChronoUnit.DAYS.between(zaak.uiterlijkeEinddatumAfdoening, fatalDate)
                suspensionZaakHelper!!.extendZaakFatalDate(zaak, numberOfDays, REDEN_PAST_FATALE_DATUM)
            }
        }

        if (humanTaskData.taakStuurGegevens!!.sendMail) {
            val mail = Mail.valueOf(
                humanTaskData.taakStuurGegevens!!.mail!!
            )

            val mailTemplate = zaakafhandelParameters.mailtemplateKoppelingen.stream()
                .map { obj: MailtemplateKoppeling -> obj.mailTemplate }
                .filter { template: MailTemplate -> template.mail == mail }
                .findFirst()
                .orElseGet { mailTemplateService!!.readMailtemplate(mail) }

            val afzender = configuratieService!!.readGemeenteNaam()
            TaakVariabelenService.setMailBody(taakdata, mailService!!.sendMail(
                MailGegevens(
                    TaakVariabelenService.readMailFrom(taakdata)
                        .map { email: String? ->
                            MailAdres(
                                email!!, afzender
                            )
                        }
                        .orElseGet { mailService!!.gemeenteMailAdres },
                    TaakVariabelenService.readMailTo(taakdata)
                        .map { email: String? ->
                            MailAdres(
                                email!!
                            )
                        }
                        .orElse(null),
                    TaakVariabelenService.readMailReplyTo(taakdata)
                        .map { email: String? ->
                            MailAdres(
                                email!!, afzender
                            )
                        }
                        .orElse(null),
                    mailTemplate.onderwerp,
                    TaakVariabelenService.readMailBody(taakdata).orElse(null),
                    TaakVariabelenService.readMailAttachments(taakdata).orElse(null),
                    true),
                zaak.getBronnenFromZaak()))
        }
        cmmnService!!.startHumanTaskPlanItem(
            humanTaskData.planItemInstanceId,
            humanTaskData.groep!!.id,
            if (humanTaskData.medewerker != null && !humanTaskData.medewerker.toString()
                    .isEmpty()
            ) humanTaskData.medewerker!!.id else null,
            DateTimeConverterUtil.convertToDate(fatalDate),
            humanTaskData.toelichting,
            taakdata,
            zaakUUID
        )
        indexingService!!.addOrUpdateZaak(zaakUUID, false)
    }

    @POST
    @Path("doProcessTaskPlanItem")
    fun doProcessTaskplanItem(processTaskData: RESTProcessTaskData) {
        cmmnService!!.startProcessTaskPlanItem(processTaskData.planItemInstanceId, processTaskData.data)
    }

    @POST
    @Path("doUserEventListenerPlanItem")
    fun doUserEventListenerPlanItem(userEventListenerData: RESTUserEventListenerData) {
        val zaak = zrcClientService!!.readZaak(userEventListenerData.zaakUuid)
        val zaakRechten = policyService!!.readZaakRechten(zaak)
        PolicyService.assertPolicy(zaakRechten.startenTaak)
        if (userEventListenerData.restMailGegevens != null) {
            PolicyService.assertPolicy(zaakRechten.versturenEmail)
        }
        when (userEventListenerData.actie) {
            UserEventListenerActie.INTAKE_AFRONDEN -> {
                val planItemInstance = cmmnService!!.readOpenPlanItem(
                    userEventListenerData.planItemInstanceId
                )
                zaakVariabelenService!!.setOntvankelijk(planItemInstance, userEventListenerData.zaakOntvankelijk)
                if (!userEventListenerData.zaakOntvankelijk!!) {
                    policyService!!.checkZaakAfsluitbaar(zaak)
                    val zaakafhandelParameters = zaakafhandelParameterService!!.readZaakafhandelParameters(
                        UriUtil.uuidFromURI(zaak.zaaktype)
                    )
                    zgwApiService!!.createResultaatForZaak(
                        zaak,
                        zaakafhandelParameters.nietOntvankelijkResultaattype,
                        userEventListenerData.resultaatToelichting
                    )
                }
            }

            UserEventListenerActie.ZAAK_AFHANDELEN -> {
                policyService!!.checkZaakAfsluitbaar(zaak)
                if (!brcClientService!!.listBesluiten(zaak).isEmpty()) {
                    val resultaat = zrcClientService!!.readResultaat(zaak.resultaat)
                    resultaat.toelichting = userEventListenerData.resultaatToelichting
                    zrcClientService!!.updateResultaat(resultaat)
                } else {
                    zgwApiService!!.createResultaatForZaak(
                        zaak,
                        userEventListenerData.resultaattypeUuid,
                        userEventListenerData.resultaatToelichting
                    )
                }
            }
        }
        cmmnService!!.startUserEventListenerPlanItem(userEventListenerData.planItemInstanceId)
        if (userEventListenerData.restMailGegevens != null) {
            mailService!!.sendMail(
                restMailGegevensConverter!!.convert(userEventListenerData.restMailGegevens),
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
        val humanTaskParameters = zaakafhandelParameters.findHumanTaskParameter(
            planItem
                .planItemDefinitionId
        )
        val zaakFatalDate = zaak.uiterlijkeEinddatumAfdoening

        if (humanTaskData.fataledatum != null) {
            if (!isAanvullendeInformatieTask(planItem)) {
                validateFatalDate(humanTaskData.fataledatum!!, zaakFatalDate)
            }
            return humanTaskData.fataledatum
        } else {
            if (humanTaskParameters.isPresent && humanTaskParameters.get().doorlooptijd != null) {
                var calculatedFinalDate = LocalDate.now().plusDays(humanTaskParameters.get().doorlooptijd.toLong())
                if (calculatedFinalDate.isAfter(zaakFatalDate)) {
                    calculatedFinalDate = zaakFatalDate
                }
                return calculatedFinalDate
            }
        }

        return null
    }

    companion object {
        private const val REDEN_OPSCHORTING = "Aanvullende informatie opgevraagd"
        private const val REDEN_PAST_FATALE_DATUM = "Aanvullende informatie opgevraagd"

        private fun isAanvullendeInformatieTask(planItem: PlanItemInstance): Boolean {
            return FormulierDefinitie.AANVULLENDE_INFORMATIE.toString() == planItem.planItemDefinitionId
        }

        private fun validateFatalDate(taskFatalDate: LocalDate, zaakFatalDate: LocalDate) {
            if (taskFatalDate.isAfter(zaakFatalDate)) {
                throw InputValidationFailedException(
                    String.format(
                        "Fatal date of a task (%s) cannot be later than the fatal date of the zaak (%s)",
                        taskFatalDate,
                        zaakFatalDate
                    )
                )
            }
        }
    }
}
