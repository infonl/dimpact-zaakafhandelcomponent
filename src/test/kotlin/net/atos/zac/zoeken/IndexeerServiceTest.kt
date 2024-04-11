package net.atos.zac.zoeken

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.flowable.TakenService
import net.atos.zac.zoeken.converter.AbstractZoekObjectConverter
import net.atos.zac.zoeken.converter.ZaakZoekObjectConverter
import net.atos.zac.zoeken.model.ZoekObject
import net.atos.zac.zoeken.model.createZaakZoekObject
import net.atos.zac.zoeken.model.index.ZoekObjectType
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.eclipse.microprofile.config.ConfigProvider
import java.net.URI

@MockKExtension.CheckUnnecessaryStub
class IndexeerServiceTest : BehaviorSpec({
    val solrUrl = "http://localhost/dummySolrUrl"
    // add static mocking for config provider or else the IndexeerService class cannot be instantiated
    // since it references the config provider statically
    mockkStatic(ConfigProvider::class)
    every {
        ConfigProvider.getConfig().getValue("solr.url", String::class.java)
    } returns solrUrl

    // TODO: how to mock the Http2SolrClient that is created in the constructor of the IndexeerService class?
    val http2SolrClient = mockk<Http2SolrClient>()
    // mock the constructor for the Solr client builder to be able to mock the Solr client
    // instantiated in the constructor of the IndexeerService class
    mockkConstructor(Http2SolrClient.Builder::class)
    mockkStatic(Http2SolrClient.Builder("$solrUrl/solr/zac")::class)
    every { Http2SolrClient.Builder("$solrUrl/solr/zac") } returns mockk<Http2SolrClient.Builder>()
    every { anyConstructed<Http2SolrClient.Builder>().build() } returns http2SolrClient

    // https://stackoverflow.com/questions/52742926/how-to-mock-a-new-object-using-mockk

    val zaakZoekObjectConverter = mockk<ZaakZoekObjectConverter>()
    val converterInstances = mockk<Instance<AbstractZoekObjectConverter<out ZoekObject?>>>()
    val converterInstancesIterator = mockk<MutableIterator<AbstractZoekObjectConverter<out ZoekObject?>>>()
    val drcClientService = mockk<DRCClientService>()
    val takenService = mockk<TakenService>()
    val helper = mockk<IndexeerServiceHelper>()
    val zrcClientService = mockk<ZRCClientService>()

    val indexeerService = IndexeerService(
        converterInstances,
        zrcClientService,
        drcClientService,
        takenService,
        helper
    )

    every { zaakZoekObjectConverter.supports(ZoekObjectType.ZAAK) } returns true
    every { converterInstances.iterator() } returns converterInstancesIterator
    every { converterInstancesIterator.hasNext() } returns true andThen true andThen false
    every { converterInstancesIterator.next() } returns zaakZoekObjectConverter andThen zaakZoekObjectConverter

    Given("Two zaken") {
        val zaakType = createZaakType()
        val zaaktypeURI = URI("http://example.com/${zaakType.url}")
        val zaken = listOf(
            createZaak(zaaktypeURI = zaaktypeURI),
            createZaak(zaaktypeURI = zaaktypeURI)
        )
        val zaakZoekObjecten = listOf(
            createZaakZoekObject(),
            createZaakZoekObject()
        )
        zaken.forEachIndexed { index, zaak ->
            every { zaakZoekObjectConverter.convert(zaak.uuid.toString()) } returns zaakZoekObjecten[index]
        }

        When("The indexeer direct method is called to index the two zaken") {
            indexeerService.indexeerDirect(zaken.map { it.uuid.toString() }, ZoekObjectType.ZAAK)

            Then("") {
            }
        }
    }
})
