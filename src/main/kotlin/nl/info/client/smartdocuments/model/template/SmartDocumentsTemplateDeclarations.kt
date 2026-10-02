/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.smartdocuments.model.template

import jakarta.json.bind.annotation.JsonbProperty
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
    @get:JsonbProperty("accessible")
    @set:JsonbProperty("accessible")
    var isAccessible: Boolean
)

@NoArgConstructor
data class SmartDocumentsResponseTemplateGroup(
    var id: String,
    var name: String,
    @get:JsonbProperty("allDescendants")
    @set:JsonbProperty("allDescendants")
    var hasAllDescendants: Boolean,
    var templateGroups: List<SmartDocumentsResponseTemplateGroup>?,
    var templates: List<SmartDocumentsResponseTemplate>?,
    @get:JsonbProperty("accessible")
    @set:JsonbProperty("accessible")
    var isAccessible: Boolean?
)

@NoArgConstructor
data class SmartDocumentsResponseHeadersStructure(
    var headerGroups: List<HeaderGroup>,
    @get:JsonbProperty("accessible")
    @set:JsonbProperty("accessible")
    var isAccessible: Boolean
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
    @get:JsonbProperty("favorite")
    @set:JsonbProperty("favorite")
    var isFavorite: Boolean
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
    @get:JsonbProperty("accessible")
    @set:JsonbProperty("accessible")
    var isAccessible: Boolean
)

@NoArgConstructor
data class SmartDocumentsResponseUsersStructure(
    var groupsAccess: SmartDocumentsResponseGroupsAccess,
    var userGroups: List<SmartDocumentsResponseUserGroup>,
    @get:JsonbProperty("accessible")
    @set:JsonbProperty("accessible")
    var isAccessible: Boolean
)
