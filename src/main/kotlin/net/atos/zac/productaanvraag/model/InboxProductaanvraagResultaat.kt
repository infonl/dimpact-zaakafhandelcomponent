/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag.model

import nl.info.zac.shared.model.Resultaat

class InboxProductaanvraagResultaat(
    items: List<InboxProductaanvraag>,
    count: Long,
    val typeFilter: List<String>
) : Resultaat<InboxProductaanvraag>(items, count)
