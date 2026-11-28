/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util

/**
 * List of media types used by the ZAC backend.
 *
 * See [Media Types](https://www.iana.org/assignments/media-types/media-types.xhtml) of IANA.
 *
 * Thanks goes out to [Youngbin Kim](https://gist.github.com/retheviper) for providing the original code
 * which contains a much more complete list of media types:
 * https://gist.github.com/retheviper/9b4e28f66b354d9f706e43d399100676
 */
object MediaTypes {
    /** Hypertext Application Language (HAL) for JSON */
    const val MEDIA_TYPE_HAL_JSON = "application/hal+json"

    /** Standardized format (RFC 7807) for representing error responses in HTTP APIs */
    const val MEDIA_TYPE_PROBLEM_JSON = "application/problem+json"

    enum class Application(val extensions: Array<String>, val mediaType: String) {
        /** Microsoft Excel */
        MS_EXCEL(arrayOf(".xls"), "application/vnd.ms-excel"),

        /** Microsoft Excel (OpenXML) */
        MS_EXCEL_OPEN_XML(arrayOf(".xlsx"), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

        /** Microsoft PowerPoint */
        MS_POWER_POINT(arrayOf(".ppt"), "application/vnd.ms-powerpoint"),

        /** Microsoft PowerPoint (OpenXML) */
        MS_POWER_POINT_OPEN_XML(
            arrayOf(".pptx"),
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        ),

        /** Microsoft Word */
        MS_WORD(arrayOf(".doc"), "application/msword"),

        /** Microsoft Word (OpenXML) */
        MS_WORD_OPEN_XML(arrayOf(".docx"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),

        /** Adobe Portable Document Format (PDF) */
        PDF(arrayOf(".pdf"), "application/pdf"),

        /** Zip Archive */
        ZIP(arrayOf(".zip"), "application/zip")
    }

    enum class Text(val extensions: Array<String>, val mediaType: String) {
        /** Comma-separated values (CSV) */
        CSV(arrayOf(".csv"), "text/csv")
    }
}
