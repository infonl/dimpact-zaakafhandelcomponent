/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.rest

import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException

/**
 * Validates that all elements in a templates group set are part of a templates group superset.
 * The superset can be the SmartDocuments structure API response.
 *
 * In other words we validate if groups and templates have the same IDs, names and hierarchy as in superset
 *
 * @param supersetTemplates set of RESTSmartDocumentsTemplateGroup to validate against
 * @throws SmartDocumentsConfigurationException when there are no templates to validate against, or unknown templates are detected
 */
infix fun Set<RestMappedSmartDocumentsTemplateGroup>.isSubsetOf(
    supersetTemplates: Set<RestSmartDocumentsTemplateGroup>
) {
    if (supersetTemplates.isEmpty()) {
        throw SmartDocumentsConfigurationException("Validation failed. No SmartDocuments templates available")
    }

    val superset = supersetTemplates.toStringRepresentation()
    val subset = this.toStringRepresentation()

    val errors = subset.filterNot { superset.contains(it) }
    if (errors.isNotEmpty()) {
        throw SmartDocumentsConfigurationException("Validation failed. Unknown entities: $errors")
    }
}
