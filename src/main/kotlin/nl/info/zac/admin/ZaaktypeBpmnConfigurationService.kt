/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeBpmnConfigurationService @Inject constructor(
    private val zaaktypeBpmnConfigurationBeheerService: ZaaktypeBpmnConfigurationBeheerService
) {
    companion object {
        private val LOG = Logger.getLogger(ZaaktypeBpmnConfigurationService::class.java.name)
    }

    fun checkIfProductaanvraagtypeIsNotAlreadyInUse(productaanvraagtype: String) {
        zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(productaanvraagtype)?.let {
            LOG.info("Productaanvraagtype '$it' is already in use by BPMN zaaktype ${it.zaaktypeOmschrijving}")
            throw InputValidationFailedException(ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE)
        }
    }

    fun checkIfProductaanvraagtypeIsNotAlreadyInUse(zaaktypeBpmnConfiguration: ZaaktypeBpmnConfiguration) {
        zaaktypeBpmnConfiguration.productaanvraagtype?.let {
            zaaktypeBpmnConfigurationBeheerService.findConfigurationByProductAanvraagType(it)?.let { zaaktype ->
                if (zaaktype.zaaktypeUuid != zaaktypeBpmnConfiguration.zaaktypeUuid) {
                    LOG.info(
                        "Productaanvraagtype '$it' is already in use by BPMN zaaktype ${zaaktype.zaaktypeOmschrijving}"
                    )
                    throw InputValidationFailedException(ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE)
                }
            }
        }
    }
}
