/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHEERDER_TEST_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_BEHEERDER_TEST_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME

fun authenticateAsBeheerderElkZaaktype() = if (FEATURE_FLAG_PABC_INTEGRATION) {
    authenticate(
        username = TEST_BEHEERDER_TEST_1_USERNAME,
        password = TEST_BEHEERDER_TEST_1_PASSWORD
    )
} else {
    authenticate(
        username = TEST_USER_1_USERNAME,
        password = TEST_USER_1_PASSWORD
    )
}
