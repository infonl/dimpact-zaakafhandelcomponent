/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import net.atos.zac.app.zaken.model.RESTCommunicatiekanaal
import net.atos.zac.zaaksturing.model.ReferentieTabelWaarde

fun convertToRESTCommunicatiekanaal(communicatieKanaal: ReferentieTabelWaarde): RESTCommunicatiekanaal =
    RESTCommunicatiekanaal(
        id = communicatieKanaal.id,
        naam = communicatieKanaal.naam
    )

fun convertToRESTCommunicatiekanalen(
    communicatieKanalen: List<ReferentieTabelWaarde>
): List<RESTCommunicatiekanaal> = communicatieKanalen.stream()
    .map { convertToRESTCommunicatiekanaal(it) }
    .toList()
