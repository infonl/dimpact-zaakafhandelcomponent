/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.atos.client.zgw.drc.DRCClientService
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
import net.atos.zac.app.taken.model.RESTTaakVrijgevenGegevens
import net.atos.zac.authentication.ActiveSession
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.util.TaskUtil
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import net.atos.zac.shared.helper.OpschortenZaakHelper
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.task.TaskService
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.util.UriUtil
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Singleton
@Path("taken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class TakenRESTService @Inject constructor(
    private val taskService: TaskService,
    private val flowableTaskService: FlowableTaskService,
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
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        return restTaakConverter.convert(flowableTaskService.listTasksForZaak(zaakUUID))
    }

    @GET
    @Path("{taskId}")
    fun readTaak(@PathParam("taskId") taskId: String): RESTTaak {
        flowableTaskService.readTask(taskId).let {
            assertPolicy(policyService.readTaakRechten(it).lezen)
            deleteSignaleringen(it)
            return restTaakConverter.convert(it)
        }
    }

    @PUT
    @Path("taakdata")
    fun updateTaakdata(restTaak: RESTTaak): RESTTaak {
        flowableTaskService.readOpenTask(restTaak.id).let {
            assertPolicy(TaskUtil.isOpen(it) && policyService.readTaakRechten(it).wijzigen)
            taakVariabelenService.setTaakdata(it, restTaak.taakdata)
            taakVariabelenService.setTaakinformatie(it, restTaak.taakinformatie)
            val updatedTask = updateDescriptionAndDueDate(restTaak)
            eventingService.send(ScreenEventType.TAAK.updated(updatedTask))
            eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(restTaak.zaakUuid))
            return restTaak
        }
    }

    private fun updateDescriptionAndDueDate(restTaak: RESTTaak): Task {
        flowableTaskService.readOpenTask(restTaak.id).let {
            it.description = restTaak.toelichting
            it.dueDate = DateTimeConverterUtil.convertToDate(restTaak.fataledatum)
            return flowableTaskService.updateTask(it)
        }
    }

    @PUT
    @Path("lijst/verdelen")
    fun verdelenVanuitLijst(@Valid restTaakVerdelenGegevens: RESTTaakVerdelenGegevens) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            taskService.assignTasks(
                restTaakVerdelenGegevens = restTaakVerdelenGegevens,
                loggedInUser = loggedInUserInstance.get(),
                screenEventResourceId = restTaakVerdelenGegevens.screenEventResourceId
            )
        }
    }

    @PUT
    @Path("lijst/vrijgeven")
    fun vrijgevenVanuitLijst(@Valid restTaakVrijgevenGegevens: RESTTaakVrijgevenGegevens) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            taskService.releaseTasks(
                restTaakVrijgevenGegevens = restTaakVrijgevenGegevens,
                loggedInUser = loggedInUserInstance.get(),
                screenEventResourceId = restTaakVrijgevenGegevens.screenEventResourceId
            )
        }
    }

    @PATCH
    @Path("lijst/toekennen/mij")
    fun toekennenAanIngelogdeMedewerkerVanuitLijst(
        restTaakToekennenGegevens: RESTTaakToekennenGegevens
    ): RESTTaak {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        ingelogdeMedewerkerToekennenAanTaak(restTaakToekennenGegevens).let {
            return restTaakConverter.convert(it)
        }
    }

    @PATCH
    @Path("toekennen")
    fun toekennen(restTaakToekennenGegevens: RESTTaakToekennenGegevens) {
        val task = flowableTaskService.readOpenTask(restTaakToekennenGegevens.taakId)
        assertPolicy(TaskUtil.isOpen(task) && policyService.readTaakRechten(task).toekennen)
        taskService.assignTask(
            restTaakToekennenGegevens,
            task,
            loggedInUserInstance.get()
        )
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
        val task = flowableTaskService.readOpenTask(restTaak.id)
        val zaak = zrcClientService.readZaak(restTaak.zaakUuid)
        assertPolicy(TaskUtil.isOpen(task) && policyService.readTaakRechten(task).wijzigen)

        val loggedInUserId = loggedInUserInstance.get().id
        if (restTaak.behandelaar == null || restTaak.behandelaar!!.id != loggedInUserId) {
            flowableTaskService.assignTaskToUser(task.id, loggedInUserId, REDEN_TAAK_AFGESLOTEN)
        }
        val updatedTask = updateDescriptionAndDueDate(restTaak)
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
            ondertekenEnkelvoudigInformatieObjecten(taakdata, zaak)
        }
        taakVariabelenService.setTaakdata(updatedTask, restTaak.taakdata)
        taakVariabelenService.setTaakinformatie(updatedTask, restTaak.taakinformatie)
        flowableTaskService.completeTask(updatedTask).let {
            indexeerService.addOrUpdateZaak(restTaak.zaakUuid, false)
            eventingService.send(ScreenEventType.TAAK.updated(it))
            eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(restTaak.zaakUuid))
            return restTaakConverter.convert(it)
        }
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
        assertPolicy(policyService.readTaakRechten(flowableTaskService.readTask(taskId)).lezen)
        flowableTaskService.listHistorieForTask(taskId).let {
            return taakHistorieConverter.convert(it)
        }
    }

    private fun ingelogdeMedewerkerToekennenAanTaak(restTaakToekennenGegevens: RESTTaakToekennenGegevens): Task {
        val task = flowableTaskService.readOpenTask(restTaakToekennenGegevens.taakId)
        assertPolicy(TaskUtil.isOpen(task) && policyService.readTaakRechten(task).toekennen)
        taskService.assignTaskToUser(
            taskId = task.id,
            assignee = loggedInUserInstance.get().id,
            loggedInUser = loggedInUserInstance.get(),
            explanation = restTaakToekennenGegevens.reden
        ).let {
            taskService.sendScreenEventsOnTaskChange(it, restTaakToekennenGegevens.zaakUuid)
            indexeerService.indexeerDirect(restTaakToekennenGegevens.taakId, ZoekObjectType.TAAK, true)
            return it
        }
    }

    @Suppress("NestedBlockDepth")
    private fun createDocuments(restTaak: RESTTaak, zaak: Zaak) {
        val httpSession = httpSession.get()
        restTaak.taakdata?.let { taakdata ->
            for (key in taakdata.keys) {
                val fileKey = "_FILE__${restTaak.id}__$key"
                httpSession.getAttribute(fileKey)?.let { uploadedFile ->
                    taakdata[key]?.let { jsonDocumentData ->
                        try {
                            val restTaakDocumentData = ObjectMapper().readValue(
                                jsonDocumentData,
                                RESTTaakDocumentData::class.java
                            )
                            val document = restInformatieobjectConverter.convert(
                                restTaakDocumentData,
                                uploadedFile as RESTFileUpload
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
                        } catch (jsonProcessingException: JsonProcessingException) {
                            throw IllegalArgumentException(
                                "Invalid JSON document data received: '$jsonDocumentData'",
                                jsonProcessingException
                            )
                        } finally {
                            httpSession.removeAttribute(fileKey)
                        }
                        // document can be uploaded but removed afterwards
                    } ?: httpSession.removeAttribute(fileKey)
                }
            }
        }
    }

    private fun ondertekenEnkelvoudigInformatieObjecten(taakdata: Map<String, String>, zaak: Zaak) {
        val signatures = taakVariabelenService.readOndertekeningen(taakdata)
        signatures.ifPresent { signature ->
            signature.split(
                TaakVariabelenService.TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER.toRegex()
            ).dropLastWhile { it.isEmpty() }.toTypedArray()
                .filter { it.isNotEmpty() }
                .map { UUID.fromString(it) }
                .map { drcClientService.readEnkelvoudigInformatieobject(it) }
                .forEach { enkelvoudigInformatieobject ->
                    assertPolicy(
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
        loggedInUserInstance.get().let { loggedInUser ->
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
            .forEach { documentUUID ->
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
