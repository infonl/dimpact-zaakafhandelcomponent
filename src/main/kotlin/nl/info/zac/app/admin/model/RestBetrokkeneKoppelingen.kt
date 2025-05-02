/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.model

class RestBetrokkeneKoppelingen {
    var id: Long? = null
    var zaakafhandelParameters: RestZaakafhandelParameters? = null
    var brpKoppelen = true
    var kvkKoppelen = true
}
