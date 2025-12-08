/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc.model

import nl.info.client.pabc.model.generated.ApplicationRoleModel
import nl.info.client.pabc.model.generated.EntityTypeModel
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.client.pabc.model.generated.GetApplicationRolesResponseModel
import java.util.UUID

fun createApplicationRolesResponse(
    applicationId: UUID = UUID.randomUUID(),
): GetApplicationRolesResponse {
    val entityType = EntityTypeModel().apply {
        id = "zaaktype_test_1"
        name = "Test zaaktype 1"
        type = "zaaktype"
    }

    val applicationRoles = listOf(
        ApplicationRoleModel().apply {
            name = "raadpleger"
            this.applicationId = applicationId
        },
        ApplicationRoleModel().apply {
            name = "behandelaar"
            this.applicationId = applicationId
        },
        ApplicationRoleModel().apply {
            name = "coordinator"
            this.applicationId = applicationId
        },
        ApplicationRoleModel().apply {
            name = "beheerder"
            this.applicationId = applicationId
        },
        ApplicationRoleModel().apply {
            name = "recordmanager"
            this.applicationId = applicationId
        }
    )

    val responseModel = GetApplicationRolesResponseModel().apply {
        this.entityType = entityType
        this.applicationRoles = applicationRoles
    }

    return GetApplicationRolesResponse().apply {
        results = listOf(responseModel)
    }
}

fun createApplicationRolesResponseModel(
    entityTypeId: String?,
    roleNames: List<String>
): GetApplicationRolesResponseModel {
    val entityType = entityTypeId?.let {
        EntityTypeModel().apply {
            id = it
            name = it
            type = "zaaktype"
        }
    }

    val applicationRoles = roleNames.map { role ->
        ApplicationRoleModel().apply {
            name = role
            this.applicationId = applicationId
        }
    }

    return GetApplicationRolesResponseModel().apply {
        this.entityType = entityType
        this.applicationRoles = applicationRoles
    }
}
