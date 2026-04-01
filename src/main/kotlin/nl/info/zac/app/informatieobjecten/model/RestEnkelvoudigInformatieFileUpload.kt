/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model

import jakarta.ws.rs.FormParam
import nl.info.zac.app.informatieobjecten.model.validation.ValidRestEnkelvoudigInformatieFileUploadForm

@ValidRestEnkelvoudigInformatieFileUploadForm
abstract class RestEnkelvoudigInformatieFileUpload {
    // this can be empty when adding a new version in which only the metadata changes
    @field:FormParam("file")
    var file: ByteArray? = null

    @field:FormParam("bestandsnaam")
    var bestandsnaam: String? = null
}
