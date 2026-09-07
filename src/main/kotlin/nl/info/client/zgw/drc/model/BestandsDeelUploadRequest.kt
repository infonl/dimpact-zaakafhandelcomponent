/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc.model

import jakarta.ws.rs.FormParam
import jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM
import jakarta.ws.rs.core.MediaType.TEXT_PLAIN
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.jboss.resteasy.annotations.providers.multipart.PartFilename
import org.jboss.resteasy.annotations.providers.multipart.PartType
import java.io.InputStream

/**
 * Body of a `PUT /bestandsdelen/{uuid}` request. The content is a stream so that a part never has
 * to be held on the heap in full.
 */
@NoArgConstructor
@AllOpen
class BestandsDeelUploadRequest(
    @field:FormParam("inhoud")
    @field:PartType(APPLICATION_OCTET_STREAM)
    @field:PartFilename("bestandsdeel")
    var inhoud: InputStream? = null,

    @field:FormParam("lock")
    @field:PartType(TEXT_PLAIN)
    var lock: String? = null
)
