/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.json.Json
import jakarta.json.bind.JsonbBuilder
import java.io.StringReader

class PolicyInputJsonbTest : BehaviorSpec({
    val jsonb = JsonbBuilder.create()

    fun toJsonObject(value: Any) = Json.createReader(StringReader(jsonb.toJson(value))).readObject()

    given("zaak data sent to OPA") {
        val zaakData = ZaakData(
            isOpen = true,
            zaaktype = "fakeZaaktype",
            isOpgeschort = true,
            isVerlengd = true,
            isIntake = true,
            isBesloten = true,
            isHeropend = true,
            isBrondatumBepaald = true,
            isZaakspecifiekGeautoriseerd = true,
            isLoggedInUserGeautoriseerdeMedewerker = true
        )

        `when`("it is serialized") {
            val jsonObject = toJsonObject(zaakData)

            then("the boolean fields keep the names the zaak-rechten policy reads") {
                jsonObject.keys shouldBe setOf(
                    "open",
                    "zaaktype",
                    "opgeschort",
                    "verlengd",
                    "intake",
                    "besloten",
                    "heropend",
                    "brondatumBepaald",
                    "zaakspecifiekGeautoriseerd",
                    "loggedInUserIsGeautoriseerdeMedewerker"
                )
            }
        }
    }

    given("taak data sent to OPA") {
        val taakData = TaakData(
            isOpen = true,
            zaaktype = "fakeZaaktype",
            isZaakspecifiekGeautoriseerd = true,
            isLoggedInUserGeautoriseerdeMedewerker = true
        )

        `when`("it is serialized") {
            val jsonObject = toJsonObject(taakData)

            then("the boolean fields keep the names the taak-rechten policy reads") {
                jsonObject.keys shouldBe setOf(
                    "open",
                    "zaaktype",
                    "zaakspecifiekGeautoriseerd",
                    "loggedInUserIsGeautoriseerdeMedewerker"
                )
            }
        }
    }

    given("document data sent to OPA") {
        val documentData = DocumentData(
            isDefinitief = true,
            isVergrendeld = true,
            isOndertekend = true,
            vergrendeldDoor = "fakeUserId",
            zaaktype = "fakeZaaktype",
            isZaakOpen = true,
            isZaakspecifiekGeautoriseerd = true,
            isLoggedInUserGeautoriseerdeMedewerker = true
        )

        `when`("it is serialized") {
            val jsonObject = toJsonObject(documentData)

            then("the boolean fields keep the names the document-rechten policy reads") {
                jsonObject.keys shouldBe setOf(
                    "definitief",
                    "vergrendeld",
                    "ondertekend",
                    "vergrendeld_door",
                    "zaaktype",
                    "zaak_open",
                    "zaakspecifiekGeautoriseerd",
                    "loggedInUserIsGeautoriseerdeMedewerker"
                )
            }
        }
    }
})
