/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_1_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.BEHEERDER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.BEHEERDER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_BEHANDELAAR_1_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_BEHANDELAAR_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_BEHANDELAAR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_USER_1_USERNAME
import nl.info.zac.itest.util.TestUser

fun authenticateAsBeheerderElkZaaktype() = if (FEATURE_FLAG_PABC_INTEGRATION) {
    authenticate(
        username = BEHEERDER_1_USERNAME,
        password = BEHEERDER_1_PASSWORD
    )
} else {
    authenticate(
        username = OLD_IAM_TEST_USER_1_USERNAME,
        password = OLD_IAM_TEST_USER_1_PASSWORD
    )
}

fun getBehandelaarDomainTest1User(): TestUser = if (FEATURE_FLAG_PABC_INTEGRATION) {
    TestUser(
        username = BEHANDELAAR_1_USERNAME,
        password = BEHANDELAAR_1_PASSWORD,
        displayName = BEHANDELAAR_1_NAME,
        email = BEHANDELAAR_1_EMAIL
    )
} else {
    TestUser(
        username = OLD_IAM_TEST_BEHANDELAAR_1_USERNAME,
        password = OLD_IAM_TEST_BEHANDELAAR_1_PASSWORD,
        displayName = OLD_IAM_TEST_BEHANDELAAR_1_NAME,
        email = OLD_IAM_TEST_BEHANDELAAR_1_EMAIL
    )
}
