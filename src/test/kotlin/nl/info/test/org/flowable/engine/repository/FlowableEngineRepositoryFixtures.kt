/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.test.org.flowable.engine.repository

import org.flowable.engine.history.HistoricProcessInstance
import org.flowable.engine.repository.ProcessDefinition
import java.util.Date

@Suppress("LongParameterList")
fun createProcessDefinition(
    id: String = "fakeId",
    category: String = "fakeCategory",
    name: String = "fakeName",
    key: String = "fakeKey",
    description: String = "fakeDescription",
    version: Int = 1,
    resourceName: String = "fakeResourceName",
    deploymentId: String = "fakeDeploymentId",
    diagramResourceName: String = "fakeDiagramResourceName",
    tenantId: String = "tenantId",
    derivedFrom: String = "fakeDerivedFrom",
    derivedFromRoot: String = "fakeDerivedFromRoot",
    derivedVersion: Int = 1,
    engineVersion: String = "fakeEngineVersion",
    hasStartFormKey: Boolean = false,
    hasGraphicalNotation: Boolean = false,
    isSuspended: Boolean = false
): ProcessDefinition = ProcessDefinitionTestImpl(
    id,
    category,
    name,
    key,
    description,
    version,
    resourceName,
    deploymentId,
    diagramResourceName,
    tenantId,
    derivedFrom,
    derivedFromRoot,
    derivedVersion,
    engineVersion,
    hasStartFormKey,
    hasGraphicalNotation,
    isSuspended
)

@Suppress("LongParameterList")
private class ProcessDefinitionTestImpl(
    private val id: String = "fakeId",
    private val category: String = "fakeCategory",
    private val name: String = "fakeName",
    private val key: String = "fakeKey",
    private val description: String = "fakeDescription",
    private val version: Int = 1,
    private val resourceName: String = "fakeResourceName",
    private val deploymentId: String = "fakeDeploymentId",
    private val diagramResourceName: String = "fakeDiagramResourceName",
    private val tenantId: String = "tenantId",
    private val derivedFrom: String = "fakeDerivedFrom",
    private val derivedFromRoot: String = "fakeDerivedFromRoot",
    private val derivedVersion: Int = 1,
    private val engineVersion: String = "fakeEngineVersion",
    private val hasStartFormKey: Boolean = false,
    private val hasGraphicalNotation: Boolean = false,
    private val isSuspended: Boolean = false
) : ProcessDefinition {
    override fun getId() = id
    override fun getCategory() = category
    override fun getName() = name
    override fun getKey() = key
    override fun getDescription() = description
    override fun getVersion() = version
    override fun getResourceName() = resourceName
    override fun getDeploymentId() = deploymentId
    override fun getDiagramResourceName() = diagramResourceName
    override fun getTenantId() = tenantId
    override fun getDerivedFrom() = derivedFrom
    override fun getDerivedFromRoot() = derivedFromRoot
    override fun getDerivedVersion() = derivedVersion
    override fun getEngineVersion() = engineVersion
    override fun hasStartFormKey() = hasStartFormKey
    override fun hasGraphicalNotation() = hasGraphicalNotation
    override fun isSuspended() = isSuspended
}

