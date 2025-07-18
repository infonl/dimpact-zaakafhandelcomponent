package nl.info.zac.shared.config

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import nl.info.zac.util.AllOpen
import okhttp3.OkHttpClient

@AllOpen
@ApplicationScoped
class OkHttpClientProducer {
    @Produces
    fun produceOkHttpClient(): OkHttpClient = OkHttpClient()
}
