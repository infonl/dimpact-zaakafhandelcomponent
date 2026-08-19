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
        private const val UNUSED_FORMS_FOLDER = "unused-forms"
    }

    /**
     * Returns the process definition and all task forms stored for its version as a zip.
     * Forms that no user task references end up in a separate folder, so that whoever imports
     * the zip elsewhere can leave them out.
     *
     * All content is resolved before streaming starts, so that the database and the Flowable
     * repository are only read while the REST resource method is still active.
     */
    fun getZipStreamOutput(processDefinition: ProcessDefinition): StreamingOutput =
        collectFiles(processDefinition).let { files ->
            StreamingOutput { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOutputStream ->
                    files.forEach { (path, content) -> addToZip(path, content, zipOutputStream) }
                    zipOutputStream.finish()
                }
                outputStream.flush()
                outputStream.close()
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
            ?.substringAfterLast('/')
            ?.takeIf { it.endsWith(BPMN_FILE_EXTENSION) }
            ?: "${processDefinition.key}$BPMN_FILE_EXTENSION"

    /**
     * Task form filenames are not unique per process definition version, whereas their names are.
     * A filename that is already taken is therefore disambiguated using the form name,
     * so that no form is silently left out of the zip.
     */
    private fun getFormPath(
        folderPrefix: String,
        filename: String,
        name: String,
        usedPaths: MutableSet<String>
    ): String {
        val path = "$folderPrefix$filename"
        if (usedPaths.add(path)) return path
        val extension = filename.substringAfterLast('.', "")
        return if (extension.isEmpty()) {
            "$path-$name"
        } else {
            "$folderPrefix${filename.dropLast(extension.length + 1)}-$name.$extension"
        }.also(usedPaths::add)
    }

    private fun addToZip(path: String, content: ByteArray, zipOutputStream: ZipOutputStream) {
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
