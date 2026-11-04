/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.healthcheck

import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.BRP_DOELBINDING_RAADPLEEG_WAARDE
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.BRP_DOELBINDING_ZOEK_WAARDE
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.BRP_VERWERKINGSREGISTER_WAARDE
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.util.isPureAscii

/**
 * The BRP settings are the same for every zaaktype, while a request can check hundreds of zaaktypen. Request scope
 * reads the reference tables once per request, and still lets a request see a change that a beheerder just saved.
 */
@RequestScoped
@AllOpen
@NoArgConstructor
class BrpInstellingenCheck @Inject constructor(
    private val referenceTableService: ReferenceTableService
) {
    private var isBrpInstellingenCorrect: Boolean? = null

    fun isBrpInstellingenCorrect(): Boolean =
        isBrpInstellingenCorrect ?: readIsBrpInstellingenCorrect().also { isBrpInstellingenCorrect = it }

    private fun readIsBrpInstellingenCorrect() =
        listOf(BRP_DOELBINDING_ZOEK_WAARDE, BRP_DOELBINDING_RAADPLEEG_WAARDE, BRP_VERWERKINGSREGISTER_WAARDE)
            .all(::isReferenceTableValidForAuditLogHeaders)

    private fun isReferenceTableValidForAuditLogHeaders(systemReferenceTable: SystemReferenceTable): Boolean =
        referenceTableService.readSystemReferenceTable(systemReferenceTable).values.let { values ->
            values.isNotEmpty() && values.all { it.name.isPureAscii() }
        }
}
