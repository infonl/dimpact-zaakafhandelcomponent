/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.configuration.model

import nl.info.zac.configuration.AllowedFileType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * REST representation of a single entry from the ZAC upload allowlist.
 *
 * Frontend uses these pairs to populate the file picker hint, drive the
 * mimetype -> extension display, and decorate downloads.
 */
@AllOpen
@NoArgConstructor
data class RestAllowedFileType(
    var extension: String,
    var mediaType: String
)

fun AllowedFileType.toRestAllowedFileType() = RestAllowedFileType(
    extension = this.extension,
    mediaType = this.mediaType
)
