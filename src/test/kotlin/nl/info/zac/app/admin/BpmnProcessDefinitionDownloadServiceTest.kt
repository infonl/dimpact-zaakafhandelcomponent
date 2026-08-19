/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.core.StreamingOutput
import nl.info.test.org.flowable.engine.repository.createProcessDefinition
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.exception.BpmnProcessDefinitionDownloadException
import nl.info.zac.flowable.bpmn.model.createBpmnProcessDefinitionMetadata
import nl.info.zac.flowable.bpmn.model.createBpmnProcessDefinitionTaskForm
import org.flowable.engine.repository.ProcessDefinition
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Random
import java.util.zip.ZipInputStream

private const val PROCESS_DEFINITION_ID = "fakeProcessDefinitionId"
private const val PROCESS_DEFINITION_KEY = "fakeProcessKey"
private const val PROCESS_DEFINITION_VERSION = 3
private const val PROCESS_DEFINITION_MODEL = "<definitions id=\"fakeDefinitions\"/>"

private fun StreamingOutput.readZipEntries(): Map<String, String> =
    ByteArrayOutputStream().also { write(it) }.toByteArray().let { zipBytes ->
        ZipInputStream(zipBytes.inputStream()).use { zipInputStream ->
            buildMap {
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    put(zipEntry.name, zipInputStream.readBytes().decodeToString())
                    zipEntry = zipInputStream.nextEntry
                }
            }
        }
    }

