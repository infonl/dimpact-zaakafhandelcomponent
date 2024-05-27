/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model

class File {
    lateinit var fileName: String

    lateinit var document: Document

    var outputFormat: String? = null
}
