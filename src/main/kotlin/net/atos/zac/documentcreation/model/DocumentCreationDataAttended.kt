/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import net.atos.client.zgw.zrc.model.Zaak

data class DocumentCreationDataAttended(
    val taskId: String? = null,

    val templateGroupId: String,

    val templateId: String,

    val zaak: Zaak
)
