/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("TooManyFunctions")

package nl.info.zac.flowable.task

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAKTYPE_OMSCHRIJVING
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAKTYPE_UUID
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_IDENTIFICATIE
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_UUID
import net.atos.zac.flowable.exception.VariableNotFoundException
import nl.info.zac.flowable.util.isCmmnTask
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.BooleanUtils
import org.flowable.engine.TaskService
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo
import java.util.UUID

private const val TAAK_DATA_ONDERTEKENEN = "ondertekenen"
private const val TAAK_DATA_ZAAK_OPSCHORTEN = "zaakOpschorten"
private const val TAAK_DATA_ZAAK_HERVATTEN = "zaakHervatten"
private const val TAAK_DATA_MAIL_FROM = "verzender"
private const val TAAK_DATA_MAIL_REPLYTO = "replyTo"
private const val TAAK_DATA_MAIL_TO = "emailadres"
private const val TAAK_DATA_MAIL_BODY = "body"
private const val TAAK_DATA_MAIL_BIJLAGEN = "bijlagen"
private const val TAAK_DATA_STUUR_GEGEVENS_SENDMAIL = "taakStuurGegevens.sendMail"
private const val TAAK_DATA_STUUR_GEGEVENS_MAIL = "taakStuurGegevens.mail"

private const val VAR_TASK_TAAKDATA = "taakdata"
private const val VAR_TASK_TAAKDOCUMENTEN = "taakdocumenten"
private const val VAR_TASK_TAAKINFORMATIE = "taakinformatie"

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
class TaakVariabelenService @Inject constructor(
    private val taskService: TaskService
) {
    companion object {
        const val TAAK_DATA_MULTIPLE_VALUE_JOIN_CHARACTER = ";"
        const val TAAK_DATA_DOCUMENTEN_VERZENDEN_POST = "documentenVerzendenPost"
        const val TAAK_DATA_VERZENDDATUM = "verzenddatum"
        const val TAAK_DATA_TOELICHTING = "toelichting"
    }

    fun setTaskData(task: Task, taakdata: Map<String, Any>?) =
        setTaskVariable(task, VAR_TASK_TAAKDATA, taakdata)

    fun setTaskinformation(task: Task, taakinformatie: Map<String, String>?) =
        setTaskVariable(task, VAR_TASK_TAAKINFORMATIE, taakinformatie)

    fun setTaakdocumenten(task: Task, taakdocumenten: List<UUID>) =
        setTaskVariable(task, VAR_TASK_TAAKDOCUMENTEN, taakdocumenten)

    private fun setTaskVariable(task: Task, variableName: String, value: Any?) =
        taskService.setVariableLocal(task.id, variableName, value)
}

@Suppress("UNCHECKED_CAST")
fun readTaskData(taskInfo: TaskInfo) =
    (findTaskVariable(taskInfo, VAR_TASK_TAAKDATA) as Map<String, Any>?).orEmpty()

@Suppress("UNCHECKED_CAST")
fun readTaskInformation(taskInfo: TaskInfo) =
    (findTaskVariable(taskInfo, VAR_TASK_TAAKINFORMATIE) as Map<String, String>?).orEmpty()

@Suppress("UNCHECKED_CAST")
fun readTaskDocuments(taskInfo: TaskInfo) =
    (findTaskVariable(taskInfo, VAR_TASK_TAAKDOCUMENTEN) as List<UUID>?).orEmpty()

fun readMailFrom(taakData: Map<String, String>) = findStringTaskDataElement(taakData, TAAK_DATA_MAIL_FROM)

fun readMailReplyTo(taakData: Map<String, String>) = findStringTaskDataElement(taakData, TAAK_DATA_MAIL_REPLYTO)

fun readMailTo(taakData: Map<String, String>) = findStringTaskDataElement(taakData, TAAK_DATA_MAIL_TO)

fun readMailBody(taakData: Map<String, String>) = findStringTaskDataElement(taakData, TAAK_DATA_MAIL_BODY)

fun setMailBody(taakData: MutableMap<String, String>, body: String?) {
    if (body != null) taakData[TAAK_DATA_MAIL_BODY] = body else taakData.remove(TAAK_DATA_MAIL_BODY)
}

fun readMailAttachments(taakData: Map<String, String>) = findStringTaskDataElement(taakData, TAAK_DATA_MAIL_BIJLAGEN)

fun readSignatures(taakData: Map<String, Any>) = findObjectTaskDataElement(taakData, TAAK_DATA_ONDERTEKENEN)

fun readSendDataMail(taakData: Map<String, Any>) = findObjectTaskDataElement(taakData, TAAK_DATA_STUUR_GEGEVENS_MAIL)

fun isZaakOpschorten(taakData: Map<String, String>) =
    findStringTaskDataElement(taakData, TAAK_DATA_ZAAK_OPSCHORTEN) == BooleanUtils.TRUE

fun isZaakHervatten(taakData: Map<String, Any>) =
    findObjectTaskDataElement(taakData, TAAK_DATA_ZAAK_HERVATTEN) == BooleanUtils.TRUE

fun isSendDataSendMail(taakData: Map<String, Any>) =
    findObjectTaskDataElement(taakData, TAAK_DATA_STUUR_GEGEVENS_SENDMAIL) == BooleanUtils.TRUE

fun readZaakUUID(taskInfo: TaskInfo) = readVariable(taskInfo, VAR_ZAAK_UUID) as UUID

fun readZaakIdentificatie(taskInfo: TaskInfo) = readVariable(taskInfo, VAR_ZAAK_IDENTIFICATIE) as String

fun readZaaktypeUUID(taskInfo: TaskInfo) = readVariable(taskInfo, VAR_ZAAKTYPE_UUID) as UUID

fun readZaaktypeOmschrijving(taskInfo: TaskInfo) = readVariable(taskInfo, VAR_ZAAKTYPE_OMSCHRIJVING) as String

private fun getVariables(taskInfo: TaskInfo): Map<String, Any> =
    if (taskInfo.isCmmnTask()) taskInfo.caseVariables else taskInfo.processVariables

private fun readVariable(taskInfo: TaskInfo, variableName: String) =
    getVariables(taskInfo)[variableName] ?: throw VariableNotFoundException(
        "No variable found with name '$variableName' for task with name '${taskInfo.name}' and id '${taskInfo.id}'"
    )

private fun findTaskVariable(taskInfo: TaskInfo, variableName: String): Any? =
    taskInfo.taskLocalVariables[variableName]

private fun findStringTaskDataElement(taakData: Map<String, String>, elementName: String) =
    taakData[elementName]?.takeIf { it.isNotEmpty() }

private fun findObjectTaskDataElement(taakData: Map<String, Any>, elementName: String) =
    (taakData[elementName] as? String)?.takeIf { it.isNotEmpty() }
