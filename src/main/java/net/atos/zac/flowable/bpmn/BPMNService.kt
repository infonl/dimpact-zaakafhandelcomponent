/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.bpmn

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.client.zgw.util.extractUuid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.model.generated.ZaakType
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAKTYPE_OMSCHRIJVING
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAKTYPE_UUUID
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE
import net.atos.zac.flowable.ZaakVariabelenService.VAR_ZAAK_UUID
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.repository.Deployment
import org.flowable.engine.repository.ProcessDefinition
import org.flowable.engine.runtime.ProcessInstance
import java.io.InputStream
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@NoArgConstructor
open class BPMNService @Inject constructor(
    private val repositoryService: RepositoryService,
    private val runtimeService: RuntimeService,
    private val processEngine: ProcessEngine
) {
    companion object {
        private val LOG = Logger.getLogger(BPMNService::class.java.getName())
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
                    "gif",
                    runtimeService.getActiveActivityIds(processInstance.id),
                    mutableListOf<String>(),
                    processEngineConfiguration.getActivityFontName(),
                    processEngineConfiguration.getLabelFontName(),
                    processEngineConfiguration.getAnnotationFontName(),
                    processEngineConfiguration.getClassLoader(),
                    1.0,
                    processEngineConfiguration.isDrawSequenceFlowNameWithNoLabelDI
                )
        }

    fun isProcesGestuurd(zaakUUID: UUID): Boolean = findProcessInstance(zaakUUID) != null

    fun findProcessDefinitionByprocessDefinitionKey(processDefinitionKey: String?): ProcessDefinition? =
        repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(processDefinitionKey)
            .active()
            .latestVersion()
            .singleResult()

    @Suppress("TooGenericExceptionCaught")
    fun readProcessDefinitionByprocessDefinitionKey(processDefinitionKey: String): ProcessDefinition =
        findProcessDefinitionByprocessDefinitionKey(processDefinitionKey)
            ?: throw RuntimeException("No process definition found with process definition key: '$processDefinitionKey'")

    fun startProcess(
        zaak: Zaak,
        zaaktype: ZaakType,
        zaakData: MutableMap<String, Any>? = mutableMapOf()
    ) {
        val processDefinitionKey = zaaktype.referentieproces.naam
        LOG.info("Starting zaak '${zaak.uuid}' using BPMN model '$processDefinitionKey'")
        runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey(processDefinitionKey)
            .businessKey(zaak.uuid.toString())
            .variable(VAR_ZAAK_UUID, zaak.uuid)
            .variable(VAR_ZAAK_IDENTIFICATIE, zaak.identificatie)
            .variable(VAR_ZAAKTYPE_UUUID, zaaktype.url.extractUuid())
            .variable(VAR_ZAAKTYPE_OMSCHRIJVING, zaaktype.omschrijving)
            .variables(zaakData)
            .start()
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

    fun deleteProcessDefinition(processDefinitionKey: String) =
        repositoryService.createDeploymentQuery()
            .processDefinitionKey(processDefinitionKey)
            .list()
            .forEach { repositoryService.deleteDeployment(it.id, true) }

    /**
     * Returns a process instance for the given zaak UUID or null if no process instance is found.
     */
    private fun findProcessInstance(zaakUUID: UUID): ProcessInstance? =
        runtimeService.createProcessInstanceQuery()
            .processInstanceBusinessKey(zaakUUID.toString())
            .singleResult()
}