class BpmnProcessDefinitionDownloadServiceTest : BehaviorSpec({
    val bpmnService = mockk<BpmnService>()
    val bpmnProcessDefinitionTaskFormService = mockk<BpmnProcessDefinitionTaskFormService>()
    val bpmnProcessDefinitionDownloadService = BpmnProcessDefinitionDownloadService(
        bpmnService,
        bpmnProcessDefinitionTaskFormService
    )

    val processDefinition = createProcessDefinition(
        id = PROCESS_DEFINITION_ID,
        key = PROCESS_DEFINITION_KEY,
        version = PROCESS_DEFINITION_VERSION,
        resourceName = "fakeProcess.bpmn"
    )

    fun stubProcessDefinitionModel(
        processDefinitionToStub: ProcessDefinition = processDefinition,
        formKeys: List<String> = emptyList()
    ) {
        every { bpmnService.readProcessDefinitionModel(PROCESS_DEFINITION_ID) } returns
            PROCESS_DEFINITION_MODEL.byteInputStream()
        every { bpmnService.getProcessDefinitionMetadata(processDefinitionToStub) } returns
            createBpmnProcessDefinitionMetadata(formKeys = formKeys)
    }

    fun createFormForProcessDefinition(name: String, filename: String) =
        createBpmnProcessDefinitionTaskForm(
            bpmnProcessDefinitionKey = PROCESS_DEFINITION_KEY,
            bpmnProcessDefinitionVersion = PROCESS_DEFINITION_VERSION,
            name = name,
            filename = filename,
            content = """{ "name": "$name" }"""
        )

    afterEach { checkUnnecessaryStub() }

    given("a process definition with a task form that no user task references") {
        stubProcessDefinitionModel(formKeys = listOf("fakeUsedFormName"))
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(
            createFormForProcessDefinition(name = "fakeUsedFormName", filename = "fakeUsedForm.json"),
            createFormForProcessDefinition(name = "fakeUnusedFormName", filename = "fakeUnusedForm.json")
        )

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
                .readZipEntries()

            then("the unused task form is placed in its own folder, separate from the used one") {
                zipEntries.keys shouldContainExactly setOf(
                    "fakeProcess.bpmn",
                    "fakeUsedForm.json",
                    "unused-forms/fakeUnusedForm.json"
                )
                zipEntries["fakeProcess.bpmn"] shouldBe PROCESS_DEFINITION_MODEL
                zipEntries["fakeUsedForm.json"] shouldBe """{ "name": "fakeUsedFormName" }"""
                zipEntries["unused-forms/fakeUnusedForm.json"] shouldBe """{ "name": "fakeUnusedFormName" }"""
            }
        }
    }

    given("task forms belonging to another process definition key and to another version") {
        stubProcessDefinitionModel(formKeys = listOf("fakeCurrentFormName"))
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(
            createFormForProcessDefinition(name = "fakeCurrentFormName", filename = "fakeCurrentForm.json"),
            createBpmnProcessDefinitionTaskForm(
                bpmnProcessDefinitionKey = PROCESS_DEFINITION_KEY,
                bpmnProcessDefinitionVersion = PROCESS_DEFINITION_VERSION - 1,
                name = "fakePreviousVersionFormName",
                filename = "fakePreviousVersionForm.json"
            ),
            createBpmnProcessDefinitionTaskForm(
                bpmnProcessDefinitionKey = "fakeOtherProcessKey",
                bpmnProcessDefinitionVersion = PROCESS_DEFINITION_VERSION,
                name = "fakeOtherProcessFormName",
                filename = "fakeOtherProcessForm.json"
            )
        )

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
                .readZipEntries()

            then("only the task forms of the requested key and version are included") {
                zipEntries.keys shouldContainExactly setOf(
                    "fakeProcess.bpmn",
                    "fakeCurrentForm.json"
                )
            }
        }
    }

    given("two used task forms that were uploaded under the same filename") {
        stubProcessDefinitionModel(formKeys = listOf("fakeFirstFormName", "fakeSecondFormName"))
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(
            createFormForProcessDefinition(name = "fakeFirstFormName", filename = "fakeForm.json"),
            createFormForProcessDefinition(name = "fakeSecondFormName", filename = "fakeForm.json")
        )

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
                .readZipEntries()

            then("both are included, the second one disambiguated by its form name") {
                zipEntries.keys shouldContainExactly setOf(
                    "fakeProcess.bpmn",
                    "fakeForm.json",
                    "fakeForm-fakeSecondFormName.json"
                )
                zipEntries["fakeForm.json"] shouldBe """{ "name": "fakeFirstFormName" }"""
                zipEntries["fakeForm-fakeSecondFormName.json"] shouldBe
                    """{ "name": "fakeSecondFormName" }"""
            }
        }
    }

    given("a task form whose filename is already taken by the disambiguated path of another form") {
        stubProcessDefinitionModel(
            formKeys = listOf("fakeAlphaFormName", "fakeBetaFormName", "fakeGammaFormName")
        )
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(
            createFormForProcessDefinition(name = "fakeAlphaFormName", filename = "fakeForm-fakeGammaFormName.json"),
            createFormForProcessDefinition(name = "fakeBetaFormName", filename = "fakeForm.json"),
            createFormForProcessDefinition(name = "fakeGammaFormName", filename = "fakeForm.json")
        )

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
                .readZipEntries()

            then("all three are included, the last one falling back to a numbered path") {
                zipEntries.keys shouldContainExactly setOf(
                    "fakeProcess.bpmn",
                    "fakeForm-fakeGammaFormName.json",
                    "fakeForm.json",
                    "fakeForm-fakeGammaFormName-2.json"
                )
                zipEntries["fakeForm-fakeGammaFormName.json"] shouldBe """{ "name": "fakeAlphaFormName" }"""
                zipEntries["fakeForm.json"] shouldBe """{ "name": "fakeBetaFormName" }"""
                zipEntries["fakeForm-fakeGammaFormName-2.json"] shouldBe """{ "name": "fakeGammaFormName" }"""
            }
        }
    }

    given("a task form whose filename and name contain directories") {
        stubProcessDefinitionModel(formKeys = listOf("../fakeTraversedFormName"))
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(
            createBpmnProcessDefinitionTaskForm(
                bpmnProcessDefinitionKey = PROCESS_DEFINITION_KEY,
                bpmnProcessDefinitionVersion = PROCESS_DEFINITION_VERSION,
                name = "../fakeTraversedFormName",
                filename = "../../fakeTraversedForm.json"
            )
        )

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
                .readZipEntries()

            then("the task form is placed in the root of the zip") {
                zipEntries.keys shouldContainExactly setOf(
                    "fakeProcess.bpmn",
                    "fakeTraversedForm.json"
                )
            }
        }
    }

    given("a used and an unused task form that were uploaded under the same filename") {
        stubProcessDefinitionModel(formKeys = listOf("fakeUsedFormName"))
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(
            createFormForProcessDefinition(name = "fakeUsedFormName", filename = "fakeForm.json"),
            createFormForProcessDefinition(name = "fakeUnusedFormName", filename = "fakeForm.json")
        )

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
                .readZipEntries()

            then("both keep their original filename because only the unused one is put in a folder") {
                zipEntries.keys shouldContainExactly setOf(
                    "fakeProcess.bpmn",
                    "fakeForm.json",
                    "unused-forms/fakeForm.json"
                )
                zipEntries["fakeForm.json"] shouldBe """{ "name": "fakeUsedFormName" }"""
                zipEntries["unused-forms/fakeForm.json"] shouldBe """{ "name": "fakeUnusedFormName" }"""
            }
        }
    }

    given("a used task form that was uploaded under the same filename as the BPMN model") {
        stubProcessDefinitionModel(formKeys = listOf("fakeFormName"))
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns listOf(
            createFormForProcessDefinition(name = "fakeFormName", filename = "fakeProcess.bpmn")
        )

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
                .readZipEntries()

            then("the task form is disambiguated by its form name so the BPMN model is not overwritten") {
                zipEntries.keys shouldContainExactly setOf(
                    "fakeProcess.bpmn",
                    "fakeProcess-fakeFormName.bpmn"
                )
                zipEntries["fakeProcess.bpmn"] shouldBe PROCESS_DEFINITION_MODEL
                zipEntries["fakeProcess-fakeFormName.bpmn"] shouldBe """{ "name": "fakeFormName" }"""
            }
        }
    }

    given("a process definition whose resource name is not a BPMN file") {
        val processDefinitionWithoutBpmnResourceName = createProcessDefinition(
            id = PROCESS_DEFINITION_ID,
            key = PROCESS_DEFINITION_KEY,
            version = PROCESS_DEFINITION_VERSION,
            resourceName = "fakeResourceName"
        )
        stubProcessDefinitionModel(processDefinitionWithoutBpmnResourceName)
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns emptyList()

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService
                .getZipStreamOutput(processDefinitionWithoutBpmnResourceName)
                .readZipEntries()

            then("the BPMN model is named after the process definition key") {
                zipEntries.keys shouldContainExactly setOf("$PROCESS_DEFINITION_KEY.bpmn")
            }
        }
    }

    given("a process definition whose resource name contains a folder") {
        val processDefinitionWithFolderInResourceName = createProcessDefinition(
            id = PROCESS_DEFINITION_ID,
            key = PROCESS_DEFINITION_KEY,
            version = PROCESS_DEFINITION_VERSION,
            resourceName = "fakeFolder/fakeProcess.bpmn"
        )
        stubProcessDefinitionModel(processDefinitionWithFolderInResourceName)
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns emptyList()

        `when`("the zip is written") {
            val zipEntries = bpmnProcessDefinitionDownloadService
                .getZipStreamOutput(processDefinitionWithFolderInResourceName)
                .readZipEntries()

            then("the BPMN model is placed in the root of the zip") {
                zipEntries.keys shouldContainExactly setOf("fakeProcess.bpmn")
            }
        }
    }

    given("a deployed BPMN model that cannot be read") {
        every { bpmnService.readProcessDefinitionModel(PROCESS_DEFINITION_ID) } returns
            object : InputStream() {
                override fun read(): Int = throw IOException("fakeReadFailure")
            }

        `when`("the zip stream output is requested") {
            val bpmnProcessDefinitionDownloadException = shouldThrow<BpmnProcessDefinitionDownloadException> {
                bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
            }

            then("a BPMN process definition download exception is thrown for the process definition") {
                bpmnProcessDefinitionDownloadException.message shouldContain PROCESS_DEFINITION_KEY
            }
        }
    }

    given("an output stream that fails while the zip is being written") {
        val incompressibleModel = ByteArray(64 * 1024).also { Random(42).nextBytes(it) }
        every { bpmnService.readProcessDefinitionModel(PROCESS_DEFINITION_ID) } returns
            incompressibleModel.inputStream()
        every { bpmnService.getProcessDefinitionMetadata(processDefinition) } returns
            createBpmnProcessDefinitionMetadata()
        every { bpmnProcessDefinitionTaskFormService.listForms() } returns emptyList()
        val failingOutputStream = object : OutputStream() {
            override fun write(byte: Int) = throw IOException("fakeWriteFailure")
            override fun write(bytes: ByteArray, offset: Int, length: Int) = throw IOException("fakeWriteFailure")
        }

        `when`("the zip is written to it") {
            val bpmnProcessDefinitionDownloadException = shouldThrow<BpmnProcessDefinitionDownloadException> {
                bpmnProcessDefinitionDownloadService.getZipStreamOutput(processDefinition)
                    .write(failingOutputStream)
            }

            then("a BPMN process definition download exception is thrown for the failing entry") {
                bpmnProcessDefinitionDownloadException.message shouldContain "fakeProcess.bpmn"
            }
        }
    }
})
