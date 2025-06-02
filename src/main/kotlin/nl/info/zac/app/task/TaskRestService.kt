/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.task

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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.atos.client.zgw.drc.DrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.RESTFileUpload
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.flowable.task.TaakVariabelenService.TAAK_DATA_DOCUMENTEN_VERZENDEN_POST
import net.atos.zac.flowable.task.TaakVariabelenService.TAAK_DATA_TOELICHTING
import net.atos.zac.flowable.task.TaakVariabelenService.TAAK_DATA_VERZENDDATUM
import net.atos.zac.flowable.task.TaakVariabelenService.isZaakHervatten
import net.atos.zac.flowable.task.TaakVariabelenService.readSignatures
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakUUID
import net.atos.zac.flowable.util.TaskUtil
import net.atos.zac.formulieren.FormulierRuntimeService
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.app.task.converter.RestTaskConverter
import nl.info.zac.app.task.converter.RestTaskHistoryConverter
import nl.info.zac.app.task.model.RestTask
import nl.info.zac.app.task.model.RestTaskAssignData
import nl.info.zac.app.task.model.RestTaskDistributeData
import nl.info.zac.app.task.model.RestTaskDocumentData
import nl.info.zac.app.task.model.RestTaskHistoryLine
import nl.info.zac.app.task.model.RestTaskReleaseData
import nl.info.zac.authentication.ActiveSession
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.task.TaskService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

private const val REDEN_ZAAK_HERVATTEN = "Aanvullende informatie geleverd"
private const val REDEN_TAAK_AFGESLOTEN = "Afgesloten"

