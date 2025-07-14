/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import net.atos.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.exception.MailTemplateNotFoundException
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.mailtemplates.model.createMailTemplate

class MailTemplateServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<MailTemplate>>(relaxed = true)
    val predicate = mockk<Predicate>()
    val root = mockk<Root<MailTemplate>>()
    val mailTemplateNamePath = mockk<Path<String>>()
    val typedQuery = mockk<TypedQuery<MailTemplate>>()
    val mailTemplateService = MailTemplateService(entityManager)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Mail templates can be deleted") {
        Given("an existing mail template") {
            val mailTemplateId = 1234L
            val mailTemplate = createMailTemplate()

            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns mailTemplate
            every { entityManager.remove(mailTemplate) } just Runs

            When("delete is called with the ID") {
                mailTemplateService.delete(mailTemplateId)

                Then("it should remove the mail template") {
                    verify(exactly = 1) {
                        entityManager.remove(mailTemplate)
                    }
                }
            }
        }
    }

    Context("Default mail templates can be found for a mail type") {
        val mail = Mail.ZAAK_ONTVANKELIJK
        val mailTemplate = createMailTemplate()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
        every { criteriaQuery.from(MailTemplate::class.java) } returns root
        every { root.get<String>(MailTemplate.MAIL) } returns mailTemplateNamePath
        every { root.get<String>(MailTemplate.DEFAULT_MAILTEMPLATE) } returns mailTemplateNamePath
        every { criteriaBuilder.equal(mailTemplateNamePath, mail) } returns predicate
        every { criteriaBuilder.equal(mailTemplateNamePath, true) } returns predicate
        every { criteriaBuilder.and(predicate, predicate) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        Given("a default MailTemplate exists for the given mail type") {
            every { typedQuery.resultList } returns listOf(mailTemplate)

            When("findDefaultMailtemplate is called with the mail type") {
                val result = mailTemplateService.findDefaultMailtemplate(mail)

                Then("it should return the default mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        Given("no default mail template exists for the given mail type") {
            every { typedQuery.resultList } returns emptyList()

            When("findDefaultMailtemplate is called with the mail type") {
                val result = mailTemplateService.findDefaultMailtemplate(mail)

                Then("it should return null") {
                    result shouldBe null
                }
            }
        }
    }

    Context("Mail templates can be found by name") {
        val mailTemplateName = "fakeTemplateName"

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
        every { criteriaQuery.from(MailTemplate::class.java) } returns root
        every { root.get<String>("mailTemplateNaam") } returns mailTemplateNamePath
        every { criteriaBuilder.equal(mailTemplateNamePath, mailTemplateName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        Given("a mail template exists for the given name") {
            val mailTemplate = createMailTemplate()

            every { typedQuery.resultList } returns listOf(mailTemplate)

            When("findMailtemplateByName is called") {
                val result = mailTemplateService.findMailtemplateByName(mailTemplateName)

                Then("it should return the matching mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        Given("no mail template exists for the given name") {
            every { typedQuery.resultList } returns emptyList()

            When("findMailtemplateByName is called") {
                val result = mailTemplateService.findMailtemplateByName(mailTemplateName)

                Then("it should return null") {
                    result shouldBe null
                }
            }
        }
    }

    Context("All mail templates can be listed") {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
        every { criteriaQuery.from(MailTemplate::class.java) } returns root
        every { criteriaQuery.orderBy(criteriaBuilder.asc(root.get<String>("mailTemplateNaam"))) } returns criteriaQuery
        every { criteriaQuery.select(root) } returns criteriaQuery
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        Given("multiple mail templates exist") {
            val mailTemplates = listOf(createMailTemplate(), createMailTemplate())
            every { typedQuery.resultList } returns mailTemplates

            When("listMailtemplates is called") {
                val result = mailTemplateService.listMailtemplates()

                Then("it should return all mail templates") {
                    result shouldBe mailTemplates
                }
            }
        }
    }

    Context("Mail templates can be read by mail type") {
        val mail = Mail.ZAAK_ONTVANKELIJK
        val mailTemplate = createMailTemplate()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(MailTemplate::class.java) } returns criteriaQuery
        every { criteriaQuery.from(MailTemplate::class.java) } returns root
        every { root.get<String>(MailTemplate.MAIL) } returns mailTemplateNamePath
        every { root.get<String>(MailTemplate.DEFAULT_MAILTEMPLATE) } returns mailTemplateNamePath
        every { criteriaBuilder.equal(mailTemplateNamePath, mail) } returns predicate
        every { criteriaBuilder.equal(mailTemplateNamePath, true) } returns predicate
        every { criteriaBuilder.and(predicate, predicate) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery

        Given("a mail template exists for the given mail type") {
            every { typedQuery.resultList } returns listOf(mailTemplate)

            When("readMailtemplate is called with the mail type") {
                val result = mailTemplateService.readMailtemplate(mail)

                Then("it should return the mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        Given("no mail template exists for the given mail type") {
            every { typedQuery.resultList } returns emptyList()

            When("readMailtemplate is called with the mail type") {
                val exception = shouldThrow<MailTemplateNotFoundException> {
                    mailTemplateService.readMailtemplate(mail)
                }
                Then("it should throw MailTemplateNotFoundException") {
                    exception shouldBe MailTemplateNotFoundException(mail)
                }
            }
        }
    }

    Context("Mail templates can be read by ID") {
        val mailTemplateId = 1234L
        val mailTemplate = createMailTemplate()

        every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns mailTemplate

        Given("a mail template exists for the given ID") {
            When("readMailtemplate is called with the ID") {
                val result = mailTemplateService.readMailtemplate(mailTemplateId)

                Then("it should return the mail template") {
                    result shouldBe mailTemplate
                }
            }
        }

        Given("no mail template exists for the given ID") {
            every { entityManager.find(MailTemplate::class.java, mailTemplateId) } returns null

            When("readMailtemplate is called with the ID") {
                val exception = shouldThrow<MailTemplateNotFoundException> {
                    mailTemplateService.readMailtemplate(mailTemplateId)
                }
                Then("it should throw MailTemplateNotFoundException") {
                    exception shouldBe MailTemplateNotFoundException(mailTemplateId)
                }
            }
        }
    }

    Context("Mail templates can be stored") {
        val mailTemplate = createMailTemplate()

        Given("a new mail template") {
            every { entityManager.find(MailTemplate::class.java, mailTemplate.id) } returns null
            every { entityManager.persist(mailTemplate) } just Runs

            When("storeMailtemplate is called") {
                val result = mailTemplateService.storeMailtemplate(mailTemplate)

                Then("it should persist the mail template") {
                    verify(exactly = 1) { entityManager.persist(mailTemplate) }
                    result shouldBe mailTemplate
                }
            }
        }

        Given("an existing mail template") {
            every { entityManager.find(MailTemplate::class.java, mailTemplate.id) } returns mailTemplate
            every { entityManager.merge(mailTemplate) } returns mailTemplate

            When("storeMailtemplate is called with an existing ID") {
                val result = mailTemplateService.storeMailtemplate(mailTemplate)

                Then("it should merge the mail template") {
                    verify(exactly = 1) { entityManager.merge(mailTemplate) }
                    result shouldBe mailTemplate
                }
            }
        }
    }
})
