/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.info.zac.admin.exception.ZaaktypeInUseException
import nl.info.zac.flowable.bpmn.model.ZaaktypeBpmnConfiguration
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeBpmnConfigurationService @Inject constructor(
    private val entityManager: EntityManager,
    private val zaaktypeCmmnConfigurationBeheerService: ZaaktypeCmmnConfigurationBeheerService
) {
    fun storeZaaktypeBpmnConfiguration(zaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration) {
        zaaktypeCmmnConfigurationBeheerService.readZaaktypeCmmnConfiguration(
            zaaktypeBpmnConfiguration.zaaktypeUuid
        )?.let {
            throw ZaaktypeInUseException(
                "CMMN configuration for zaaktype '${zaaktypeBpmnConfiguration.zaaktypeOmschrijving}' already exists"
            )
        }
        zaaktypeBpmnConfiguration.id?.let {
            entityManager.merge(zaaktypeBpmnConfiguration)
        } ?: entityManager.persist(zaaktypeBpmnConfiguration)
    }

    fun deleteZaaktypeBpmnConfiguration(zaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration) {
        entityManager.remove(zaaktypeBpmnConfiguration)
    }

    /**
     * Returns the zaaktype - BPMN process definition relation for the given zaaktype UUID or 'null'
     * if no BPMN process definition could be found for the given zaaktype UUID.
     */
    fun findZaaktypeBpmnConfigurationByZaaktypeUuid(zaaktypeUUID: UUID): ZaaktypeBpmnConfiguration? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java).let {
                    query.where(
                        criteriaBuilder.equal(
                            it.get<UUID>(ZaaktypeBpmnConfiguration.ZAAKTYPE_UUID_VARIABLE_NAME),
                            zaaktypeUUID
                        )
                    )
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }

    /**
     * Returns a list of all BPMN process definitions.
     */
    fun listZaaktypeBpmnConfigurations(): List<ZaaktypeBpmnConfiguration> =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java)
                entityManager.createQuery(query).resultList
            }
        }

    fun findZaaktypeBpmnConfigurationByProductAanvraagType(
        productAanvraagType: String
    ): ZaaktypeBpmnConfiguration? =
        entityManager.criteriaBuilder.let { criteriaBuilder ->
            criteriaBuilder.createQuery(ZaaktypeBpmnConfiguration::class.java).let { query ->
                query.from(ZaaktypeBpmnConfiguration::class.java).let {
                    query.where(
                        criteriaBuilder.equal(
                            it.get<String>(ZaaktypeBpmnConfiguration.PRODUCTAANVRAAGTTYPE_VARIABELE_NAME),
                            productAanvraagType
                        )
                    )
                }
                entityManager.createQuery(query).resultStream.findFirst().orElse(null)
            }
        }
}
