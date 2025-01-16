/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
data class RestDatumRange(
    var van: LocalDate? = null,
    var tot: LocalDate? = null
) {
    fun hasValue() = van != null || tot != null
}
