/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import jakarta.ws.rs.FormParam
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestFileUpload(
    @field:FormParam("file")
    var file: ByteArray? = null,

    @field:FormParam("filesize")
    var fileSize: Long = 0,

    @field:FormParam("filename")
    var filename: String? = null,

    @field:FormParam("type")
    var type: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RestFileUpload

        if (fileSize != other.fileSize) return false
        if (!java.util.Arrays.equals(file, other.file)) return false
        if (filename != other.filename) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileSize.hashCode()
        result = 31 * result + (file?.contentHashCode() ?: 0)
        result = 31 * result + (filename?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }
}
