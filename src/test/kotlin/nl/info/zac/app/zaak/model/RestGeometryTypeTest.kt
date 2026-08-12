/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum

class RestGeometryTypeTest : BehaviorSpec({
    given("RestGeometryType enum") {
        `when`("compared to GeometryTypeEnum") {
            then("it should have an entry for every GeometryTypeEnum value") {
                GeometryTypeEnum.entries.forEach { geometryTypeEnum ->
                    RestGeometryType.valueOf(geometryTypeEnum.name) shouldBe RestGeometryType.valueOf(geometryTypeEnum.name)
                }
                RestGeometryType.entries.size shouldBe GeometryTypeEnum.entries.size
            }
        }
    }
})
