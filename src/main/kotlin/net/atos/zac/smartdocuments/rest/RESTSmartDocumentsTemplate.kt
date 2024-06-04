package net.atos.zac.smartdocuments.rest

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RESTSmartDocumentsTemplate(
    var id: String,
    var name: String,
)
