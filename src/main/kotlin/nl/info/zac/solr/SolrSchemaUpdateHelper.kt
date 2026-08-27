/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("TooManyFunctions")
package nl.info.zac.solr

import org.apache.solr.client.solrj.request.schema.SchemaRequest

const val NAME = "name"

private const val TYPE = "type"
private const val INDEXED = "indexed"
private const val STORED = "stored"
private const val DEFAULT = "default"
private const val DOC_VALUES = "docValues"
private const val MULTI_VALUED = "multiValued"

fun addField(name: String, type: FieldType) =
    SchemaRequest.AddField(mapOf(NAME to name, TYPE to type.value))

fun addField(name: String, type: FieldType, defaultValue: String) =
    SchemaRequest.AddField(mapOf(NAME to name, TYPE to type.value, DEFAULT to defaultValue))

fun addField(name: String, type: FieldType, docValues: Boolean) =
    SchemaRequest.AddField(mapOf(NAME to name, TYPE to type.value, DOC_VALUES to docValues))

fun addField(name: String, type: FieldType, indexed: Boolean, stored: Boolean) =
    SchemaRequest.AddField(mapOf(NAME to name, TYPE to type.value, INDEXED to indexed, STORED to stored))

fun addField(name: String, type: FieldType, indexed: Boolean, stored: Boolean, docValues: Boolean) =
    SchemaRequest.AddField(
        mapOf(NAME to name, TYPE to type.value, INDEXED to indexed, STORED to stored, DOC_VALUES to docValues)
    )

fun addFieldMultiValued(name: String, type: FieldType) =
    SchemaRequest.AddField(mapOf(NAME to name, TYPE to type.value, MULTI_VALUED to true))

fun addFieldMultiValued(name: String, type: FieldType, docValues: Boolean) =
    SchemaRequest.AddField(
        mapOf(NAME to name, TYPE to type.value, DOC_VALUES to docValues, MULTI_VALUED to true)
    )

fun addFieldMultiValued(name: String, type: FieldType, indexed: Boolean, stored: Boolean) =
    SchemaRequest.AddField(
        mapOf(NAME to name, TYPE to type.value, INDEXED to indexed, STORED to stored, MULTI_VALUED to true)
    )

fun addCopyField(source: String, vararg dest: String) =
    SchemaRequest.AddCopyField(source, dest.toList())

fun deleteCopyField(source: String, vararg dest: String) =
    SchemaRequest.DeleteCopyField(source, dest.toList())

fun addDynamicField(name: String, type: FieldType, indexed: Boolean, stored: Boolean) =
    SchemaRequest.AddDynamicField(mapOf(NAME to name, TYPE to type.value, INDEXED to indexed, STORED to stored))

fun addDynamicField(name: String, type: FieldType, indexed: Boolean, stored: Boolean, multiValued: Boolean) =
    SchemaRequest.AddDynamicField(
        mapOf(NAME to name, TYPE to type.value, INDEXED to indexed, STORED to stored, MULTI_VALUED to multiValued)
    )

fun deleteField(name: String) = SchemaRequest.DeleteField(name)
