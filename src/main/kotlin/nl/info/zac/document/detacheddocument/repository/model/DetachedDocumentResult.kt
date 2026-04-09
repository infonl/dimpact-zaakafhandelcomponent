/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.repository.model

import nl.info.zac.shared.model.Resultaat

class DetachedDocumentResult(
    items: List<DetachedDocument>,
    count: Long,
    val detachedByFilter: List<String>
) : Resultaat<DetachedDocument>(items, count)
