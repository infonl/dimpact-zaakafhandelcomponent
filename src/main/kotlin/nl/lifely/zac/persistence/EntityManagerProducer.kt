/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import nl.lifely.zac.util.AllOpen

@AllOpen
@ApplicationScoped
class EntityManagerProducer {
    companion object {
        const val PERSISTENCE_UNIT_NAME = "ZaakafhandelcomponentPU"
    }

    @PersistenceContext(unitName = PERSISTENCE_UNIT_NAME)
    private lateinit var entityManager: EntityManager

    @Produces
    @ApplicationScoped
    fun getEntityManager() = entityManager
}
