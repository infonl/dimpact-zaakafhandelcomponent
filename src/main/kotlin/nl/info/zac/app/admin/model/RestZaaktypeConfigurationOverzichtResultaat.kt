/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.app.shared.RestResultaat

/**
 * A page of the Zaakafhandelparameters page, with the number of rows that match the filters, and the zaaktypes and
 * process definitions of all rows to filter on.
 */
class RestZaaktypeConfigurationOverzichtResultaat(
    results: Collection<RestZaaktypeConfigurationOverzicht>,
    resultCount: Long,
    val filterZaaktypes: List<RestZaaktypeFilterOptie>,
    val filterProcessDefinitions: List<RestProcessDefinitionOverzicht>
) : RestResultaat<RestZaaktypeConfigurationOverzicht>(results, resultCount)
