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
 * Records which productaanvraag objects ZAC has already taken on, so that a notification which
 * Open Notificaties redelivers does not result in a second zaak.
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
         * PostgreSQL evaluates the insert and the conflicting-row update as one atomic statement, so two
         * concurrent callers - in the same or in a different ZAC instance - can never both claim the same
         * productaanvraag. A row is only reclaimed when its previous claim was never completed and has since
         * gone stale.
         */
        private const val CLAIM_QUERY = """
            INSERT INTO $TABLE (uuid_productaanvraag_object, status, gestart_op)
            VALUES (CAST(:productaanvraagObjectUUID AS uuid), 'IN_PROGRESS', now())
            ON CONFLICT (uuid_productaanvraag_object) DO UPDATE
            SET status = 'IN_PROGRESS', gestart_op = now()
            WHERE verwerkte_productaanvraag.status = 'IN_PROGRESS'
              AND verwerkte_productaanvraag.gestart_op <
                  now() - make_interval(mins => CAST(:claimTimeoutMinutes AS int))
        """

        private const val MARK_DONE_QUERY = """
            UPDATE $TABLE
            SET status = 'DONE'
            WHERE uuid_productaanvraag_object = CAST(:productaanvraagObjectUUID AS uuid)
        """
    }

    /**
     * Commits in its own transaction so that a concurrent notification for the same productaanvraag
     * observes the claim immediately, independent of the caller's transaction scope.
     */
    @Transactional(REQUIRES_NEW)
    fun claim(productaanvraagObjectUUID: UUID): Boolean =
        entityManager.createNativeQuery(CLAIM_QUERY)
            .setParameter("productaanvraagObjectUUID", productaanvraagObjectUUID.toString())
            .setParameter("claimTimeoutMinutes", claimTimeoutMinutes)
            .executeUpdate() > 0

    @Transactional(REQUIRED)
    fun markDone(productaanvraagObjectUUID: UUID) {
        entityManager.createNativeQuery(MARK_DONE_QUERY)
            .setParameter("productaanvraagObjectUUID", productaanvraagObjectUUID.toString())
            .executeUpdate()
    }
}
