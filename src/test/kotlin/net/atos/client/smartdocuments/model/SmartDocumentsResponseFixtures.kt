/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.smartdocuments.model

import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseDocumentsStructure
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseGroupsAccess
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseHeadersStructure
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplate
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplateGroup
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplatesStructure
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseUserGroup
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseUsersStructure
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplatesResponse
import net.atos.client.smartdocuments.model.templates.User
import net.atos.client.smartdocuments.model.wizard.WizardResponse
import java.util.UUID

fun createWizardResponse(
    ticket: String = "dummyTicket",
) = WizardResponse().apply {
    this.ticket = ticket
}

fun createUserGroup(userGroupName: String) = SmartDocumentsResponseUserGroup(
    id = UUID.randomUUID().toString(),
    name = userGroupName,
    groupsAccess = SmartDocumentsResponseGroupsAccess(
        templateGroups = listOf(
            SmartDocumentsResponseTemplateGroup(
                id = UUID.randomUUID().toString(),
                name = "Dimpact",
                allDescendants = true,
                templates = emptyList(),
                templateGroups = emptyList(),
                accessible = true
            )
        ),
        headerGroups = emptyList()
    ),
    userGroups = emptyList(),
    users = listOf(
        User(
            id = UUID.randomUUID().toString(),
            name = "zaakafhandelcomponent"
        )
    ),
    accessible = true
)

fun createTemplate(templateName: String) = SmartDocumentsResponseTemplate(
    id = UUID.randomUUID().toString(),
    name = templateName,
    favorite = false
)

fun createTemplates() = listOf(
    createTemplate("Aanvullende informatie nieuw"),
    createTemplate("Aanvullende informatie oud"),
)

fun createTemplateGroup(groupName: String, templatesList: List<SmartDocumentsResponseTemplate>) =
    SmartDocumentsResponseTemplateGroup(
        templateGroups = emptyList(),
        templates = templatesList,
        accessible = true,
        id = UUID.randomUUID().toString(),
        name = groupName,
        allDescendants = true,
    )

fun createTemplateGroups() = listOf(
    createTemplateGroup(
        "Intern zaaktype voor test volledig gebruik ZAC",
        listOf(
            createTemplate("Intern zaaktype voor test volledig gebruik ZAC"),
        )
    ),
    createTemplateGroup(
        "Indienen aansprakelijkstelling door derden behandelen",
        listOf(
            createTemplate("Data Test"),
            createTemplate("OpenZaakTest"),
        )
    )
)

fun createTemplatesResponse() = SmartDocumentsTemplatesResponse(
    documentsStructure = SmartDocumentsResponseDocumentsStructure(
        templatesStructure = SmartDocumentsResponseTemplatesStructure(
            templateGroups = listOf(
                SmartDocumentsResponseTemplateGroup(
                    id = UUID.randomUUID().toString(),
                    name = "Dimpact",
                    templateGroups = createTemplateGroups(),
                    templates = createTemplates(),
                    accessible = true,
                    allDescendants = true,
                )
            ),
            accessible = true
        ),
        headersStructure = SmartDocumentsResponseHeadersStructure(
            headerGroups = emptyList(),
            accessible = true
        )
    ),
    usersStructure = SmartDocumentsResponseUsersStructure(
        groupsAccess = SmartDocumentsResponseGroupsAccess(
            templateGroups = emptyList(),
            headerGroups = emptyList()
        ),
        userGroups = listOf(createUserGroup("Atos"), createUserGroup("Dimpact")),
        accessible = true
    )
)
