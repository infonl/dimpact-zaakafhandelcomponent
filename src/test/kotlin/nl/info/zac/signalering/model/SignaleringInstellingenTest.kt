/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SignaleringInstellingenTest : BehaviorSpec({
    val signaleringType = SignaleringType().apply { type = SignaleringType.Type.ZAAK_OP_NAAM }

    context("Constructing signalering instellingen for an owner") {
        given("a group as owner") {
            `when`("the instellingen are constructed") {
                val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.GROUP, "fakeGroupId")

                then("the group is the owner") {
                    with(signaleringInstellingen) {
                        groep shouldBe "fakeGroupId"
                        medewerker shouldBe null
                        ownerType shouldBe SignaleringTarget.GROUP
                        isValid shouldBe true
                    }
                }
            }
        }

        given("a user as owner") {
            `when`("the instellingen are constructed") {
                val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.USER, "fakeUserId")

                then("the user is the owner") {
                    with(signaleringInstellingen) {
                        groep shouldBe null
                        medewerker shouldBe "fakeUserId"
                        ownerType shouldBe SignaleringTarget.USER
                        isValid shouldBe true
                    }
                }
            }
        }
    }

    context("Validating signalering instellingen") {
        given("instellingen with both a group and a user") {
            val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.GROUP, "fakeGroupId")
                .apply { medewerker = "fakeUserId" }

            `when`("they are validated") {
                val isValid = signaleringInstellingen.isValid

                then("they are not valid") {
                    isValid shouldBe false
                }
            }
        }

        given("instellingen with neither a group nor a user") {
            val signaleringInstellingen = SignaleringInstellingen()

            `when`("they are validated") {
                val isValid = signaleringInstellingen.isValid

                then("they are not valid and have no owner type") {
                    isValid shouldBe false
                    signaleringInstellingen.ownerType shouldBe null
                }
            }
        }

        given("instellingen with a group and a blank user") {
            val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.GROUP, "fakeGroupId")
                .apply { medewerker = " " }

            `when`("they are validated") {
                val isValid = signaleringInstellingen.isValid

                then("they are valid, because a blank user does not count") {
                    isValid shouldBe true
                }
            }
        }

        given("instellingen with a blank group and a user") {
            val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.USER, "fakeUserId")
                .apply { groep = " " }

            `when`("they are validated") {
                val isValid = signaleringInstellingen.isValid

                then("they are valid, because a blank group does not count") {
                    isValid shouldBe true
                }
            }
        }
    }

    context("Checking whether signalering instellingen are empty") {
        given("instellingen with neither dashboard nor mail") {
            val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.USER, "fakeUserId")

            `when`("it is checked whether they are empty") {
                val isEmpty = signaleringInstellingen.isEmpty

                then("they are empty") {
                    isEmpty shouldBe true
                }
            }
        }

        given("instellingen with only dashboard") {
            val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.USER, "fakeUserId")
                .apply { isDashboard = true }

            `when`("it is checked whether they are empty") {
                val isEmpty = signaleringInstellingen.isEmpty

                then("they are not empty") {
                    isEmpty shouldBe false
                }
            }
        }

        given("instellingen with only mail") {
            val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.USER, "fakeUserId")
                .apply { isMail = true }

            `when`("it is checked whether they are empty") {
                val isEmpty = signaleringInstellingen.isEmpty

                then("they are not empty") {
                    isEmpty shouldBe false
                }
            }
        }
    }

    context("Describing signalering instellingen") {
        given("instellingen of a group") {
            val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.GROUP, "fakeGroupId")

            `when`("they are described") {
                val description = signaleringInstellingen.toString()

                then("the description names the type and the group") {
                    description shouldBe "$signaleringType-signaleringinstellingen voor fakeGroupId"
                }
            }
        }

        given("instellingen of a user") {
            val signaleringInstellingen = SignaleringInstellingen(signaleringType, SignaleringTarget.USER, "fakeUserId")

            `when`("they are described") {
                val description = signaleringInstellingen.toString()

                then("the description names the type and the user") {
                    description shouldBe "$signaleringType-signaleringinstellingen voor fakeUserId"
                }
            }
        }
    }
})
