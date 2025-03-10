/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.smartdocuments.model.template

import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class SmartDocumentsTemplatesResponse(
    var documentsStructure: SmartDocumentsResponseDocumentsStructure,
    var usersStructure: SmartDocumentsResponseUsersStructure
)

@NoArgConstructor
data class SmartDocumentsResponseDocumentsStructure(
    var templatesStructure: SmartDocumentsResponseTemplatesStructure,
    var headersStructure: SmartDocumentsResponseHeadersStructure
)

@NoArgConstructor
data class SmartDocumentsResponseTemplatesStructure(
    var templateGroups: List<SmartDocumentsResponseTemplateGroup>,
    var accessible: Boolean
)

@NoArgConstructor
data class SmartDocumentsResponseTemplateGroup(
    var id: String,
    var name: String,
    var allDescendants: Boolean,
    var templateGroups: List<SmartDocumentsResponseTemplateGroup>?,
    var templates: List<SmartDocumentsResponseTemplate>?,
    var accessible: Boolean?
)

@NoArgConstructor
data class SmartDocumentsResponseHeadersStructure(
    var headerGroups: List<HeaderGroup>,
    var accessible: Boolean
)

class HeaderGroup

@NoArgConstructor
data class SmartDocumentsResponseGroupsAccess(
    var templateGroups: List<SmartDocumentsResponseTemplateGroup>,
    var headerGroups: List<Any>
)

@NoArgConstructor
data class SmartDocumentsResponseTemplate(
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
data class SmartDocumentsResponseUserGroup(
    var id: String,
    var name: String,
    var groupsAccess: SmartDocumentsResponseGroupsAccess,
    var userGroups: List<SmartDocumentsResponseUserGroup>,
    var users: List<User>,
    var accessible: Boolean
)

@NoArgConstructor
data class SmartDocumentsResponseUsersStructure(
    var groupsAccess: SmartDocumentsResponseGroupsAccess,
    var userGroups: List<SmartDocumentsResponseUserGroup>,
    var accessible: Boolean
)
