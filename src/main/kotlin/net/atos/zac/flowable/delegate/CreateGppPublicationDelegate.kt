/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate

import net.atos.client.gpppublicatiebank.DocumentCreate
import net.atos.client.gpppublicatiebank.PublicatiestatusEnum
import net.atos.client.gpppublicatiebank.PublicationRead
import net.atos.client.gpppublicatiebank.PublicationWrite
import net.atos.zac.flowable.FlowableHelper
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

class CreateGppPublicationDelegate : AbstractDelegate() {
    companion object {
        private val LOG = Logger.getLogger(CreateGppPublicationDelegate::class.java.name)
    }

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var aantalDagen: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var opschortingReden: Expression

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var selectedDocuments: Expression

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val publication = createGppPublication(flowableHelper, execution)
        if (publication.urlPublicatieIntern?.isNotBlank() == true) {
            LOG.info { "Internal publication URL: '${publication.urlPublicatieIntern}'" }
        }
        selectedDocuments.resolveValueAsList(execution).forEach { document ->
            LOG.info { "Adding document: '$document' to publication: '${publication.uuid}'" }
            // TODO: hardcode a document UUID for now
            //  we need the UUID and from BPMN form we only have the title for now
//            val documentUUID = UUID.fromString("74f86a7a-5670-4268-9754-99e345d03888")
//            createDocumentForPublication(
//                flowableHelper = flowableHelper,
//                publicationUUID = publication.uuid,
//                creationDate = LocalDate.now(),
//                officialTitle = "Test titel",
//                documentUrl = URI("http://localhost:8001/documenten/api/v1/enkelvoudiginformatieobjecten/$documentUUID")
//            )
        }
        suspendZaak(flowableHelper, execution)
    }

    private fun createGppPublication(flowableHelper: FlowableHelper, execution: DelegateExecution): PublicationRead {
        LOG.info {
            "Creating GPP publication for zaak '${getZaakIdentificatie(execution)}' " +
                "and selected documents: '${selectedDocuments.resolveValueAsList(execution)}'."
        }
        val publicationWrite = PublicationWrite().apply {
            publicatiestatus = PublicatiestatusEnum.CONCEPT
            officieleTitel = "Publicatie voor zaak '${getZaakIdentificatie(execution)}'"
        }
        val publication = flowableHelper.gppPublicatiebankClientService.createPublicatie(publicationWrite)
        LOG.info("Created GPP publication: '$publication'")
        return publication
    }

    /**
     * Does not work yet because GPP Publicatiebank needs to be authorised to access ZAC's Open Zaak first.
     */
    @Suppress("LongParameterList")
    private fun createDocumentForPublication(
        flowableHelper: FlowableHelper,
        publicationUUID: UUID,
        creationDate: LocalDate,
        officialTitle: String,
        documentUrl: URI
    ) {
        LOG.info {
            "Creating document for publication with UUID '$publicationUUID', " +
                "document title: '$officialTitle', and document URL: '$documentUrl'."
        }
        val documentCreate = DocumentCreate().apply {
            publicatie = publicationUUID
            creatiedatum = creationDate
            officieleTitel = officialTitle
            this.documentUrl = documentUrl
        }
        val document = flowableHelper.gppPublicatiebankClientService.createDocument(documentCreate)
        LOG.info("Created GPP document: '$document' for publication: '$publicationUUID'")
    }

    private fun suspendZaak(flowableHelper: FlowableHelper, execution: DelegateExecution) {
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))
        LOG.info(
            "Suspending zaak '${zaak.identificatie}' from activity '${execution.currentActivityName}' " +
                "for $aantalDagen days with reason '$opschortingReden'"
        )
        flowableHelper.suspensionZaakHelper.suspendZaak(
            zaak = zaak,
            numberOfDays = aantalDagen.resolveValueAsLong(execution),
            suspensionReason = opschortingReden.resolveValueAsString(execution)
        )
    }
}
