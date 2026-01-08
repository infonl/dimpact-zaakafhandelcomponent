/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc.model

import nl.info.client.pabc.model.generated.ApplicationRoleModel
import nl.info.client.pabc.model.generated.EntityTypeModel
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.client.pabc.model.generated.GetApplicationRolesResponseModel
import nl.info.client.pabc.model.generated.GroupRepresentation

fun createApplicationRoleModel(
    name: String = "fakeRoleName",
    application: String = "fakeApplicationName"
) = ApplicationRoleModel().apply {
    this.name = name
    this.application = application
}

fun createApplicationRolesResponse(
    id: String = "fakeEntityTypeId",
    name: String = "fakeEntityTypeName",
    type: String = "fakeEntityTypeId",
    applicationName: String = "fakeApplicationName"
): GetApplicationRolesResponse {
    val entityType = EntityTypeModel().apply {
        this.id = id
        this.name = name
        this.type = type
    }
    val applicationRoles = listOf(
        createApplicationRoleModel(
            name = "fakeApplicationRole1",
            application = applicationName
        ),
        createApplicationRoleModel(
            name = "fakeApplicationRole2",
            application = applicationName
        )
    )
    val responseModel = createGetApplicationRolesResponseModel(
        entityType = entityType,
        applicationRoles = applicationRoles
    )
    return GetApplicationRolesResponse().apply {
        results = listOf(responseModel)
    }
}

fun createApplicationRolesResponseModel(
    entityTypeId: String?,
    entityType: String = "fakeEntityTypeId",
    roleNames: List<String>,
    applicationName: String = "fakeApplicationName"
): GetApplicationRolesResponseModel {
    val entityType = entityTypeId?.let {
        createEntityTypeModel(
            id = it,
            name = it,
            type = entityType
        )
    }
    val applicationRoles = roleNames.map { role ->
        createApplicationRoleModel(
            name = role,
            application = applicationName
        )
    }
    return GetApplicationRolesResponseModel().apply {
        this.entityType = entityType
        this.applicationRoles = applicationRoles
    }
}

fun createEntityTypeModel(
    id: String = "fakeEntityTypeId",
    name: String = "fakeEntityTypeName",
    type: String = "fakeEntityTypeId"
) = EntityTypeModel().apply {
    this.id = id
    this.name = name
    this.type = type
}

fun createGetApplicationRolesResponseModel(
    entityType: EntityTypeModel,
    applicationRoles: List<ApplicationRoleModel>
) = GetApplicationRolesResponseModel().apply {
    this.entityType = entityType
    this.applicationRoles = applicationRoles
}

fun createPabcGroupRepresentation(
    name: String = "fakeGroupName",
    description: String? = null
) = GroupRepresentation().apply {
    this.name = name
    this.description = description
}
