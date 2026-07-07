/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.zaak.model.ZaakKoppelenData
import java.util.UUID

class ZaakKoppelenServiceTest : BehaviorSpec({

    fun createZaakKoppelenData(
        isOpen: Boolean = true,
        isHoofdzaak: Boolean = false,
        isDeelzaak: Boolean = false,
        zaaktypeUUID: UUID = UUID.randomUUID(),
        lezen: Boolean = true,
        koppelen: Boolean = true
    ) = ZaakKoppelenData(
        isOpen = isOpen,
        isHoofdzaak = isHoofdzaak,
        isDeelzaak = isDeelzaak,
        zaaktypeUUID = zaaktypeUUID,
        lezen = lezen,
        koppelen = koppelen
    )

    Context("canBeRelated") {
        Given("a source zaak with koppelen rights and a target zaak with lezen rights") {
            val from = createZaakKoppelenData(koppelen = true)
            val to = createZaakKoppelenData(lezen = true)

            When("canBeRelated is called") {
                val result = canBeRelated(from, to)

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("a source zaak without koppelen rights") {
            val from = createZaakKoppelenData(koppelen = false)
            val to = createZaakKoppelenData(lezen = true)

            When("canBeRelated is called") {
                val result = canBeRelated(from, to)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a target zaak without lezen rights") {
            val from = createZaakKoppelenData(koppelen = true)
            val to = createZaakKoppelenData(lezen = false)

            When("canBeRelated is called") {
                val result = canBeRelated(from, to)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("canBeHoofdAndDeelzaak") {
        val zaaktypeUUID = UUID.randomUUID()

        Given("a valid hoofdzaak and deelzaak with matching zaaktype and both open") {
            val hoofdzaak = createZaakKoppelenData()
            val deelzaak = createZaakKoppelenData(zaaktypeUUID = zaaktypeUUID)

            When("canBeHoofdAndDeelzaak is called") {
                val result = canBeHoofdAndDeelzaak(hoofdzaak, deelzaak, setOf(zaaktypeUUID))

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("a hoofdzaak without koppelen rights") {
            val hoofdzaak = createZaakKoppelenData(koppelen = false)
            val deelzaak = createZaakKoppelenData(zaaktypeUUID = zaaktypeUUID)

            When("canBeHoofdAndDeelzaak is called") {
                val result = canBeHoofdAndDeelzaak(hoofdzaak, deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a hoofdzaak that is already a deelzaak") {
            val hoofdzaak = createZaakKoppelenData(isDeelzaak = true)
            val deelzaak = createZaakKoppelenData(zaaktypeUUID = zaaktypeUUID)

            When("canBeHoofdAndDeelzaak is called") {
                val result = canBeHoofdAndDeelzaak(hoofdzaak, deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a deelzaak without koppelen rights") {
            val hoofdzaak = createZaakKoppelenData()
            val deelzaak = createZaakKoppelenData(zaaktypeUUID = zaaktypeUUID, koppelen = false)

            When("canBeHoofdAndDeelzaak is called") {
                val result = canBeHoofdAndDeelzaak(hoofdzaak, deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a deelzaak that is already a hoofdzaak") {
            val hoofdzaak = createZaakKoppelenData()
            val deelzaak = createZaakKoppelenData(zaaktypeUUID = zaaktypeUUID, isHoofdzaak = true)

            When("canBeHoofdAndDeelzaak is called") {
                val result = canBeHoofdAndDeelzaak(hoofdzaak, deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a deelzaak that is already a deelzaak") {
            val hoofdzaak = createZaakKoppelenData()
            val deelzaak = createZaakKoppelenData(zaaktypeUUID = zaaktypeUUID, isDeelzaak = true)

            When("canBeHoofdAndDeelzaak is called") {
                val result = canBeHoofdAndDeelzaak(hoofdzaak, deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("an open hoofdzaak and a closed deelzaak") {
            val hoofdzaak = createZaakKoppelenData(isOpen = true)
            val deelzaak = createZaakKoppelenData(zaaktypeUUID = zaaktypeUUID, isOpen = false)

            When("canBeHoofdAndDeelzaak is called") {
                val result = canBeHoofdAndDeelzaak(hoofdzaak, deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a deelzaak with a zaaktype not in the allowed set") {
            val hoofdzaak = createZaakKoppelenData()
            val deelzaak = createZaakKoppelenData()

            When("canBeHoofdAndDeelzaak is called") {
                val result = canBeHoofdAndDeelzaak(hoofdzaak, deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("hoofdAndDeelzaakCanBeOntkoppeld") {
        Given("both hoofdzaak and deelzaak have koppelen rights") {
            val hoofdzaak = createZaakKoppelenData(koppelen = true)
            val deelzaak = createZaakKoppelenData(koppelen = true)

            When("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdAndDeelzaakCanBeOntkoppeld(hoofdzaak, deelzaak)

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("the hoofdzaak does not have koppelen rights") {
            val hoofdzaak = createZaakKoppelenData(koppelen = false)
            val deelzaak = createZaakKoppelenData(koppelen = true)

            When("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdAndDeelzaakCanBeOntkoppeld(hoofdzaak, deelzaak)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("the deelzaak does not have koppelen rights") {
            val hoofdzaak = createZaakKoppelenData(koppelen = true)
            val deelzaak = createZaakKoppelenData(koppelen = false)

            When("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdAndDeelzaakCanBeOntkoppeld(hoofdzaak, deelzaak)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("relatedZakenCanBeOntkoppeld") {
        Given("from zaak has koppelen rights and to zaak has lezen rights") {
            val from = createZaakKoppelenData(koppelen = true, lezen = true)
            val to = createZaakKoppelenData(lezen = true)

            When("relatedZakenCanBeOntkoppeld is called") {
                val result = relatedZakenCanBeOntkoppeld(from, to)

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("from zaak does not have koppelen rights") {
            val from = createZaakKoppelenData(koppelen = false)
            val to = createZaakKoppelenData(lezen = true)

            When("relatedZakenCanBeOntkoppeld is called") {
                val result = relatedZakenCanBeOntkoppeld(from, to)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("to zaak does not have lezen rights") {
            val from = createZaakKoppelenData(koppelen = true)
            val to = createZaakKoppelenData(lezen = false)

            When("relatedZakenCanBeOntkoppeld is called") {
                val result = relatedZakenCanBeOntkoppeld(from, to)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }
})
