/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc.model

import nl.info.client.pabc.model.generated.ApplicationRoleModel
import nl.info.client.pabc.model.generated.EntityTypeModel
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.client.pabc.model.generated.GetApplicationRolesResponseModel

fun createApplicationRolesResponse(): GetApplicationRolesResponse {
    val entityType = EntityTypeModel().apply {
        id = "zaaktype_test_1"
        name = "Test zaaktype 1"
        type = "zaaktype"
    }

    val applicationRoles = listOf(
        ApplicationRoleModel().apply {
            name = "raadpleger"
            application = "zaakafhandelcomponent"
        },
        ApplicationRoleModel().apply {
            name = "behandelaar"
            application = "zaakafhandelcomponent"
        },
        ApplicationRoleModel().apply {
            name = "coordinator"
            application = "zaakafhandelcomponent"
        },
        ApplicationRoleModel().apply {
            name = "beheerder"
            application = "zaakafhandelcomponent"
        },
        ApplicationRoleModel().apply {
            name = "recordmanager"
            application = "zaakafhandelcomponent"
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
