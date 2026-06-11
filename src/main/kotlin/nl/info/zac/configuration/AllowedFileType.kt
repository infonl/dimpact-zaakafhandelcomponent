/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration

/**
 * Canonical allowlist of file extensions and corresponding media types that ZAC accepts
 * for uploads of enkelvoudig informatieobjecten and Flowable task documents.
 *
 * This is the single source of truth for upload validation. Adding a new file type
 * requires extending this enum.
 *
 * Only the extension is validated. The media type reported by browsers comes from the
 * client OS and varies per machine (registry on Windows), so it cannot be validated
 * against; [mediaType] is the canonical type ZAC associates with the extension.
 */
enum class AllowedFileType(
    val extension: String,
    val mediaType: String
) {
    AVI(".avi", "video/x-msvideo"),
    BMP(".bmp", "image/bmp"),
    DOC(".doc", "application/msword"),
    DOCX(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    EML(".eml", "message/rfc822"),
    FLV(".flv", "video/x-flv"),
    GIF(".gif", "image/gif"),
    JPEG(".jpeg", "image/jpeg"),
    JPG(".jpg", "image/jpeg"),
    MKV(".mkv", "video/x-matroska"),
    MOV(".mov", "video/quicktime"),
    MP4(".mp4", "video/mp4"),
    MPEG(".mpeg", "video/mpeg"),
    MSG(".msg", "application/vnd.ms-outlook"),
    ODS(".ods", "application/vnd.oasis.opendocument.spreadsheet"),
    ODT(".odt", "application/vnd.oasis.opendocument.text"),
    PDF(".pdf", "application/pdf"),
    PNG(".png", "image/png"),
    PPT(".ppt", "application/vnd.ms-powerpoint"),
    PPTX(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    RTF(".rtf", "application/rtf"),
    TXT(".txt", "text/plain"),
    VSD(".vsd", "application/vnd.visio"),
    WMV(".wmv", "video/x-ms-wmv"),
    XLS(".xls", "application/vnd.ms-excel"),
    XLSX(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    companion object {
        private val byExtension = entries.associateBy(AllowedFileType::extension)

        fun fromFilename(filename: String?): AllowedFileType? =
            filename?.takeUnless { it.isBlank() }?.let { name ->
                name.lastIndexOf('.')
                    .takeIf { it >= 0 }
                    ?.let { byExtension[name.substring(it).lowercase()] }
            }

        /**
         * Returns true when [filename]'s extension is on the allowlist. Matching is
         * case-insensitive.
         */
        fun isAllowed(filename: String?): Boolean = fromFilename(filename) != null
    }
}
