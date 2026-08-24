/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.REQUIRES_NEW
import jakarta.transaction.Transactional.TxType.SUPPORTS
import nl.info.zac.database.flyway.FlywayIntegrator
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.UUID

/**
 * Records the productaanvraag objects that ZAC processes. If a notification request for the same productaanvraag object is received again by ZAC, ZAC does not handle this productaanvraag again and will not create a second zaak.
 */
@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class ProductaanvraagClaimRepository @Inject constructor(
    private val entityManager: EntityManager,

    @ConfigProperty(name = "PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES", defaultValue = "10")
    private val claimTimeoutMinutes: Int
) {
    companion object {
        private const val TABLE = "${FlywayIntegrator.SCHEMA}.verwerkte_productaanvraag"

        /**
         * ZAC inserts its other entities with entityManager.persist. A claim needs more: an insert or a
         * conditional update in one atomic statement. The JPA specification has no such operation, because
         * persist only inserts and JPQL has no INSERT clause. Hibernate adds a conflict clause to HQL as a
         * vendor extension. That extension needs an entity mapping for a table which ZAC never reads
         * through JPA. The interval arithmetic below stays PostgreSQL-specific in both forms.
         *
         * PostgreSQL runs the insert and the conflicting-row update as one atomic statement. Thus two
         * concurrent callers, in the same ZAC instance or in a different one, never claim the same
         * productaanvraag. ZAC reclaims a row only if the previous claim did not complete and is now stale.
         */
        private const val CLAIM_QUERY = """
            INSERT INTO $TABLE (uuid_productaanvraag_object, status, gestart_op)
            VALUES (:productaanvraagObjectUUID, 'IN_PROGRESS', now())
            ON CONFLICT (uuid_productaanvraag_object) DO UPDATE
            SET status = 'IN_PROGRESS', gestart_op = now()
            WHERE verwerkte_productaanvraag.status = 'IN_PROGRESS'
              AND verwerkte_productaanvraag.gestart_op <
                  now() - make_interval(mins => :claimTimeoutMinutes)
        """

        private const val MARK_DONE_QUERY = """
            UPDATE $TABLE
            SET status = 'DONE'
            WHERE uuid_productaanvraag_object = :productaanvraagObjectUUID
        """
    }

    /**
     * This function commits in its own transaction. Thus a concurrent notification for the same
     * productaanvraag sees the claim immediately, independent of the transaction scope of the caller.
     */
    @Transactional(REQUIRES_NEW)
    fun claim(productaanvraagObjectUUID: UUID): Boolean =
        entityManager.createNativeQuery(CLAIM_QUERY)
            .setParameter("productaanvraagObjectUUID", productaanvraagObjectUUID)
            .setParameter("claimTimeoutMinutes", claimTimeoutMinutes)
            .executeUpdate() > 0

    @Transactional(REQUIRED)
    fun markDone(productaanvraagObjectUUID: UUID) {
        entityManager.createNativeQuery(MARK_DONE_QUERY)
            .setParameter("productaanvraagObjectUUID", productaanvraagObjectUUID)
            .executeUpdate()
    }
}
