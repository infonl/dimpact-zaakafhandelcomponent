/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.app.shared.RestResultaat
import nl.info.zac.app.shared.takePage
import nl.info.zac.healthcheck.model.ZaaktypeInrichtingscheck
import nl.info.zac.search.model.SorteerRichting
import nl.info.zac.util.dutchCollator

fun List<ZaaktypeInrichtingscheck>.toRestZaaktypeInrichtingscheckResultaat(
    restInrichtingscheckParameters: RestInrichtingscheckParameters
): RestResultaat<RestZaaktypeInrichtingscheck> {
    val matchingZaaktypeInrichtingschecks = filter {
        it.matchesZoekterm(restInrichtingscheckParameters.zoekterm) &&
            it.matchesStatus(restInrichtingscheckParameters.status)
    }.sortedWith(restInrichtingscheckParameters.toComparator())
    return RestResultaat(
        results = matchingZaaktypeInrichtingschecks.takePage(restInrichtingscheckParameters)
            .map { it.toRestZaaktypeInrichtingscheck() },
        resultCount = matchingZaaktypeInrichtingschecks.size.toLong()
    )
}

private fun ZaaktypeInrichtingscheck.matchesZoekterm(zoekterm: String?) =
    zoekterm.isNullOrBlank() ||
        zoekterm.trim().let { trimmedZoekterm ->
            zaaktype.omschrijving.contains(trimmedZoekterm, ignoreCase = true) ||
                zaaktype.doel?.contains(trimmedZoekterm, ignoreCase = true) == true
        }

private fun ZaaktypeInrichtingscheck.matchesStatus(status: RestInrichtingscheckStatus?) =
    when (status) {
        null -> true
        RestInrichtingscheckStatus.AANDACHT_NODIG -> !isValide || hasWaarschuwingen
        RestInrichtingscheckStatus.IN_ORDE -> isValide && !hasWaarschuwingen
    }

private fun RestInrichtingscheckParameters.toComparator(): Comparator<ZaaktypeInrichtingscheck> {
    val byOmschrijving = compareBy<ZaaktypeInrichtingscheck, String>(dutchCollator()) { it.zaaktype.omschrijving }
    val bySortField = when (sortField) {
        RestInrichtingscheckSortField.OMSCHRIJVING -> byOmschrijving
        RestInrichtingscheckSortField.DOEL -> compareBy(nullsFirst(dutchCollator())) { it.zaaktype.doel }
        RestInrichtingscheckSortField.BEGIN_GELDIGHEID -> compareBy { it.zaaktype.beginGeldigheid }
        RestInrichtingscheckSortField.VALIDE -> compareBy { it.isValide }
    }.thenComparing(byOmschrijving).thenComparing { zaaktypeInrichtingscheck -> zaaktypeInrichtingscheck.zaaktype.url }
    return if (sortOrder == SorteerRichting.DESC) bySortField.reversed() else bySortField
}
