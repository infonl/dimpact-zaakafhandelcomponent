/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.test.org.flowable.engine.repository.createProcessDefinition
import nl.info.zac.flowable.bpmn.model.BpmnProcessDefinitionTaskForm
import nl.info.zac.formio.createBpmnProcessDefinitionTaskForm
import org.flowable.engine.RepositoryService
import org.flowable.engine.repository.ProcessDefinitionQuery
import java.util.stream.Stream

class BpmnProcessDefinitionTaskFormServiceTest : BehaviorSpec({
    val entityManager = mockk<EntityManager>()
    val repositoryService = mockk<RepositoryService>()
    val criteriaBuilder = mockk<CriteriaBuilder>()
    val criteriaQuery = mockk<CriteriaQuery<BpmnProcessDefinitionTaskForm>>(relaxed = true)
    val root = mockk<Root<BpmnProcessDefinitionTaskForm>>()
    val typedQuery = mockk<TypedQuery<BpmnProcessDefinitionTaskForm>>()
    val processDefinitionQuery = mockk<ProcessDefinitionQuery>()
    val bpmnProcessDefinitionKeyPath = mockk<Path<String>>()
    val bpmnProcessDefinitionVersionPath = mockk<Path<Int>>()
    val namePath = mockk<Path<String>>()
    val predicate = mockk<Predicate>()
    val order = mockk<Order>()
    val service = BpmnProcessDefinitionTaskFormService(entityManager, repositoryService)

    afterEach {
        checkUnnecessaryStub()
    }

    Given("A process definition ID and a form name that exists") {
        val processDefinitionId = "fakeProcessDefinitionId"
        val formName = "testForm"
        val processDefinitionKey = "processKey"
        val processDefinitionVersion = 2
        val formContent = """{"name": "$formName", "title": "Test Form"}"""
        val form = createBpmnProcessDefinitionTaskForm(
            bpmnProcessDefinition = processDefinitionKey,
            bpmnProcessDefinitionVersion = processDefinitionVersion,
            name = formName,
            content = formContent
        )
        val processDefinition = createProcessDefinition(
            id = processDefinitionId,
            key = processDefinitionKey,
            version = processDefinitionVersion
        )

        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionId(processDefinitionId) } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, formName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultStream } returns Stream.of(form)

        When("readForm is called") {
            val result = service.readForm(processDefinitionId, formName)

            Then("it should return the form content as JsonObject") {
                result.getString("name") shouldBe formName
                result.getString("title") shouldBe "Test Form"
            }
        }
    }

    Given("A process definition ID and a form name that does not exist") {
        val processDefinitionId = "fakeProcessDefinitionId"
        val formName = "testForm"
        val processDefinitionKey = "processKey"
        val processDefinitionVersion = 2
        val processDefinition = createProcessDefinition(
            id = processDefinitionId,
            key = processDefinitionKey,
            version = processDefinitionVersion
        )

        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionId(processDefinitionId) } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, formName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultStream } returns Stream.empty()

        When("readForm is called") {
            Then("it should throw NoSuchElementException") {
                val exception = shouldThrow<NoSuchElementException> {
                    service.readForm(processDefinitionId, formName)
                }
                exception.message shouldBe "No BpmnProcessDefinitionTaskForm found with name: '$formName' " +
                    "for processDefinition key='$processDefinitionKey', version=$processDefinitionVersion, " +
                    "id='$processDefinitionId'"
            }
        }
    }

    Given("Multiple forms exist") {
        val form1 = createBpmnProcessDefinitionTaskForm(
            id = 1L,
            bpmnProcessDefinition = "process1",
            bpmnProcessDefinitionVersion = 1,
            name = "form1"
        )
        val form2 = createBpmnProcessDefinitionTaskForm(
            id = 2L,
            bpmnProcessDefinition = "process1",
            bpmnProcessDefinitionVersion = 2,
            name = "form2"
        )
        val form3 = createBpmnProcessDefinitionTaskForm(
            id = 3L,
            bpmnProcessDefinition = "process2",
            bpmnProcessDefinitionVersion = 1,
            name = "form3"
        )

        val bpmnProcessDefinitionVersionStringPath = mockk<Path<String>>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<String>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionStringPath
        every { root.get<String>("name") } returns namePath
        every { criteriaBuilder.asc(bpmnProcessDefinitionKeyPath) } returns order
        every { criteriaBuilder.asc(bpmnProcessDefinitionVersionStringPath) } returns order
        every { criteriaBuilder.asc(namePath) } returns order
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultList } returns listOf(form1, form2, form3)

        When("listForms is called") {
            val result = service.listForms()

            Then("it should return all forms ordered by key, version, and name") {
                result.size shouldBe 3
                result[0] shouldBe form1
                result[1] shouldBe form2
                result[2] shouldBe form3
            }
        }
    }

    Given("A new form is being added") {
        val processDefinitionKey = "processKey"
        val filename = "testForm.json"
        val formName = "Test Form"
        val formTitle = "Test Form Title"
        val formContent = """{"name": "$formName", "title": "$formTitle"}"""
        val processDefinitionVersion = 1
        val processDefinition = createProcessDefinition(
            key = processDefinitionKey,
            version = processDefinitionVersion
        )
        val formSlot = slot<BpmnProcessDefinitionTaskForm>()

        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionKey(processDefinitionKey) } returns processDefinitionQuery
        every { processDefinitionQuery.active() } returns processDefinitionQuery
        every { processDefinitionQuery.latestVersion() } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, formName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultStream } returns Stream.empty()
        every { entityManager.merge(capture(formSlot)) } returns mockk()

        When("addForm is called") {
            service.addForm(processDefinitionKey, filename, formContent)

            Then("it should merge the form entity with correct properties") {
                verify(exactly = 1) { entityManager.merge(any<BpmnProcessDefinitionTaskForm>()) }
                formSlot.captured.bpmnProcessDefinitionKey shouldBe processDefinitionKey
                formSlot.captured.bpmnProcessDefinitionVersion shouldBe processDefinitionVersion
                formSlot.captured.filename shouldBe filename
                formSlot.captured.content shouldBe formContent
                formSlot.captured.name shouldBe formName
                formSlot.captured.title shouldBe formTitle
            }
        }
    }

    Given("An existing form is being updated") {
        val processDefinitionKey = "processKey"
        val filename = "testForm.json"
        val formName = "Test Form"
        val formTitle = "Test Form Title"
        val formContent = """{"name": "$formName", "title": "$formTitle"}"""
        val processDefinitionVersion = 1
        val processDefinition = createProcessDefinition(
            key = processDefinitionKey,
            version = processDefinitionVersion
        )
        val existingForm = createBpmnProcessDefinitionTaskForm(
            id = 123L,
            bpmnProcessDefinition = processDefinitionKey,
            bpmnProcessDefinitionVersion = processDefinitionVersion,
            name = formName
        )
        val formSlot = slot<BpmnProcessDefinitionTaskForm>()

        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionKey(processDefinitionKey) } returns processDefinitionQuery
        every { processDefinitionQuery.active() } returns processDefinitionQuery
        every { processDefinitionQuery.latestVersion() } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, formName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultStream } returns Stream.of(existingForm)
        every { entityManager.merge(capture(formSlot)) } returns mockk()

        When("addForm is called") {
            service.addForm(processDefinitionKey, filename, formContent)

            Then("it should merge the form entity with the existing ID") {
                verify(exactly = 1) { entityManager.merge(any<BpmnProcessDefinitionTaskForm>()) }
                formSlot.captured.id shouldBe 123L
                formSlot.captured.bpmnProcessDefinitionKey shouldBe processDefinitionKey
                formSlot.captured.name shouldBe formName
            }
        }
    }

    Given("Form content has no name field") {
        val processDefinitionKey = "processKey"
        val filename = "testForm.json"
        val formTitle = "Test Form Title"
        val contentWithoutName = """{"title": "$formTitle"}"""
        val processDefinitionVersion = 1
        val processDefinition = createProcessDefinition(
            key = processDefinitionKey,
            version = processDefinitionVersion
        )
        val formSlot = slot<BpmnProcessDefinitionTaskForm>()

        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionKey(processDefinitionKey) } returns processDefinitionQuery
        every { processDefinitionQuery.active() } returns processDefinitionQuery
        every { processDefinitionQuery.latestVersion() } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, "testForm") } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultStream } returns Stream.empty()
        every { entityManager.merge(capture(formSlot)) } returns mockk()

        When("addForm is called") {
            service.addForm(processDefinitionKey, filename, contentWithoutName)

            Then("it should use the filename without .json extension as name") {
                formSlot.captured.name shouldBe "testForm"
            }
        }
    }

    Given("Form content has no title field") {
        val processDefinitionKey = "processKey"
        val filename = "testForm.json"
        val formName = "Test Form"
        val contentWithoutTitle = """{"name": "$formName"}"""
        val processDefinitionVersion = 1
        val processDefinition = createProcessDefinition(
            key = processDefinitionKey,
            version = processDefinitionVersion
        )
        val formSlot = slot<BpmnProcessDefinitionTaskForm>()

        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionKey(processDefinitionKey) } returns processDefinitionQuery
        every { processDefinitionQuery.active() } returns processDefinitionQuery
        every { processDefinitionQuery.latestVersion() } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, formName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultStream } returns Stream.empty()
        every { entityManager.merge(capture(formSlot)) } returns mockk()

        When("addForm is called") {
            service.addForm(processDefinitionKey, filename, contentWithoutTitle)

            Then("it should use empty string as title") {
                formSlot.captured.title shouldBe ""
            }
        }
    }

    Given("A form exists and needs to be deleted") {
        val processDefinitionKey = "processKey"
        val formName = "testForm"
        val processDefinitionVersion = 1
        val processDefinition = createProcessDefinition(
            key = processDefinitionKey,
            version = processDefinitionVersion
        )
        val existingForm = createBpmnProcessDefinitionTaskForm(
            bpmnProcessDefinition = processDefinitionKey,
            bpmnProcessDefinitionVersion = processDefinitionVersion,
            name = formName
        )

        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionKey(processDefinitionKey) } returns processDefinitionQuery
        every { processDefinitionQuery.active() } returns processDefinitionQuery
        every { processDefinitionQuery.latestVersion() } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, formName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultStream } returns Stream.of(existingForm)
        every { entityManager.remove(existingForm) } just Runs

        When("deleteForm is called") {
            service.deleteForm(processDefinitionKey, formName)

            Then("it should remove the form") {
                verify(exactly = 1) { entityManager.remove(existingForm) }
            }
        }
    }

    Given("A form does not exist and deletion is attempted") {
        val processDefinitionKey = "processKey"
        val formName = "testForm"
        val processDefinitionVersion = 1
        val processDefinition = createProcessDefinition(
            key = processDefinitionKey,
            version = processDefinitionVersion
        )

        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionKey(processDefinitionKey) } returns processDefinitionQuery
        every { processDefinitionQuery.active() } returns processDefinitionQuery
        every { processDefinitionQuery.latestVersion() } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, formName) } returns predicate
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
        every { typedQuery.resultStream } returns Stream.empty()

        When("deleteForm is called") {
            service.deleteForm(processDefinitionKey, formName)

            Then("it should not attempt to remove anything") {
                verify(exactly = 0) { entityManager.remove(any<BpmnProcessDefinitionTaskForm>()) }
            }
        }
    }
})
