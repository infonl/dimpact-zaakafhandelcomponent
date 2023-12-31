/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.zaaksturing.model

import java.util.UUID

fun createZaakafhandelParameters(
    id: Long = 1234L,
    zaakTypeUUID: UUID = UUID.randomUUID(),
) =
    ZaakafhandelParameters().apply {
        this.id = id
        this.zaakTypeUUID = zaakTypeUUID
    }
