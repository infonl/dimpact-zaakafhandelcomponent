/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.inboxDocumenten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.model.InboxDocument
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.zrc.ZrcClientService
import java.time.LocalDate
import java.util.UUID

class InboxDocumentenServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()

    val inboxDocumentenService = InboxDocumentenService(entityManager, zrcClientService, drcClientService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("an EnkelvoudigInformatieObject is available from the DRC service by UUID") {
        val uuid = UUID.randomUUID()
        val identificatie = "DOC-123"
        val creatiedatum = LocalDate.now()
        val titel = "fakeDocument"
        val bestandsnaam = "document.pdf"

        val enkelvoudigInformatieObject = createEnkelvoudigInformatieObject().apply {
            setIdentificatie(identificatie)
            setCreatiedatum(creatiedatum)
            setTitel(titel)
            setBestandsnaam(bestandsnaam)
        }

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { entityManager.persist(any<InboxDocument>()) } just Runs

        When("the Inbox Document Service retrieves creates a Document from the EnkelvoudigInformatieObject's UUID") {
            val result = inboxDocumentenService.create(uuid)

            Then("the Service should have stored an Inbox Document") {
                verify { entityManager.persist(result) }
                result.enkelvoudiginformatieobjectUUID shouldBe uuid
                result.enkelvoudiginformatieobjectID shouldBe identificatie
                result.creatiedatum shouldBe creatiedatum
                result.titel shouldBe titel
                result.bestandsnaam shouldBe bestandsnaam
            }
        }
    }
})
