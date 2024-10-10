/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.formio

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.Json
import jakarta.json.JsonObject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import net.atos.zac.formio.model.FomioFormulier
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils

@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
@NoArgConstructor
@AllOpen
class FormioService @Inject constructor(
    private val entityManager: EntityManager
) {

    fun readFormioFormulier(name: String): JsonObject =
        findFormulierByName(name)?.let {
            parseJsonObject(it.content)
        } ?: throw NoSuchElementException("No FormioFormulier found with name: $name")

    fun listFormulieren(): List<FomioFormulier> =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(FomioFormulier::class.java).let { query ->
                query.from(FomioFormulier::class.java).let {
                    query.orderBy(criteriaBuilder.asc(it.get<String>("name")))
                }
                entityManager.createQuery(query).resultList
            }
        }

    @Transactional(Transactional.TxType.REQUIRED)
    fun addFormulier(filename: String, content: String) {
        FomioFormulier().apply {
            val formulier = parseJsonObject(content)
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
        entityManager.remove(entityManager.find(FomioFormulier::class.java, id))
    }

    private fun findFormulierByName(name: String): FomioFormulier? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(FomioFormulier::class.java).let { query ->
                query.from(FomioFormulier::class.java).let {
                    query.where(criteriaBuilder.equal(it.get<String>("name"), name))
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }

    private fun parseJsonObject(content: String): JsonObject =
        Json.createReader(content.reader()).readObject()
}
