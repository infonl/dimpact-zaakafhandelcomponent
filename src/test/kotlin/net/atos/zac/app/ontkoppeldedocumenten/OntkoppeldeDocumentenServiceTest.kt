package net.atos.zac.app.ontkoppeldedocumenten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import jakarta.persistence.EntityManager
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.documenten.model.OntkoppeldDocument
import nl.info.client.zgw.drc.model.createEnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.zac.authentication.LoggedInUser
import java.time.LocalDate
import java.util.UUID

class OntkoppeldeDocumentenServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>(relaxed = true)
    val loggedInUser = mockk<LoggedInUser>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    val ontkoppeldeDocumentenService = OntkoppeldeDocumentenService()

    beforeTest {
        OntkoppeldeDocumentenService::class.java.getDeclaredField("entityManager").apply {
            isAccessible = true
            set(ontkoppeldeDocumentenService, entityManager)
        }
        OntkoppeldeDocumentenService::class.java.getDeclaredField("loggedInUserInstance").apply {
            isAccessible = true
            set(ontkoppeldeDocumentenService, loggedInUserInstance)
        }
    }

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a valid EnkelvoudigInformatieObject for a given Zaak") {
        val documentUuid = UUID.randomUUID()
        val identificatie = "DOC-456"
        val creatiedatum = LocalDate.now()
        val titel = "Ontkoppeld Document"
        val bestandsnaam = "ontkoppeld.pdf"
        val reden = "Test reden"
        val userId = "user-123"
        val zaak = createZaak(identificatie = "ZAAK-789")

        val informatieobject = createEnkelvoudigInformatieObject(
            uuid = documentUuid
        ).apply {
            setIdentificatie(identificatie)
            setCreatiedatum(creatiedatum)
            setTitel(titel)
            setBestandsnaam(bestandsnaam)
        }

        every { loggedInUser.id } returns userId
        every { loggedInUserInstance.get() } returns loggedInUser
        every { entityManager.persist(any<OntkoppeldDocument>()) } just Runs

        When("the ontkoppelde documenten create is invoked") {
            val result = ontkoppeldeDocumentenService.create(informatieobject, zaak, reden)

            Then("an OntkoppeldDocument is created and stored") {
                result.documentUUID shouldBe documentUuid
                result.documentID shouldBe identificatie
                result.creatiedatum shouldBe creatiedatum
                result.titel shouldBe titel
                result.bestandsnaam shouldBe bestandsnaam
                result.ontkoppeldDoor shouldBe userId
                result.zaakID shouldBe zaak.identificatie
                result.reden shouldBe reden
                result.ontkoppeldOp shouldNotBe null
            }
        }
    }
})