@Singleton
@Path("taken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions", "LongParameterList")
class TaskRestService @Inject constructor(
    private val taskService: TaskService,
    private val flowableTaskService: FlowableTaskService,
    private val taakVariabelenService: TaakVariabelenService,
    private val indexingService: IndexingService,
    private val restTaskConverter: RestTaskConverter,
    private val eventingService: EventingService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    @ActiveSession
    private val httpSession: Instance<HttpSession>,
    private val restInformatieobjectConverter: RestInformatieobjectConverter,
    private val zgwApiService: ZGWApiService,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService,
    private val signaleringService: SignaleringService,
    private val taakHistorieConverter: RestTaskHistoryConverter,
    private val policyService: PolicyService,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
    private val opschortenZaakHelper: SuspensionZaakHelper,
    private val formulierRuntimeService: FormulierRuntimeService,
    private val zaakVariabelenService: ZaakVariabelenService,

    /**
     * Declare a Kotlin coroutine dispatcher here so that it can be overridden in unit tests with a test dispatcher
     * while in normal operation it will be injected using [nl.info.zac.util.CoroutineDispatcherProducer].
     */
    private val dispatcher: CoroutineDispatcher
) {
    @GET
    @Path("zaak/{zaakUUID}")
    fun listTasksForZaak(@PathParam("zaakUUID") zaakUUID: UUID): List<RestTask> {
        assertPolicy(policyService.readZaakRechten(zrcClientService.readZaak(zaakUUID)).lezen)
        return restTaskConverter.convert(taskService.listTasksForZaak(zaakUUID))
    }

    @GET
    @Path("{taskId}")
    fun readTask(@PathParam("taskId") taskId: String): RestTask {
        flowableTaskService.readTask(taskId).let { task ->
            assertPolicy(policyService.readTaakRechten(task).lezen)
            deleteSignaleringen(task)
            val restTask = restTaskConverter.convert(task)
            if (TaskUtil.isOpen(task)) {
                restTask.formulierDefinitie?.let {
                    formulierRuntimeService.renderFormulierDefinitie(restTask)
                }
                restTask.formioFormulier?.let {
                    restTask.formioFormulier = formulierRuntimeService.renderFormioFormulier(restTask)
                    addZaakdata(restTask)
                }
            }
            return restTask
        }
    }

    @PUT
    @Path("taakdata")
    fun updateTaskData(restTask: RestTask): RestTask {
        flowableTaskService.readOpenTask(restTask.id).let {
            assertPolicy(TaskUtil.isOpen(it) && policyService.readTaakRechten(it).wijzigen)
            taakVariabelenService.setTaskData(it, restTask.taakdata)
            taakVariabelenService.setTaskinformation(it, restTask.taakinformatie)
            val updatedTask = updateDescriptionAndDueDate(restTask)
            eventingService.send(ScreenEventType.TAAK.updated(updatedTask))
            eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(restTask.zaakUuid))
            return restTask
        }
    }

    private fun updateDescriptionAndDueDate(restTask: RestTask): Task {
        flowableTaskService.readOpenTask(restTask.id).let {
            it.description = restTask.toelichting
            it.dueDate = DateTimeConverterUtil.convertToDate(restTask.fataledatum)
            return flowableTaskService.updateTask(it)
        }
    }

    @PUT
    @Path("lijst/verdelen")
    fun assignTasksFromList(@Valid restTaskDistributeData: RestTaskDistributeData) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        CoroutineScope(dispatcher).launch {
            taskService.assignTasks(
                restTaskDistributeData = restTaskDistributeData,
                loggedInUser = loggedInUserInstance.get(),
                screenEventResourceId = restTaskDistributeData.screenEventResourceId
            )
        }
    }

    @PUT
    @Path("lijst/vrijgeven")
    fun releaseTaskFromList(@Valid restTaskReleaseData: RestTaskReleaseData) {
        assertPolicy(policyService.readWerklijstRechten().zakenTakenVerdelen)
        // this can be a long-running operation so run it asynchronously
        CoroutineScope(dispatcher).launch {
            taskService.releaseTasks(
                restTaskReleaseData = restTaskReleaseData,
                loggedInUser = loggedInUserInstance.get(),
                screenEventResourceId = restTaskReleaseData.screenEventResourceId
            )
        }
    }

    @PATCH
    @Path("lijst/toekennen/mij")
    fun assignTaskToLoggedInUserFromList(
        restTaskAssignData: RestTaskAssignData
    ): RestTask {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        assignLoggedInUserToTask(restTaskAssignData).let {
            return restTaskConverter.convert(it)
        }
    }

    @PATCH
    @Path("toekennen")
    fun assignTask(restTaskAssignData: RestTaskAssignData) {
        val task = flowableTaskService.readOpenTask(restTaskAssignData.taakId)
        assertPolicy(TaskUtil.isOpen(task) && policyService.readTaakRechten(task).toekennen)
        taskService.assignOrReleaseTask(
            restTaskAssignData,
            task,
            loggedInUserInstance.get()
        )
    }

    @PATCH
    @Path("toekennen/mij")
    fun assignTaskToLoggedInUser(restTaskAssignData: RestTaskAssignData) =
        assignLoggedInUserToTask(restTaskAssignData).let {
            restTaskConverter.convert(it)
        }

    @PATCH
    @Path("complete")
    fun completeTask(restTask: RestTask): RestTask {
        val task = flowableTaskService.readOpenTask(restTask.id)
        assertPolicy(TaskUtil.isOpen(task) && policyService.readTaakRechten(task).wijzigen)

        val loggedInUserId = loggedInUserInstance.get().id
        if (restTask.behandelaar == null || restTask.behandelaar!!.id != loggedInUserId) {
            flowableTaskService.assignTaskToUser(task.id, loggedInUserId, REDEN_TAAK_AFGESLOTEN)
        }

        val zaak = zrcClientService.readZaak(restTask.zaakUuid)
        val updatedTask = if (restTask.formulierDefinitie != null || restTask.formioFormulier != null) {
            formulierRuntimeService.submit(restTask, task, zaak)
        } else {
            processHardCodedFormTask(restTask, zaak)
        }

        return flowableTaskService.completeTask(updatedTask).also {
            indexingService.addOrUpdateZaak(restTask.zaakUuid, false)
            eventingService.send(ScreenEventType.TAAK.updated(it))
            eventingService.send(ScreenEventType.ZAAK_TAKEN.updated(restTask.zaakUuid))
        }.let(restTaskConverter::convert)
    }

    private fun addZaakdata(restTask: RestTask) = restTask.taakdata?.apply {
        putAll(readFilteredZaakdata(restTask))
    } ?: {
        restTask.taakdata = readFilteredZaakdata(restTask).toMutableMap()
    }

    private fun readFilteredZaakdata(restTask: RestTask) =
        zaakVariabelenService.readProcessZaakdata(restTask.zaakUuid)
            .filterNot {
                it.key.equals(ZaakVariabelenService.VAR_ZAAK_UUID) ||
                    it.key.equals(
                        ZaakVariabelenService.VAR_ZAAKTYPE_UUUID
                    )
            }

    private fun processHardCodedFormTask(restTask: RestTask, zaak: Zaak): Task {
        val updatedTask = updateDescriptionAndDueDate(restTask)
        createDocuments(restTask, zaak)
        if (isZaakHervatten(restTask.taakdata)) {
            opschortenZaakHelper.resumeZaak(zaak, REDEN_ZAAK_HERVATTEN)
        }
        restTask.taakdata?.let { taakdata ->
            taakdata[TAAK_DATA_DOCUMENTEN_VERZENDEN_POST]?.let {
                updateVerzenddatumEnkelvoudigInformatieObjecten(
                    documenten = it.toString(),
                    // implicitly assume that the verzenddatum key is present in taakdata
                    verzenddatumString = taakdata[TAAK_DATA_VERZENDDATUM]!!.toString(),
                    toelichting = taakdata[TAAK_DATA_TOELICHTING]?.toString()
                )
            }
            ondertekenEnkelvoudigInformatieObjecten(taakdata, zaak)
        }
        taakVariabelenService.setTaskData(updatedTask, restTask.taakdata)
        taakVariabelenService.setTaskinformation(updatedTask, restTask.taakinformatie)
        return updatedTask
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
    fun listHistory(@PathParam("taskId") taskId: String): List<RestTaskHistoryLine> {
        assertPolicy(policyService.readTaakRechten(flowableTaskService.readTask(taskId)).lezen)
        flowableTaskService.listHistorieForTask(taskId).let {
            return taakHistorieConverter.convert(it)
        }
    }

    private fun assignLoggedInUserToTask(restTaskAssignData: RestTaskAssignData): Task {
        val task = flowableTaskService.readOpenTask(restTaskAssignData.taakId)
        assertPolicy(TaskUtil.isOpen(task) && policyService.readTaakRechten(task).toekennen)
        taskService.assignTaskToUser(
            taskId = task.id,
            assignee = loggedInUserInstance.get().id,
            loggedInUser = loggedInUserInstance.get(),
            explanation = restTaskAssignData.reden
        ).let {
            taskService.sendScreenEventsOnTaskChange(it, restTaskAssignData.zaakUuid)
            indexingService.indexeerDirect(restTaskAssignData.taakId, ZoekObjectType.TAAK, true)
            return it
        }
    }

    @Suppress("NestedBlockDepth")
    private fun createDocuments(restTask: RestTask, zaak: Zaak) {
        val httpSession = httpSession.get()
        restTask.taakdata?.let { taakdata ->
            for (key in taakdata.keys) {
                val fileKey = "_FILE__${restTask.id}__$key"
                httpSession.getAttribute(fileKey)?.let { uploadedFile ->
                    taakdata[key]?.let { jsonDocumentData ->
                        try {
                            val restTaskDocumentData = ObjectMapper().readValue(
                                jsonDocumentData as String,
                                RestTaskDocumentData::class.java
                            )
                            val enkelvoudigInformatieObjectCreateLockRequest = restInformatieobjectConverter.convert(
                                restTaskDocumentData,
                                uploadedFile as RESTFileUpload
                            )
                            val zaakInformatieobject = zgwApiService.createZaakInformatieobjectForZaak(
                                zaak,
                                enkelvoudigInformatieObjectCreateLockRequest,
                                enkelvoudigInformatieObjectCreateLockRequest.titel,
                                ConfiguratieService.OMSCHRIJVING_TAAK_DOCUMENT,
                                ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
                            )
                            taakdata.replace(
                                key,
                                zaakInformatieobject.informatieobject.extractUuid().toString()
                            )
                        } catch (jsonProcessingException: JsonProcessingException) {
                            throw IllegalArgumentException(
                                "Invalid JSON document data received: '$jsonDocumentData'",
                                jsonProcessingException
                            )
                        } finally {
                            httpSession.removeAttribute(fileKey)
                        }
                        // document can be uploaded but removed afterward
                    } ?: httpSession.removeAttribute(fileKey)
                }
            }
        }
    }

    private fun ondertekenEnkelvoudigInformatieObjecten(taakdata: Map<String, Any>, zaak: Zaak) {
        val signatures = readSignatures(taakdata)
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
                            enkelvoudigInformatieobject.ondertekening == null
                            ) && policyService.readDocumentRechten(enkelvoudigInformatieobject, zaak).ondertekenen
                    )
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                        enkelvoudigInformatieobject.url.extractUuid()
                    )
                }
        }
    }

    private fun deleteSignaleringen(taskInfo: TaskInfo) {
        loggedInUserInstance.get().let { loggedInUser ->
            signaleringService.deleteSignaleringen(
                SignaleringZoekParameters(loggedInUser)
                    .types(SignaleringType.Type.TAAK_OP_NAAM)
                    .subject(taskInfo)
            )
            signaleringService.deleteSignaleringen(
                SignaleringZoekParameters(loggedInUser)
                    .types(SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD)
                    .subjectZaak(readZaakUUID(taskInfo))
            )
        }
    }

    private fun updateVerzenddatumEnkelvoudigInformatieObjecten(
        documenten: String,
        verzenddatumString: String,
        toelichting: String?
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
            informatieobject.url.extractUuid(),
            verzenddatum,
            toelichting
        )
    }
}
