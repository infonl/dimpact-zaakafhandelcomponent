/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.shared.exception.ZgwRuntimeException

class ObjectTypeTest : BehaviorSpec({
    given("a URL for a known ZGW audit trail object type") {
        `when`("getObjectType is called") {
            then("it returns the matching ObjectType") {
                ObjectType.getObjectType(
                    "https://example.com/besluiten/api/v1/besluiten/123"
                ) shouldBe ObjectType.BESLUIT
            }
        }
    }

    given("a URL for an unsupported ZGW audit trail object type") {
        `when`("getObjectType is called") {
            then("it throws a ZgwRuntimeException") {
                shouldThrow<ZgwRuntimeException> {
                    ObjectType.getObjectType("https://example.com/unsupported/api/v1/unsupported/123")
                }
            }
        }
    }
})
