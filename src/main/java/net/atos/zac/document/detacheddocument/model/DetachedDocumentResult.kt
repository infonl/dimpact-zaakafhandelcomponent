/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.document.detacheddocument.model

import nl.info.zac.shared.model.Resultaat

class DetachedDocumentResult(
    items: List<DetachedDocument>,
    count: Long,
    val ontkoppeldDoorFilter: List<String>
) : Resultaat<DetachedDocument>(items, count)
