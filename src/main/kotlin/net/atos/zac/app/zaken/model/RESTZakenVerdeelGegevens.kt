/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import java.util.UUID

class RESTZakenVerdeelGegevens {
    var uuids: List<UUID>? = null

    var groepId: String? = null

    var behandelaarGebruikersnaam: String? = null

    var reden: String? = null
}
