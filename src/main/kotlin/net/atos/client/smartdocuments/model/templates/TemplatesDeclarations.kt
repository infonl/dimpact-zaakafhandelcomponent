/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
class TemplatesResponse {
    lateinit var documentsStructure: DocumentsStructure
    lateinit var usersStructure: UsersStructure
}

@NoArgConstructor
class DocumentsStructure {
    lateinit var templatesStructure: TemplatesStructure
    lateinit var headersStructure: HeadersStructure
}

@NoArgConstructor
class TemplatesStructure {
    lateinit var templateGroups: List<SmartDocumentsTemplateGroup>
    var accessible: Boolean = false
}

@NoArgConstructor
class SmartDocumentsTemplateGroup {
    lateinit var id: String
    lateinit var name: String
    var allDescendants: Boolean = false

    var templateGroups: List<SmartDocumentsTemplateGroup>? = null
    var templates: List<SmartDocumentsTemplate>? = null
    var accessible: Boolean? = null
}

@NoArgConstructor
class HeadersStructure {
    lateinit var headerGroups: List<HeaderGroup>
    var accessible: Boolean = false
}

class HeaderGroup

@NoArgConstructor
class GroupsAccess {
    lateinit var templateGroups: List<SmartDocumentsTemplateGroup>
    lateinit var headerGroups: List<Any>
}

@NoArgConstructor
class SmartDocumentsTemplate {
    lateinit var id: String
    lateinit var name: String
    var favorite: Boolean = false
}

@NoArgConstructor
class User {
    lateinit var id: String
    lateinit var name: String
}

@NoArgConstructor
class UserGroup {
    lateinit var id: String
    lateinit var name: String
    lateinit var groupsAccess: GroupsAccess
    lateinit var userGroups: List<UserGroup>
    lateinit var users: List<User>
    var accessible: Boolean = false
}

@NoArgConstructor
class UsersStructure {
    lateinit var groupsAccess: GroupsAccess
    lateinit var userGroups: List<UserGroup>
    var accessible: Boolean = false
}
