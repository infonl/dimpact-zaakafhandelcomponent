/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.converter

import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.app.klant.model.klant.RestRoltype

fun toRestRoltypes(roltypen: List<RolType>): List<RestRoltype> =
    roltypen.map { toRestRoltype(it) }

fun toRestRoltype(roltype: RolType) = RestRoltype(
    uuid = URIUtil.parseUUIDFromResourceURI(roltype.url),
    naam = roltype.omschrijving,
    omschrijvingGeneriekEnum = roltype.omschrijvingGeneriek
)
