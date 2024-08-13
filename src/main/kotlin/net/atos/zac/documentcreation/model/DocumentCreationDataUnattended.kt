/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import net.atos.client.zgw.zrc.model.Zaak

data class DocumentCreationDataUnattended(
    val taskId: String? = null,

    val templateGroupName: String,

    val templateName: String,

    val zaak: Zaak
)
