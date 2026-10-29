/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import nl.info.zac.admin.model.createReferenceTableValue

class RestReferenceTableValueTest : BehaviorSpec({
    given("A reference table value that has not been persisted yet") {
        val referenceTableValue = createReferenceTableValue(id = null)

        `when`("the value is converted to a REST reference table value") {
            val restReferenceTableValue = referenceTableValue.toRestReferenceTableValue()

            then("the REST reference table value has no id") {
                restReferenceTableValue.id.shouldBeNull()
            }
        }
    }
})
