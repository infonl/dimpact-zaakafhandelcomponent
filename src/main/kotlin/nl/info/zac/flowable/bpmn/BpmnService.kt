/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.flowable.bpmn.exception.BpmnProcessDefinitionNotFoundException
import nl.info.zac.flowable.bpmn.model.BpmnProcessDefinitionMetadata
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.bpmn.model.ExtensionElement
import org.flowable.bpmn.model.UserTask
import org.flowable.engine.HistoryService
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.repository.Deployment
import org.flowable.engine.repository.ProcessDefinition
import org.flowable.engine.runtime.ProcessInstance
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.ZonedDateTime
import java.util.UUID
import java.util.logging.Logger
import javax.imageio.ImageIO

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class BpmnService @Inject constructor(
    private val repositoryService: RepositoryService,
    private val runtimeService: RuntimeService,
    private val historyService: HistoryService,
    private val processEngine: ProcessEngine,
    private val zaaktypeBpmnConfigurationBeheerService: ZaaktypeBpmnConfigurationBeheerService,
    private val bpmnProcessDefinitionTaskFormService: BpmnProcessDefinitionTaskFormService
) {
    companion object {
        private val LOG = Logger.getLogger(BpmnService::class.java.getName())
        private const val DIAGRAM_PADDING = 10
    }

    /**
     * Returns the BPMN diagram for the process instance of the given zaak UUID as an InputStream
     * or 'null' if no process instance for the given zaak UUID could be found.
     */
    fun getProcessDiagram(zaakUUID: UUID): InputStream? =
        findProcessInstance(zaakUUID)?.let { processInstance ->
            val processDefinition = repositoryService.getProcessDefinition(processInstance.processDefinitionId)
            val bpmnModel = repositoryService.getBpmnModel(processDefinition.id)
            val processEngineConfiguration = processEngine.processEngineConfiguration
            return processEngineConfiguration.getProcessDiagramGenerator()
                .generateDiagram(
                    bpmnModel,
                    "png",
                    runtimeService.getActiveActivityIds(processInstance.id),
                    mutableListOf<String>(),
                    processEngineConfiguration.getActivityFontName(),
                    processEngineConfiguration.getLabelFontName(),
                    processEngineConfiguration.getAnnotationFontName(),
                    processEngineConfiguration.getClassLoader(),
                    1.0,
                    processEngineConfiguration.isDrawSequenceFlowNameWithNoLabelDI
                )
                .let { trimWhitespace(it) }
        }

    private fun trimWhitespace(inputStream: InputStream): InputStream {
        val image = ImageIO.read(inputStream)
        val width = image.width
        val height = image.height

        var top = 0
        var bottom = height - 1
        var left = 0
        var right = width - 1

        outer@ for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isBackground(image, x, y)) { top = y; break@outer }
            }
        }
        outer@ for (y in height - 1 downTo top) {
            for (x in 0 until width) {
                if (!isBackground(image, x, y)) { bottom = y; break@outer }
            }
        }
        outer@ for (x in 0 until width) {
            for (y in top..bottom) {
                if (!isBackground(image, x, y)) { left = x; break@outer }
            }
        }
        outer@ for (x in width - 1 downTo left) {
            for (y in top..bottom) {
                if (!isBackground(image, x, y)) { right = x; break@outer }
            }
        }

        val paddedLeft = maxOf(0, left - DIAGRAM_PADDING)
        val paddedTop = maxOf(0, top - DIAGRAM_PADDING)
        val paddedRight = minOf(width - 1, right + DIAGRAM_PADDING)
        val paddedBottom = minOf(height - 1, bottom + DIAGRAM_PADDING)

        val cropped = image.getSubimage(
            paddedLeft,
            paddedTop,
            paddedRight - paddedLeft + 1,
            paddedBottom - paddedTop + 1
        )
        return ByteArrayOutputStream().also { ImageIO.write(cropped, "png", it) }
            .let { ByteArrayInputStream(it.toByteArray()) }
    }

    private fun isBackground(image: BufferedImage, x: Int, y: Int): Boolean {
        val color = Color(image.getRGB(x, y), true)
        return color.alpha == 0 || (color.red > 240 && color.green > 240 && color.blue > 240)
    }

    fun isZaakProcessDriven(zaakUUID: UUID): Boolean = findProcessInstance(zaakUUID) != null

    fun findProcessDefinitionByZaak(zaakUUID: UUID): ProcessDefinition? =
        findProcessInstance(zaakUUID)?.let { processInstance ->
            repositoryService.getProcessDefinition(processInstance.processDefinitionId)
        }

    fun findProcessDefinitionByProcessDefinitionKey(processDefinitionKey: String): ProcessDefinition? =
        repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(processDefinitionKey)
            .active()
            .latestVersion()
            .singleResult()

    fun readProcessDefinitionByProcessDefinitionKey(processDefinitionKey: String): ProcessDefinition =
        findProcessDefinitionByProcessDefinitionKey(processDefinitionKey)
            ?: throw BpmnProcessDefinitionNotFoundException(
                "No BPMN process definition found for process definition key: '$processDefinitionKey'"
            )

    fun startProcess(
        zaak: Zaak,
        zaaktype: ZaakType,
        processDefinitionKey: String,
        zaakData: Map<String, Any>? = null
    ) {
        val zaaktypeUUID = zaaktype.url.extractUuid()
        LOG.info("Starting zaak '${zaak.uuid}' using BPMN model '$processDefinitionKey'")
        val processInstanceBuilder = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey(processDefinitionKey)
            .businessKey(zaak.uuid.toString())
            .variable(ZaakVariabelenService.VAR_ZAAK_UUID, zaak.uuid)
            .variable(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE, zaak.identificatie)
            .variable(ZaakVariabelenService.VAR_ZAAKTYPE_UUID, zaaktypeUUID)
            .variable(ZaakVariabelenService.VAR_ZAAKTYPE_OMSCHRIJVING, zaaktype.omschrijving)
        zaakData?.let(processInstanceBuilder::variables)
        processInstanceBuilder.start()
    }

    fun listProcessDefinitions(): List<ProcessDefinition> =
        repositoryService.createProcessDefinitionQuery()
            .latestVersion()
            .orderByProcessDefinitionName().asc()
            .list()

    fun addProcessDefinition(filename: String, processDefinitionContent: String): Deployment =
        repositoryService.createDeployment()
            .addString(filename, processDefinitionContent)
            .name(filename)
            .enableDuplicateFiltering()
            .deploy()

    fun deleteProcessDefinition(processDefinitionKey: String) {
        repositoryService.createDeploymentQuery()
            .processDefinitionKey(processDefinitionKey)
            .list()
            .forEach { repositoryService.deleteDeployment(it.id, true) }
        bpmnProcessDefinitionTaskFormService.deleteAllFormsForProcessDefinition(processDefinitionKey)
    }

    /**
     * Returns the BPMN process definition for the given zaaktype UUID
     *
     * @param zaaktypeUUID UUID of the zaaktype for which the process definition is requested
     * @throws BpmnProcessDefinitionNotFoundException if no process definition is found for the given zaaktype UUID
     */
    fun findProcessDefinitionForZaaktype(zaaktypeUUID: UUID) =
        zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeUUID)
            ?: throw BpmnProcessDefinitionNotFoundException("No BPMN process definition found for zaaktype with UUID: '$zaaktypeUUID'")

    /**
     * Returns a process instance for the given zaak UUID or null if no process instance is found.
     */
    private fun findProcessInstance(zaakUUID: UUID): ProcessInstance? =
        runtimeService.createProcessInstanceQuery()
            .processInstanceBusinessKey(zaakUUID.toString())
            .singleResult()

    /**
     * Terminate a case
     * This also terminates all open tasks related to the case.
     *
     * @param zaakUUID UUID of the zaak, for which the case should be terminated.
     */
    fun terminateCase(zaakUUID: UUID) =
        findProcessInstance(zaakUUID)?.let {
            runtimeService.deleteProcessInstance(it.id, null)
        }

    /**
     * Returns a list of unique BPMN process definition keys used in process instances
     */
    fun findUniqueBpmnProcessDefinitionKeysFromProcessInstances() =
        historyService.createHistoricProcessInstanceQuery()
            .list()
            .map { it.processDefinitionKey }
            .toSet()

    /**
     * Returns a list of unique BPMN process definition keys used in zaaktype BPMN configurations
     */
    fun findUniqueBpmnProcessDefinitionKeysFromConfigurations() =
        zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeysFromZaaktypeConfigurations().toSet()

    /**
     * Returns if a process definition has current or historic process instances
     * linked to it
     *
     * @param processDefinitionKey Process definition key
     */
    fun hasProcessInstances(processDefinitionKey: String) =
        historyService.createHistoricProcessInstanceQuery()
            .processDefinitionKey(processDefinitionKey)
            .count() > 0

    /**
     * Returns if a process definition has zaaktype BPMN configurations linked to it
     *
     * @param processDefinitionKey Process definition key
     */
    fun hasLinkedZaaktypeBpmnConfiguration(processDefinitionKey: String) =
        zaaktypeBpmnConfigurationBeheerService.findUniqueBpmnProcessDefinitionKeysFromZaaktypeConfigurations()
            .contains(processDefinitionKey)

    /**
     * Returns if a process definition is in use
     *
     * @param processDefinitionKey Process definition key
     */
    fun isProcessDefinitionInUse(processDefinitionKey: String) =
        hasProcessInstances(processDefinitionKey) || hasLinkedZaaktypeBpmnConfiguration(processDefinitionKey)

    fun getProcessDefinitionMetadata(processDefinition: ProcessDefinition): BpmnProcessDefinitionMetadata {
        var documentation: String? = null
        var modificationDate: ZonedDateTime? = null
        val formKeys: MutableList<String> = mutableListOf()
        val bpmnModel = repositoryService.getBpmnModel(processDefinition.id)

        // Fill metadata based on the first process
        bpmnModel.processes.firstOrNull()?.let { first ->
            documentation = first.documentation
            modificationDate = getModificationDate(first.extensionElements)
        }
        // Find all user tasks with form keys
        bpmnModel.processes.forEach { process ->
            process.flowElements
                .filterIsInstance<UserTask>()
                .forEach { userTask ->
                    userTask.formKey?.let { formKey ->
                        formKeys.add(formKey)
                    }
                }
        }
        return BpmnProcessDefinitionMetadata(
            documentation,
            modificationDate,
            getUploadDate(processDefinition.deploymentId),
            formKeys,
        )
    }

    private fun getModificationDate(extensionElements: Map<String, List<ExtensionElement>>): ZonedDateTime? {
        return extensionElements["modificationdate"]
            ?.firstOrNull()
            ?.elementText
            ?.let { runCatching { ZonedDateTime.parse(it) }.getOrNull() }
    }

    private fun getUploadDate(deploymentId: String): ZonedDateTime? {
        return repositoryService.createDeploymentQuery()
            .deploymentId(deploymentId)
            .singleResult()
            ?.deploymentTime
            ?.let { ZonedDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) }
    }
}
