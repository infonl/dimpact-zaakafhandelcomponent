/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.formio

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.Json
import jakarta.json.JsonObject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.zac.formio.model.FormioFormulier
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
@NoArgConstructor
@AllOpen
class FormioService @Inject constructor(
    private val entityManager: EntityManager
) {
    fun readFormioFormulier(name: String): JsonObject =
        findFormulierByName(name)?.content?.toJsonObject() ?: throw NoSuchElementException("No Formio form found with name: '$name'")

    fun listFormulieren(): List<FormioFormulier> =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(FormioFormulier::class.java).let { query ->
                query.from(FormioFormulier::class.java).let {
                    query.orderBy(criteriaBuilder.asc(it.get<String>("name")))
                }
                entityManager.createQuery(query).resultList
            }
        }

    @Transactional(Transactional.TxType.REQUIRED)
    fun addFormulier(filename: String, content: String) {
        val formulier = content.toJsonObject()
        FormioFormulier().apply {
            this.filename = filename
            this.content = content
            name = formulier.getJsonString("name")?.string ?: filename.removeSuffix(".json")
            title = formulier.getJsonString("title")?.string ?: StringUtils.EMPTY
            findFormulierByName(name)?.let {
                id = it.id
            }
        }.let { entityManager.merge(it) }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    fun deleteFormulier(id: Long) {
        entityManager.find(FormioFormulier::class.java, id)?.let { entityManager.remove(it) }
    }

    private fun findFormulierByName(name: String): FormioFormulier? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(FormioFormulier::class.java).let { query ->
                query.from(FormioFormulier::class.java).let {
                    query.where(criteriaBuilder.equal(it.get<String>("name"), name))
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }

    private fun String.toJsonObject(): JsonObject = Json.createReader(this.reader()).readObject()
}
