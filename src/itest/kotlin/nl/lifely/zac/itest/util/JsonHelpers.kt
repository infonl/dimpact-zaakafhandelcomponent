/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.util

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.compareJsonOptions
import io.kotest.assertions.json.shouldEqualJson

infix fun String.shouldEqualJsonIgnoringOrder(other: String) = shouldEqualJson(
    other,
    compareJsonOptions {
        arrayOrder = ArrayOrder.Lenient
    }
)

infix fun String.shouldEqualJsonIgnoringExtraneousFields(other: String) = shouldEqualJson(
    other,
    compareJsonOptions {
        fieldComparison = FieldComparison.Lenient
    }
)

infix fun String.shouldEqualJsonIgnoringOrderAndExtraneousFields(other: String) = shouldEqualJson(
    other,
    compareJsonOptions {
        arrayOrder = ArrayOrder.Lenient
        fieldComparison = FieldComparison.Lenient
    }
)
