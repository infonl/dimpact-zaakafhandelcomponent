/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.gebruikersvoorkeuren.model.TabelInstellingen
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringDetail
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringVerzendInfo
import net.atos.zac.signalering.model.SignaleringVerzondenZoekParameters
import net.atos.zac.util.UriUtil
import net.atos.zac.zoeken.ZoekenService
import net.atos.zac.zoeken.model.DatumRange
import net.atos.zac.zoeken.model.DatumVeld
import net.atos.zac.zoeken.model.FilterVeld
import net.atos.zac.zoeken.model.FilterWaarde
import net.atos.zac.zoeken.model.ZoekParameters
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.flowable.task.api.Task
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@Suppress("TooManyFunctions")
@NoArgConstructor
@AllOpen
class ZaakTaskDueDateEmailNotificationService @Inject constructor(
    private val signaleringService: SignaleringService,
    private val configuratieService: ConfiguratieService,
    private val ztcClientService: ZtcClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val zoekenService: ZoekenService,
    private val flowableTaskService: FlowableTaskService
) {
    companion object {
        private val LOG = Logger.getLogger(ZaakTaskDueDateEmailNotificationService::class.java.name)
        const val ZAAK_AFGEHANDELD_QUERY = "zaak_afgehandeld"
    }

    /**
     * Send zaak and task due date email notifications as warnings that the
     * user should take action.
     */
    fun sendDueDateEmailNotifications() {
        sendZaakDueDateEmailNotifications()
        sendTaskDueDateEmailNotifications()
    }

    /**
     * Sends e-mail notifications about tasks that are at or past their due date.
     * Typically run as part of a cron job.
     */
    private fun sendTaskDueDateEmailNotifications() {
        val signaleringVerzendInfo = SignaleringVerzendInfo()
        LOG.info("Sending task due date email notifications...")
        signaleringVerzendInfo.fataledatumVerzonden += sendTaskDueDateNotifications()
        deleteUnjustlySentTaskDueSignaleringen()
        LOG.info(
            "Finished sending task due date email notifications (${signaleringVerzendInfo.fataledatumVerzonden} fatal date warnings)"
        )
    }

    /**
     * Sends e-mail notifications about zaken that are approaching their target date or their
     * fatal date.
     * Typically run as part of a cron job.
     */
    private fun sendZaakDueDateEmailNotifications() {
        val signaleringVerzendInfo = SignaleringVerzendInfo()
        LOG.info("Sending zaak due date email notifications...")
        ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())
            .map { zaaktype ->
                zaakafhandelParameterService.readZaakafhandelParameters(
                    URIUtil.parseUUIDFromResourceURI(zaaktype.url)
                ).let { parameters ->
                    parameters.einddatumGeplandWaarschuwing?.let {
                        signaleringVerzendInfo.streefdatumVerzonden += zaakEinddatumGeplandVerzenden(
                            zaaktype,
                            it
                        )
                        zaakEinddatumGeplandOnterechtVerzondenVerwijderen(
                            zaaktype,
                            parameters.einddatumGeplandWaarschuwing
                        )
                    }
                    parameters.uiterlijkeEinddatumAfdoeningWaarschuwing?.let {
                        signaleringVerzendInfo.fataledatumVerzonden += zaakUiterlijkeEinddatumAfdoeningVerzenden(
                            zaaktype,
                            it
                        )
                        zaakUiterlijkeEinddatumAfdoeningOnterechtVerzondenVerwijderen(
                            zaaktype,
                            parameters.uiterlijkeEinddatumAfdoeningWaarschuwing
                        )
                    }
                }
            }
        LOG.info(
            "Finished sending zaak due date email notifications (${signaleringVerzendInfo.streefdatumVerzonden} target date " +
                "warnings, ${signaleringVerzendInfo.fataledatumVerzonden} fatal date warnings)"
        )
    }

    /**
     * Sends einddatum gepland zaak email notifications
     */
    private fun zaakEinddatumGeplandVerzenden(zaaktype: ZaakType, venster: Int): Int =
        zoekenService.zoek(getZaakSignaleringTeVerzendenZoekParameters(DatumVeld.ZAAK_STREEFDATUM, zaaktype, venster))
            .items
            .map { it as ZaakZoekObject }
            .filter { hasZaakSignaleringTarget(it, SignaleringDetail.STREEFDATUM) }
            .map { buildZaakSignalering(it.behandelaarGebruikersnaam!!, it, SignaleringDetail.STREEFDATUM) }
            .sumOf(::verzendZaakSignalering)

    /**
     * Sends fatal date zaak email notifications
     */
    private fun zaakUiterlijkeEinddatumAfdoeningVerzenden(
        zaaktype: ZaakType,
        venster: Int
    ): Int =
        zoekenService.zoek(getZaakSignaleringTeVerzendenZoekParameters(DatumVeld.ZAAK_FATALE_DATUM, zaaktype, venster))
            .items
            .map { it as ZaakZoekObject }
            .filter { hasZaakSignaleringTarget(it, SignaleringDetail.FATALE_DATUM) }
            .map { buildZaakSignalering(it.behandelaarGebruikersnaam!!, it, SignaleringDetail.FATALE_DATUM) }
            .sumOf(::verzendZaakSignalering)

    private fun hasZaakSignaleringTarget(zaakZoekObject: ZaakZoekObject, detail: SignaleringDetail): Boolean =
        signaleringService.readInstellingenUser(
            SignaleringType.Type.ZAAK_VERLOPEND, zaakZoekObject.behandelaarGebruikersnaam!!
        ).isMail &&
            !signaleringService.findSignaleringVerzonden(
                getZaakSignaleringVerzondenParameters(zaakZoekObject.behandelaarGebruikersnaam!!, zaakZoekObject.id, detail)
            ).isPresent

    private fun buildZaakSignalering(
        target: String,
        zaakZoekObject: ZaakZoekObject,
        detail: SignaleringDetail
    ): Signalering {
        val zaak = Zaak().apply {
            uuid = UUID.fromString(zaakZoekObject.id)
        }
        return signaleringService.signaleringInstance(
            SignaleringType.Type.ZAAK_VERLOPEND
        ).apply {
            setTargetUser(target)
            setSubject(zaak)
            setDetailFromSignaleringDetail(detail)
        }
    }

    private fun verzendZaakSignalering(signalering: Signalering): Int {
        signaleringService.sendSignalering(signalering)
        signaleringService.createSignaleringVerzonden(signalering)
        return 1
    }

    /**
     * Make sure already sent E-Mail warnings will get send again (in cases where the einddatum gepland has changed)
     */
    private fun zaakEinddatumGeplandOnterechtVerzondenVerwijderen(
        zaaktype: ZaakType,
        venster: Int
    ) = zoekenService.zoek(
        getZaakSignaleringLaterTeVerzendenZoekParameters(DatumVeld.ZAAK_STREEFDATUM, zaaktype, venster)
    ).items.map { it as ZaakZoekObject }
        .map {
            getZaakSignaleringVerzondenParameters(
                it.behandelaarGebruikersnaam!!,
                it.getObjectId(),
                SignaleringDetail.STREEFDATUM
            )
        }.forEach(signaleringService::deleteSignaleringVerzonden)

    /**
     * Make sure already sent E-Mail warnings will get send again
     * (in cases where the uiterlijke einddatum afdoening has changed)
     */
    private fun zaakUiterlijkeEinddatumAfdoeningOnterechtVerzondenVerwijderen(
        zaaktype: ZaakType,
        venster: Int
    ) = zoekenService.zoek(
        getZaakSignaleringLaterTeVerzendenZoekParameters(DatumVeld.ZAAK_FATALE_DATUM, zaaktype, venster)
    ).items.map { it as ZaakZoekObject }
        .map {
            getZaakSignaleringVerzondenParameters(
                it.behandelaarGebruikersnaam!!,
                it.getObjectId(),
                SignaleringDetail.FATALE_DATUM
            )
        }
        .forEach(signaleringService::deleteSignaleringVerzonden)

    private fun getZaakSignaleringTeVerzendenZoekParameters(
        veld: DatumVeld,
        zaaktype: ZaakType,
        venster: Int
    ): ZoekParameters {
        val now = LocalDate.now()
        val parameters = getOpenZaakMetBehandelaarZoekParameters(zaaktype)
        parameters.addDatum(veld, DatumRange(now, now.plusDays(venster.toLong())))
        return parameters
    }

    private fun getZaakSignaleringLaterTeVerzendenZoekParameters(
        veld: DatumVeld,
        zaaktype: ZaakType,
        venster: Int
    ): ZoekParameters {
        val now = LocalDate.now()
        val parameters = getOpenZaakMetBehandelaarZoekParameters(zaaktype)
        parameters.addDatum(veld, DatumRange(now.plusDays(venster.toLong() + 1), null))
        return parameters
    }

    private fun getOpenZaakMetBehandelaarZoekParameters(zaaktype: ZaakType): ZoekParameters {
        val parameters = ZoekParameters(ZoekObjectType.ZAAK)
        parameters.addFilter(FilterVeld.ZAAK_ZAAKTYPE_UUID, UriUtil.uuidFromURI(zaaktype.url).toString())
        parameters.addFilter(FilterVeld.ZAAK_BEHANDELAAR, FilterWaarde.NIET_LEEG.toString())
        parameters.addFilterQuery(ZAAK_AFGEHANDELD_QUERY, "false")
        parameters.rows = TabelInstellingen.AANTAL_PER_PAGINA_MAX
        return parameters
    }

    private fun getZaakSignaleringVerzondenParameters(
        target: String,
        zaakUUID: String,
        detail: SignaleringDetail
    ) = SignaleringVerzondenZoekParameters(SignaleringTarget.USER, target)
        .types(SignaleringType.Type.ZAAK_VERLOPEND)
        .subjectZaak(UUID.fromString(zaakUUID))
        .detail(detail)

    private fun sendTaskDueDateNotifications(): Int =
        flowableTaskService.listOpenTasksDueNow()
            .filter(::hasTaskSignaleringTarget)
            .map { buildTaskSignalering(it.assignee, it) }
            .sumOf(::sendTaskSignalering)

    private fun hasTaskSignaleringTarget(task: Task): Boolean =
        signaleringService.readInstellingenUser(
            SignaleringType.Type.TAAK_VERLOPEN,
            task.assignee
        ).isMail &&
            // skip signalering if it was already sent
            !signaleringService.findSignaleringVerzonden(
                getTaskSignaleringSentParameters(task.assignee, task.id)
            ).isPresent

    private fun buildTaskSignalering(target: String, task: Task): Signalering =
        signaleringService.signaleringInstance(
            SignaleringType.Type.TAAK_VERLOPEN
        ).apply {
            setTargetUser(target)
            setSubject(task)
            setDetailFromSignaleringDetail(SignaleringDetail.STREEFDATUM)
        }

    private fun sendTaskSignalering(signalering: Signalering): Int {
        signaleringService.sendSignalering(signalering)
        signaleringService.createSignaleringVerzonden(signalering)
        return 1
    }

    /**
     * Make sure already sent task email notifications will get sent again (in cases where the due date has changed)
     * by deleting the corresponding 'signalering verzonden' record from the database.
     */
    private fun deleteUnjustlySentTaskDueSignaleringen() {
        flowableTaskService.listOpenTasksDueLater()
            .map { getTaskSignaleringSentParameters(it.assignee, it.id) }
            .forEach(signaleringService::deleteSignaleringVerzonden)
    }

    private fun getTaskSignaleringSentParameters(
        target: String,
        taskId: String
    ) = SignaleringVerzondenZoekParameters(SignaleringTarget.USER, target)
        .types(SignaleringType.Type.TAAK_VERLOPEN)
        .subjectTaak(taskId)
        .detail(SignaleringDetail.STREEFDATUM)
}
