/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class ZaakLinkDataTest : BehaviorSpec({

    context("canBeRelated") {
        given("a source zaak with koppelen rights and a target zaak with lezen rights") {
            val from = createZaakLinkData(koppelen = true)
            val to = createZaakLinkData(lezen = true)

            `when`("canBeRelated is called") {
                val result = from.canBeRelatedTo(to)

                then("it should return true") {
                    result shouldBe true
                }
            }
        }

        given("a source zaak without koppelen rights") {
            val from = createZaakLinkData(koppelen = false)
            val to = createZaakLinkData(lezen = true)

            `when`("canBeRelated is called") {
                val result = from.canBeRelatedTo(to)

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("a target zaak without lezen rights") {
            val from = createZaakLinkData(koppelen = true)
            val to = createZaakLinkData(lezen = false)

            `when`("canBeRelated is called") {
                val result = from.canBeRelatedTo(to)

                then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    context("canBeHoofdAndDeelzaak") {
        val zaaktypeUUID = UUID.randomUUID()

        given("a valid hoofdzaak and deelzaak with matching zaaktype and both open") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID)

            `when`("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                then("it should return true") {
                    result shouldBe true
                }
            }
        }

        given("a hoofdzaak without koppelen rights") {
            val hoofdzaak = createZaakLinkData(koppelen = false)
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID)

            `when`("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("a hoofdzaak that is already a deelzaak") {
            val hoofdzaak = createZaakLinkData(isDeelzaak = true)
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID)

            `when`("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("a deelzaak without koppelen rights") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID, koppelen = false)

            `when`("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("a deelzaak that is already a hoofdzaak") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID, isHoofdzaak = true)

            `when`("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("a deelzaak that is already a deelzaak") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID, isDeelzaak = true)

            `when`("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("an open hoofdzaak and a closed deelzaak") {
            val hoofdzaak = createZaakLinkData(isOpen = true)
            val deelzaak = createZaakLinkData(zaaktypeUUID = zaaktypeUUID, isOpen = false)

            `when`("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("a deelzaak with a zaaktype not in the allowed set") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData()

            `when`("canBeHoofdAndDeelzaak is called") {
                val result = hoofdzaak.canBeHoofdzaakFor(deelzaak, setOf(zaaktypeUUID))

                then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    context("hoofdAndDeelzaakCanBeOntkoppeld") {
        given("both hoofdzaak and deelzaak have koppelen rights") {
            val hoofdzaak = createZaakLinkData(koppelen = true)
            val deelzaak = createZaakLinkData(koppelen = true)

            `when`("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdzaak.canBeUnlinkedFromDeelzaak(deelzaak)

                then("it should return true") {
                    result shouldBe true
                }
            }
        }

        given("the hoofdzaak does not have koppelen rights") {
            val hoofdzaak = createZaakLinkData(koppelen = false)
            val deelzaak = createZaakLinkData(koppelen = true)

            `when`("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdzaak.canBeUnlinkedFromDeelzaak(deelzaak)

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("the deelzaak does not have koppelen rights") {
            val hoofdzaak = createZaakLinkData(koppelen = true)
            val deelzaak = createZaakLinkData(koppelen = false)

            `when`("hoofdAndDeelzaakCanBeOntkoppeld is called") {
                val result = hoofdzaak.canBeUnlinkedFromDeelzaak(deelzaak)

                then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }

    context("relatedZakenCanBeOntkoppeld") {
        given("from zaak has koppelen rights and to zaak has lezen rights") {
            val from = createZaakLinkData(koppelen = true, lezen = true)
            val to = createZaakLinkData(lezen = true)

            `when`("relatedZakenCanBeOntkoppeld is called") {
                val result = from.canBeUnlinkedFromRelatedZaak(to)

                then("it should return true") {
                    result shouldBe true
                }
            }
        }

        given("from zaak does not have koppelen rights") {
            val from = createZaakLinkData(koppelen = false)
            val to = createZaakLinkData(lezen = true)

            `when`("relatedZakenCanBeOntkoppeld is called") {
                val result = from.canBeUnlinkedFromRelatedZaak(to)

                then("it should return false") {
                    result shouldBe false
                }
            }
        }

        given("to zaak does not have lezen rights") {
            val from = createZaakLinkData(koppelen = true)
            val to = createZaakLinkData(lezen = false)

            `when`("relatedZakenCanBeOntkoppeld is called") {
                val result = from.canBeUnlinkedFromRelatedZaak(to)

                then("it should return false") {
                    result shouldBe false
                }
            }
        }
    }
})
