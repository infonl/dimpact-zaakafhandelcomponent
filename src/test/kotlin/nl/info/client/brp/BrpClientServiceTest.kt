/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.NotFoundException
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummerResponse
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.admin.model.ZaaktypeCmmnBrpParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import java.util.Optional

const val ZAAK = "ZAAK-2000-00002"
const val QUERY_PERSONEN_PURPOSE = "testQueryPurpose"
const val RETRIEVE_PERSOON_PURPOSE = "testRetrievePurpose"
const val DEFAULT_PROCESSING_VALUE = "General"
const val ICONNECT_PROVIDER = "iConnect"

class BrpClientServiceTest : BehaviorSpec({
    val personenApi: PersonenApi = mockk<PersonenApi>()
    val zrcClientService: ZrcClientService = mockk()
    val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService = mockk()

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("iConnect audit log provider") {
        val configuredBrpClientService = BrpClientService(
            personenApi = personenApi,
            queryPersonenDefaultPurpose = Optional.of(QUERY_PERSONEN_PURPOSE),
            retrievePersoonDefaultPurpose = Optional.of(RETRIEVE_PERSOON_PURPOSE),
            processingRegisterDefault = Optional.of(DEFAULT_PROCESSING_VALUE),
            auditLogProvider = Optional.of(ICONNECT_PROVIDER),
            zrcClientService = zrcClientService,
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
        )

        Given("A person for a given BSN") {
            val bsn = "123456789"
            val person = createPersoon(
                bsn = bsn
            )
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )
            val zaak = createZaak()
            val retrievePersoonPurpose = "raadpleegWaarde"
            val processingValue = "Leerplicht"
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                    raadpleegWaarde = retrievePersoonPurpose
                    verwerkingsregisterWaarde = processingValue
                }
            )

            every {
                zrcClientService.readZaakByID(ZAAK)
            } returns zaak
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
            } returns zaaktypeCmmnConfiguration
            every {
                personenApi.personen(
                    personenQuery = any(),
                    purpose = eq(retrievePersoonPurpose),
                    auditEvent = eq("$processingValue@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}")
                )
            } returns raadpleegMetBurgerservicenummerResponse

            When("find person is called with the BSN of the person") {
                val personResponse = configuredBrpClientService.retrievePersoon(bsn, ZAAK)

                Then("it should return the person") {
                    personResponse shouldBe person
                }
            }
        }

        Given("No person for a given BSN") {
            every {
                personenApi.personen(any(), RETRIEVE_PERSOON_PURPOSE, DEFAULT_PROCESSING_VALUE)
            } returns createRaadpleegMetBurgerservicenummerResponse(persons = emptyList())

            When("find person is called with the BSN of the person") {
                val personResponse = configuredBrpClientService.retrievePersoon("123456789", ZAAK)

                Then("it should return null") {
                    personResponse shouldBe null
                }
            }
        }

        Given("Multiple persons for a given BSN") {
            val persons = listOf(
                createPersoon(bsn = "123456789"),
                createPersoon(bsn = "123456789")
            )
            every {
                personenApi.personen(
                    personenQuery = any(),
                    purpose = RETRIEVE_PERSOON_PURPOSE,
                    auditEvent = DEFAULT_PROCESSING_VALUE
                )
            } returns createRaadpleegMetBurgerservicenummerResponse(persons = persons)

            When("find person is called with the BSN of the person") {
                val personResponse = configuredBrpClientService.retrievePersoon("123456789", ZAAK)

                Then("it should return the first person") {
                    personResponse shouldBe persons[0]
                }
            }
        }

        Given("Another person for a given BSN") {
            val bsn = "123456789"
            val person = createPersoon(
                bsn = bsn
            )
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )
            val zaak = createZaak()
            val queryPersonenPurpose = "zoekWaarde"
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                    zoekWaarde = queryPersonenPurpose
                    verwerkingsregisterWaarde = "Leerplicht"
                }
            )

            every {
                zrcClientService.readZaakByID(ZAAK)
            } returns zaak
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
            } returns zaaktypeCmmnConfiguration
            every {
                personenApi.personen(
                    personenQuery = any(),
                    purpose = eq(queryPersonenPurpose),
                    auditEvent = eq("Leerplicht@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}")
                )
            } returns raadpleegMetBurgerservicenummerResponse

            When("a query is run on personen for this BSN") {
                val personResponse = configuredBrpClientService.queryPersonen(
                    createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                    ZAAK
                )

                Then("it should return the person") {
                    personResponse shouldBe raadpleegMetBurgerservicenummerResponse
                }
            }
        }

        Given("No purpose is configured for BRP search") {
            val bsn = "123456789"
            val person = createPersoon(
                bsn = bsn
            )
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )
            val brpClientService = BrpClientService(
                personenApi = personenApi,
                queryPersonenDefaultPurpose = Optional.empty(),
                retrievePersoonDefaultPurpose = Optional.of(RETRIEVE_PERSOON_PURPOSE),
                processingRegisterDefault = Optional.of(DEFAULT_PROCESSING_VALUE),
                zrcClientService = zrcClientService,
                auditLogProvider = Optional.of(ICONNECT_PROVIDER),
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
            )

            every {
                personenApi.personen(personenQuery = any(), purpose = null, auditEvent = DEFAULT_PROCESSING_VALUE)
            } returns raadpleegMetBurgerservicenummerResponse

            When("queryPersonen is called") {
                val personResponse = brpClientService.queryPersonen(
                    createRaadpleegMetBurgerservicenummer(listOf(bsn))
                )

                Then("it should return the person") {
                    personResponse shouldBe raadpleegMetBurgerservicenummerResponse
                }
            }
        }

        Given("No purpose is configured for BRP person retrieval") {
            val bsn = "123456789"
            val person = createPersoon(
                bsn = bsn
            )
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )
            val brpClientService = BrpClientService(
                personenApi = personenApi,
                queryPersonenDefaultPurpose = Optional.of(QUERY_PERSONEN_PURPOSE),
                retrievePersoonDefaultPurpose = Optional.empty(),
                processingRegisterDefault = Optional.of(DEFAULT_PROCESSING_VALUE),
                zrcClientService = zrcClientService,
                auditLogProvider = Optional.of(ICONNECT_PROVIDER),
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
            )

            every {
                personenApi.personen(any(), null, DEFAULT_PROCESSING_VALUE)
            } returns raadpleegMetBurgerservicenummerResponse

            When("find person is called with the BSN of the person") {
                val personResponse = brpClientService.retrievePersoon(bsn, ZAAK)

                Then("it should return the person") {
                    personResponse shouldBe person
                }
            }
        }

        Given("No processing value configured for BRP person retrieval") {
            val bsn = "123456789"
            val person = createPersoon(
                bsn = bsn
            )
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )
            val brpClientService = BrpClientService(
                personenApi = personenApi,
                queryPersonenDefaultPurpose = Optional.of(QUERY_PERSONEN_PURPOSE),
                retrievePersoonDefaultPurpose = Optional.of(RETRIEVE_PERSOON_PURPOSE),
                processingRegisterDefault = Optional.empty(),
                auditLogProvider = Optional.of(ICONNECT_PROVIDER),
                zrcClientService = zrcClientService,
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
            )

            every {
                personenApi.personen(personenQuery = any(), purpose = RETRIEVE_PERSOON_PURPOSE, auditEvent = null)
            } returns raadpleegMetBurgerservicenummerResponse

            When("find person is called with the BSN of the person") {
                val personResponse = brpClientService.retrievePersoon(bsn, ZAAK)

                Then("it should return the person") {
                    personResponse shouldBe person
                }
            }
        }

        Given("Unicode processing value configured for BRP person retrieval") {
            val bsn = "123456789"
            val person = createPersoon(
                bsn = bsn
            )
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )
            val brpClientService = BrpClientService(
                personenApi = personenApi,
                queryPersonenDefaultPurpose = Optional.of(QUERY_PERSONEN_PURPOSE),
                retrievePersoonDefaultPurpose = Optional.of(RETRIEVE_PERSOON_PURPOSE),
                processingRegisterDefault = Optional.of(DEFAULT_PROCESSING_VALUE),
                auditLogProvider = Optional.of(ICONNECT_PROVIDER),
                zrcClientService = zrcClientService,
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
            )
            val zaak = createZaak()
            val retrievePersoonPurpose = "raadpleegWaarde"
            val processingValue = "Bíj́na"
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                    raadpleegWaarde = retrievePersoonPurpose
                    verwerkingsregisterWaarde = processingValue
                }
            )

            every {
                zrcClientService.readZaakByID(ZAAK)
            } returns zaak
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
            } returns zaaktypeCmmnConfiguration
            every {
                // Since we have a processing value in Unicode, the default value is used instead
                personenApi.personen(
                    personenQuery = any(),
                    purpose = retrievePersoonPurpose,
                    auditEvent = "$DEFAULT_PROCESSING_VALUE@fakeZaaktypeOmschrijving"
                )
            } returns raadpleegMetBurgerservicenummerResponse

            When("find person is called with the BSN of the person") {
                val personResponse = brpClientService.retrievePersoon(bsn, ZAAK)

                Then("it should still return the person") {
                    personResponse shouldBe person
                }
            }
        }

        Given("A person exists for a given BSN") {
            val bsn = "123456789"
            val person = createPersoon(
                bsn = bsn
            )
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )

            every {
                zrcClientService.readZaakByID(ZAAK)
            } throws NotFoundException("Zaak not found")

            every {
                personenApi.personen(
                    personenQuery = any(),
                    purpose = eq(RETRIEVE_PERSOON_PURPOSE),
                    auditEvent = eq(DEFAULT_PROCESSING_VALUE)
                )
            } returns raadpleegMetBurgerservicenummerResponse

            When("no zaak is found for the given audit event and retrieve persoon is called") {
                val personResponse = configuredBrpClientService.retrievePersoon(bsn, ZAAK)

                Then("retrieving a person should still work") {
                    personResponse shouldBe person
                }
            }
        }
    }

    Context("2Secure audit log provider") {
        val configuredBrpClientService = BrpClientService(
            personenApi = personenApi,
            queryPersonenDefaultPurpose = Optional.of(QUERY_PERSONEN_PURPOSE),
            retrievePersoonDefaultPurpose = Optional.of(RETRIEVE_PERSOON_PURPOSE),
            processingRegisterDefault = Optional.of(DEFAULT_PROCESSING_VALUE),
            auditLogProvider = Optional.of("2Secure"),
            zrcClientService = zrcClientService,
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
        )

        Given("A person for a given BSN") {
            val bsn = "123456789"
            val person = createPersoon(
                bsn = bsn
            )
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )
            val zaak = createZaak()
            val retrievePersoonPurpose = "raadpleegWaarde"
            val processingValue = "Leerplicht"
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
                zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                    raadpleegWaarde = retrievePersoonPurpose
                    verwerkingsregisterWaarde = processingValue
                }
            )

            every {
                zrcClientService.readZaakByID(ZAAK)
            } returns zaak
            every {
                zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
            } returns zaaktypeCmmnConfiguration
            every {
                personenApi.personen(
                    personenQuery = any(),
                    recipient = eq("$processingValue@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}")
                )
            } returns raadpleegMetBurgerservicenummerResponse

            When("find person is called with the BSN of the person") {
                val personResponse = configuredBrpClientService.retrievePersoon(bsn, ZAAK)

                Then("it should return the person") {
                    personResponse shouldBe person
                }
            }
        }
    }
})
