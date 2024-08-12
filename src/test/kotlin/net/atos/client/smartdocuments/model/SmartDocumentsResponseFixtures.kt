/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.smartdocuments.model

import net.atos.client.smartdocuments.model.document.AttendedResponse
import net.atos.client.smartdocuments.model.document.Document
import net.atos.client.smartdocuments.model.document.File
import net.atos.client.smartdocuments.model.document.Registratie
import net.atos.client.smartdocuments.model.document.Selection
import net.atos.client.smartdocuments.model.document.SmartDocument
import net.atos.client.smartdocuments.model.document.UnattendedResponse
import net.atos.client.smartdocuments.model.template.SmartDocumentsResponseDocumentsStructure
import net.atos.client.smartdocuments.model.template.SmartDocumentsResponseGroupsAccess
import net.atos.client.smartdocuments.model.template.SmartDocumentsResponseHeadersStructure
import net.atos.client.smartdocuments.model.template.SmartDocumentsResponseTemplate
import net.atos.client.smartdocuments.model.template.SmartDocumentsResponseTemplateGroup
import net.atos.client.smartdocuments.model.template.SmartDocumentsResponseTemplatesStructure
import net.atos.client.smartdocuments.model.template.SmartDocumentsResponseUserGroup
import net.atos.client.smartdocuments.model.template.SmartDocumentsResponseUsersStructure
import net.atos.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import net.atos.client.smartdocuments.model.template.User
import net.atos.client.zgw.drc.model.generated.StatusEnum
import java.net.URI
import java.time.LocalDate
import java.util.UUID

fun createAttendedResponse(
    ticket: String = "dummyTicket",
) = AttendedResponse().apply {
    this.ticket = ticket
}

fun createUnattendedResponse(
    files: List<File> = listOf(createFile())
) = UnattendedResponse(
    files = files
)

fun createUnattendedResponseFromTemplateName(
    templateName: String
) = UnattendedResponse(
    files = listOf(
        createFile(
            fileName = "$templateName.docx",
            createDocument(),
            outputFormat = "DOCX"
        ),
        createFile(
            fileName = "$templateName.html",
            createDocument(),
            outputFormat = "HTML"
        ),
        createFile(
            fileName = "${templateName}_answer.xml",
            createDocument(),
            outputFormat = "XML"
        ),
        createFile(
            fileName = "$templateName.pdf",
            createDocument(),
            outputFormat = "PDF"
        )
    )
)

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
    data: String = "dummyDocumentData",
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

@Suppress("LongParameterList")
fun createRegistratie(
    zaak: URI = URI("http://example.com/dummyZaak"),
    informatieObjectStatus: StatusEnum = StatusEnum.IN_BEWERKING,
    informatieObjectType: URI = URI("http://example.com/dummyInformatieObjectType"),
    bronOrganisatie: String = "dummyBronOrganisatie",
    creatieDatum: LocalDate = LocalDate.now(),
    auditToelichting: String = "dummyAuditToelichting"
) =
    Registratie(
        zaak = zaak,
        informatieObjectStatus = informatieObjectStatus,
        informatieObjectType = informatieObjectType,
        bronOrganisatie = bronOrganisatie,
        creatieDatum = creatieDatum,
        auditToelichting = auditToelichting
    )

fun createSmartDocument() = SmartDocument(
    selection = createSelection()
)

fun createSelection(
    templateGroup: String = "dummyTemplateGroup",
    template: String = "dummyTemplate"
) = Selection(
    templateGroup = templateGroup,
    template = template
)
