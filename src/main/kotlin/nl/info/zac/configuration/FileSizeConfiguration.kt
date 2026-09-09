/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import nl.info.zac.configuration.exception.FileSizeExceededException
import nl.info.zac.configuration.exception.FileTooLargeToOpenException
import nl.info.zac.configuration.exception.InvalidFileSizeConfigurationException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.InputStream
import java.util.logging.Logger

/**
 * The two file size limits that ZAC enforces, and the check that the configured values fit in the
 * available heap.
 *
 * ZAC distinguishes two limits because the two code paths have fundamentally different memory
 * characteristics:
 *
 * - [maxFileSizeMB] applies to storing and retrieving a document. Both are streamed end to end, so
 *   the heap cost does not grow with the size of the document. What does grow is the temporary disk
 *   space used by RESTEasy, see `dev.resteasy.entity.file.threshold` in the WildFly configuration.
 * - [maxInMemoryFileSizeMB] applies to the operations that have to hold the whole document in
 *   memory: converting to PDF, sending as a mail attachment and editing through WebDAV. Here the
 *   heap cost is a multiple of the document size, which is why this value is validated against the
 *   heap on startup and is normally much lower than [maxFileSizeMB].
 *
 * It doubles as the threshold above which a document is uploaded to the documents registry in parts
 * rather than as a single base64 encoded request body.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
class FileSizeConfiguration @Inject constructor(
    @ConfigProperty(name = ENV_VAR_MAX_FILE_SIZE_MB, defaultValue = DEFAULT_MAX_FILE_SIZE_MB)
    val maxFileSizeMB: Long,

    @ConfigProperty(name = ENV_VAR_MAX_IN_MEMORY_FILE_SIZE_MB, defaultValue = DEFAULT_MAX_IN_MEMORY_FILE_SIZE_MB)
    val maxInMemoryFileSizeMB: Long
) {
    companion object {
        const val ENV_VAR_MAX_FILE_SIZE_MB = "MAX_FILE_SIZE_MB"
        const val ENV_VAR_MAX_IN_MEMORY_FILE_SIZE_MB = "MAX_IN_MEMORY_FILE_SIZE_MB"

        const val DEFAULT_MAX_FILE_SIZE_MB = "500"
        const val DEFAULT_MAX_IN_MEMORY_FILE_SIZE_MB = "80"

        const val BYTES_PER_MB = 1024L * 1024L

        /**
         * Peak heap usage of an operation that holds a whole document in memory, as a multiple of the
         * document size: the document itself, its base64 representation which is a third larger, and
         * the buffer used to serialise the request or response.
         */
        const val IN_MEMORY_OPERATION_HEAP_FACTOR = 3

        /**
         * Share of the heap that in-memory document operations may claim. The remainder is left for
         * everything else ZAC does while such an operation is running.
         */
        const val IN_MEMORY_OPERATION_HEAP_BUDGET_FRACTION = 0.5

        /**
         * The largest document ZAC can describe to the documents registry. `bestandsomvang` is an
         * `Int` in the ZGW Documenten API, so a larger document cannot be expressed in bytes without
         * overflowing it.
         */
        const val MAX_SUPPORTED_FILE_SIZE_MB = Int.MAX_VALUE / (1024 * 1024)

        private val LOG = Logger.getLogger(FileSizeConfiguration::class.java.name)
    }

    val maxFileSizeBytes = maxFileSizeMB * BYTES_PER_MB

    val maxInMemoryFileSizeBytes = maxInMemoryFileSizeMB * BYTES_PER_MB

    /**
     * The in-memory maximum as an array length. A byte array cannot hold more than [Int.MAX_VALUE]
     * bytes however large the configured maximum is, and reading one byte beyond the limit has to
     * stay within that too.
     */
    val inMemoryLimitAsInt = maxInMemoryFileSizeBytes
        .coerceAtMost(Int.MAX_VALUE.toLong() - Byte.MAX_VALUE)
        .toInt()

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) {
        validate()
        LOG.info {
            """ZAC file size configuration:
            |- $ENV_VAR_MAX_FILE_SIZE_MB: '$maxFileSizeMB'
            |- $ENV_VAR_MAX_IN_MEMORY_FILE_SIZE_MB: '$maxInMemoryFileSizeMB'
            |- maximum heap size in MB: '${maxHeapSizeMB()}'
            """.trimMargin()
        }
    }

    fun assertFileSizeAllowed(fileSizeBytes: Long) {
        if (fileSizeBytes > maxFileSizeBytes) {
            throw FileSizeExceededException(
                "File size of $fileSizeBytes bytes exceeds the maximum of $maxFileSizeBytes bytes " +
                    "($ENV_VAR_MAX_FILE_SIZE_MB=$maxFileSizeMB)"
            )
        }
    }

    fun assertFileCanBeHeldInMemory(fileSizeBytes: Long) {
        if (fileSizeBytes > maxInMemoryFileSizeBytes) {
            throw FileTooLargeToOpenException(
                "File size of $fileSizeBytes bytes exceeds the maximum of $maxInMemoryFileSizeBytes bytes " +
                    "for operations that cannot stream ($ENV_VAR_MAX_IN_MEMORY_FILE_SIZE_MB=$maxInMemoryFileSizeMB)"
            )
        }
    }

    /**
     * Reads [inputStream] completely, refusing content beyond the in-memory maximum.
     *
     * The size the documents registry reports is optional, so a document that reports no size at all
     * passes [assertFileCanBeHeldInMemory]. Counting the bytes that actually arrive is what keeps
     * such a document from being read onto the heap in full.
     *
     * @throws FileTooLargeToOpenException when the content exceeds the in-memory maximum.
     */
    fun readWithinInMemoryLimit(inputStream: InputStream): ByteArray {
        // read one byte beyond the limit so that content of exactly the limit is still accepted
        val bytes = inputStream.readNBytes(inMemoryLimitAsInt + 1)
        assertFileCanBeHeldInMemory(bytes.size.toLong())
        return bytes
    }

    fun isUploadedInParts(fileSizeBytes: Long) = fileSizeBytes > maxInMemoryFileSizeBytes

    private fun validate() {
        validationFailure()?.let { throw InvalidFileSizeConfigurationException(it) }
    }

    private fun validationFailure(): String? {
        val requiredHeapMB = maxInMemoryFileSizeMB * IN_MEMORY_OPERATION_HEAP_FACTOR
        val availableHeapMB = (maxHeapSizeMB() * IN_MEMORY_OPERATION_HEAP_BUDGET_FRACTION).toLong()
        return when {
            maxFileSizeMB <= 0 || maxInMemoryFileSizeMB <= 0 ->
                "$ENV_VAR_MAX_FILE_SIZE_MB ($maxFileSizeMB) and " +
                    "$ENV_VAR_MAX_IN_MEMORY_FILE_SIZE_MB ($maxInMemoryFileSizeMB) must both be greater than zero"
            maxFileSizeMB > MAX_SUPPORTED_FILE_SIZE_MB ->
                "$ENV_VAR_MAX_FILE_SIZE_MB ($maxFileSizeMB) cannot be larger than " +
                    "$MAX_SUPPORTED_FILE_SIZE_MB MB, because the documents registry expresses the size of a " +
                    "document as a 32 bit integer number of bytes"
            maxInMemoryFileSizeMB > maxFileSizeMB ->
                "$ENV_VAR_MAX_IN_MEMORY_FILE_SIZE_MB ($maxInMemoryFileSizeMB) cannot be larger than " +
                    "$ENV_VAR_MAX_FILE_SIZE_MB ($maxFileSizeMB)"
            requiredHeapMB > availableHeapMB ->
                "$ENV_VAR_MAX_IN_MEMORY_FILE_SIZE_MB ($maxInMemoryFileSizeMB) requires at least " +
                    "$requiredHeapMB MB of heap but only $availableHeapMB MB of the ${maxHeapSizeMB()} MB heap " +
                    "is available for it. Either lower $ENV_VAR_MAX_IN_MEMORY_FILE_SIZE_MB to at most " +
                    "${availableHeapMB / IN_MEMORY_OPERATION_HEAP_FACTOR} MB or raise the JVM maximum heap size."
            else -> null
        }
    }

    private fun maxHeapSizeMB() = Runtime.getRuntime().maxMemory() / BYTES_PER_MB
}
