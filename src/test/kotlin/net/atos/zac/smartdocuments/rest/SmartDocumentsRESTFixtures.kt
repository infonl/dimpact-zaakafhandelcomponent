/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.rest

import java.util.UUID

fun createRESTTemplateGroup(
    id: String = UUID.randomUUID().toString(),
    name: String = "templateGroup1"
) = RESTSmartDocumentsTemplateGroup(
    id = id,
    name = name,
    groups = null,
    templates = null
)

fun createRESTTemplate(
    id: String = UUID.randomUUID().toString(),
    name: String = "template1"
) = RESTSmartDocumentsTemplate(
    id = id,
    name = name,
    informatieObjectTypeUUID = smartDocumentsTemplate.informatieObjectTypeUUID
)
