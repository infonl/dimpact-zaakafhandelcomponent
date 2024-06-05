package net.atos.zac.smartdocuments

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.zac.documentcreatie.DocumentCreatieService
import net.atos.zac.smartdocuments.rest.RESTSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toModel
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toREST
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class SmartDocumentsService @Inject constructor(
    private val documentCreatieService: DocumentCreatieService,
    private var zaakafhandelParameterService: ZaakafhandelParameterService,
) {
    companion object {
        @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
        lateinit var entityManager: EntityManager

        private val LOG = Logger.getLogger(SmartDocumentsService::class.java.name)
    }

    fun listTemplates() = documentCreatieService.listTemplates().toREST()

    @Transactional(REQUIRED)
    fun storeTemplatesMapping(restTemplateGroups: Set<RESTSmartDocumentsTemplateGroup>, zaakafhandelUUID: UUID) {
        LOG.info { "Storing template mapping for zaakafhandelParameters UUID $zaakafhandelUUID"}

        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelUUID)
        val modelTemplateGroups = restTemplateGroups.toModel(zaakafhandelParameters)

        modelTemplateGroups.forEach { templateGroup ->
            entityManager.merge(templateGroup)
        }
    }

    fun getTemplatesMapping(zaakafhandelUUID: UUID): Set<SmartDocumentsTemplateGroup> {
        LOG.info { "Fetching template mapping for zaakafhandelParameters UUID $zaakafhandelUUID"}

        val zaakafhandelParametersId = zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelUUID).id
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            SmartDocumentsTemplateGroup::class.java
        )
        val root = query.from(
            SmartDocumentsTemplateGroup::class.java
        )
        return entityManager.createQuery(
            query.select(root)
                .where(
                    builder.equal(
                        root.get<ZaakafhandelParameters>("zaakafhandelParameters").get<Long>("id"),
                        zaakafhandelParametersId
                    )
                )
        ).resultList.toSet()
    }
}
