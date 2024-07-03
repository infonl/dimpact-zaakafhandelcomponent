package nl.lifely.zac.itest.util

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.compareJsonOptions
import io.kotest.assertions.json.shouldEqualJson

infix fun String.lenientShouldEqualJson(other: String) = this.shouldEqualJson(
    other,
    compareJsonOptions {
        arrayOrder = ArrayOrder.Lenient
        fieldComparison = FieldComparison.Lenient
    }
)
