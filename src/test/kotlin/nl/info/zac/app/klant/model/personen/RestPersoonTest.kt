/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.personen

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createPersoonBeperkt
import nl.info.client.brp.model.generated.AbstractDatum
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.Adressering
import nl.info.client.brp.model.generated.AdresseringBeperkt
import nl.info.client.brp.model.generated.OpschortingBijhouding
import nl.info.client.brp.model.generated.PersoonInOnderzoek
import nl.info.client.brp.model.generated.PersoonInOnderzoekBeperkt
import nl.info.client.brp.model.generated.RniDeelnemer
import nl.info.client.brp.model.generated.VerblijfadresBinnenland
import nl.info.client.brp.model.generated.VerblijfadresBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland
import nl.info.client.brp.model.generated.Waardetabel
import java.util.EnumSet

class RestPersoonTest : BehaviorSpec({

    given("BRP Persoon with all flags but MINISTERIELE_REGELING and EMIGRATION") {
        val date = AbstractDatum().apply {
            type = "type"
            langFormaat = "langFormaat"
        }
        val persoon = createPersoon(
            confidentialPersonalData = true,
            suspensionMaintenance = OpschortingBijhouding().apply {
                reden = Waardetabel().apply {
                    code = "O"
                    omschrijving = "overlijden"
                }
                datum = date
            },
            indicationCuratoriesRegister = true,
            personInResearch = PersoonInOnderzoek(),
            rniDeelnemerList = listOf(RniDeelnemer()),
            address = Adressering().apply { indicatieVastgesteldVerblijftNietOpAdres = true }
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            then("conversion is correct") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.complementOf(
                    EnumSet.of(
                        RestPersoonIndicaties.MINISTERIELE_REGELING,
                        RestPersoonIndicaties.EMIGRATIE
                    )
                )
            }
        }
    }

    given("BRP Persoon with MINISTERIELE_REGELING") {
        val date = AbstractDatum().apply {
            type = "type"
            langFormaat = "langFormaat"
        }
        val persoon = createPersoon(
            suspensionMaintenance = OpschortingBijhouding().apply {
                reden = Waardetabel().apply {
                    code = "M"
                    omschrijving = "ministerieel besluit"
                }
                datum = date
            },
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            then("conversion is correct") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.of(
                    RestPersoonIndicaties.OPSCHORTING_BIJHOUDING,
                    RestPersoonIndicaties.MINISTERIELE_REGELING
                )
            }
        }
    }

    given("BRP Persoon with EMIGRATION") {
        val date = AbstractDatum().apply {
            type = "type"
            langFormaat = "langFormaat"
        }
        val persoon = createPersoon(
            suspensionMaintenance = OpschortingBijhouding().apply {
                reden = Waardetabel().apply {
                    code = "E"
                    omschrijving = "emigratie"
                }
                datum = date
            },
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            then("conversion is correct") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.of(
                    RestPersoonIndicaties.OPSCHORTING_BIJHOUDING,
                    RestPersoonIndicaties.EMIGRATIE
                )
            }
        }
    }

    given("BRP Persoon with domestic address") {
        val persoon = createPersoon(
            verblijfplaats = Adres().apply {
                verblijfadres = VerblijfadresBinnenland().apply {
                    officieleStraatnaam = "street name"
                    huisnummer = 11
                    huisnummertoevoeging = "number description"
                    huisletter = "a"
                }
            }
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            then("conversion contains the address details, separated with NBSP") {
                restPersoon.verblijfplaats shouldBe "street\u00A0name\u00A011\u00A0number\u00A0description\u00A0a"
            }
        }
    }

    given("BRP Persoon with foreign address") {
        val persoon = createPersoon(
            verblijfplaats = VerblijfplaatsBuitenland().apply {
                verblijfadres = VerblijfadresBuitenland().apply {
                    regel1 = "first line"
                    land = Waardetabel().apply {
                        code = "Galaxy"
                        omschrijving = "Far away"
                    }
                }
            }
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            then("conversion contains the address details, separated with NBSP") {
                restPersoon.verblijfplaats shouldBe "first\u00A0line,\u00A0Far\u00A0away"
            }
        }
    }

    given("BRP PersoonBeperkt with all flags but OVERLEDEN and EMIGRATION") {
        val date = AbstractDatum().apply {
            type = "type"
            langFormaat = "langFormaat"
        }
        val persoonBeperkt = createPersoonBeperkt(
            confidentialPersonalData = true,
            suspensionMaintenance = OpschortingBijhouding().apply {
                reden = Waardetabel().apply {
                    code = "M"
                    omschrijving = "ministerieel besluit"
                }
                datum = date
            },
            personInResearch = PersoonInOnderzoekBeperkt(),
            rniDeelnemerList = listOf(RniDeelnemer()),
            address = AdresseringBeperkt().apply { indicatieVastgesteldVerblijftNietOpAdres = true }
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoonBeperkt.toRestPersoon()

            then("conversion is correct") {
                restPersoon.bsn shouldBe persoonBeperkt.burgerservicenummer
                // check for all but OVERLIJDEN and EMIGRATION
                restPersoon.indicaties shouldBe EnumSet.complementOf(
                    EnumSet.of(
                        RestPersoonIndicaties.ONDER_CURATELE,
                        RestPersoonIndicaties.OVERLEDEN,
                        RestPersoonIndicaties.EMIGRATIE
                    )
                )
            }
        }
    }

    given("BRP PersoonBeperkt with MINISTERIELE_REGELING") {
        val date = AbstractDatum().apply {
            type = "type"
            langFormaat = "langFormaat"
        }
        val persoonBeperkt = createPersoonBeperkt(
            suspensionMaintenance = OpschortingBijhouding().apply {
                reden = Waardetabel().apply {
                    code = "M"
                    omschrijving = "ministerieel besluit"
                }
                datum = date
            },
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoonBeperkt.toRestPersoon()

            then("conversion is correct") {
                restPersoon.bsn shouldBe persoonBeperkt.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.of(
                    RestPersoonIndicaties.OPSCHORTING_BIJHOUDING,
                    RestPersoonIndicaties.MINISTERIELE_REGELING
                )
            }
        }
    }

    given("BRP PersoonBeperkt with EMIGRATION") {
        val date = AbstractDatum().apply {
            type = "type"
            langFormaat = "langFormaat"
        }
        val persoonBeperkt = createPersoonBeperkt(
            suspensionMaintenance = OpschortingBijhouding().apply {
                reden = Waardetabel().apply {
                    code = "E"
                    omschrijving = "emigratie"
                }
                datum = date
            },
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoonBeperkt.toRestPersoon()

            then("conversion is correct") {
                restPersoon.bsn shouldBe persoonBeperkt.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.of(
                    RestPersoonIndicaties.OPSCHORTING_BIJHOUDING,
                    RestPersoonIndicaties.EMIGRATIE
                )
            }
        }
    }

    given("BRP Persoon that's in research and has confidential personal data") {
        val persoon = createPersoon(
            confidentialPersonalData = true,
            personInResearch = PersoonInOnderzoek(),
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            then("conversion is correct") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.of(
                    RestPersoonIndicaties.IN_ONDERZOEK,
                    RestPersoonIndicaties.GEHEIMHOUDING_OP_PERSOONSGEGEVENS
                )
            }
        }
    }

    given("BRP PersoonBeperkt that's in research and has confidential personal data") {
        val persoonBeperkt = createPersoonBeperkt(
            confidentialPersonalData = true,
            personInResearch = PersoonInOnderzoekBeperkt(),
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoonBeperkt.toRestPersoon()

            then("conversion is correct") {
                restPersoon.bsn shouldBe persoonBeperkt.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.of(
                    RestPersoonIndicaties.IN_ONDERZOEK,
                    RestPersoonIndicaties.GEHEIMHOUDING_OP_PERSOONSGEGEVENS
                )
            }
        }
    }

    given("BRP Persoon that has no indication relative data") {
        val persoon = createPersoon()

        `when`("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            then("conversion yields no indications") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.noneOf(RestPersoonIndicaties::class.java)
            }
        }
    }

    given("BRP PersoonBeperkt that has no indication relative data") {
        val persoonBeperkt = createPersoonBeperkt()

        `when`("converted to RestPersoon") {
            val restPersoon = persoonBeperkt.toRestPersoon()

            then("conversion yields no indications") {
                restPersoon.bsn shouldBe persoonBeperkt.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.noneOf(RestPersoonIndicaties::class.java)
            }
        }
    }

    given("BRP PersoonBeperkt with foreign address") {
        val persoon = createPersoonBeperkt(
            address = AdresseringBeperkt().apply {
                adresregel1 = "first line"
                land = Waardetabel().apply {
                    code = "Galaxy"
                    omschrijving = "Far away"
                }
            }
        )

        `when`("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            then("conversion contains the country description") {
                restPersoon.verblijfplaats shouldBe "first\u00A0line,\u00A0Far\u00A0away"
            }
        }
    }
})
