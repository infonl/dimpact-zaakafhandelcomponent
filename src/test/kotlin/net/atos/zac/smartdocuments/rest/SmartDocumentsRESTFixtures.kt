/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.rest

import java.util.UUID

fun createRESTMappedTemplateGroup(
    id: String = UUID.randomUUID().toString(),
    name: String = "templateGroup1",
    groups: Set<RestMappedSmartDocumentsTemplateGroup>,
    templates: Set<RestMappedSmartDocumentsTemplate>
) = RestMappedSmartDocumentsTemplateGroup(
    id = id,
    name = name,
    groups = groups,
    templates = templates
)

fun createRESTMappedTemplate(
    id: String = UUID.randomUUID().toString(),
    name: String = "template1",
    informatieObjectTypeUUID: UUID = UUID.randomUUID()
) = RestMappedSmartDocumentsTemplate(
    id = id,
    name = name,
    informatieObjectTypeUUID = informatieObjectTypeUUID
)

fun createRESTTemplateGroup(
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

fun createRESTTemplate(
    id: String = UUID.randomUUID().toString(),
    name: String = "template1"
) = RestSmartDocumentsTemplate(
    id = id,
    name = name
)
