/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.klant

import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import java.util.UUID

data class RestRoltype(
    var uuid: UUID? = null,
    var naam: String? = null,
    var omschrijvingGeneriekEnum: OmschrijvingGeneriekEnum? = null
)
