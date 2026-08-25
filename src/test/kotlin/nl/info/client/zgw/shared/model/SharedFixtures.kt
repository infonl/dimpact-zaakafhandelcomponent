/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.shared.model

import nl.info.client.zgw.zrc.model.zaakobjecten.Zaakobject
import java.net.URI

fun createResultsOfZaakObjecten(
    list: List<Zaakobject> = emptyList(),
    count: Int = 0
): Results<Zaakobject> = Results(
    list,
    count
)

@Suppress("LongParameterList")
fun createValidationZgwError(
    type: URI = URI("https://localhost:8080/validation-error"),
    code: String = "fakeCode",
    title: String = "fakeTitle",
    status: Int = 123,
    detail: String = "fakeDetail",
    instance: URI = URI("https://localhost:8080/validation-error-instance"),
    invalidParams: List<FieldValidationError> = listOf(createFieldValidationError())
) = ZgwValidationError(
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