@Suppress("LongParameterList")
fun createHistoricProcessInstance(
    id: String = "fakeId",
    businessKey: String = "fakeBusinessKey",
    businessStatus: String = "fakeBusinessStatus",
    processDefinitionId: String = "fakeProcessDefinitionId",
    processDefinitionName: String = "fakeProcessDefinitionName",
    processDefinitionKey: String = "fakeProcessDefinitionKey",
    processDefinitionVersion: Int = 1,
    processDefinitionCategory: String = "fakeProcessDefinitionCategory",
    deploymentId: String = "fakeDeploymentId",
    startTime: Date = Date(),
    endTime: Date = Date(),
    endActivityId: String = "fakeEndActivityId",
    startUserId: String = "fakeStartUserId",
    startActivityId: String = "fakeStartActivityId",
    deleteReason: String = "fakeDeleteReason",
    superProcessInstanceId: String = "fakeSuperProcessInstanceId",
    tenantId: String = "fakeTenantId",
    name: String = "fakeName",
    description: String = "fakeDescription",
    callbackId: String = "fakeCallbackId",
    callbackType: String = "fakeCallbackType",
    referenceId: String = "fakeReferenceId",
    referenceType: String = "fakeReferenceType",
    propagatedStageInstanceId: String = "fakePropagatedStageInstanceId",
    processVariables: Map<String, Any> = emptyMap(),
): HistoricProcessInstance = HistoricProcessInstanceImpl(
    id,
    businessKey,
    businessStatus,
    processDefinitionId,
    processDefinitionName,
    processDefinitionKey,
    processDefinitionVersion,
    processDefinitionCategory,
    deploymentId,
    startTime,
    endTime,
    endActivityId,
    startUserId,
    startActivityId,
    deleteReason,
    superProcessInstanceId,
    tenantId,
    name,
    description,
    callbackId,
    callbackType,
    referenceId,
    referenceType,
    propagatedStageInstanceId,
    processVariables
)

@Suppress("LongParameterList")
private class HistoricProcessInstanceImpl(
    private val id: String = "fakeId",
    private val businessKey: String = "fakeBusinessKey",
    private val businessStatus: String = "fakeBusinessStatus",
    private val processDefinitionId: String = "fakeProcessDefinitionId",
    private val processDefinitionName: String = "fakeProcessDefinitionName",
    private val processDefinitionKey: String = "fakeProcessDefinitionKey",
    private val processDefinitionVersion: Int = 1,
    private val processDefinitionCategory: String = "fakeProcessDefinitionCategory",
    private val deploymentId: String = "fakeDeploymentId",
    private val startTime: Date = Date(),
    private val endTime: Date = Date(),
    private val endActivityId: String = "fakeEndActivityId",
    private val startUserId: String = "fakeStartUserId",
    private val startActivityId: String = "fakeStartActivityId",
    private val deleteReason: String = "fakeDeleteReason",
    private val superProcessInstanceId: String = "fakeSuperProcessInstanceId",
    private val tenantId: String = "fakeTenantId",
    private val name: String = "fakeName",
    private val description: String = "fakeDescription",
    private val callbackId: String = "fakeCallbackId",
    private val callbackType: String = "fakeCallbackType",
    private val referenceId: String = "fakeReferenceId",
    private val referenceType: String = "fakeReferenceType",
    private val propagatedStageInstanceId: String = "fakePropagatedStageInstanceId",
    private val processVariables: Map<String, Any> = emptyMap(),
) : HistoricProcessInstance {
    override fun getId() = id
    override fun getBusinessKey() = businessKey
    override fun getBusinessStatus() = businessStatus
    override fun getProcessDefinitionId() = processDefinitionId
    override fun getProcessDefinitionName() = processDefinitionName
    override fun getProcessDefinitionKey() = processDefinitionKey
    override fun getProcessDefinitionVersion() = processDefinitionVersion
    override fun getProcessDefinitionCategory() = processDefinitionCategory
    override fun getDeploymentId() = deploymentId
    override fun getStartTime() = startTime
    override fun getEndTime() = endTime
    override fun getDurationInMillis() = endTime.time - startTime.time
    override fun getEndActivityId() = endActivityId
    override fun getStartUserId() = startUserId
    override fun getStartActivityId() = startActivityId
    override fun getDeleteReason() = deleteReason
    override fun getSuperProcessInstanceId() = superProcessInstanceId
    override fun getTenantId() = tenantId
    override fun getName() = name
    override fun getDescription() = description
    override fun getCallbackId() = callbackId
    override fun getCallbackType() = callbackType
    override fun getReferenceId() = referenceId
    override fun getReferenceType() = referenceType
    override fun getPropagatedStageInstanceId() = propagatedStageInstanceId
    override fun getProcessVariables() = processVariables
}
