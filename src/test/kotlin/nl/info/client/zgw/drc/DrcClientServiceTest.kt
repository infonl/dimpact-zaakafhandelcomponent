/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import net.atos.client.zgw.drc.DrcClient
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import nl.info.client.zgw.drc.model.createLockEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.LockEnkelvoudigInformatieObject
import nl.info.zac.configuratie.ConfiguratieService
import java.util.UUID

class DrcClientServiceTest : BehaviorSpec({
    val drcClient = mockk<DrcClient>()
    val zgwClientHeadersFactory = mockk<ZGWClientHeadersFactory>()
    val configuratieService = mockk<ConfiguratieService>()
    val drcClientService = DrcClientService(
        drcClient,
        zgwClientHeadersFactory,
        configuratieService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A valid UUID for an EnkelvoudigInformatieobject") {
        val enkelvoudigInformatieobjectUUID = UUID.randomUUID()
        val lock = "fakeLock"
        val lockEnkelvoudigInformatieObject = createLockEnkelvoudigInformatieObject(
            lock = lock,
        )
        val lockEnkelvoudigInformatieObjectSlot = slot<LockEnkelvoudigInformatieObject>()
        every {
            drcClient.enkelvoudigInformatieobjectLock(
                enkelvoudigInformatieobjectUUID,
                capture(lockEnkelvoudigInformatieObjectSlot)
            )
        } returns lockEnkelvoudigInformatieObject

        When("locking the EnkelvoudigInformatieobject") {
            val result = drcClientService.lockEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID)

            Then(
                """
                it should call the DRC client with a LockEnkelvoudigInformatieObject instance with a lock string 
                generated from a (random) UUID
                """
            ) {
                UUID.fromString(lockEnkelvoudigInformatieObjectSlot.captured.lock.toString()).shouldBeInstanceOf<UUID>()
            }

            And("it should return the lock ID") {
                result shouldBe lock
            }
        }
    }
})
