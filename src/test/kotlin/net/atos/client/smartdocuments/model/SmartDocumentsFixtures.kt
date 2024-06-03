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

fun createUserGroup(userGroupName: String) = UserGroup().apply {
    this.groupsAccess = GroupsAccess().apply {
        templateGroups = listOf(
            SmartDocumentsTemplateGroup().apply {
                id = UUID.randomUUID().toString()
                name = "Dimpact"
                allDescendants = true
            }
        )
        headerGroups = emptyList()
    }
    userGroups = emptyList()
    users = listOf(
        User().apply {
            id = UUID.randomUUID().toString()
            name = "zaakafhandelcomponent"
        }
    )
    accessible = true
    id = UUID.randomUUID().toString()
    name = userGroupName
}

fun createTemplate(templateName: String) = SmartDocumentsTemplate().apply {
    id = UUID.randomUUID().toString()
    name = templateName
    favorite = false
}

fun createTemplates() = listOf(
    createTemplate("Aanvullende informatie nieuw"),
    createTemplate("Aanvullende informatie oud"),
)

fun createTemplateGroup(groupName: String, templatesList: List<SmartDocumentsTemplate>) =
    SmartDocumentsTemplateGroup().apply {
        templateGroups = emptyList()
        templates = templatesList
        accessible = true
        id = UUID.randomUUID().toString()
        name = groupName
    }

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

fun createTemplatesResponse() = TemplatesResponse().apply {
    this.documentsStructure = DocumentsStructure().apply {
        this.templatesStructure = TemplatesStructure().apply {
            this.templateGroups = listOf(
                SmartDocumentsTemplateGroup().apply {
                    templateGroups = createTemplateGroups()
                    templates = createTemplates()
                    accessible = true
                    id = UUID.randomUUID().toString()
                    name = "Dimpact"
                }
            )
            accessible = true
        }
        this.headersStructure = HeadersStructure().apply {
            headerGroups = emptyList()
            accessible = true
        }
    }
    this.usersStructure = UsersStructure().apply {
        groupsAccess = GroupsAccess()
        userGroups = listOf(createUserGroup("Atos"), createUserGroup("Dimpact"))
        accessible = true
    }
}
