/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import net.atos.zac.app.admin.model.RestFormioFormulierContent

fun createRestFormioFormulierContent(
    filename: String = "testForm.json",
    content: String = """{ "dummyKey": "dummyValue" }"""
) = RestFormioFormulierContent(
    filename = filename,
    content = content
)
