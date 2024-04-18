/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.Ondertekening
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.RESTFileUpload
import net.atos.zac.app.taken.converter.RESTTaakConverter
import net.atos.zac.app.taken.converter.RESTTaakHistorieConverter
import net.atos.zac.app.taken.model.RESTTaak
import net.atos.zac.app.taken.model.RESTTaakDocumentData
import net.atos.zac.app.taken.model.RESTTaakHistorieRegel
import net.atos.zac.app.taken.model.RESTTaakToekennenGegevens
import net.atos.zac.app.taken.model.RESTTaakVerdelenGegevens
import net.atos.zac.app.taken.model.RESTTaakVerdelenTaak
import net.atos.zac.app.taken.model.TaakStatus
import net.atos.zac.authentication.ActiveSession
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.TakenService
import net.atos.zac.flowable.util.TaskUtil
import net.atos.zac.policy.PolicyService
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.util.UriUtil
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.function.Consumer

@Singleton
@Path("taken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class TakenRESTService @Inject constructor(
    private val takenService: TakenService,
    private val taakVariabelenService: TaakVariabelenService,
    private val indexeerService: IndexeerService,
    private val restTaakConverter: RESTTaakConverter,
    private val eventingService: EventingService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    @ActiveSession
    private val httpSession: Instance<HttpSession>,
    private val restInformatieobjectConverter: RESTInformatieobjectConverter,
    private val zgwApiService: ZGWApiService,
    private val zrcClientService: ZRCClientService,
    private val drcClientService: DRCClientService,
    private val signaleringenService: SignaleringenService,
    private val taakHistorieConverter: RESTTaakHistorieConverter,
    private val policyService: PolicyService,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
    private val opschortenZaakHelper: OpschortenZaakHelper
) {
    companion object {
        private const val REDEN_ZAAK_HERVATTEN = "Aanvullende informatie geleverd"
        private const val REDEN_TAAK_AFGESLOTEN = "Afgesloten"
    }

    @GET
    @Path("zaak/{zaakUUID}")
    fun listTakenVoorZaak(@PathParam("zaakUUID") zaakUUID: UUID): List<RESTTaak> {
        PolicyService.assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        return restTaakConverter.convert(takenService.listTasksForZaak(zaakUUID))
    }

    @GET
    @Path("{taskId}")
    fun readTaak(@PathParam("taskId") taskId: String): RESTTaak {
        val task = takenService.readTask(taskId)
        PolicyService.assertPolicy(policyService.readTaakRechten(task).lezen)
        deleteSignaleringen(task)
        return restTaakConverter.convert(task)
    }

    @PUT
    @Path("taakdata")
    fun updateTaakdata(restTaak: RESTTaak): RESTTaak {
        var task = takenService.readOpenTask(restTaak.id)
        PolicyService.assertPolicy(TaskUtil.isOpen(task) && policyService.readTaakRechten(task).wijzigen)
        taakVariabelenService.setTaakdata(task, restTaak.taakdata)
        taakVariabelenService.setTaakinformatie(task, restTaak.taakinformatie)
        task = updateDescriptionAndDueDate(restTaak)
        eventingService.send(ScreenEventType.TAAK.updated(task))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(restTaak.zaakUuid))
        return restTaak
    }

    private fun updateDescriptionAndDueDate(restTaak: RESTTaak): Task {
        var task = takenService.readOpenTask(restTaak.id)
        task.description = restTaak.toelichting
        task.dueDate = DateTimeConverterUtil.convertToDate(restTaak.fataledatum)
        task = takenService.updateTask(task)
        return task
    }

    @PUT
    @Path("lijst/verdelen")
    fun verdelenVanuitLijst(restTaakVerdelenGegevens: RESTTaakVerdelenGegevens) {
        PolicyService.assertPolicy(
            policyService.readWerklijstRechten().zakenTaken && policyService.readWerklijstRechten().zakenTakenVerdelen
        )
        val taakIds: MutableList<String?> = ArrayList()
        restTaakVerdelenGegevens.taken.forEach { taak ->
            var task = takenService.readOpenTask(taak.taakId)
            if (restTaakVerdelenGegevens.behandelaarGebruikersnaam != null) {
                task = assignTaak(
                    task.id,
                    restTaakVerdelenGegevens.behandelaarGebruikersnaam,
                    restTaakVerdelenGegevens.reden
                )
            }
            task = takenService.assignTaskToGroup(
                task,
                restTaakVerdelenGegevens.groepId,
                restTaakVerdelenGegevens.reden
            )
            taakBehandelaarGewijzigd(task, taak.zaakUuid)
            taakIds.add(taak.taakId)
        }
        indexeerService.indexeerDirect(taakIds, ZoekObjectType.TAAK)
    }

    @PUT
    @Path("lijst/vrijgeven")
    fun vrijgevenVanuitLijst(restTaakVerdelenGegevens: RESTTaakVerdelenGegevens) {
        PolicyService.assertPolicy(
            policyService.readWerklijstRechten().zakenTaken && policyService.readWerklijstRechten()
                .zakenTakenVerdelen
        )
        val taakIds: MutableList<String> = ArrayList()
        restTaakVerdelenGegevens.taken.forEach(
            Consumer { taak: RESTTaakVerdelenTaak? ->
                val task = assignTaak(taak!!.taakId, null, restTaakVerdelenGegevens.reden)
                taakBehandelaarGewijzigd(task, taak.zaakUuid)
                taakIds.add(task.id)
            }
        )
        indexeerService.indexeerDirect(taakIds, ZoekObjectType.TAAK)
    }

    @PATCH
    @Path("lijst/toekennen/mij")
    fun toekennenAanIngelogdeMedewerkerVanuitLijst(
        restTaakToekennenGegevens: RESTTaakToekennenGegevens
    ): RESTTaak {
        PolicyService.assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        val task = ingelogdeMedewerkerToekennenAanTaak(restTaakToekennenGegevens)
        return restTaakConverter.convert(task)
    }

    @PATCH
    @Path("toekennen")
    fun toekennen(restTaakToekennenGegevens: RESTTaakToekennenGegevens) {
        var task = takenService.readOpenTask(restTaakToekennenGegevens.taakId)
        PolicyService.assertPolicy(
            TaskUtil.getTaakStatus(task) != TaakStatus.AFGEROND && policyService.readTaakRechten(task).toekennen
        )
        val behandelaar = task.assignee
        val groep = restTaakConverter.extractGroupId(task.identityLinks)
        var changed = false
        if (!StringUtils.equals(behandelaar, restTaakToekennenGegevens.behandelaarId)) {
            task = assignTaak(task.id, restTaakToekennenGegevens.behandelaarId, restTaakToekennenGegevens.reden)
            changed = true
        }

        if (!StringUtils.equals(groep, restTaakToekennenGegevens.groepId)) {
            task = takenService.assignTaskToGroup(
                task, restTaakToekennenGegevens.groepId,
                restTaakToekennenGegevens.reden
            )
            changed = true
        }
        if (changed) {
            taakBehandelaarGewijzigd(task, restTaakToekennenGegevens.zaakUuid)
            indexeerService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK)
        }
    }

    @PATCH
    @Path("toekennen/mij")
    fun toekennenAanIngelogdeMedewerker(restTaakToekennenGegevens: RESTTaakToekennenGegevens) =
        ingelogdeMedewerkerToekennenAanTaak(restTaakToekennenGegevens).let {
            restTaakConverter.convert(it)
        }

    @PATCH
    @Path("complete")
    fun completeTaak(restTaak: RESTTaak): RESTTaak {
        var task = takenService.readOpenTask(restTaak.id)
        val zaak = zrcClientService.readZaak(restTaak.zaakUuid)
        PolicyService.assertPolicy(TaskUtil.isOpen(task) && policyService.readTaakRechten(task).wijzigen)

        val loggedInUserId = loggedInUserInstance.get().id
        if (restTaak.behandelaar == null || restTaak.behandelaar!!.id != loggedInUserId) {
            takenService.assignTaskToUser(task.id, loggedInUserId, REDEN_TAAK_AFGESLOTEN)
        }
        task = updateDescriptionAndDueDate(restTaak)

        createDocuments(restTaak, zaak)
        if (taakVariabelenService.isZaakHervatten(restTaak.taakdata)) {
            opschortenZaakHelper.hervattenZaak(zaak, REDEN_ZAAK_HERVATTEN)
        }
        restTaak.taakdata?.let { taakdata ->
            taakdata[TaakVariabelenService.TAAK_DATA_DOCUMENTEN_VERZENDEN_POST]?.let {
                updateVerzenddatumEnkelvoudigInformatieObjecten(
                    documenten = it,
                    // implicitly assume that the following keys are present in taakdata
                    verzenddatumString = taakdata[TaakVariabelenService.TAAK_DATA_VERZENDDATUM]!!,
                    toelichting = taakdata[TaakVariabelenService.TAAK_DATA_TOELICHTING]!!
                )
            }
        }
        restTaak.taakdata?.let {
            ondertekenEnkelvoudigInformatieObjecten(it, zaak)
        }
        taakVariabelenService.setTaakdata(task, restTaak.taakdata)
        taakVariabelenService.setTaakinformatie(task, restTaak.taakinformatie)
        val completedTask = takenService.completeTask(task)
        indexeerService.addOrUpdateZaak(restTaak.zaakUuid, false)
        eventingService.send(ScreenEventType.TAAK.updated(completedTask))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(restTaak.zaakUuid))
        return restTaakConverter.convert(completedTask)
    }

    @POST
    @Path("upload/{uuid}/{field}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadFile(
        @PathParam("field") field: String,
        @PathParam("uuid") uuid: UUID,
        @MultipartForm data: RESTFileUpload
    ): Response {
        httpSession.get().setAttribute("_FILE__${uuid}__$field", data)
        return Response.ok("\"Success\"").build()
    }

    @GET
    @Path("{taskId}/historie")
    fun listHistorie(@PathParam("taskId") taskId: String): List<RESTTaakHistorieRegel> {
        PolicyService.assertPolicy(policyService.readTaakRechten(takenService.readTask(taskId)).lezen)
        val historicTaskLogEntries = takenService.listHistorieForTask(taskId)
        return taakHistorieConverter.convert(historicTaskLogEntries)
    }

    private fun ingelogdeMedewerkerToekennenAanTaak(restTaakToekennenGegevens: RESTTaakToekennenGegevens): Task {
        var task = takenService.readOpenTask(restTaakToekennenGegevens.taakId)
        PolicyService.assertPolicy(
            TaskUtil.getTaakStatus(task) != TaakStatus.AFGEROND && policyService.readTaakRechten(task).toekennen
        )
        task = assignTaak(task.id, loggedInUserInstance.get().id, restTaakToekennenGegevens.reden)
        taakBehandelaarGewijzigd(task, restTaakToekennenGegevens.zaakUuid)
        indexeerService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK)
        return task
    }

    private fun assignTaak(taskId: String, assignee: String?, reden: String?): Task {
        val task = takenService.assignTaskToUser(taskId, assignee, reden)
        eventingService.send(
            SignaleringEventUtil.event(SignaleringType.Type.TAAK_OP_NAAM, task, loggedInUserInstance.get())
        )
        return task
    }

    private fun createDocuments(restTaak: RESTTaak, zaak: Zaak) {
        val httpSession = httpSession.get()
        restTaak.taakdata?.let { taakdata ->
            for (key in taakdata.keys) {
                val fileKey = "_FILE__${restTaak.id}__$key"
                val uploadedFile = httpSession.getAttribute(fileKey) as RESTFileUpload
                val jsonDocumentData = taakdata.get(key)
                if (StringUtils.isEmpty(jsonDocumentData)) { // document uploaded but removed afterwards
                    httpSession.removeAttribute(fileKey)
                    break
                }
                val restTaakDocumentData: RESTTaakDocumentData
                try {
                    restTaakDocumentData = ObjectMapper().readValue(jsonDocumentData, RESTTaakDocumentData::class.java)
                } catch (jsonProcessingException: JsonProcessingException) {
                    throw IllegalArgumentException(
                        "Invalid JSON document data received: '$jsonDocumentData'",
                        jsonProcessingException
                    )
                }
                val document = restInformatieobjectConverter.convert(
                    restTaakDocumentData,
                    uploadedFile
                )
                val zaakInformatieobject = zgwApiService.createZaakInformatieobjectForZaak(
                    zaak,
                    document,
                    document.titel,
                    ConfiguratieService.OMSCHRIJVING_TAAK_DOCUMENT,
                    ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
                )
                taakdata.replace(
                    key,
                    UriUtil.uuidFromURI(zaakInformatieobject.informatieobject).toString()
                )
                httpSession.removeAttribute(fileKey)
            }
        }
    }

    private fun ondertekenEnkelvoudigInformatieObjecten(taakdata: Map<String, String>, zaak: Zaak) {
        val signatures = taakVariabelenService.readOndertekeningen(taakdata)
        signatures.ifPresent { signature ->
            signature.split(
                TaakVariabelenService.TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER.toRegex()
            ).dropLastWhile { it.isEmpty() }.toTypedArray()
                .filter { cs: String? -> StringUtils.isNotEmpty(cs) }
                .map { name: String? -> UUID.fromString(name) }
                .map { uuid: UUID? -> drcClientService.readEnkelvoudigInformatieobject(uuid) }
                .forEach { enkelvoudigInformatieobject: EnkelvoudigInformatieObject ->
                    PolicyService.assertPolicy(
                        (
                            // this extra check is because the API can return an empty ondertekening soort
                            enkelvoudigInformatieobject.ondertekening == null ||
                                // when no signature is present (even if this is not
                                // permitted according to the original OpenAPI spec)
                                enkelvoudigInformatieobject.ondertekening.soort == Ondertekening.SoortEnum.EMPTY
                            ) && policyService.readDocumentRechten(enkelvoudigInformatieobject, zaak).ondertekenen
                    )
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                        URIUtil.parseUUIDFromResourceURI(enkelvoudigInformatieobject.url)
                    )
                }
        }
    }

    private fun deleteSignaleringen(taskInfo: TaskInfo) {
        val loggedInUser = loggedInUserInstance.get()
        signaleringenService.deleteSignaleringen(
            SignaleringZoekParameters(loggedInUser)
                .types(SignaleringType.Type.TAAK_OP_NAAM).subject(taskInfo)
        )
        signaleringenService.deleteSignaleringen(
            SignaleringZoekParameters(loggedInUser)
                .types(SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD)
                .subjectZaak(taakVariabelenService.readZaakUUID(taskInfo))
        )
    }

    private fun taakBehandelaarGewijzigd(task: Task, zaakUuid: UUID) {
        eventingService.send(ScreenEventType.TAAK.updated(task))
        eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(zaakUuid))
    }

    private fun updateVerzenddatumEnkelvoudigInformatieObjecten(
        documenten: String,
        verzenddatumString: String,
        toelichting: String
    ) {
        val verzenddatum = ZonedDateTime.parse(verzenddatumString).toLocalDate()
        documenten.split(
            TaakVariabelenService.TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER.toRegex()
        ).dropLastWhile { it.isEmpty() }.toTypedArray()
            .forEach { documentUUID: String? ->
                setVerzenddatumEnkelvoudigInformatieObject(
                    UUID.fromString(documentUUID),
                    verzenddatum,
                    toelichting
                )
            }
    }

    private fun setVerzenddatumEnkelvoudigInformatieObject(
        uuid: UUID,
        verzenddatum: LocalDate,
        toelichting: String?
    ) {
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(uuid)
        enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
            URIUtil.parseUUIDFromResourceURI(informatieobject.url),
            verzenddatum,
            toelichting
        )
    }
}
