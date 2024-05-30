package net.atos.client.sd.model

import net.atos.client.sd.model.templates.DocumentsStructure
import net.atos.client.sd.model.templates.GroupsAccess
import net.atos.client.sd.model.templates.HeadersStructure
import net.atos.client.sd.model.templates.Template
import net.atos.client.sd.model.templates.TemplateGroup
import net.atos.client.sd.model.templates.TemplatesResponse
import net.atos.client.sd.model.templates.TemplatesStructure
import net.atos.client.sd.model.templates.User
import net.atos.client.sd.model.templates.UserGroup
import net.atos.client.sd.model.templates.UsersStructure
import net.atos.client.sd.model.wizard.WizardResponse
import java.util.UUID

fun createWizardResponse(
    ticket: String = "dummyTicket",
) = WizardResponse().apply {
    this.ticket = ticket
}

fun createUserGroup(userGroupName: String) = UserGroup().apply {
    this.groupsAccess = GroupsAccess().apply {
        templateGroups = listOf(
            TemplateGroup().apply {
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

fun createTemplate(templateName: String) = Template().apply {
    id = UUID.randomUUID().toString()
    name = templateName
    favorite = false
}

fun createTemplates() = listOf(
    createTemplate("Aanvullende informatie nieuw"),
    createTemplate("Aanvullende informatie oud"),
)

fun createTemplateGroup(groupName: String, templatesList: List<Template>) = TemplateGroup().apply {
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

fun createListTemplatesResponse() = TemplatesResponse().apply {
    this.documentsStructure = DocumentsStructure().apply {
        this.templatesStructure = TemplatesStructure().apply {
            this.templateGroups = listOf(
                TemplateGroup().apply {
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
