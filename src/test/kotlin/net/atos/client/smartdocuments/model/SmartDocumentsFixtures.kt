package net.atos.client.smartdocuments.model

import net.atos.client.smartdocuments.model.templates.DocumentsStructure
import net.atos.client.smartdocuments.model.templates.GroupsAccess
import net.atos.client.smartdocuments.model.templates.HeadersStructure
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplate
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplateGroup
import net.atos.client.smartdocuments.model.templates.TemplatesResponse
import net.atos.client.smartdocuments.model.templates.TemplatesStructure
import net.atos.client.smartdocuments.model.templates.User
import net.atos.client.smartdocuments.model.templates.UserGroup
import net.atos.client.smartdocuments.model.templates.UsersStructure
import net.atos.client.smartdocuments.model.wizard.WizardResponse
import java.util.UUID

fun createWizardResponse(
    ticket: String = "dummyTicket",
) = WizardResponse().apply {
    this.ticket = ticket
}

fun createUserGroup(userGroupName: String) = UserGroup(
    id = UUID.randomUUID().toString(),
    name = userGroupName,
    groupsAccess = GroupsAccess(
        templateGroups = listOf(
            SmartDocumentsTemplateGroup(
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

fun createTemplate(templateName: String) = SmartDocumentsTemplate(
    id = UUID.randomUUID().toString(),
    name = templateName,
    favorite = false
)

fun createTemplates() = listOf(
    createTemplate("Aanvullende informatie nieuw"),
    createTemplate("Aanvullende informatie oud"),
)

fun createTemplateGroup(groupName: String, templatesList: List<SmartDocumentsTemplate>) =
    SmartDocumentsTemplateGroup(
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

fun createTemplatesResponse() = TemplatesResponse(
    documentsStructure = DocumentsStructure(
        templatesStructure = TemplatesStructure(
            templateGroups = listOf(
                SmartDocumentsTemplateGroup(
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
        headersStructure = HeadersStructure(
            headerGroups = emptyList(),
            accessible = true
        )
    ),
    usersStructure = UsersStructure(
        groupsAccess = GroupsAccess(
            templateGroups = emptyList(),
            headerGroups = emptyList()
        ),
        userGroups = listOf(createUserGroup("Atos"), createUserGroup("Dimpact")),
        accessible = true
    )
)
