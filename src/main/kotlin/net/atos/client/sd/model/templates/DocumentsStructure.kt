/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
class DocumentsStructure {
    lateinit var templatesStructure: TemplatesStructure
    lateinit var headersStructure: HeadersStructure
}
