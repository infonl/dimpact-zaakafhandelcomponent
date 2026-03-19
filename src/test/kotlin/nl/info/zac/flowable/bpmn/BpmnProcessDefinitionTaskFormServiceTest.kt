/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import nl.info.test.org.flowable.engine.repository.createProcessDefinition
import nl.info.zac.flowable.bpmn.model.BpmnProcessDefinitionTaskForm
import nl.info.zac.flowable.bpmn.model.createBpmnProcessDefinitionTaskForm
import org.flowable.engine.RepositoryService
import org.flowable.engine.repository.ProcessDefinition
import org.flowable.engine.repository.ProcessDefinitionQuery

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

    /**
     * Sets up the common criteria query chain for querying BpmnProcessDefinitionTaskForm entities.
     * This includes setting up the entity manager, criteria builder, query, root, and paths.
     */
    fun setupCriteriaQueryBase() {
        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createQuery(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaQuery
        every { criteriaQuery.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every { entityManager.createQuery(criteriaQuery) } returns typedQuery
    }

    /**
     * Sets up the criteria query predicates for a specific form lookup.
     */
    fun setupFormQueryPredicates(
        processDefinitionKey: String,
        processDefinitionVersion: Int,
        formName: String
    ) {
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate
        every { criteriaBuilder.equal(namePath, formName) } returns predicate
    }

    /**
     * Sets up the repository service query chain to return a specific process definition.
     */
    fun setupProcessDefinitionQuery(
        processDefinitionKey: String,
        processDefinition: ProcessDefinition
    ) {
        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionKey(processDefinitionKey) } returns processDefinitionQuery
        every { processDefinitionQuery.active() } returns processDefinitionQuery
        every { processDefinitionQuery.latestVersion() } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
    }

    /**
     * Sets up the repository service query for a specific process definition ID lookup.
     */
    fun setupProcessDefinitionQueryById(
        processDefinitionId: String,
        processDefinition: ProcessDefinition
    ) {
        every { repositoryService.createProcessDefinitionQuery() } returns processDefinitionQuery
        every { processDefinitionQuery.processDefinitionId(processDefinitionId) } returns processDefinitionQuery
        every { processDefinitionQuery.singleResult() } returns processDefinition
    }

    /**
     * Sets up the criteria query for bulk delete of forms for a given process definition key.
     */
    fun setupBulkDeleteForProcessDefinitionQuery(processDefinitionKey: String, deletedCount: Int = 0) {
        val criteriaDelete = mockk<CriteriaDelete<BpmnProcessDefinitionTaskForm>>()
        val deleteQuery = mockk<Query>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createCriteriaDelete(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaDelete
        every { criteriaDelete.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate
        every { criteriaDelete.where(predicate) } returns criteriaDelete
        every { entityManager.createQuery(criteriaDelete) } returns deleteQuery
        every { deleteQuery.executeUpdate() } returns deletedCount
    }

    /**
     * Sets up the criteria query for bulk delete of a specific form by key, version, and name.
     */
    fun setupBulkDeleteFormQuery(
        processDefinitionKey: String,
        processDefinitionVersion: Int,
        formName: String,
        deletedCount: Int = 0
    ) {
        val criteriaDelete = mockk<CriteriaDelete<BpmnProcessDefinitionTaskForm>>()
        val deleteQuery = mockk<Query>()
        val predicate1 = mockk<Predicate>()
        val predicate2 = mockk<Predicate>()
        val predicate3 = mockk<Predicate>()

        every { entityManager.criteriaBuilder } returns criteriaBuilder
        every { criteriaBuilder.createCriteriaDelete(BpmnProcessDefinitionTaskForm::class.java) } returns criteriaDelete
        every { criteriaDelete.from(BpmnProcessDefinitionTaskForm::class.java) } returns root
        every { root.get<String>("bpmnProcessDefinitionKey") } returns bpmnProcessDefinitionKeyPath
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionPath
        every { root.get<String>("name") } returns namePath
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionKeyPath, processDefinitionKey)
        } returns predicate1
        every {
            criteriaBuilder.equal(bpmnProcessDefinitionVersionPath, processDefinitionVersion)
        } returns predicate2
        every { criteriaBuilder.equal(namePath, formName) } returns predicate3
        every { criteriaDelete.where(predicate1, predicate2, predicate3) } returns criteriaDelete
        every { entityManager.createQuery(criteriaDelete) } returns deleteQuery
        every { deleteQuery.executeUpdate() } returns deletedCount
    }

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
            bpmnProcessDefinitionKey = processDefinitionKey,
            bpmnProcessDefinitionVersion = processDefinitionVersion,
            name = formName,
            content = formContent
        )
        val processDefinition = createProcessDefinition(
            id = processDefinitionId,
            key = processDefinitionKey,
            version = processDefinitionVersion
        )

        setupProcessDefinitionQueryById(processDefinitionId, processDefinition)
        setupCriteriaQueryBase()
        setupFormQueryPredicates(processDefinitionKey, processDefinitionVersion, formName)
        every { typedQuery.resultList } returns listOf(form)

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

        setupProcessDefinitionQueryById(processDefinitionId, processDefinition)
        setupCriteriaQueryBase()
        setupFormQueryPredicates(processDefinitionKey, processDefinitionVersion, formName)
        every { typedQuery.resultList } returns emptyList()

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
            bpmnProcessDefinitionKey = "process1",
            bpmnProcessDefinitionVersion = 1,
            name = "form1"
        )
        val form2 = createBpmnProcessDefinitionTaskForm(
            id = 2L,
            bpmnProcessDefinitionKey = "process1",
            bpmnProcessDefinitionVersion = 2,
            name = "form2"
        )
        val form3 = createBpmnProcessDefinitionTaskForm(
            id = 3L,
            bpmnProcessDefinitionKey = "process2",
            bpmnProcessDefinitionVersion = 1,
            name = "form3"
        )

        val bpmnProcessDefinitionVersionIntPath = mockk<Path<Int>>()

        setupCriteriaQueryBase()
        every { root.get<Int>("bpmnProcessDefinitionVersion") } returns bpmnProcessDefinitionVersionIntPath
        every { criteriaBuilder.asc(bpmnProcessDefinitionKeyPath) } returns order
        every { criteriaBuilder.asc(bpmnProcessDefinitionVersionIntPath) } returns order
        every { criteriaBuilder.asc(namePath) } returns order
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

        setupProcessDefinitionQuery(processDefinitionKey, processDefinition)
        setupCriteriaQueryBase()
        setupFormQueryPredicates(processDefinitionKey, processDefinitionVersion, formName)
        every { typedQuery.resultList } returns emptyList()
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
            bpmnProcessDefinitionKey = processDefinitionKey,
            bpmnProcessDefinitionVersion = processDefinitionVersion,
            name = formName
        )
        val formSlot = slot<BpmnProcessDefinitionTaskForm>()

        setupProcessDefinitionQuery(processDefinitionKey, processDefinition)
        setupCriteriaQueryBase()
        setupFormQueryPredicates(processDefinitionKey, processDefinitionVersion, formName)
        every { typedQuery.resultList } returns listOf(existingForm)
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

        setupProcessDefinitionQuery(processDefinitionKey, processDefinition)
        setupCriteriaQueryBase()
        setupFormQueryPredicates(processDefinitionKey, processDefinitionVersion, "testForm")
        every { typedQuery.resultList } returns emptyList()
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

        setupProcessDefinitionQuery(processDefinitionKey, processDefinition)
        setupCriteriaQueryBase()
        setupFormQueryPredicates(processDefinitionKey, processDefinitionVersion, formName)
        every { typedQuery.resultList } returns emptyList()
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

        setupProcessDefinitionQuery(processDefinitionKey, processDefinition)
        setupBulkDeleteFormQuery(processDefinitionKey, processDefinitionVersion, formName, deletedCount = 1)

        When("deleteForm is called") {
            service.deleteForm(processDefinitionKey, formName)

            Then("it should execute a bulk delete operation") {
                verify(exactly = 1) { entityManager.criteriaBuilder }
                verify(exactly = 0) { entityManager.remove(any<BpmnProcessDefinitionTaskForm>()) }
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

        setupProcessDefinitionQuery(processDefinitionKey, processDefinition)
        setupBulkDeleteFormQuery(processDefinitionKey, processDefinitionVersion, formName, deletedCount = 0)

        When("deleteForm is called") {
            service.deleteForm(processDefinitionKey, formName)

            Then("it should execute a bulk delete operation without errors") {
                verify(exactly = 1) { entityManager.criteriaBuilder }
                verify(exactly = 0) { entityManager.remove(any<BpmnProcessDefinitionTaskForm>()) }
            }
        }
    }

    Given("Multiple forms exist for a process definition") {
        val processDefinitionKey = "processKey"

        setupBulkDeleteForProcessDefinitionQuery(processDefinitionKey, deletedCount = 3)

        When("deleteFormsForProcessDefinition is called") {
            service.deleteAllFormsForProcessDefinition(processDefinitionKey)

            Then("it should execute a bulk delete operation") {
                verify(exactly = 1) { entityManager.criteriaBuilder }
                verify(exactly = 0) { entityManager.remove(any<BpmnProcessDefinitionTaskForm>()) }
            }
        }
    }

    Given("No forms exist for a process definition") {
        val processDefinitionKey = "processKey"

        setupBulkDeleteForProcessDefinitionQuery(processDefinitionKey, deletedCount = 0)

        When("deleteFormsForProcessDefinition is called") {
            service.deleteAllFormsForProcessDefinition(processDefinitionKey)

            Then("it should execute a bulk delete operation without errors") {
                verify(exactly = 1) { entityManager.criteriaBuilder }
                verify(exactly = 0) { entityManager.remove(any<BpmnProcessDefinitionTaskForm>()) }
            }
        }
    }
})
