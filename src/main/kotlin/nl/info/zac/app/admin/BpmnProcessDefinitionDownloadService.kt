/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin

import jakarta.inject.Inject
import jakarta.ws.rs.core.StreamingOutput
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.flowable.bpmn.exception.BpmnProcessDefinitionDownloadException
import nl.info.zac.flowable.bpmn.model.isUsedIn
import org.flowable.engine.repository.ProcessDefinition
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BpmnProcessDefinitionDownloadService @Inject constructor(
    private val bpmnService: BpmnService,
    private val bpmnProcessDefinitionTaskFormService: BpmnProcessDefinitionTaskFormService
) {
    companion object {
        private const val BPMN_FILE_EXTENSION = ".bpmn"
        private const val JSON_FILE_EXTENSION = ".json"
        private const val UNUSED_FORMS_FOLDER = "unused-forms"
    }

    /**
     * Returns the process definition and all task forms stored for its version as a zip.
     * Forms that no user task references end up in a separate folder, so that whoever imports
     * the zip elsewhere can leave them out.
     *
     * All content is read before the zip is streamed, so that no database or Flowable repository
     * access is needed anymore once writing has started.
     */
    fun getProcessDefinitionAndTaskFormsAsZipStream(processDefinition: ProcessDefinition): StreamingOutput =
        collectFiles(processDefinition).let { files ->
            StreamingOutput { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOutputStream ->
                    files.forEach { (path, content) -> addToZipOutputStream(path, content, zipOutputStream) }
                }
            }
        }

    private fun collectFiles(processDefinition: ProcessDefinition): List<Pair<String, ByteArray>> {
        val usedPaths = mutableSetOf<String>()
        val files = mutableListOf(
            getProcessDefinitionPath(processDefinition).also(usedPaths::add) to
                readProcessDefinitionModel(processDefinition)
        )
        val formKeys = bpmnService.getProcessDefinitionMetadata(processDefinition).formKeys
        bpmnProcessDefinitionTaskFormService.listForms()
            .filter {
                it.bpmnProcessDefinitionKey == processDefinition.key &&
                    it.bpmnProcessDefinitionVersion == processDefinition.version
            }
            .forEach { form ->
                val folderPrefix = if (form.isUsedIn(formKeys)) "" else "$UNUSED_FORMS_FOLDER/"
                files.add(
                    getFormPath(folderPrefix, form.filename, form.name, usedPaths) to
                        form.content.toByteArray()
                )
            }
        return files
    }

    private fun readProcessDefinitionModel(processDefinition: ProcessDefinition): ByteArray =
        try {
            bpmnService.readProcessDefinitionModel(processDefinition.id).use { it.readBytes() }
        } catch (ioException: IOException) {
            throw BpmnProcessDefinitionDownloadException(
                "Failed to read the BPMN model of process definition '${processDefinition.key}'",
                ioException
            )
        }

    private fun getProcessDefinitionPath(processDefinition: ProcessDefinition) =
        processDefinition.resourceName
            ?.withoutDirectories()
            ?.takeIf { it.endsWith(BPMN_FILE_EXTENSION) }
            ?: "${processDefinition.key}$BPMN_FILE_EXTENSION"

    private fun String.withoutDirectories() = substringAfterLast('/').substringAfterLast('\\')

    private fun getFormPath(
        folderPrefix: String,
        filename: String,
        name: String,
        usedPaths: MutableSet<String>
    ): String {
        val formName = name.withoutDirectories()
        val formFilename = filename.withoutDirectories().ifBlank { "$formName$JSON_FILE_EXTENSION" }
        val baseName = formFilename.substringBeforeLast('.')
        val extension = formFilename.removePrefix(baseName)
        return (
            sequenceOf("$baseName$extension", "$baseName-$formName$extension") +
                generateSequence(2) { it + 1 }.map { "$baseName-$formName-$it$extension" }
            )
            .map { "$folderPrefix$it" }
            .first(usedPaths::add)
    }

    private fun addToZipOutputStream(path: String, content: ByteArray, zipOutputStream: ZipOutputStream) {
        try {
            zipOutputStream.putNextEntry(ZipEntry(path))
            zipOutputStream.write(content)
            zipOutputStream.closeEntry()
        } catch (ioException: IOException) {
            throw BpmnProcessDefinitionDownloadException(
                "Failed to add '$path' to zip outputStream",
                ioException
            )
        }
    }
}
