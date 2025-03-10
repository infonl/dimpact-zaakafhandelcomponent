/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.smartdocuments.model

import nl.info.client.smartdocuments.model.document.AttendedResponse
import nl.info.client.smartdocuments.model.document.Document
import nl.info.client.smartdocuments.model.document.File
import nl.info.client.smartdocuments.model.document.Selection
import nl.info.client.smartdocuments.model.document.SmartDocument
import nl.info.client.smartdocuments.model.document.Variables
import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseDocumentsStructure
import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseGroupsAccess
import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseHeadersStructure
import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseTemplate
import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseTemplateGroup
import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseTemplatesStructure
import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseUserGroup
import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseUsersStructure
import nl.info.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import nl.info.client.smartdocuments.model.template.User
import java.util.UUID

fun createAttendedResponse(
    ticket: String = "dummyTicket",
) = AttendedResponse().apply {
    this.ticket = ticket
}

fun createFile(
    fileName: String = "dummyFileName",
    document: Document = createDocument(),
    outputFormat: String = "dummyOutputFormat",
) = File(
    fileName = fileName,
    document = document,
    outputFormat = outputFormat
)

fun createDocument(
    data: String = "dummyDocumentData"
) = Document(
    data = data
)

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

fun createsmartDocumentsTemplatesResponse() = SmartDocumentsTemplatesResponse(
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

fun createSmartDocument(
    variables: Variables
) = SmartDocument(
    selection = createSelection(),
    variables = variables
)

fun createSelection(
    templateGroup: String = "dummyTemplateGroup",
    template: String = "dummyTemplate"
) = Selection(
    templateGroup = templateGroup,
    template = template
)
