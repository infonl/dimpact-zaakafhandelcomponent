/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.model

import nl.info.client.zgw.zrc.model.RolMedewerker

data class ZaakspecifiekGeautoriseerdeMedewerkerRol(
    val medewerkerId: String,
    val rol: RolMedewerker,
    val isDetachable: Boolean
)
