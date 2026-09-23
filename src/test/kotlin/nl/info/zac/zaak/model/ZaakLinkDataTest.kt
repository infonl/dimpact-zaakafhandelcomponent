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
    context("statusNotLinkableReason") {
        given("a zaak that is open and a found zaak that is closed") {
            val zaak = createZaakLinkData(isOpen = true)
            val foundZaak = createZaakLinkData(isOpen = false)

            `when`("the status reason is determined") {
                val reason = zaak.statusNotLinkableReason(foundZaak)

                then("a closed zaak cannot be linked to an open zaak") {
                    reason shouldBe ZaakNotLinkableReason.FOUND_ZAAK_AFGEHANDELD
                }
            }
        }

        given("a zaak that is closed and a found zaak that is open") {
            val zaak = createZaakLinkData(isOpen = false)
            val foundZaak = createZaakLinkData(isOpen = true)

            `when`("the status reason is determined") {
                val reason = zaak.statusNotLinkableReason(foundZaak)

                then("an open zaak cannot be linked to a closed zaak") {
                    reason shouldBe ZaakNotLinkableReason.FOUND_ZAAK_OPEN
                }
            }
        }

        given("two zaken that are both open") {
            val zaak = createZaakLinkData(isOpen = true)
            val foundZaak = createZaakLinkData(isOpen = true)

            `when`("the status reason is determined") {
                val reason = zaak.statusNotLinkableReason(foundZaak)

                then("the status does not block the link") {
                    reason shouldBe null
                }
            }
        }

        given("two zaken that are both closed") {
            val zaak = createZaakLinkData(isOpen = false)
            val foundZaak = createZaakLinkData(isOpen = false)

            `when`("the status reason is determined") {
                val reason = zaak.statusNotLinkableReason(foundZaak)

                then("the status does not block the link") {
                    reason shouldBe null
                }
            }
        }
    }

    context("hoofdzaakDeelzaakNotLinkableReason") {
        val deelzaaktypeUUID = UUID.randomUUID()

        given("a hoofdzaak that is a deelzaak itself") {
            val hoofdzaak = createZaakLinkData(isDeelzaak = true)
            val deelzaak = createZaakLinkData(zaaktypeUUID = deelzaaktypeUUID)

            `when`("the reason is determined") {
                val reason = hoofdzaak.hoofdzaakDeelzaakNotLinkableReason(deelzaak, setOf(deelzaaktypeUUID))

                then("a deelzaak cannot be a hoofdzaak") {
                    reason shouldBe ZaakNotLinkableReason.FOUND_ZAAK_IS_DEELZAAK_SO_NO_HOOFDZAAK
                }
            }
        }

        given("a deelzaak that is already a deelzaak of another zaak") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = deelzaaktypeUUID, isDeelzaak = true)

            `when`("the reason is determined") {
                val reason = hoofdzaak.hoofdzaakDeelzaakNotLinkableReason(deelzaak, setOf(deelzaaktypeUUID))

                then("a zaak cannot be a deelzaak of two hoofdzaken") {
                    reason shouldBe ZaakNotLinkableReason.FOUND_ZAAK_ALREADY_DEELZAAK
                }
            }
        }

        given("a deelzaak that has deelzaken of its own") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = deelzaaktypeUUID, isHoofdzaak = true)

            `when`("the reason is determined") {
                val reason = hoofdzaak.hoofdzaakDeelzaakNotLinkableReason(deelzaak, setOf(deelzaaktypeUUID))

                then("a hoofdzaak cannot become a deelzaak") {
                    reason shouldBe ZaakNotLinkableReason.FOUND_ZAAK_HAS_DEELZAKEN
                }
            }
        }

        given("a deelzaak whose zaaktype is not allowed by the hoofdzaak zaaktype") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = UUID.randomUUID())

            `when`("the reason is determined") {
                val reason = hoofdzaak.hoofdzaakDeelzaakNotLinkableReason(deelzaak, setOf(deelzaaktypeUUID))

                then("the zaaktype blocks the link") {
                    reason shouldBe ZaakNotLinkableReason.ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK
                }
            }
        }

        given("a deelzaak the user has no koppelen rights on") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = deelzaaktypeUUID, koppelen = false)

            `when`("the reason is determined") {
                val reason = hoofdzaak.hoofdzaakDeelzaakNotLinkableReason(deelzaak, setOf(deelzaaktypeUUID))

                then("the missing koppelen right blocks the link") {
                    reason shouldBe ZaakNotLinkableReason.NO_KOPPELEN_RIGHT
                }
            }
        }

        given("a deelzaak that has deelzaken of its own and whose zaaktype is not allowed either") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = UUID.randomUUID(), isHoofdzaak = true)

            `when`("the reason is determined") {
                val reason = hoofdzaak.hoofdzaakDeelzaakNotLinkableReason(deelzaak, setOf(deelzaaktypeUUID))

                then("the relation structure reason is reported before the zaaktype reason") {
                    reason shouldBe ZaakNotLinkableReason.FOUND_ZAAK_HAS_DEELZAKEN
                }
            }
        }

        given("a deelzaak whose zaaktype is not allowed and on which the user has no koppelen rights either") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = UUID.randomUUID(), koppelen = false)

            `when`("the reason is determined") {
                val reason = hoofdzaak.hoofdzaakDeelzaakNotLinkableReason(deelzaak, setOf(deelzaaktypeUUID))

                then("the zaaktype reason is reported before the authorisation reason") {
                    reason shouldBe ZaakNotLinkableReason.ZAAKTYPE_DOES_NOT_ALLOW_DEELZAAK
                }
            }
        }

        given("a hoofdzaak and a deelzaak that can be linked") {
            val hoofdzaak = createZaakLinkData()
            val deelzaak = createZaakLinkData(zaaktypeUUID = deelzaaktypeUUID)

            `when`("the reason is determined") {
                val reason = hoofdzaak.hoofdzaakDeelzaakNotLinkableReason(deelzaak, setOf(deelzaaktypeUUID))

                then("nothing blocks the link") {
                    reason shouldBe null
                }
            }
        }
    }

    context("gerelateerdNotLinkableReason") {
        given("a found zaak the user cannot read") {
            val zaak = createZaakLinkData()
            val foundZaak = createZaakLinkData(lezen = false)

            `when`("the reason is determined") {
                val reason = zaak.gerelateerdNotLinkableReason(foundZaak)

                then("the missing lezen right blocks the link") {
                    reason shouldBe ZaakNotLinkableReason.NO_LEZEN_RIGHT
                }
            }
        }

        given("a closed found zaak of a zaaktype that is not an allowed deelzaaktype") {
            val zaak = createZaakLinkData(isOpen = true)
            val foundZaak = createZaakLinkData(isOpen = false, zaaktypeUUID = UUID.randomUUID())

            `when`("the reason is determined") {
                val reason = zaak.gerelateerdNotLinkableReason(foundZaak)

                then("neither the status nor the zaaktype blocks relating the zaken") {
                    reason shouldBe null
                }
            }
        }

        given("a found zaak the user can read but has no koppelen rights on") {
            val zaak = createZaakLinkData(koppelen = true)
            val foundZaak = createZaakLinkData(lezen = true, koppelen = false)

            `when`("the reason is determined") {
                val reason = zaak.gerelateerdNotLinkableReason(foundZaak)

                then("lezen rights on the found zaak are enough to relate it") {
                    reason shouldBe null
                }
            }
        }
    }
})
