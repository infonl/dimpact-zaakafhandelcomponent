package net.atos.zac.smartdocuments

import jakarta.annotation.Resource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import jakarta.transaction.UserTransaction
import net.atos.zac.documentcreatie.DocumentCreatieService
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toREST
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class SmartDocumentsService @Inject constructor(
    private val documentCreatieService: DocumentCreatieService
) {
    companion object {
        @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
        lateinit var entityManager: EntityManager

        @Resource
        lateinit var userTransaction: UserTransaction
    }

    fun listTemplates() = documentCreatieService.listTemplates().toREST()

    fun storeTemplatesMapping(modelTemplateGroups: Set<SmartDocumentsTemplateGroup>) {
        userTransaction.begin()
        modelTemplateGroups.forEach {
            entityManager.persist(it)
        }
        userTransaction.commit()
    }

    fun getTemplatesMapping(zaakafhandelParametersId: Long): Set<SmartDocumentsTemplateGroup> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            SmartDocumentsTemplateGroup::class.java
        )
        val root = query.from(
            SmartDocumentsTemplateGroup::class.java
        )
        return entityManager.createQuery(
            query.select(root)
                .where(builder.equal(root.get<Long>("zaakafhandelParameterId"), zaakafhandelParametersId))
        ).resultList.toSet()
    }
}
