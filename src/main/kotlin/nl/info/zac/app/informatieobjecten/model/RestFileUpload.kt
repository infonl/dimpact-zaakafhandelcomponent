/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import jakarta.ws.rs.FormParam

class RestFileUpload {
    @field:FormParam("file")
    var file: ByteArray? = null

    @field:FormParam("filesize")
    var fileSize: Long = 0

    @field:FormParam("filename")
    var filename: String? = null

    @field:FormParam("type")
    var type: String? = null
}
