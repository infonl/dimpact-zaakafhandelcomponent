/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest.util

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.shouldEqualJson

infix fun String.shouldEqualJsonIgnoringOrder(other: String) =
    this shouldEqualJson {
        arrayOrder = ArrayOrder.Lenient
        other
    }

infix fun String.shouldEqualJsonIgnoringExtraneousFields(other: String) =
    this shouldEqualJson {
        fieldComparison = FieldComparison.Lenient
        other
    }

infix fun String.shouldEqualJsonIgnoringOrderAndExtraneousFields(other: String) =
    this shouldEqualJson {
        arrayOrder = ArrayOrder.Lenient
        fieldComparison = FieldComparison.Lenient
        other
    }
