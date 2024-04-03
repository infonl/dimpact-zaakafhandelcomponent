/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.TakenService
import net.atos.zac.gebruikersvoorkeuren.model.TabelInstellingen
import net.atos.zac.signalering.model.Signalering
import net.atos.zac.signalering.model.SignaleringDetail
import net.atos.zac.signalering.model.SignaleringTarget
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringVerzendInfo
import net.atos.zac.signalering.model.SignaleringVerzondenZoekParameters
import net.atos.zac.util.UriUtil
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zoeken.ZoekenService
import net.atos.zac.zoeken.model.DatumRange
import net.atos.zac.zoeken.model.DatumVeld
import net.atos.zac.zoeken.model.FilterVeld
import net.atos.zac.zoeken.model.FilterWaarde
import net.atos.zac.zoeken.model.ZoekObject
import net.atos.zac.zoeken.model.ZoekParameters
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import nl.lifely.zac.util.NoArgConstructor
import org.flowable.task.api.Task
import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import java.util.function.Consumer
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@Suppress("TooManyFunctions")
@NoArgConstructor
open class SignaleringenJob @Inject constructor(
    private val signaleringenService: SignaleringenService,
    private val configuratieService: ConfiguratieService,
    private val ztcClientService: ZTCClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val zoekenService: ZoekenService,
    private val takenService: TakenService
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(SignaleringenJob::class.java.name)
        const val ZAAK_AFGEHANDELD_QUERY: String = "zaak_afgehandeld"
    }

    open fun signaleringenVerzenden() {
        zaakSignaleringenVerzenden()
        taakSignaleringenVerzenden()
    }

    /**
     * This is the batchjob to send E-Mail warnings about cases that are approaching their einddatum gepland or their
     * uiterlijke einddatum afdoening.
     */
    private fun zaakSignaleringenVerzenden() {
        val info = SignaleringVerzendInfo()
        LOG.info("Zaak signaleringen verzenden: gestart...")
        ztcClientService.listZaaktypen(configuratieService.readDefaultCatalogusURI())
            .forEach(
                Consumer<ZaakType> { zaaktype: ZaakType ->
                    val parameters = zaakafhandelParameterService.readZaakafhandelParameters(
                        URIUtil.parseUUIDFromResourceURI(zaaktype.url)
                    )
                    if (parameters.einddatumGeplandWaarschuwing != null) {
                        info.streefdatumVerzonden += zaakEinddatumGeplandVerzenden(
                            zaaktype,
                            parameters.einddatumGeplandWaarschuwing
                        )
                        zaakEinddatumGeplandOnterechtVerzondenVerwijderen(
                            zaaktype,
                            parameters.einddatumGeplandWaarschuwing
                        )
                    }
                    if (parameters.uiterlijkeEinddatumAfdoeningWaarschuwing != null) {
                        info.fataledatumVerzonden += zaakUiterlijkeEinddatumAfdoeningVerzenden(
                            zaaktype,
                            parameters.uiterlijkeEinddatumAfdoeningWaarschuwing
                        )
                        zaakUiterlijkeEinddatumAfdoeningOnterechtVerzondenVerwijderen(
                            zaaktype,
                            parameters.uiterlijkeEinddatumAfdoeningWaarschuwing
                        )
                    }
                }
            )
        LOG.info(
            """Zaak signaleringen verzenden: gestopt (${info.streefdatumVerzonden} streefdatum waarschuwingen,
            | ${info.fataledatumVerzonden} fatale datum waarschuwingen)
            """.trimMargin()
        )
    }

    /**
     * Send the E-Mail warnings about the einddatum gepland
     *
     * @return the number of E-Mails sent
     */
    private fun zaakEinddatumGeplandVerzenden(zaaktype: ZaakType, venster: Int): Int {
        val verzonden = IntArray(1)
        zoekenService.zoek(getZaakSignaleringTeVerzendenZoekParameters(DatumVeld.ZAAK_STREEFDATUM, zaaktype, venster))
            .items.stream()
            .map { zaakZoekObject: ZoekObject? -> zaakZoekObject as ZaakZoekObject? }
            .map { zaakZoekObject: ZaakZoekObject? ->
                buildZaakSignalering(
                    getZaakSignaleringTarget(zaakZoekObject, SignaleringDetail.STREEFDATUM),
                    zaakZoekObject,
                    SignaleringDetail.STREEFDATUM
                )
            }
            .filter { obj: Signalering? -> Objects.nonNull(obj) }
            .forEach { signalering: Signalering? -> verzonden[0] += verzendZaakSignalering(signalering) }
        return verzonden[0]
    }

    /**
     * Send the E-Mail warnings about the uiterlijke einddatum afdoening
     *
     * @return the number of E-Mails sent
     */
    private fun zaakUiterlijkeEinddatumAfdoeningVerzenden(
        zaaktype: ZaakType,
        venster: Int
    ): Int {
        val verzonden = IntArray(1)
        zoekenService.zoek(
            getZaakSignaleringTeVerzendenZoekParameters(
                DatumVeld.ZAAK_FATALE_DATUM,
                zaaktype,
                venster
            )
        )
            .items.stream()
            .map { zaakZoekObject: ZoekObject? -> zaakZoekObject as ZaakZoekObject? }
            .map { zaakZoekObject: ZaakZoekObject? ->
                buildZaakSignalering(
                    getZaakSignaleringTarget(zaakZoekObject, SignaleringDetail.FATALE_DATUM),
                    zaakZoekObject,
                    SignaleringDetail.FATALE_DATUM
                )
            }
            .filter { obj: Signalering? -> Objects.nonNull(obj) }
            .forEach { signalering: Signalering? -> verzonden[0] += verzendZaakSignalering(signalering) }
        return verzonden[0]
    }

    private fun getZaakSignaleringTarget(zaak: ZaakZoekObject?, detail: SignaleringDetail): String? {
        if (signaleringenService.readInstellingenUser(
                SignaleringType.Type.ZAAK_VERLOPEND,
                zaak!!.behandelaarGebruikersnaam
            ).isMail &&
            !signaleringenService.findSignaleringVerzonden(
                getZaakSignaleringVerzondenParameters(
                    zaak.behandelaarGebruikersnaam, zaak.uuid,
                    detail
                )
            ).isPresent
        ) {
            return zaak.behandelaarGebruikersnaam
        }
        return null
    }

    private fun buildZaakSignalering(
        target: String?,
        zaakZoekObject: ZaakZoekObject?,
        detail: SignaleringDetail
    ): Signalering? {
        if (target != null) {
            val zaak = Zaak()
            zaak.uuid = UUID.fromString(zaakZoekObject!!.uuid)
            val signalering = signaleringenService.signaleringInstance(
                SignaleringType.Type.ZAAK_VERLOPEND
            )
            signalering.setTargetUser(target)
            signalering.setSubject(zaak)
            signalering.setDetail(detail)
            return signalering
        }
        return null
    }

    private fun verzendZaakSignalering(signalering: Signalering?): Int {
        signaleringenService.sendSignalering(signalering)
        signaleringenService.createSignaleringVerzonden(signalering)
        return 1
    }

    /**
     * Make sure already sent E-Mail warnings will get send again (in cases where the einddatum gepland has changed)
     */
    private fun zaakEinddatumGeplandOnterechtVerzondenVerwijderen(
        zaaktype: ZaakType,
        venster: Int
    ) {
        zoekenService.zoek(
            getZaakSignaleringLaterTeVerzendenZoekParameters(DatumVeld.ZAAK_STREEFDATUM, zaaktype, venster)
        )
            .items.stream()
            .map { zaakZoekObject: ZoekObject? -> zaakZoekObject as ZaakZoekObject? }
            .map { zaakZoekObject: ZaakZoekObject? ->
                getZaakSignaleringVerzondenParameters(
                    zaakZoekObject!!.behandelaarGebruikersnaam,
                    zaakZoekObject.uuid,
                    SignaleringDetail.STREEFDATUM
                )
            }
            .forEach { verzonden: SignaleringVerzondenZoekParameters ->
                signaleringenService.deleteSignaleringVerzonden(
                    verzonden
                )
            }
    }

    /**
     * Make sure already sent E-Mail warnings will get send again
     * (in cases where the uiterlijke einddatum afdoening has changed)
     */
    private fun zaakUiterlijkeEinddatumAfdoeningOnterechtVerzondenVerwijderen(
        zaaktype: ZaakType,
        venster: Int
    ) {
        zoekenService.zoek(
            getZaakSignaleringLaterTeVerzendenZoekParameters(DatumVeld.ZAAK_FATALE_DATUM, zaaktype, venster)
        )
            .items.stream()
            .map { zaakZoekObject: ZoekObject? -> zaakZoekObject as ZaakZoekObject? }
            .map { zaakZoekObject: ZaakZoekObject? ->
                getZaakSignaleringVerzondenParameters(
                    zaakZoekObject!!.behandelaarGebruikersnaam,
                    zaakZoekObject.uuid,
                    SignaleringDetail.FATALE_DATUM
                )
            }
            .forEach { verzonden: SignaleringVerzondenZoekParameters ->
                signaleringenService.deleteSignaleringVerzonden(
                    verzonden
                )
            }
    }

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
    ): SignaleringVerzondenZoekParameters {
        return SignaleringVerzondenZoekParameters(SignaleringTarget.USER, target)
            .types(SignaleringType.Type.ZAAK_VERLOPEND)
            .subjectZaak(UUID.fromString(zaakUUID))
            .detail(detail)
    }

    /**
     * This is the batchjob to send E-Mail warnings about tasks that are at or past their due date.
     */
    private fun taakSignaleringenVerzenden() {
        val info = SignaleringVerzendInfo()
        LOG.info("Taak signaleringen verzenden: gestart...")
        info.fataledatumVerzonden += taakDueVerzenden()
        taakDueOnterechtVerzondenVerwijderen()
        LOG.info("Taak signaleringen verzenden: gestopt (${info.fataledatumVerzonden} fatale datum waarschuwingen)")
    }

    /**
     * Send the E-Mail warnings about the due date
     *
     * @return the number of E-Mails sent
     */
    private fun taakDueVerzenden(): Int {
        val verzonden = IntArray(1)
        takenService.listOpenTasksDueNow().stream()
            .map { task: Task -> buildTaakSignalering(getTaakSignaleringTarget(task), task) }
            .filter { obj: Signalering? -> Objects.nonNull(obj) }
            .forEach { signalering: Signalering? -> verzonden[0] += verzendTaakSignalering(signalering) }
        return verzonden[0]
    }

    private fun getTaakSignaleringTarget(task: Task): String? {
        if (signaleringenService.readInstellingenUser(SignaleringType.Type.TAAK_VERLOPEN, task.assignee)
                .isMail &&
            !signaleringenService.findSignaleringVerzonden(
                getTaakSignaleringVerzondenParameters(task.assignee, task.id)
            ).isPresent
        ) {
            return task.assignee
        }
        return null
    }

    private fun buildTaakSignalering(target: String?, task: Task): Signalering? {
        if (target != null) {
            val signalering = signaleringenService.signaleringInstance(
                SignaleringType.Type.TAAK_VERLOPEN
            )
            signalering.setTargetUser(target)
            signalering.setSubject(task)
            signalering.setDetail(SignaleringDetail.STREEFDATUM)
            return signalering
        }
        return null
    }

    private fun verzendTaakSignalering(signalering: Signalering?): Int {
        signaleringenService.sendSignalering(signalering)
        signaleringenService.createSignaleringVerzonden(signalering)
        return 1
    }

    /**
     * Make sure already sent E-Mail warnings will get send again (in cases where the due date has changed)
     */
    private fun taakDueOnterechtVerzondenVerwijderen() {
        takenService.listOpenTasksDueLater().stream()
            .map { task: Task -> getTaakSignaleringVerzondenParameters(task.assignee, task.id) }
            .forEach { verzonden: SignaleringVerzondenZoekParameters ->
                signaleringenService.deleteSignaleringVerzonden(
                    verzonden
                )
            }
    }

    private fun getTaakSignaleringVerzondenParameters(
        target: String,
        taakId: String
    ): SignaleringVerzondenZoekParameters {
        return SignaleringVerzondenZoekParameters(SignaleringTarget.USER, target)
            .types(SignaleringType.Type.TAAK_VERLOPEN)
            .subjectTaak(taakId)
            .detail(SignaleringDetail.STREEFDATUM)
    }
}
