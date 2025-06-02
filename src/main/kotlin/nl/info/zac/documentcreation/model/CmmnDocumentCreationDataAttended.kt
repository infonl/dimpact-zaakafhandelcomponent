/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation.model

import nl.info.client.zgw.zrc.model.generated.Zaak
import java.time.ZonedDateTime

data class CmmnDocumentCreationDataAttended(
    val zaak: Zaak,

    val taskId: String?,

    val templateGroupId: String,

    val templateId: String,

    var title: String,

    var description: String? = null,

    var author: String? = null,

    var creationDate: ZonedDateTime,
)
