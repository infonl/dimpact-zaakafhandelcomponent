/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.rest

import java.util.UUID

fun createRestMappedSmartDocumentsTemplateGroup(
    id: String = UUID.randomUUID().toString(),
    name: String = "templateGroup1",
    groups: Set<RestMappedSmartDocumentsTemplateGroup>? = null,
    templates: Set<RestMappedSmartDocumentsTemplate>? = null
) = RestMappedSmartDocumentsTemplateGroup(
    id = id,
    name = name,
    groups = groups,
    templates = templates
)

fun createRestMappedSmartDocumentsTemplate(
    id: String = UUID.randomUUID().toString(),
    name: String = "template1",
    informatieObjectTypeUUID: UUID = UUID.randomUUID()
) = RestMappedSmartDocumentsTemplate(
    id = id,
    name = name,
    informatieObjectTypeUUID = informatieObjectTypeUUID
)

fun createRestSmartDocumentsTemplateGroup(
    id: String = UUID.randomUUID().toString(),
    name: String = "templateGroup1",
    groups: Set<RestSmartDocumentsTemplateGroup>,
    templates: Set<RestSmartDocumentsTemplate>
) = RestSmartDocumentsTemplateGroup(
    id = id,
    name = name,
    groups = groups,
    templates = templates
)

fun createRestSmartDocumentsTemplate(
    id: String = UUID.randomUUID().toString(),
    name: String = "template1"
) = RestSmartDocumentsTemplate(
    id = id,
    name = name
)
