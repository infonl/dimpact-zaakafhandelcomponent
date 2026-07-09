/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.shared.model

import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject

fun createResultsOfZaakObjecten(
    list: List<Zaakobject> = emptyList(),
    count: Int = 0
): Results<Zaakobject> = Results(
    list,
    count
)
