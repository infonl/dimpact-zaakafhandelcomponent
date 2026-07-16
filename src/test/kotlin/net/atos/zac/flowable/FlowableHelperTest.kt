/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.task.TaakVariabelenService
import nl.info.client.klant.KlantClientService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.mail.MailService
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.IndexingService
import nl.info.zac.shared.helper.SuspensionZaakHelper
import nl.info.zac.zaak.ZaakService
import org.flowable.engine.HistoryService

class FlowableHelperTest : BehaviorSpec({
    val fakeEventingService = mockk<EventingService>()
    val fakeIdentityService = mockk<IdentityService>()
    val fakeIndexingService = mockk<IndexingService>()
    val fakeMailService = mockk<MailService>()
    val fakeMailTemplateService = mockk<MailTemplateService>()
    val fakeSuspensionZaakHelper = mockk<SuspensionZaakHelper>()
    val fakeTaakVariabelenService = mockk<TaakVariabelenService>()
    val fakeZaakService = mockk<ZaakService>()
    val fakeZaakVariabelenService = mockk<ZaakVariabelenService>()
    val fakeZgwApiService = mockk<ZgwApiService>()
    val fakeZrcClientService = mockk<ZrcClientService>()
    val fakeZtcClientService = mockk<ZtcClientService>()
    val fakeHistoryService = mockk<HistoryService>()
    val fakeDrcClientService = mockk<DrcClientService>()
    val fakeKlantClientService = mockk<KlantClientService>()
    val fakePolicyService = mockk<PolicyService>()

    @Suppress("UNCHECKED_CAST")
    val fakeLoggedInUserInstance = mockk<Instance<LoggedInUser>>()

    context("FlowableHelper constructor injection") {
        given("All 16 service dependencies are provided") {
            val flowableHelper = FlowableHelper(
                eventingService = fakeEventingService,
                identityService = fakeIdentityService,
                indexeerService = fakeIndexingService,
                mailService = fakeMailService,
                mailTemplateService = fakeMailTemplateService,
                suspensionZaakHelper = fakeSuspensionZaakHelper,
                taakVariabelenService = fakeTaakVariabelenService,
                zaakService = fakeZaakService,
                zaakVariabelenService = fakeZaakVariabelenService,
                zgwApiService = fakeZgwApiService,
                zrcClientService = fakeZrcClientService,
                ztcClientService = fakeZtcClientService,
                flowableHistoryService = fakeHistoryService,
                drcClientService = fakeDrcClientService,
                klantClientService = fakeKlantClientService,
                policyService = fakePolicyService,
                loggedInUserInstance = fakeLoggedInUserInstance
            )

            `when`("all properties are accessed") {
                then("each property returns the corresponding injected dependency") {
                    flowableHelper.eventingService shouldBe fakeEventingService
                    flowableHelper.identityService shouldBe fakeIdentityService
                    flowableHelper.indexeerService shouldBe fakeIndexingService
                    flowableHelper.mailService shouldBe fakeMailService
                    flowableHelper.mailTemplateService shouldBe fakeMailTemplateService
                    flowableHelper.suspensionZaakHelper shouldBe fakeSuspensionZaakHelper
                    flowableHelper.taakVariabelenService shouldBe fakeTaakVariabelenService
                    flowableHelper.zaakService shouldBe fakeZaakService
                    flowableHelper.zaakVariabelenService shouldBe fakeZaakVariabelenService
                    flowableHelper.zgwApiService shouldBe fakeZgwApiService
                    flowableHelper.zrcClientService shouldBe fakeZrcClientService
                    flowableHelper.ztcClientService shouldBe fakeZtcClientService
                    flowableHelper.flowableHistoryService shouldBe fakeHistoryService
                    flowableHelper.drcClientService shouldBe fakeDrcClientService
                    flowableHelper.klantClientService shouldBe fakeKlantClientService
                    flowableHelper.policyService shouldBe fakePolicyService
                    flowableHelper.loggedInUserInstance shouldBe fakeLoggedInUserInstance
                }
            }
        }
    }
})
