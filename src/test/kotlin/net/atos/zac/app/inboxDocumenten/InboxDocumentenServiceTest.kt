package net.atos.zac.app.inboxDocumenten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import jakarta.persistence.EntityManager
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.model.InboxDocument
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.zrc.ZrcClientService
import java.time.LocalDate
import java.util.UUID

class InboxDocumentenServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val drcClientService = mockk<DrcClientService>()
    val zrcClientService = mockk<ZrcClientService>()

    val inboxDocumentenService = InboxDocumentenService(
        zrcClientService,
        drcClientService
    )

    val field = InboxDocumentenService::class.java.getDeclaredField("entityManager")
    field.isAccessible = true
    field.set(inboxDocumentenService, entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a UUID for an EnkelvoudigInformatieObject") {
        val uuid = UUID.randomUUID()
        val identificatie = "DOC-123"
        val creatiedatum = LocalDate.now()
        val titel = "Test Document"
        val bestandsnaam = "document.pdf"

        val enkelvoudigInformatieObject = EnkelvoudigInformatieObject().apply {
            setIdentificatie(identificatie)
            setCreatiedatum(creatiedatum)
            setTitel(titel)
            setBestandsnaam(bestandsnaam)
        }

        every { drcClientService.readEnkelvoudigInformatieobject(uuid) } returns enkelvoudigInformatieObject
        every { entityManager.persist(any<InboxDocument>()) } just Runs

        When("create is called") {
            val result = inboxDocumentenService.create(uuid)

            Then("it should create and persist an InboxDocument with expected values") {
                result.enkelvoudiginformatieobjectUUID shouldBe uuid
                result.enkelvoudiginformatieobjectID shouldBe identificatie
                result.creatiedatum shouldBe creatiedatum
                result.titel shouldBe titel
                result.bestandsnaam shouldBe bestandsnaam
            }
        }
    }
})
