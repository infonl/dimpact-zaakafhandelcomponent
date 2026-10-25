/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SignaleringTypeTest : BehaviorSpec({
    context("Reading the type of a signaleringtype") {
        given("a signaleringtype that only has an id, as read from the database") {
            val signaleringType = SignaleringType().apply { id = "TAAK_VERLOPEN" }

            `when`("the type is read") {
                val type = signaleringType.type

                then("the type is derived from the id") {
                    type shouldBe SignaleringType.Type.TAAK_VERLOPEN
                }
            }
        }
    }

    context("Comparing signaleringtypes") {
        given("signaleringtypes with different subjecttypes and types") {
            val zaakOpNaam = createSignaleringType(SignaleringType.Type.ZAAK_OP_NAAM, SignaleringSubject.ZAAK)
            val zaakDocumentToegevoegd = createSignaleringType(
                SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD,
                SignaleringSubject.ZAAK
            )
            val taakOpNaam = createSignaleringType(SignaleringType.Type.TAAK_OP_NAAM, SignaleringSubject.TAAK)

            `when`("they are sorted") {
                val sortedSignaleringTypes = listOf(taakOpNaam, zaakOpNaam, zaakDocumentToegevoegd).sorted()

                then("they are ordered by subjecttype first and type second") {
                    sortedSignaleringTypes shouldContainExactly listOf(zaakDocumentToegevoegd, zaakOpNaam, taakOpNaam)
                }
            }
        }

        given("two signaleringtypes with the same type") {
            val signaleringType = createSignaleringType(SignaleringType.Type.ZAAK_OP_NAAM)
            val otherSignaleringType = createSignaleringType(SignaleringType.Type.ZAAK_OP_NAAM)

            `when`("they are compared for equality") {
                then("they are equal and have the same hash code") {
                    signaleringType shouldBe otherSignaleringType
                    signaleringType.hashCode() shouldBe otherSignaleringType.hashCode()
                }
            }
        }

        given("two signaleringtypes with a different type") {
            `when`("they are compared for equality") {
                then("they are not equal") {
                    createSignaleringType(SignaleringType.Type.ZAAK_OP_NAAM) shouldNotBe
                        createSignaleringType(SignaleringType.Type.ZAAK_VERLOPEND)
                }
            }
        }
    }

    context("The properties of the signaleringtype types") {
        given("the taak verlopen type") {
            `when`("its properties are read") {
                then("it has no dashboard card, but is mailed to users only") {
                    with(SignaleringType.Type.TAAK_VERLOPEN) {
                        isDashboard shouldBe false
                        isMail shouldBe true
                        isTarget(SignaleringTarget.USER) shouldBe true
                        isTarget(SignaleringTarget.GROUP) shouldBe false
                    }
                }
            }
        }

        given("the zaak op naam type") {
            `when`("its properties are read") {
                then("it has a dashboard card and targets both users and groups") {
                    with(SignaleringType.Type.ZAAK_OP_NAAM) {
                        isDashboard shouldBe true
                        isTarget(SignaleringTarget.USER) shouldBe true
                        isTarget(SignaleringTarget.GROUP) shouldBe true
                    }
                }
            }
        }
    }
})
