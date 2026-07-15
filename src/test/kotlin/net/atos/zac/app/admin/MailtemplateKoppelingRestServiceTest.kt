/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.zac.app.admin.converter.RESTMailtemplateKoppelingConverter
import net.atos.zac.app.admin.model.RESTMailtemplateKoppeling
import net.atos.zac.app.admin.model.createRestMailTemplate
import nl.info.zac.admin.MailTemplateKoppelingenService
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.admin.model.createMailtemplateKoppelingen
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createOverigeRechten

class MailtemplateKoppelingRestServiceTest : BehaviorSpec({
    val mailTemplateKoppelingenService = mockk<MailTemplateKoppelingenService>()
    val restZaakafhandelParametersConverter = mockk<RestZaakafhandelParametersConverter>()
    val policyService = mockk<PolicyService>()
    val service = MailtemplateKoppelingRestService(
        mailTemplateKoppelingenService,
        restZaakafhandelParametersConverter,
        policyService
    )

    afterEach { checkUnnecessaryStub() }

    context("Policy enforcement") {
        given("Policy denies beheren") {
            every { policyService.readOverigeRechten(null) } returns createOverigeRechten(beheren = false)

            `when`("readMailtemplateKoppeling is called") {
                shouldThrow<PolicyException> {
                    service.readMailtemplateKoppeling(1L)
                }

                then("service is not called") {
                    verify(exactly = 0) { mailTemplateKoppelingenService.readMailtemplateKoppeling(any()) }
                }
            }
        }
    }

    context("readMailtemplateKoppeling") {
        given("Policy permits and koppeling exists") {
            val fakeKoppeling = createMailtemplateKoppelingen(
                id = 42L,
                zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
                mailTemplate = createMailTemplate()
            )
            every { policyService.readOverigeRechten(null) } returns createOverigeRechten(beheren = true)
            every { mailTemplateKoppelingenService.readMailtemplateKoppeling(42L) } returns fakeKoppeling

            `when`("readMailtemplateKoppeling is called with ID 42") {
                val result = service.readMailtemplateKoppeling(42L)

                then("the converted RESTMailtemplateKoppeling is returned") {
                    result.id shouldBe RESTMailtemplateKoppelingConverter.convert(fakeKoppeling).id
                }
            }
        }
    }

    context("deleteMailtemplateKoppeling") {
        given("Policy permits") {
            every { policyService.readOverigeRechten(null) } returns createOverigeRechten(beheren = true)
            every { mailTemplateKoppelingenService.delete(55L) } just runs

            `when`("deleteMailtemplateKoppeling is called with ID 55") {
                service.deleteMailtemplateKoppeling(55L)

                then("MailTemplateKoppelingenService.delete is called with the ID") {
                    verify { mailTemplateKoppelingenService.delete(55L) }
                }
            }
        }
    }

    context("storeMailtemplateKoppeling") {
        given("Policy permits and a REST koppeling is provided") {
            val fakeKoppeling = createMailtemplateKoppelingen(
                zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(),
                mailTemplate = createMailTemplate()
            )
            val restKoppeling = RESTMailtemplateKoppeling().apply {
                mailtemplate = createRestMailTemplate()
            }
            every { policyService.readOverigeRechten(null) } returns createOverigeRechten(beheren = true)
            every { mailTemplateKoppelingenService.storeMailtemplateKoppeling(any()) } returns fakeKoppeling

            `when`("storeMailtemplateKoppeling is called") {
                val result = service.storeMailtemplateKoppeling(restKoppeling)

                then("MailTemplateKoppelingenService.storeMailtemplateKoppeling is called and result is returned") {
                    verify { mailTemplateKoppelingenService.storeMailtemplateKoppeling(any()) }
                    result.id shouldBe RESTMailtemplateKoppelingConverter.convert(fakeKoppeling).id
                }
            }
        }
    }
})
