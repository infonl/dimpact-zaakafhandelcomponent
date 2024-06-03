/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.smartdocuments.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class TemplatesResponse(
    var documentsStructure: DocumentsStructure,
    var usersStructure: UsersStructure
)

@NoArgConstructor
data class DocumentsStructure(
    var templatesStructure: TemplatesStructure,
    var headersStructure: HeadersStructure
)

@NoArgConstructor
data class TemplatesStructure(
    var templateGroups: List<SmartDocumentsTemplateGroup>,
    var accessible: Boolean
)

@NoArgConstructor
data class SmartDocumentsTemplateGroup(
    var id: String,
    var name: String,
    var allDescendants: Boolean,
    var templateGroups: List<SmartDocumentsTemplateGroup>?,
    var templates: List<SmartDocumentsTemplate>?,
    var accessible: Boolean?
)

@NoArgConstructor
data class HeadersStructure(
    var headerGroups: List<HeaderGroup>,
    var accessible: Boolean
)

class HeaderGroup

@NoArgConstructor
data class GroupsAccess(
    var templateGroups: List<SmartDocumentsTemplateGroup>,
    var headerGroups: List<Any>
)

@NoArgConstructor
data class SmartDocumentsTemplate(
    var id: String,
    var name: String,
    var favorite: Boolean
)

@NoArgConstructor
data class User(
    var id: String,
    var name: String
)

@NoArgConstructor
data class UserGroup(
    var id: String,
    var name: String,
    var groupsAccess: GroupsAccess,
    var userGroups: List<UserGroup>,
    var users: List<User>,
    var accessible: Boolean
)

@NoArgConstructor
data class UsersStructure(
    var groupsAccess: GroupsAccess,
    var userGroups: List<UserGroup>,
    var accessible: Boolean
)
