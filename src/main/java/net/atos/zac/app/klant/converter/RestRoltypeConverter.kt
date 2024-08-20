/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.converter

import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.app.klant.model.klant.RestRoltype

fun toRestRoltypes(roltypen: List<RolType>): List<RestRoltype> =
    // return roltypen.map(Function<RolType?, RestRoltype> { obj: RolType? -> convert() }).toList()
    roltypen.map { toRestRoltype(it) }.toList()

fun toRestRoltype(roltype: RolType) = RestRoltype(
    uuid = URIUtil.parseUUIDFromResourceURI(roltype.url),
    naam = roltype.omschrijving,
    omschrijvingGeneriekEnum = roltype.omschrijvingGeneriek
)
