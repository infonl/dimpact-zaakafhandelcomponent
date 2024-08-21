/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.klant

import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.client.zgw.ztc.model.generated.RolType
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

fun List<RolType>.toRestRoltypes(): List<RestRoltype> = this.map { it.toRestRoltype() }

fun RolType.toRestRoltype() = RestRoltype(
    uuid = URIUtil.parseUUIDFromResourceURI(this.url),
    naam = this.omschrijving,
    omschrijvingGeneriekEnum = this.omschrijvingGeneriek
)
