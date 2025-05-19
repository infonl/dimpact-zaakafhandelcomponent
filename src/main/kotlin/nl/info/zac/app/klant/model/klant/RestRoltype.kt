/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.klant

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
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
    uuid = this.url.extractUuid(),
    naam = this.omschrijving,
    omschrijvingGeneriekEnum = this.omschrijvingGeneriek
)
