/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model

import jakarta.ws.rs.FormParam
import nl.info.zac.app.informatieobjecten.model.validation.ValidRestEnkelvoudigInformatieFileUploadForm
import nl.info.zac.util.AllOpen
import java.io.InputStream

@AllOpen
@ValidRestEnkelvoudigInformatieFileUploadForm
abstract class RestEnkelvoudigInformatieFileUpload {
    // a stream rather than a byte array so that a large document is never held on the heap in full,
    // and null when adding a new version in which only the metadata changes
    @field:FormParam("file")
    var file: InputStream? = null

    @field:FormParam("bestandsnaam")
    var bestandsnaam: String? = null

    @field:FormParam("formaat")
    var formaat: String? = null
}
