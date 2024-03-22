/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import net.atos.client.zgw.zrc.model.Status
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.zac.app.zaken.model.RESTZaakStatus

fun convertToRESTZaakStatus(
    status: Status,
    statustype: StatusType
) = RESTZaakStatus(
    toelichting = status.statustoelichting,
    naam = statustype.omschrijving
)
