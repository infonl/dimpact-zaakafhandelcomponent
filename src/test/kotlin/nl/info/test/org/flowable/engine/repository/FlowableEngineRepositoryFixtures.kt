/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.test.org.flowable.engine.repository

import org.flowable.engine.repository.ProcessDefinition

@Suppress("LongParameterList")
fun createProcessDefinition(
    id: String = "dummyId",
    category: String = "dummyCategory",
    name: String = "dummyName",
    key: String = "dummyKey",
    description: String = "dummyDescription",
    version: Int = 1,
    resourceName: String = "dummyResourceName",
    deploymentId: String = "dummyDeploymentId",
    diagramResourceName: String = "dummyDiagramResourceName",
    tenantId: String = "tenantId",
    derivedFrom: String = "dummyDerivedFrom",
    derivedFromRoot: String = "dummyDerivedFromRoot",
    derivedVersion: Int = 1,
    engineVersion: String = "dummyEngineVersion",
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
    private val id: String = "dummyId",
    private val category: String = "dummyCategory",
    private val name: String = "dummyName",
    private val key: String = "dummyKey",
    private val description: String = "dummyDescription",
    private val version: Int = 1,
    private val resourceName: String = "dummyResourceName",
    private val deploymentId: String = "dummyDeploymentId",
    private val diagramResourceName: String = "dummyDiagramResourceName",
    private val tenantId: String = "tenantId",
    private val derivedFrom: String = "dummyDerivedFrom",
    private val derivedFromRoot: String = "dummyDerivedFromRoot",
    private val derivedVersion: Int = 1,
    private val engineVersion: String = "dummyEngineVersion",
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
