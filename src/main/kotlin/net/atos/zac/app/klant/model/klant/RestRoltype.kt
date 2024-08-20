/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.klant

import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestRoltype(
    var uuid: UUID? = null,
    var naam: String? = null,
    var omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum? = null
)
