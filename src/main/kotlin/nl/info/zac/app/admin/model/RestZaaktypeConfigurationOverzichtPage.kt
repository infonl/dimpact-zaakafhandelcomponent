/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.BEGIN_GELDIGHEID
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.EINDE_GELDIGHEID
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.GELDIG
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.OMSCHRIJVING
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.PROCESS_DEFINITION
import nl.info.zac.app.admin.model.RestZaaktypeConfigurationSortField.VALIDE
import nl.info.zac.app.shared.takePage
import nl.info.zac.search.model.SorteerRichting
import nl.info.zac.time.DateRange
import nl.info.zac.time.contains
import nl.info.zac.util.dutchCollator
import java.time.LocalDate

private typealias RestZaaktypeConfigurationOverzichtFilter = (RestZaaktypeConfigurationOverzicht) -> Boolean

fun List<RestZaaktypeConfigurationOverzicht>.toRestZaaktypeConfigurationOverzichtResultaat(
    restZaaktypeConfigurationListParameters: RestZaaktypeConfigurationListParameters
): RestZaaktypeConfigurationOverzichtResultaat {
    val filters = restZaaktypeConfigurationListParameters.toFilters()
    val matchingRestZaaktypeConfigurationOverzichten = filter { restZaaktypeConfigurationOverzicht ->
        filters.all { it(restZaaktypeConfigurationOverzicht) }
    }.sortedWith(restZaaktypeConfigurationListParameters.toComparator())
    return RestZaaktypeConfigurationOverzichtResultaat(
        results = matchingRestZaaktypeConfigurationOverzichten.takePage(restZaaktypeConfigurationListParameters),
        resultCount = matchingRestZaaktypeConfigurationOverzichten.size.toLong(),
        filterZaaktypes = toRestZaaktypeFilterOpties(),
        filterProcessDefinitions = toFilterProcessDefinitions()
    )
}

private fun RestZaaktypeConfigurationListParameters.toFilters() = listOfNotNull<RestZaaktypeConfigurationOverzichtFilter>(
    isValide?.let { isValide -> { it.isValide == isValide } },
    isNuGeldig?.let { isNuGeldig -> { it.zaaktype.isNuGeldig == isNuGeldig } },
    zaaktypeIdentificatie?.let { identificatie -> { it.zaaktype.identificatie == identificatie } },
    processDefinitionKey?.let { key -> { it.processDefinition?.key == key } },
    DateRange.of(beginGeldigheidFrom, beginGeldigheidThrough)
        ?.let { range -> { it.zaaktype.beginGeldigheid.isIn(range) } },
    DateRange.of(eindeGeldigheidFrom, eindeGeldigheidThrough)
        ?.let { range -> { it.zaaktype.eindeGeldigheid.isIn(range) } }
)

private fun LocalDate?.isIn(dateRange: DateRange) = this != null && this in dateRange

private fun RestZaaktypeConfigurationListParameters.toComparator(): Comparator<RestZaaktypeConfigurationOverzicht> {
    val comparator = sortField?.toComparator() ?: return Comparator { _, _ -> 0 }
    return if (sortOrder == SorteerRichting.DESC) comparator.reversed() else comparator
}

private fun RestZaaktypeConfigurationSortField.toComparator(): Comparator<RestZaaktypeConfigurationOverzicht> =
    when (this) {
        VALIDE -> compareBy { it.isValide }
        OMSCHRIJVING -> compareBy { it.zaaktype.omschrijving }
        PROCESS_DEFINITION -> compareBy(nullsLast()) { it.processDefinition?.naam }
        GELDIG -> compareBy { it.zaaktype.isNuGeldig }
        BEGIN_GELDIGHEID -> compareBy { it.zaaktype.beginGeldigheid }
        EINDE_GELDIGHEID -> compareBy(nullsLast()) { it.zaaktype.eindeGeldigheid }
    }

private fun List<RestZaaktypeConfigurationOverzicht>.toRestZaaktypeFilterOpties() =
    mapNotNull { restZaaktypeConfigurationOverzicht ->
        with(restZaaktypeConfigurationOverzicht.zaaktype) {
            identificatie?.let { RestZaaktypeFilterOptie(identificatie = it, omschrijving = omschrijving) }
        }
    }
        .distinctBy { it.identificatie }
        .sortedWith(compareBy(dutchCollator()) { it.omschrijving })

private fun List<RestZaaktypeConfigurationOverzicht>.toFilterProcessDefinitions() =
    mapNotNull { it.processDefinition }
        .distinctBy { it.key }
        .sortedWith(compareBy(nullsLast(dutchCollator())) { it.naam })
