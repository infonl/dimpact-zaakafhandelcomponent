package net.atos.zac.smartdocuments.rest

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RESTSmartDocumentsTemplateGroup(
    var id: String,
    var name: String,
    var groups: Set<RESTSmartDocumentsTemplateGroup>?,
    var templates: Set<RESTSmartDocumentsTemplate>?,
)
