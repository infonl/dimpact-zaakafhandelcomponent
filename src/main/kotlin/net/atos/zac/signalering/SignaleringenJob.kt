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
import net.atos.zac.zoeken.model.ZoekParameters
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
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
class SignaleringenJob @Inject constructor(
    private val signaleringenService: SignaleringenService,
    private val configuratieService: ConfiguratieService,
    private val ztcClientService: ZTCClientService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val zoekenService: ZoekenService,
    private val takenService: TakenService
) {

    companion object {
        private val LOG = Logger.getLogger(SignaleringenJob::class.java.name)
        const val ZAAK_AFGEHANDELD_QUERY: String = "zaak_afgehandeld"
    }

    fun signaleringenVerzenden() {
        zaakSignaleringenVerzenden()
        taakSignaleringenVerzenden()
    }

    /**
     * This is the batchjob to send E-Mail warnings about cases that are approaching their einddatum gepland or their
     * uiterlijke einddatum afdoening.
     */
    fun zaakSignaleringenVerzenden() {
        val signaleringVerzendInfo = SignaleringVerzendInfo()
        LOG.fine("Zaak signaleringen verzenden: gestart...")
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
        LOG.fine(
            """Zaak signaleringen verzenden: gestopt (${signaleringVerzendInfo.streefdatumVerzonden} streefdatum
                | waarschuwingen, ${signaleringVerzendInfo.fataledatumVerzonden} fatale datum waarschuwingen)
                |
            """.trimMargin()
        )
    }

    /**
     * Send the E-Mail warnings about the einddatum gepland
     *
     * @return the number of E-Mails sent
     */
    fun zaakEinddatumGeplandVerzenden(zaaktype: ZaakType, venster: Int): Int {
        val verzonden = IntArray(1)
        zoekenService.zoek(getZaakSignaleringTeVerzendenZoekParameters(DatumVeld.ZAAK_STREEFDATUM, zaaktype, venster))
            .items.stream()
            .map { it as ZaakZoekObject }
            .filter { hasZaakSignaleringTarget(it, SignaleringDetail.STREEFDATUM) }
            .map { buildZaakSignalering(it.behandelaarGebruikersnaam, it, SignaleringDetail.STREEFDATUM) }
            .forEach { verzonden[0] += verzendZaakSignalering(it) }
        return verzonden[0]
    }

    /**
     * Send the E-Mail warnings about the uiterlijke einddatum afdoening
     *
     * @return the number of E-Mails sent
     */
    fun zaakUiterlijkeEinddatumAfdoeningVerzenden(
        zaaktype: ZaakType,
        venster: Int
    ): Int {
        val verzonden = IntArray(1)
        zoekenService.zoek(getZaakSignaleringTeVerzendenZoekParameters(DatumVeld.ZAAK_FATALE_DATUM, zaaktype, venster))
            .items
            .map { it as ZaakZoekObject }
            .filter { hasZaakSignaleringTarget(it, SignaleringDetail.FATALE_DATUM) }
            .map { buildZaakSignalering(it.behandelaarGebruikersnaam, it, SignaleringDetail.FATALE_DATUM) }
            .forEach { verzonden[0] += verzendZaakSignalering(it) }
        return verzonden[0]
    }

    fun hasZaakSignaleringTarget(zaak: ZaakZoekObject, detail: SignaleringDetail): Boolean =
        signaleringenService.readInstellingenUser(
            SignaleringType.Type.ZAAK_VERLOPEND, zaak.behandelaarGebruikersnaam
        ).isMail &&
            !signaleringenService.findSignaleringVerzonden(
                getZaakSignaleringVerzondenParameters(zaak.behandelaarGebruikersnaam, zaak.uuid, detail)
            ).isPresent

    fun buildZaakSignalering(
        target: String,
        zaakZoekObject: ZaakZoekObject,
        detail: SignaleringDetail
    ): Signalering {
        val zaak = Zaak()
        zaak.uuid = UUID.fromString(zaakZoekObject.uuid)
        val signalering = signaleringenService.signaleringInstance(
            SignaleringType.Type.ZAAK_VERLOPEND
        )
        signalering.setTargetUser(target)
        signalering.setSubject(zaak)
        signalering.setDetail(detail)
        return signalering
    }

    fun verzendZaakSignalering(signalering: Signalering): Int {
        signaleringenService.sendSignalering(signalering)
        signaleringenService.createSignaleringVerzonden(signalering)
        return 1
    }

    /**
     * Make sure already sent E-Mail warnings will get send again (in cases where the einddatum gepland has changed)
     */
    fun zaakEinddatumGeplandOnterechtVerzondenVerwijderen(
        zaaktype: ZaakType,
        venster: Int
    ) {
        zoekenService.zoek(
            getZaakSignaleringLaterTeVerzendenZoekParameters(DatumVeld.ZAAK_STREEFDATUM, zaaktype, venster)
        )
            .items.stream()
            .map { it as ZaakZoekObject }
            .map {
                getZaakSignaleringVerzondenParameters(
                    it.behandelaarGebruikersnaam,
                    it.uuid,
                    SignaleringDetail.STREEFDATUM
                )
            }
            .forEach { signaleringenService.deleteSignaleringVerzonden(it) }
    }

    /**
     * Make sure already sent E-Mail warnings will get send again
     * (in cases where the uiterlijke einddatum afdoening has changed)
     */
    fun zaakUiterlijkeEinddatumAfdoeningOnterechtVerzondenVerwijderen(
        zaaktype: ZaakType,
        venster: Int
    ) {
        zoekenService.zoek(
            getZaakSignaleringLaterTeVerzendenZoekParameters(DatumVeld.ZAAK_FATALE_DATUM, zaaktype, venster)
        )
            .items.stream()
            .map { it as ZaakZoekObject }
            .map {
                getZaakSignaleringVerzondenParameters(
                    it.behandelaarGebruikersnaam,
                    it.uuid,
                    SignaleringDetail.FATALE_DATUM
                )
            }
            .forEach { signaleringenService.deleteSignaleringVerzonden(it) }
    }

    fun getZaakSignaleringTeVerzendenZoekParameters(
        veld: DatumVeld,
        zaaktype: ZaakType,
        venster: Int
    ): ZoekParameters {
        val now = LocalDate.now()
        val parameters = getOpenZaakMetBehandelaarZoekParameters(zaaktype)
        parameters.addDatum(veld, DatumRange(now, now.plusDays(venster.toLong())))
        return parameters
    }

    fun getZaakSignaleringLaterTeVerzendenZoekParameters(
        veld: DatumVeld,
        zaaktype: ZaakType,
        venster: Int
    ): ZoekParameters {
        val now = LocalDate.now()
        val parameters = getOpenZaakMetBehandelaarZoekParameters(zaaktype)
        parameters.addDatum(veld, DatumRange(now.plusDays(venster.toLong() + 1), null))
        return parameters
    }

    fun getOpenZaakMetBehandelaarZoekParameters(zaaktype: ZaakType): ZoekParameters {
        val parameters = ZoekParameters(ZoekObjectType.ZAAK)
        parameters.addFilter(FilterVeld.ZAAK_ZAAKTYPE_UUID, UriUtil.uuidFromURI(zaaktype.url).toString())
        parameters.addFilter(FilterVeld.ZAAK_BEHANDELAAR, FilterWaarde.NIET_LEEG.toString())
        parameters.addFilterQuery(ZAAK_AFGEHANDELD_QUERY, "false")
        parameters.rows = TabelInstellingen.AANTAL_PER_PAGINA_MAX
        return parameters
    }

    fun getZaakSignaleringVerzondenParameters(
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
    fun taakSignaleringenVerzenden() {
        val signaleringVerzendInfo = SignaleringVerzendInfo()
        LOG.info("Taak signaleringen verzenden: gestart...")
        signaleringVerzendInfo.fataledatumVerzonden += taakDueVerzenden()
        taakDueOnterechtVerzondenVerwijderen()
        LOG.info(
            """Taak signaleringen verzenden: gestopt (${signaleringVerzendInfo.fataledatumVerzonden}
            | fatale datum waarschuwingen)
            """.trimMargin()
        )
    }

    /**
     * Send the E-Mail warnings about the due date
     *
     * @return the number of E-Mails sent
     */
    fun taakDueVerzenden(): Int {
        val verzonden = IntArray(1)
        takenService.listOpenTasksDueNow().stream()
            .filter { hasTaakSignaleringTarget(it) }
            .map { buildTaakSignalering(it.assignee, it) }
            .forEach { verzonden[0] += verzendTaakSignalering(it) }
        return verzonden[0]
    }

    fun hasTaakSignaleringTarget(task: Task): Boolean =
        signaleringenService.readInstellingenUser(SignaleringType.Type.TAAK_VERLOPEN, task.assignee)
            .isMail &&
            !signaleringenService.findSignaleringVerzonden(
                getTaakSignaleringVerzondenParameters(task.assignee, task.id)
            ).isPresent

    fun buildTaakSignalering(target: String, task: Task): Signalering {
        val signalering = signaleringenService.signaleringInstance(
            SignaleringType.Type.TAAK_VERLOPEN
        )
        signalering.setTargetUser(target)
        signalering.setSubject(task)
        signalering.setDetail(SignaleringDetail.STREEFDATUM)
        return signalering
    }

    fun verzendTaakSignalering(signalering: Signalering): Int {
        signaleringenService.sendSignalering(signalering)
        signaleringenService.createSignaleringVerzonden(signalering)
        return 1
    }

    /**
     * Make sure already sent E-Mail warnings will get send again (in cases where the due date has changed)
     */
    fun taakDueOnterechtVerzondenVerwijderen() {
        takenService.listOpenTasksDueLater().stream()
            .map { getTaakSignaleringVerzondenParameters(it.assignee, it.id) }
            .forEach { signaleringenService.deleteSignaleringVerzonden(it) }
    }

    fun getTaakSignaleringVerzondenParameters(
        target: String,
        taakId: String
    ): SignaleringVerzondenZoekParameters =
        SignaleringVerzondenZoekParameters(SignaleringTarget.USER, target)
            .types(SignaleringType.Type.TAAK_VERLOPEN)
            .subjectTaak(taakId)
            .detail(SignaleringDetail.STREEFDATUM)
}
