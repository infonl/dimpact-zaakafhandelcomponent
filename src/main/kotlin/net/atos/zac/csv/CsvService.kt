/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.csv

import com.opencsv.CSVWriter
import jakarta.ws.rs.core.StreamingOutput
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.zoekobject.ZoekObject
import java.beans.Introspector
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicInteger

private const val SEPARATOR = ';'
private const val QUOTE_ESCAPE_CHAR = '"'
private const val LINE_END = "\n"

private val EXCLUDED_PROPERTIES = listOf(
    "class",
    "uuid",
    "zaaktypeUuid",
    "type",
    "zaakUUID",
    "taakData",
    "taakInformatie",
    "id",
    "zaaktypeIdentificatie",
    "zaakGeautoriseerdeMedewerkers"
)

class CsvService {
    fun exportToCsv(zoekResultaat: ZoekResultaat<out ZoekObject>): StreamingOutput {
        val headerCounter = AtomicInteger()
        val headers = mutableListOf<String>()
        val records = mutableListOf<Array<String>>()
        zoekResultaat.items.forEach { zoekObject ->
            val record = mutableListOf<String>()
            val propertyDescriptors = Introspector.getBeanInfo(zoekObject.javaClass).propertyDescriptors
            propertyDescriptors.forEach { property ->
                val getter = property.readMethod
                if (property.name !in EXCLUDED_PROPERTIES && getter != null) {
                    if (headerCounter.get() < propertyDescriptors.size) {
                        headerCounter.getAndIncrement()
                        headers.add(property.displayName)
                    }
                    record.add(
                        when (val value = getter.invoke(zoekObject)) {
                            is Boolean -> if (value) "Ja" else "Nee"
                            null -> ""
                            else -> value.toString()
                        }
                    )
                }
            }
            records.add(record.toTypedArray())
        }

        return StreamingOutput { outputStream ->
            CSVWriter(
                OutputStreamWriter(outputStream),
                SEPARATOR,
                QUOTE_ESCAPE_CHAR,
                QUOTE_ESCAPE_CHAR,
                LINE_END
            ).use { csvWriter ->
                csvWriter.writeNext(headers.toTypedArray())
                csvWriter.writeAll(records)
            }
            outputStream.flush()
            outputStream.close()
        }
    }
}
