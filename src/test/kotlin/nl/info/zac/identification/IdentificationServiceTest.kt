/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identification

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
import nl.info.zac.sensitive.SensitiveDataService
import java.util.UUID

class IdentificationServiceTest : BehaviorSpec({
    val sensitiveDataService = mockk<SensitiveDataService>()
    val identificationService = IdentificationService(sensitiveDataService)

    given("createBetrokkeneIdentificatieForInitiatorRole") {
        `when`("the initiator is a Natuurlijk Persoon") {
            val bsn = "123456789"
            val uuid = UUID.randomUUID()
            val natuurlijkPersoonIdentificatie = createNatuurlijkPersoonIdentificatie(bsn = bsn)
            val initiatorRole = createRolNatuurlijkPersoon(
                natuurlijkPersoonIdentificatie = natuurlijkPersoonIdentificatie
            )

            every { sensitiveDataService.put(bsn) } returns uuid

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            then("it should return a BetrokkeneIdentificatie with type BSN and the replaced temporaryPersonId") {
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

        `when`("the initiator is a Vestiging (legacy type)") {
            val vestigingsNummer = "987654321"
            val vestigingIdentificatie = createVestigingIdentificatie(vestigingsNummer = vestigingsNummer)
            val initiatorRole = createRolVestiging(vestigingIdentificatie = vestigingIdentificatie)

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            then("it should return a BetrokkeneIdentificatie with type VN and the vestigingsnummer") {
                with(result) {
                    type shouldBe IdentificatieType.VN
                    vestigingsnummer shouldBe vestigingsNummer
                    temporaryPersonId shouldBe null
                }
            }
        }

        `when`("the initiator is a Niet Natuurlijk Persoon with an RSIN (innNnpId)") {
            val rsin = "12345678"
            val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                innNnpId = rsin
            )
            val initiatorRole = createRolNietNatuurlijkPersoon(
                nietNatuurlijkPersoonIdentificatie = nietNatuurlijkPersoonIdentificatie
            )

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            then("it should return a BetrokkeneIdentificatie with type RSIN") {
                with(result) {
                    type shouldBe IdentificatieType.RSIN
                    rsin shouldBe rsin
                }
            }
        }

        `when`("the initiator is a Niet Natuurlijk Persoon with only a KVK number (RSIN type)") {
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

            then("it should return a BetrokkeneIdentificatie with type RSIN") {
                with(result) {
                    type shouldBe IdentificatieType.RSIN
                    kvkNummer shouldBe kvkNummer
                    vestigingsnummer shouldBe null
                }
            }
        }

        `when`("the initiator is a Niet Natuurlijk Persoon with a vestigingsnummer") {
            val vestigingsnummer = "123456789012"
            val nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                annIdentificatie = "fakeAnnId",
                vestigingsnummer = vestigingsnummer
            )
            val initiatorRole = createRolNietNatuurlijkPersoon(
                nietNatuurlijkPersoonIdentificatie = nietNatuurlijkPersoonIdentificatie,
            )

            val result = identificationService.createBetrokkeneIdentificatieForInitiatorRole(initiatorRole)!!

            then("it should return a BetrokkeneIdentificatie with type VN") {
                with(result) {
                    type shouldBe IdentificatieType.VN
                    vestigingsnummer shouldBe vestigingsnummer
                }
            }
        }
    }

    given("replaceBsnWithKey") {
        val bsn = "123456789"
        val uuid = UUID.randomUUID()
        every { sensitiveDataService.put(bsn) } returns uuid

        then("it should return the UUID from sensitiveDataService") {
            identificationService.replaceBsnWithKey(bsn) shouldBe uuid
        }
    }

    given("replaceKeyWithBsn") {
        val uuid = UUID.randomUUID()
        val bsn = "123456789"

        `when`("the key exists") {
            every { sensitiveDataService.get(uuid) } returns bsn
            then("it should return the BSN") {
                identificationService.replaceKeyWithBsn(uuid) shouldBe bsn
            }
        }

        `when`("the key does not exist") {
            every { sensitiveDataService.get(uuid) } returns null
            then("it should throw a BrpPersonNotFoundException") {
                shouldThrow<BrpTemporaryPersonIdNotCachedException> {
                    identificationService.replaceKeyWithBsn(uuid)
                }
            }
        }
    }
})
