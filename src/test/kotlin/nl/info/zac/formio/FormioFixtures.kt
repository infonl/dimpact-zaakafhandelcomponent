/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.formio

import nl.info.zac.formio.model.FormioFormulier

fun createFormioFormulier(
    id: Long = 124L,
    name: String = "testForm",
    title: String = "dummyTitle",
    content: String = """{ "dummyKey": "dummyValue" }"""
) = FormioFormulier().apply {
    this.id = id
    this.name = name
    this.title = title
    this.content = content
}
