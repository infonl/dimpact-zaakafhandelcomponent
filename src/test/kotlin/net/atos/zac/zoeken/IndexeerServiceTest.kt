package net.atos.zac.zoeken

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.zoeken.converter.AbstractZoekObjectConverter
import net.atos.zac.zoeken.converter.ZaakZoekObjectConverter
import net.atos.zac.zoeken.model.ZoekObject
import net.atos.zac.zoeken.model.createZaakZoekObject
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.response.UpdateResponse
import org.eclipse.microprofile.config.ConfigProvider
import java.net.URI

class IndexeerServiceTest : BehaviorSpec({
    // add static mocking for config provider because the IndexeerService class
    // references the config provider statically
    val solrUrl = "http://localhost/dummySolrUrl"
    mockkStatic(ConfigProvider::class)
    every {
        ConfigProvider.getConfig().getValue("solr.url", String::class.java)
    } returns solrUrl

    val solrClient = mockk<SolrClient>()
    mockkStatic(IndexeerService::class)
    every { IndexeerService.createSolrClient("$solrUrl/solr/zac") } returns solrClient

    val zaakZoekObjectConverter = mockk<ZaakZoekObjectConverter>()
    val converterInstances = mockk<Instance<AbstractZoekObjectConverter<out ZoekObject?>>>()
    val converterInstancesIterator = mockk<MutableIterator<AbstractZoekObjectConverter<out ZoekObject?>>>()
    val drcClientService = mockk<DRCClientService>()
    val flowableTaskService = mockk<FlowableTaskService>()
    val zrcClientService = mockk<ZRCClientService>()

    val indexeerService = IndexeerService(
        converterInstances,
        zrcClientService,
        drcClientService,
        flowableTaskService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given(
        """Two zaken"""
    ) {
        val zaakType = createZaakType()
        val zaaktypeURI = URI("http://example.com/${zaakType.url}")
        val zaken = listOf(
            createZaak(zaakTypeURI = zaaktypeURI),
            createZaak(zaakTypeURI = zaaktypeURI)
        )
        val zaakZoekObjecten = listOf(
            createZaakZoekObject(),
            createZaakZoekObject()
        )
        every { zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
        every { converterInstances.iterator() } returns converterInstancesIterator
        every { converterInstancesIterator.hasNext() } returns true andThen true andThen false
        every { converterInstancesIterator.next() } returns zaakZoekObjectConverter andThen zaakZoekObjectConverter
        zaken.forEachIndexed { index, zaak ->
            every { zaakZoekObjectConverter.convert(zaak.uuid.toString()) } returns zaakZoekObjecten[index]
        }
        every { solrClient.addBeans(zaakZoekObjecten) } returns UpdateResponse()

        When(
            """The indexeer direct method is called to index the two zaken"""
        ) {
            indexeerService.indexeerDirect(zaken.map { it.uuid.toString() }.stream(), ZoekObjectType.ZAAK, false)

            Then(
                """
                two zaak zoek objecten should be added to the Solr client and 
                both related object ids should be removed as 'marked for indexing'                
                """
            ) {
                verify(exactly = 1) {
                    solrClient.addBeans(any<Collection<*>>())
                }
            }
        }
    }
})
