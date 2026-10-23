/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.brp.model.createAdres
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createVerblijfadresBinnenland
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.VerblijfadresBinnenland
import nl.info.client.brp.model.generated.VerblijfadresBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsOnbekend

class BrpExtensionFunctionsTest : BehaviorSpec({
    context("toAddressString") {
        given("A persoon with a complete verblijfadres in the Netherlands") {
            val persoon = createPersoon(
                verblijfplaats = createAdres(
                    verblijfAdresBinnenland = createVerblijfadresBinnenland(
                        officieleStraatnaam = "Kerkstraat",
                        huisnummer = 12,
                        postcode = "1234AB",
                        woonplaats = "Amsterdam"
                    ).apply {
                        huisletter = "A"
                        huisnummertoevoeging = "bis"
                    }
                )
            )

            `when`("the address string is created") {
                val addressString = persoon.toAddressString()

                then("it contains the street, house number, house letter, addition, postal code and city") {
                    addressString shouldBe "Kerkstraat 12Abis, 1234AB Amsterdam"
                }
            }
        }

        given("A persoon with a verblijfadres in the Netherlands without house number and postal code") {
            val persoon = createPersoon(
                verblijfplaats = createAdres(
                    verblijfAdresBinnenland = VerblijfadresBinnenland().apply {
                        officieleStraatnaam = "Kerkstraat"
                        woonplaats = "Amsterdam"
                    }
                )
            )

            `when`("the address string is created") {
                val addressString = persoon.toAddressString()

                then("it contains only the street and city, without extra spaces") {
                    addressString shouldBe "Kerkstraat, Amsterdam"
                }
            }
        }

        given("A persoon with a verblijfadres in the Netherlands with a blank house letter and postal code") {
            val persoon = createPersoon(
                verblijfplaats = createAdres(
                    verblijfAdresBinnenland = createVerblijfadresBinnenland(
                        officieleStraatnaam = "Kerkstraat",
                        huisnummer = 12,
                        postcode = " ",
                        woonplaats = "Amsterdam"
                    ).apply {
                        huisletter = " "
                    }
                )
            )

            `when`("the address string is created") {
                val addressString = persoon.toAddressString()

                then("the blank house letter and postal code are left out, without extra spaces") {
                    addressString shouldBe "Kerkstraat 12, Amsterdam"
                }
            }
        }

        given("A persoon with a verblijfadres in the Netherlands with only a postal code and city") {
            val persoon = createPersoon(
                verblijfplaats = createAdres(
                    verblijfAdresBinnenland = VerblijfadresBinnenland().apply {
                        postcode = "1234AB"
                        woonplaats = "Amsterdam"
                    }
                )
            )

            `when`("the address string is created") {
                val addressString = persoon.toAddressString()

                then("it contains only the postal code and city, without a leading separator") {
                    addressString shouldBe "1234AB Amsterdam"
                }
            }
        }

        given("A persoon with an Adres verblijfplaats without verblijfadres") {
            val persoon = createPersoon(verblijfplaats = Adres())

            `when`("the address string is created") {
                val addressString = persoon.toAddressString()

                then("it is empty") {
                    addressString shouldBe ""
                }
            }
        }

        given("A persoon with a verblijfplaats abroad") {
            val persoon = createPersoon(
                verblijfplaats = VerblijfplaatsBuitenland().apply {
                    verblijfadres = VerblijfadresBuitenland().apply {
                        regel1 = "Hauptstrasse 1"
                        regel2 = " "
                        regel3 = "10115 Berlin"
                    }
                }
            )

            `when`("the address string is created") {
                val addressString = persoon.toAddressString()

                then("it contains the non-blank address lines separated by a comma") {
                    addressString shouldBe "Hauptstrasse 1, 10115 Berlin"
                }
            }
        }

        given("A persoon with an unknown verblijfplaats") {
            val persoon = createPersoon(verblijfplaats = VerblijfplaatsOnbekend())

            `when`("the address string is created") {
                val addressString = persoon.toAddressString()

                then("it is empty") {
                    addressString shouldBe ""
                }
            }
        }
    }
})
