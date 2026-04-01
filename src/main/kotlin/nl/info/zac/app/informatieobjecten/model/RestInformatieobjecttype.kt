/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import java.util.UUID

class RestInformatieobjecttype {
    var uuid: UUID? = null

    var omschrijving: String? = null

    var vertrouwelijkheidaanduiding: String? = null

    var concept: Boolean = false
}
