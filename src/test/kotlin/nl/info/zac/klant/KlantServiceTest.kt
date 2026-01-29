/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.klant

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import nl.info.client.brp.exception.BrpTemporaryPersonIdNotCachedException
import nl.info.client.zgw.model.createNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolNatuurlijkPersoon
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
import nl.info.client.zgw.model.createRolVestiging
import nl.info.client.zgw.model.createVestigingIdentificatie
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.identification.IdentificationService
import nl.info.zac.sensitive.SensitiveDataService
import java.util.UUID

class KlantServiceTest : BehaviorSpec({
    val sensitiveDataService = mockk<SensitiveDataService>()
    val identificationService = IdentificationService(sensitiveDataService)

    Given("createBetrokkeneIdentificatieForInitiatorRole") {
        When("the initiator is a Natuurlijk Persoon") {
            val bsn = "123456789"
            val uuid = UUID.randomUUID()
            val natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = bsn)
            val initiatorRole = createRolNatuurlijkPersoon(
                natuurlijkPersoonIdentificatie = natuurlijkPersoonIdentificatie
            )

            every { sensitiveDataService.put(bsn) } returns uuid

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            Then("it should return a BetrokkeneIdentificatie with type BSN and the replaced temporaryPersonId") {
                with(result) {
                    type shouldBe IdentificatieType.BSN
                    bsn shouldBe bsn
                    temporaryPersonId shouldBe uuid
                    kvkNummer shouldBe null
                    rsin shouldBe null
                    vestigingsnummer shouldBe null
                }
            }
        }

        When("the initiator is a Vestiging (legacy type)") {
            val vestigingsNummer = "987654321"
            val vestigingIdentificatie = createVestigingIdentificatie(vestigingsNummer = vestigingsNummer)
            val initiatorRole = createRolVestiging(vestigingIdentificatie = vestigingIdentificatie)

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            Then("it should return a BetrokkeneIdentificatie with type VN and the vestigingsnummer") {
                with(result) {
                    type shouldBe IdentificatieType.VN
                    vestigingsnummer shouldBe vestigingsNummer
                    temporaryPersonId shouldBe null
                }
            }
        }

        When("the initiator is a Niet Natuurlijk Persoon with an RSIN (innNnpId)") {
            val rsin = "12345678"
            val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                innNnpId = rsin
            )
            val initiatorRole = createRolNietNatuurlijkPersoon(
                nietNatuurlijkPersoonIdentificatie = nietNatuurlijkPersoonIdentificatie
            )

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            Then("it should return a BetrokkeneIdentificatie with type RSIN") {
                with(result) {
                    type shouldBe IdentificatieType.RSIN
                    rsin shouldBe rsin
                }
            }
        }

        When("the initiator is a Niet Natuurlijk Persoon with only a KVK number (RSIN type)") {
            val kvkNummer = "12345678"
            val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                innNnpId = null,
                kvkNummer = "12344321",
                vestigingsnummer = null
            )
            val initiatorRole = createRolNietNatuurlijkPersoon(
                nietNatuurlijkPersoonIdentificatie = nietNatuurlijkPersoonIdentificatie
            )

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            Then("it should return a BetrokkeneIdentificatie with type RSIN") {
                with(result) {
                    type shouldBe IdentificatieType.RSIN
                    kvkNummer shouldBe kvkNummer
                    vestigingsnummer shouldBe null
                }
            }
        }

        When("the initiator is a Niet Natuurlijk Persoon with a vestigingsnummer") {
            val vestigingsnummer = "123456789012"
            val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                annIdentificatie = "fakeAnnId"
            )
            val initiatorRole = createRolNietNatuurlijkPersoon(
                nietNatuurlijkPersoonIdentificatie = nietNatuurlijkPersoonIdentificatie
            )

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            Then("it should return a BetrokkeneIdentificatie with type VN") {
                with(result) {
                    type shouldBe IdentificatieType.VN
                    vestigingsnummer shouldBe vestigingsnummer
                }
            }
        }
    }

    Given("replaceBsnWithKey") {
        val bsn = "123456789"
        val uuid = UUID.randomUUID()
        every { sensitiveDataService.put(bsn) } returns uuid

        Then("it should return the UUID from sensitiveDataService") {
            identificationService.replaceBsnWithKey(bsn) shouldBe uuid
        }
    }

    Given("replaceKeyWithBsn") {
        val uuid = UUID.randomUUID()
        val bsn = "123456789"

        When("the key exists") {
            every { sensitiveDataService.get(uuid) } returns bsn
            Then("it should return the BSN") {
                identificationService.replaceKeyWithBsn(uuid) shouldBe bsn
            }
        }

        When("the key does not exist") {
            every { sensitiveDataService.get(uuid) } returns null
            Then("it should throw a BrpPersonNotFoundException") {
                shouldThrow<BrpTemporaryPersonIdNotCachedException> {
                    identificationService.replaceKeyWithBsn(uuid)
                }
            }
        }
    }
})
