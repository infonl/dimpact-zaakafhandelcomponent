/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import jakarta.inject.Inject
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.model.FormulierVeldDefinitie
import nl.info.zac.admin.model.HumanTaskReferentieTabel
import nl.info.zac.admin.model.toRestReferenceTable
import nl.info.zac.app.admin.model.RestHumanTaskReferenceTable

class RestHumanTaskReferenceTableConverter @Inject constructor(
    private val referenceTableService: ReferenceTableService
) {
    fun convertDefault(veldDefinitie: FormulierVeldDefinitie) = RestHumanTaskReferenceTable(
        veld = veldDefinitie.name,
        tabel = referenceTableService.readReferenceTable(veldDefinitie.defaultTabel.name).toRestReferenceTable(false)
    )

    fun toRestHumanTaskReferenceTables(humanTaskReferentieTabellen: Collection<HumanTaskReferentieTabel>) =
        humanTaskReferentieTabellen.map { it.toRestHumanTaskReferenceTable() }

    fun toHumanTaskReferentieTabellen(restHumanTaskReferenceTables: List<RestHumanTaskReferenceTable>) =
        restHumanTaskReferenceTables.map { it.toHumanTaskReferentieTabel() }

    private fun HumanTaskReferentieTabel.toRestHumanTaskReferenceTable() = RestHumanTaskReferenceTable(
        id = id,
        veld = checkNotNull(veld) { "Human task referentie tabel with id '$id' has no veld" },
        tabel = checkNotNull(tabel) { "Human task referentie tabel with id '$id' has no tabel" }
            .toRestReferenceTable(false)
    )

    private fun RestHumanTaskReferenceTable.toHumanTaskReferentieTabel() = HumanTaskReferentieTabel().apply {
        id = this@toHumanTaskReferentieTabel.id
        veld = this@toHumanTaskReferentieTabel.veld
        // The tabel is just a reference here, so don't update it from the REST but fetch it from the database
        tabel = referenceTableService.readReferenceTable(
            checkNotNull(this@toHumanTaskReferentieTabel.tabel?.id) { "Reference table of veld '$veld' has no id" }
        )
    }
}
