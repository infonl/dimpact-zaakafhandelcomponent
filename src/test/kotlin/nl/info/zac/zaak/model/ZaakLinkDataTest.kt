/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class ZaakLinkDataTest : BehaviorSpec({

    Context("canBeRelated") {
        Given("a source zaak with koppelen rights and a target zaak with lezen rights") {
            val from = createZaakLinkData(koppelen = true)
            val to = createZaakLinkData(lezen = true)

            When("canBeRelated is called") {
                val result = from.canBeRelatedTo(to)

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("a source zaak without koppelen rights") {
            val from = createZaakLinkData(koppelen = false)
            val to = createZaakLinkData(lezen = true)

            When("canBeRelated is called") {
                val result = from.canBeRelatedTo(to)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a target zaak without lezen rights") {
            val from = createZaakLinkData(koppelen = true)
            val to = createZaakLinkData(lezen = false)

            When("canBeRelated is called") {
                val result = from.canBeRelatedTo(to)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("canBeHoofdAndDeelzaak") {
        val zaaktypeUUID = UUID.randomUUID()

        Given("a valid hoofdzaak and deelzaak with matching zaaktype and both open") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID)

            When("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("a hoofdzaak without koppelen rights") {
            val hoofdzaak = createZaakLinkData(koppelen = false)
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID)

            When("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a hoofdzaak that is already a deelzaak") {
            val hoofdzaak = createZaakLinkData(isDeelzaak = true)
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID)

            When("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a deelzaak without koppelen rights") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID, koppelen = false)

            When("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a deelzaak that is already a hoofdzaak") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID, isHoofdzaak = true)

            When("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a deelzaak that is already a deelzaak") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID, isDeelzaak = true)

            When("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("an open hoofdzaak and a closed deelzaak") {
            val hoofdzaak = createZaakLinkData(isOpen = true)
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID, isOpen = false)

            When("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("a deelzaak with a zaaktype not in the allowed set") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData()

            When("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("hoofdAndDeelzaakCanBeOntkoppeld") {
        Given("both hoofdzaak and deelzaak have koppelen rights") {
            val hoofdzaak = createZaakLinkData(koppelen = true)
            val deelzaak = createZaakLinkData(koppelen = true)

            When("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdzaak.canBeUnlinkedFromDeelzaak(deelzaak)

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("the hoofdzaak does not have koppelen rights") {
            val hoofdzaak = createZaakLinkData(koppelen = false)
            val deelzaak = createZaakLinkData(koppelen = true)

            When("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdzaak.canBeUnlinkedFromDeelzaak(deelzaak)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("the deelzaak does not have koppelen rights") {
            val hoofdzaak = createZaakLinkData(koppelen = true)
            val deelzaak = createZaakLinkData(koppelen = false)

            When("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdzaak.canBeUnlinkedFromDeelzaak(deelzaak)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    Context("relatedZakenCanBeOntkoppeld") {
        Given("from zaak has koppelen rights and to zaak has lezen rights") {
            val from = createZaakLinkData(koppelen = true, lezen = true)
            val to = createZaakLinkData(lezen = true)

            When("relatedZakenCanBeOntkoppeld is called") {
                val result = from.canBeUnlinkedFromRelatedZaak(to)

                Then("it should return true") {
                    result shouldBe true
                }
            }
        }

        Given("from zaak does not have koppelen rights") {
            val from = createZaakLinkData(koppelen = false)
            val to = createZaakLinkData(lezen = true)

            When("relatedZakenCanBeOntkoppeld is called") {
                val result = from.canBeUnlinkedFromRelatedZaak(to)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }

        Given("to zaak does not have lezen rights") {
            val from = createZaakLinkData(koppelen = true)
            val to = createZaakLinkData(lezen = false)

            When("relatedZakenCanBeOntkoppeld is called") {
                val result = from.canBeUnlinkedFromRelatedZaak(to)

                Then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }
})
