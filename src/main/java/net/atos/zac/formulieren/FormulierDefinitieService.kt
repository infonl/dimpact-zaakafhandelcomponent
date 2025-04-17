/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import net.atos.zac.formulieren.model.FormulierDefinitie
import net.atos.zac.util.ValidationUtil
import java.time.ZonedDateTime
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Supplier

@ApplicationScoped
@Transactional
class FormulierDefinitieService {
    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private val entityManager: EntityManager? = null

    fun listFormulierDefinities(): MutableList<FormulierDefinitie?> {
        val builder = entityManager!!.getCriteriaBuilder()
        val query = builder.createQuery<FormulierDefinitie?>(FormulierDefinitie::class.java)
        val root = query.from<FormulierDefinitie?>(FormulierDefinitie::class.java)
        query.orderBy(builder.asc(root.get<Any?>("naam")))
        query.select(root)
        return entityManager.createQuery<FormulierDefinitie?>(query).getResultList()
    }

    fun readFormulierDefinitie(id: Long): FormulierDefinitie {
        val formulierDefinitie = entityManager!!.find<FormulierDefinitie?>(FormulierDefinitie::class.java, id)
        if (formulierDefinitie != null) {
            return formulierDefinitie
        } else {
            throw RuntimeException(
                "%s with id=%d not found".formatted(
                    FormulierDefinitie::class.java.getSimpleName(),
                    id
                )
            )
        }
    }

    fun readFormulierDefinitie(systeemnaam: String?): FormulierDefinitie? {
        return findFormulierDefinitie(systeemnaam)
            .orElseThrow<RuntimeException?>(Supplier {
                RuntimeException(
                    "%s with code='%s' not found".formatted(FormulierDefinitie::class.java.getSimpleName(), systeemnaam)
                )
            })
    }

    fun findFormulierDefinitie(systeemnaam: String?): Optional<FormulierDefinitie?> {
        val builder = entityManager!!.getCriteriaBuilder()
        val query = builder.createQuery<FormulierDefinitie?>(FormulierDefinitie::class.java)
        val root = query.from<FormulierDefinitie?>(FormulierDefinitie::class.java)
        query.select(root).where(builder.equal(root.get<Any?>("systeemnaam"), systeemnaam))
        val resultList = entityManager.createQuery<FormulierDefinitie?>(query).getResultList()
        return if (resultList.isEmpty()) Optional.empty<FormulierDefinitie?>() else Optional.of<FormulierDefinitie?>(
            resultList.getFirst()
        )
    }

    fun createFormulierDefinitie(formulierDefinitie: FormulierDefinitie): FormulierDefinitie? {
        val now = ZonedDateTime.now()
        formulierDefinitie.setCreatiedatum(now)
        formulierDefinitie.setWijzigingsdatum(now)
        ValidationUtil.valideerObject(formulierDefinitie)
        findFormulierDefinitie(formulierDefinitie.getSysteemnaam()).ifPresent(Consumer { e: FormulierDefinitie? ->
            if (e!!.getId() != formulierDefinitie.getId()) {
                throw RuntimeException(
                    "Er bestaat al een formulier definitie met systeemnaam '%s'".formatted(
                        formulierDefinitie.getSysteemnaam()
                    )
                )
            }
        })
        return entityManager!!.merge<FormulierDefinitie?>(formulierDefinitie)
    }

    fun updateFormulierDefinitie(formulierDefinitie: FormulierDefinitie): FormulierDefinitie? {
        val bestaandeDefinitie = readFormulierDefinitie(formulierDefinitie.getId())
        formulierDefinitie.setSysteemnaam(bestaandeDefinitie.getSysteemnaam())
        formulierDefinitie.setCreatiedatum(bestaandeDefinitie.getCreatiedatum())
        formulierDefinitie.setWijzigingsdatum(ZonedDateTime.now())
        ValidationUtil.valideerObject(formulierDefinitie)
        return entityManager!!.merge<FormulierDefinitie?>(formulierDefinitie)
    }

    fun deleteFormulierDefinitie(id: Long) {
        val formulierDefinitie = entityManager!!.find<FormulierDefinitie?>(FormulierDefinitie::class.java, id)
        // controleren op gebruik
        entityManager.remove(formulierDefinitie)
    }
}
