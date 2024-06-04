package net.atos.client.smartdocuments.model

import net.atos.zac.smartdocuments.rest.RESTSmartDocumentsTemplate
import net.atos.zac.smartdocuments.rest.RESTSmartDocumentsTemplateGroup
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
    name = name
)
