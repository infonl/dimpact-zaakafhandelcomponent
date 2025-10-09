package nl.info.client.brp.util

import nl.info.zac.configuratie.BrpConfiguration
import java.util.Optional

@Suppress("LongParameterList")
fun createBrpConfiguration(
    apiKey: Optional<String> = Optional.of("apiKey"),
    originOin: Optional<String> = Optional.of("originOin"),
    auditLogProvider: Optional<String> = Optional.of("iConnect"),
    queryPersonenDefaultPurpose: Optional<String> = Optional.of("queryPersonenPurpose"),
    retrievePersoonDefaultPurpose: Optional<String> = Optional.of("retrievePersoonPurpose"),
    processingRegisterDefault: Optional<String> = Optional.of("processingRegisterDefault")
) = BrpConfiguration(
    apiKey = apiKey,
    originOIN = originOin,
    auditLogProvider = auditLogProvider,
    queryPersonenDefaultPurpose = queryPersonenDefaultPurpose,
    retrievePersoonDefaultPurpose = retrievePersoonDefaultPurpose,
    processingRegisterDefault = processingRegisterDefault
)
