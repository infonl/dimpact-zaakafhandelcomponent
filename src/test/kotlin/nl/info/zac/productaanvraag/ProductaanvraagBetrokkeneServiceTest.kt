/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.kvk.model.createRandomKvkNumber
import nl.info.client.kvk.model.createRandomVestigingsNumber
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.productaanvraag.model.createProductaanvraagDimpact
import nl.info.zac.productaanvraag.model.generated.Betrokkene

class ProductaanvraagBetrokkeneServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val productaanvraagBetrokkeneService = ProductaanvraagBetrokkeneService(
        ztcClientService = ztcClientService,
        zrcClientService = zrcClientService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Adding the initiator and the other betrokkenen of a productaanvraag to a zaak") {
        val zaak = createZaak()

        given(
            """
            an initiator betrokkene with a BSN and the roltype omschrijving 'Initiator', followed by a second
            initiator betrokkene with a KVK number and the generic role initiator, with BRP and KVK enabled
            """
        ) {
            clearAllMocks()
            val initiatorBetrokkene = Betrokkene().apply {
                inpBsn = "fakeBsn"
                roltypeOmschrijving = "Initiator"
            }
            val secondInitiatorBetrokkene = Betrokkene().apply {
                kvkNummer = createRandomKvkNumber()
                rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.INITIATOR
            }
            val productaanvraagDimpact = createProductaanvraagDimpact(
                betrokkenen = listOf(initiatorBetrokkene, secondInitiatorBetrokkene)
            )
            val rolTypeInitiator = createRolType(zaakTypeUri = zaak.zaaktype)
            val createdRoles = mutableListOf<Rol<*>>()
            every { ztcClientService.findRoltypen(zaak.zaaktype, "Initiator") } returns listOf(rolTypeInitiator)
            every { zrcClientService.createRol(capture(createdRoles)) } returns mockk()

            `when`("the initiator and betrokkenen are added to the zaak") {
                val addedInitiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = true
                )

                then("only a 'natuurlijk persoon' initiator role is created for the first initiator") {
                    createdRoles shouldHaveSize 1
                    with(createdRoles.first()) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NATUURLIJK_PERSOON
                        identificatienummer shouldBe "fakeBsn"
                        roltype shouldBe rolTypeInitiator.url
                        roltoelichting shouldBe "Overgenomen vanuit de product aanvraag"
                        this.zaak shouldBe zaak.url
                    }
                }

                and("the roltypen of the second initiator are not looked up") {
                    verify(exactly = 0) {
                        ztcClientService.findRoltypen(any(), any<OmschrijvingGeneriekEnum>())
                    }
                }

                and("the first initiator betrokkene is returned") {
                    addedInitiator shouldBeSameInstanceAs initiatorBetrokkene
                }
            }
        }

        given(
            """
            an initiator betrokkene with a KVK number, a vestigingsnummer and the generic role initiator,
            with KVK enabled
            """
        ) {
            clearAllMocks()
            val kvkNummer = createRandomKvkNumber()
            val vestigingsNummer = createRandomVestigingsNumber()
            val initiatorBetrokkene = Betrokkene().apply {
                this.kvkNummer = kvkNummer
                this.vestigingsNummer = vestigingsNummer
                rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.INITIATOR
            }
            val productaanvraagDimpact = createProductaanvraagDimpact(betrokkenen = listOf(initiatorBetrokkene))
            val rolTypeInitiator = createRolType(zaakTypeUri = zaak.zaaktype)
            val createdRoles = mutableListOf<Rol<*>>()
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
            } returns listOf(rolTypeInitiator)
            every { zrcClientService.createRol(capture(createdRoles)) } returns mockk()

            `when`("the initiator and betrokkenen are added to the zaak") {
                val addedInitiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = false,
                    kvkEnabled = true
                )

                then("a 'niet natuurlijk persoon' initiator role with the KVK number and vestigingsnummer is created") {
                    createdRoles shouldHaveSize 1
                    with(createdRoles.first().shouldBeInstanceOf<RolNietNatuurlijkPersoon>()) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        betrokkeneIdentificatie?.kvkNummer shouldBe kvkNummer
                        betrokkeneIdentificatie?.vestigingsNummer shouldBe vestigingsNummer
                        roltype shouldBe rolTypeInitiator.url
                        this.zaak shouldBe zaak.url
                    }
                }

                and("the initiator betrokkene is returned") {
                    addedInitiator shouldBeSameInstanceAs initiatorBetrokkene
                }
            }
        }

        given(
            """
            an initiator betrokkene with a BSN and a belanghebbende betrokkene with a KVK number and a vestigingsnummer,
            while the BRP and KVK koppelingen of the zaaktype are disabled
            """
        ) {
            clearAllMocks()
            val productaanvraagDimpact = createProductaanvraagDimpact(
                betrokkenen = listOf(
                    Betrokkene().apply {
                        inpBsn = "fakeBsn"
                        roltypeOmschrijving = "Initiator"
                    },
                    Betrokkene().apply {
                        kvkNummer = createRandomKvkNumber()
                        vestigingsNummer = createRandomVestigingsNumber()
                        roltypeOmschrijving = "Belanghebbende"
                    }
                )
            )

            `when`("the initiator and betrokkenen are added to the zaak") {
                val addedInitiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = false,
                    kvkEnabled = false
                )

                then("no roltypen are looked up and no betrokkene roles are created") {
                    verify(exactly = 0) {
                        ztcClientService.findRoltypen(any(), any<String>())
                        zrcClientService.createRol(any())
                    }
                }

                and("no initiator is returned") {
                    addedInitiator shouldBe null
                }
            }
        }

        given(
            """
            an initiator betrokkene with an invalid KVK number and the generic role initiator,
            with BRP disabled and KVK enabled
            """
        ) {
            clearAllMocks()
            val initiatorBetrokkene = Betrokkene().apply {
                kvkNummer = "123456"
                rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.INITIATOR
            }
            val productaanvraagDimpact = createProductaanvraagDimpact(betrokkenen = listOf(initiatorBetrokkene))
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
            } returns listOf(createRolType(zaakTypeUri = zaak.zaaktype))

            `when`("the initiator and betrokkenen are added to the zaak") {
                val addedInitiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = false,
                    kvkEnabled = true
                )

                then("no role is created, because the KVK number is invalid") {
                    verify(exactly = 0) {
                        zrcClientService.createRol(any())
                    }
                }

                and("the initiator betrokkene is still returned as the initiator") {
                    addedInitiator shouldBeSameInstanceAs initiatorBetrokkene
                }
            }
        }

        given(
            """
            an initiator betrokkene with a valid KVK number but an invalid vestigingsnummer and the generic role
            initiator, with KVK enabled
            """
        ) {
            clearAllMocks()
            val productaanvraagDimpact = createProductaanvraagDimpact(
                betrokkenen = listOf(
                    Betrokkene().apply {
                        kvkNummer = createRandomKvkNumber()
                        vestigingsNummer = "fakeInvalidVestigingsNummer"
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.INITIATOR
                    }
                )
            )
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
            } returns listOf(createRolType(zaakTypeUri = zaak.zaaktype))

            `when`("the initiator and betrokkenen are added to the zaak") {
                productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = true
                )

                then("no role is created, because the vestigingsnummer is invalid") {
                    verify(exactly = 0) {
                        zrcClientService.createRol(any())
                    }
                }
            }
        }

        given("an initiator betrokkene with a BSN and the generic role initiator that the zaaktype does not support") {
            clearAllMocks()
            val initiatorBetrokkene = Betrokkene().apply {
                inpBsn = "fakeBsn"
                rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.INITIATOR
            }
            val productaanvraagDimpact = createProductaanvraagDimpact(betrokkenen = listOf(initiatorBetrokkene))
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.INITIATOR)
            } returns emptyList()

            `when`("the initiator and betrokkenen are added to the zaak") {
                val addedInitiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = true
                )

                then("no role is created") {
                    verify(exactly = 0) {
                        zrcClientService.createRol(any())
                    }
                }

                and("the initiator betrokkene is still returned as the initiator") {
                    addedInitiator shouldBeSameInstanceAs initiatorBetrokkene
                }
            }
        }

        given("an initiator betrokkene with the generic role initiator but without a BSN or a KVK number") {
            clearAllMocks()
            val productaanvraagDimpact = createProductaanvraagDimpact(
                betrokkenen = listOf(
                    Betrokkene().apply {
                        innNnpId = "fakeInnNnpId"
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.INITIATOR
                    }
                )
            )

            `when`("the initiator and betrokkenen are added to the zaak") {
                val addedInitiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = true
                )

                then("no roltypen are looked up and no role is created") {
                    verify(exactly = 0) {
                        ztcClientService.findRoltypen(any(), any<OmschrijvingGeneriekEnum>())
                        zrcClientService.createRol(any())
                    }
                }

                and("no initiator is returned") {
                    addedInitiator shouldBe null
                }
            }
        }

        given("a productaanvraag without betrokkenen") {
            clearAllMocks()
            val productaanvraagDimpact = createProductaanvraagDimpact().apply { betrokkenen = null }

            `when`("the initiator and betrokkenen are added to the zaak") {
                val addedInitiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = true
                )

                then("no roles are created and no initiator is returned") {
                    verify(exactly = 0) {
                        zrcClientService.createRol(any())
                    }
                    addedInitiator shouldBe null
                }
            }
        }

        given(
            """
            a list of betrokkenen of all supported role types, including behandelaar betrokkenen and a betrokkene with
            a roltype omschrijving that the zaaktype does not define, but no initiator, where the zaaktype defines no
            roltype for adviseur and two roltypen for klantcontacter
            """
        ) {
            clearAllMocks()
            val belanghebbendeKvkNummer1 = createRandomKvkNumber()
            val belanghebbendeKvkNummer2 = createRandomKvkNumber()
            val beslisserKvkNummer = createRandomKvkNumber()
            val zaakcoordinatorKvkNummer = createRandomKvkNumber()
            val productaanvraagDimpact = createProductaanvraagDimpact(
                betrokkenen = listOf(
                    Betrokkene().apply {
                        inpBsn = "fakeAdviseurBsn"
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.ADVISEUR
                    },
                    Betrokkene().apply {
                        inpBsn = "fakeBehandelaarBsn"
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.BEHANDELAAR
                    },
                    Betrokkene().apply {
                        inpBsn = "fakeBehandelaarBsn"
                        roltypeOmschrijving = "Behandelaar"
                    },
                    Betrokkene().apply {
                        kvkNummer = belanghebbendeKvkNummer1
                        roltypeOmschrijving = "Belanghebbende"
                    },
                    Betrokkene().apply {
                        kvkNummer = belanghebbendeKvkNummer2
                        roltypeOmschrijving = "Belanghebbende"
                    },
                    Betrokkene().apply {
                        inpBsn = "fakeBeslisserBsn"
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.BESLISSER
                    },
                    Betrokkene().apply {
                        kvkNummer = beslisserKvkNummer
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.BESLISSER
                    },
                    Betrokkene().apply {
                        inpBsn = "fakeKlantcontacterBsn"
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.KLANTCONTACTER
                    },
                    Betrokkene().apply {
                        inpBsn = "fakeMedeInitiatorBsn"
                        roltypeOmschrijving = "Medeaanvrager"
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.MEDE_INITIATOR
                    },
                    Betrokkene().apply {
                        kvkNummer = zaakcoordinatorKvkNummer
                        rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.ZAAKCOORDINATOR
                    },
                    Betrokkene().apply {
                        inpBsn = "fakeUnknownRoltypeBsn"
                        roltypeOmschrijving = "fakeUnknownRoltypeOmschrijving"
                    }
                )
            )
            val rolTypeBelanghebbende = createRolType(
                zaakTypeUri = zaak.zaaktype,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BELANGHEBBENDE
            )
            val rolTypeBeslisser = createRolType(
                zaakTypeUri = zaak.zaaktype,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.BESLISSER
            )
            val rolTypeKlantcontacter1 = createRolType(
                zaakTypeUri = zaak.zaaktype,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.KLANTCONTACTER
            )
            val rolTypeKlantcontacter2 = createRolType(
                zaakTypeUri = zaak.zaaktype,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.KLANTCONTACTER
            )
            val rolTypeMedeInitiator = createRolType(
                zaakTypeUri = zaak.zaaktype,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.MEDE_INITIATOR
            )
            val rolTypeZaakcoordinator = createRolType(
                zaakTypeUri = zaak.zaaktype,
                omschrijvingGeneriek = OmschrijvingGeneriekEnum.ZAAKCOORDINATOR
            )
            val createdRoles = mutableListOf<Rol<*>>()
            every { ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.ADVISEUR) } returns emptyList()
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, "Belanghebbende")
            } returns listOf(rolTypeBelanghebbende)
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.BESLISSER)
            } returns listOf(rolTypeBeslisser)
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.KLANTCONTACTER)
            } returns listOf(rolTypeKlantcontacter1, rolTypeKlantcontacter2)
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, "Medeaanvrager")
            } returns listOf(rolTypeMedeInitiator)
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, OmschrijvingGeneriekEnum.ZAAKCOORDINATOR)
            } returns listOf(rolTypeZaakcoordinator)
            every {
                ztcClientService.findRoltypen(zaak.zaaktype, "fakeUnknownRoltypeOmschrijving")
            } returns emptyList()
            every { zrcClientService.createRol(capture(createdRoles)) } returns mockk()

            `when`("the initiator and betrokkenen are added to the zaak") {
                val addedInitiator = productaanvraagBetrokkeneService.addInitiatorAndBetrokkenenToZaak(
                    productaanvraag = productaanvraagDimpact,
                    zaak = zaak,
                    brpEnabled = true,
                    kvkEnabled = true
                )

                then(
                    """
                    roles are created for all supported betrokkenen for which the zaaktype defines a roltype,
                    using the first roltype when there are several and the roltype omschrijving over the generic role,
                    except for the behandelaar betrokkenen
                    """
                ) {
                    createdRoles shouldHaveSize 7
                    createdRoles.forEach {
                        it.roltoelichting shouldBe "Overgenomen vanuit de product aanvraag"
                        it.zaak shouldBe zaak.url
                    }
                    with(createdRoles[0]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe belanghebbendeKvkNummer1
                        roltype shouldBe rolTypeBelanghebbende.url
                    }
                    with(createdRoles[1]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe belanghebbendeKvkNummer2
                        roltype shouldBe rolTypeBelanghebbende.url
                    }
                    with(createdRoles[2]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NATUURLIJK_PERSOON
                        identificatienummer shouldBe "fakeBeslisserBsn"
                        roltype shouldBe rolTypeBeslisser.url
                    }
                    with(createdRoles[3]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe beslisserKvkNummer
                        roltype shouldBe rolTypeBeslisser.url
                    }
                    with(createdRoles[4]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NATUURLIJK_PERSOON
                        identificatienummer shouldBe "fakeKlantcontacterBsn"
                        roltype shouldBe rolTypeKlantcontacter1.url
                    }
                    with(createdRoles[5]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NATUURLIJK_PERSOON
                        identificatienummer shouldBe "fakeMedeInitiatorBsn"
                        roltype shouldBe rolTypeMedeInitiator.url
                    }
                    with(createdRoles[6]) {
                        betrokkeneType shouldBe BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
                        identificatienummer shouldBe zaakcoordinatorKvkNummer
                        roltype shouldBe rolTypeZaakcoordinator.url
                    }
                }

                and("no roltypen are looked up for the behandelaar betrokkenen") {
                    verify(exactly = 0) {
                        ztcClientService.findRoltypen(any(), "Behandelaar")
                        ztcClientService.findRoltypen(any(), OmschrijvingGeneriekEnum.BEHANDELAAR)
                    }
                }

                and("no initiator is returned") {
                    addedInitiator shouldBe null
                }
            }
        }
    }
})
