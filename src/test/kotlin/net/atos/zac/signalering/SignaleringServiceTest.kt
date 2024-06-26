package net.atos.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.zac.app.zaken.converter.RESTZaakOverzichtConverter
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.mail.MailService
import net.atos.zac.signalering.model.Signalering
import java.time.ZonedDateTime

class SignaleringServiceTest : BehaviorSpec({
    val drcClientService = mockk<DrcClientService>()
    val eventingService = mockk<EventingService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val mailService = mockk<MailService>()
    val signaleringenMailHelper = mockk<SignaleringMailHelper>()
    val signaleringPredicateHelper = mockk<SignaleringPredicateHelper>()
    val zrcClientService = mockk<ZRCClientService>()
    val restZaakOverzichtConverter = mockk<RESTZaakOverzichtConverter>()
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaDeleteSignalering = mockk<CriteriaDelete<Signalering>>()
    val rootSignalering = mockk<Root<Signalering>>()
    val pathTijdstip = mockk<Path<Long>>()
    val query = mockk<Query>()

    val signaleringService = SignaleringService(
        drcClientService = drcClientService,
        eventingService = eventingService,
        flowableTaskService = flowableTaskService,
        mailService = mailService,
        signaleringenMailHelper = signaleringenMailHelper,
        signaleringPredicateHelper = signaleringPredicateHelper,
        zrcClientService = zrcClientService,
        restZaakOverzichtConverter = restZaakOverzichtConverter
    )
    signaleringService.setEntityManager(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("signaleringen older than two days") {
        val now = ZonedDateTime.now()
        val deleteOlderThanDays = 2L
        val zoneDateTimeSlot = slot<ZonedDateTime>()
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createCriteriaDelete(Signalering::class.java) } returns criteriaDeleteSignalering
        every { criteriaDeleteSignalering.from(Signalering::class.java) } returns rootSignalering
        every { rootSignalering.get<Long>("tijdstip") } returns pathTijdstip
        every { criteriaBuilder.lessThan(any(), capture(zoneDateTimeSlot)) } returns mockk<Predicate>()
        every { criteriaDeleteSignalering.where(any<Predicate>()) } returns criteriaDeleteSignalering
        every { entityManager.createQuery(criteriaDeleteSignalering) } returns query
        every { query.executeUpdate() } returns 2

        When("signaleringen older than two days are deleted") {
            val deletedCount = signaleringService.deleteOldSignaleringen(deleteOlderThanDays)

            Then("old signaleringen have been deleted") {
                deletedCount shouldBe 2
                with(zoneDateTimeSlot.captured) {
                    this shouldBeAfter now.minusDays(deleteOlderThanDays)
                    // allow for some seconds difference for slow running tests
                    this shouldBeBefore now.minusDays(deleteOlderThanDays).plusSeconds(10)
                }
            }
        }
    }
})
