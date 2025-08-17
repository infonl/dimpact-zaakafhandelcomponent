/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.model

import java.net.URI

@Suppress("LongParameterList")
fun createValidationZgwError(
    type: URI = URI("https://localhost:8080/validation-error"),
    code: String = "fakeCode",
    title: String = "fakeTitle",
    status: Int = 123,
    detail: String = "fakeDetail",
    instance: URI = URI("https://localhost:8080/validation-error-instance"),
    invalidParams: List<FieldValidationError> = listOf(createFieldValidationError())
) = ValidationZgwError(
    type,
    code,
    title,
    status,
    detail,
    instance,
    invalidParams
)

fun createFieldValidationError(
    name: String = "fakeFieldName",
    code: String = "fakeCode",
    reason: String = "fakeReason"
) = FieldValidationError(
    name,
    code,
    reason
)
