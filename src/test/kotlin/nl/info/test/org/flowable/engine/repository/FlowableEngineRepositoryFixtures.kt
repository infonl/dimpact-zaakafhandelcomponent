/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.test.org.flowable.engine.repository

import org.flowable.engine.repository.ProcessDefinition

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
