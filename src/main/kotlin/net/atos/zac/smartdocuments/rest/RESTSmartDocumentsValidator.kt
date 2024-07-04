package net.atos.zac.smartdocuments.rest

import net.atos.zac.smartdocuments.SmartDocumentsException

/**
 * Validates that all elements in a templates group set are part of pre-defined templates group superset.
 * The superset can be returned by SmartDocuments structure API or stored in DB
 *
 * @param supersetTemplates set of IRESTSmartDocumentsTemplateGroup to validate against
 */
infix fun Set<RESTMappedSmartDocumentsTemplateGroup>.isSubsetOf(
    supersetTemplates: Set<RESTSmartDocumentsTemplateGroup>
) {
    val superset = supersetTemplates.toStringRepresentation()
    val subset = this.toStringRepresentation()

    val errors = subset.filterNot { superset.contains(it) }
    if (errors.isNotEmpty()) {
        throw SmartDocumentsException("Validation failed. Unknown entities: $errors")
    }
}
