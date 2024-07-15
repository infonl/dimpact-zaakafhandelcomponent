/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.converter

import net.atos.client.zgw.ztc.model.generated.ZaakTypenRelatie
import net.atos.zac.app.zaak.model.RESTZaaktypeRelatie
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.util.UriUtil
import java.net.URI

fun convertToRESTZaaktypeRelatie(zaaktypUri: URI, relatieType: RelatieType) =
    RESTZaaktypeRelatie(
        zaaktypeUuid = UriUtil.uuidFromURI(zaaktypUri),
        relatieType = relatieType
    )

fun convertToRESTZaaktypeRelatie(zaakTypenRelatie: ZaakTypenRelatie) =
    RESTZaaktypeRelatie(
        zaaktypeUuid = UriUtil.uuidFromURI(zaakTypenRelatie.zaaktype),
        relatieType = RelatieType.valueOf(zaakTypenRelatie.aardRelatie.name)
    )
