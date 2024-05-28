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

fun createUserGroup(name: String) = UserGroup().apply {
    this.groupsAccess = GroupsAccess().apply {
        this.templateGroups = listOf(
            TemplateGroup().apply {
                this.id = UUID.randomUUID().toString()
                this.name = "Dimpact"
                this.allDescendants = true
            }
        )
        this.headerGroups = emptyList()
    }
    this.userGroups = emptyList()
    this.users = listOf(
        User().apply {
            this.id = UUID.randomUUID().toString()
            this.name = "zaakafhandelcomponent"
        }
    )
    this.accessible = true
    this.id = UUID.randomUUID().toString()
    this.name = name
}

fun createTemplate(templateName: String) = Template().apply {
    this.id = UUID.randomUUID().toString()
    this.name = templateName
    this.favorite = false
}

fun createTemplates() = listOf(
    createTemplate("Aanvullende informatie nieuw"),
    createTemplate("Aanvullende informatie oud"),
    createTemplate("Advies nieuw"),
    createTemplate("Advies ou"),
    createTemplate("Besluit nieuw"),
    createTemplate("Besluit oud"),
    createTemplate("Data Test"),
    createTemplate("Memo nieuw"),
    createTemplate("Memo oud"),
    createTemplate("Ontvangstbevestiging nieuw"),
    createTemplate("Ontvangstbevestiging oud"),
    createTemplate("Samenwerking loont"),
    createTemplate("Signature Template"),
    createTemplate("Toets nieuw"),
    createTemplate("Toets oud"),
    createTemplate("Zaakafhandelcomponent Test")
)

fun createTemplateGroup(groupName: String, templates: List<Template>) = TemplateGroup().apply {
    this.templateGroups = emptyList()
    this.templates = templates
    this.accessible = true
    this.id = UUID.randomUUID().toString()
    this.name = groupName
}

fun createTemplateGroups() = listOf(
    createTemplateGroup(
        "Intern zaaktype voor test volledig gebruik ZAC",
        listOf(
            createTemplate("Data Test"),
            createTemplate("OpenZaakTest"),
            createTemplate("Intern zaaktype voor test volledig gebruik ZAC"),
        )
    ),
    createTemplateGroup(
        "OpenZaak",
        listOf(
            createTemplate("Data Test"),
            createTemplate("OpenZaakTest"),
        )
    ),
    createTemplateGroup(
        "Indienen aansprakelijkstelling door derden behandelen",
        listOf(
            createTemplate("Data Test"),
            createTemplate("OpenZaakTest"),
        )
    ),
    createTemplateGroup(
        "Melding evenement organiseren behandelen",
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
                    this.templateGroups = createTemplateGroups()
                    this.templates = createTemplates()
                    this.accessible = true
                    this.id = UUID.randomUUID().toString()
                    this.name = "Dimpact"
                }
            )
            this.accessible = true
        }
        this.headersStructure = HeadersStructure().apply {
            this.headerGroups = emptyList()
            this.accessible = true
        }
    }
    this.usersStructure = UsersStructure().apply {
        this.groupsAccess = GroupsAccess()
        this.userGroups = listOf(createUserGroup("Atos"), createUserGroup("Dimpact"))
        this.accessible = true
    }
}
