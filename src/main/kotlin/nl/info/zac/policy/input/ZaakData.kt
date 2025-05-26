/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class ZaakData(
    var open: Boolean = false,

    var zaaktype: String? = null,

    var opgeschort: Boolean = false,

    var verlengd: Boolean = false,

    var intake: Boolean = false,

    var besloten: Boolean = false,

    var heropend: Boolean = false
)
