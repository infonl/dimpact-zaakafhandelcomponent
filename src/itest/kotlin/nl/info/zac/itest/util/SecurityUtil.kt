/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_1_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_2_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_2_NAME
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_2_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.BEHANDELAAR_2_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.BEHEERDER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.BEHEERDER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_BEHANDELAAR_1_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_BEHANDELAAR_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_BEHANDELAAR_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_BEHANDELAAR_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_USER_DOMEIN_TEST_2_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_USER_DOMEIN_TEST_2_NAME
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_USER_DOMEIN_TEST_2_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.OLD_IAM_TEST_USER_DOMEIN_TEST_2_USERNAME

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

fun getBehandelaarDomainTest2User(): TestUser = if (FEATURE_FLAG_PABC_INTEGRATION) {
    TestUser(
        username = BEHANDELAAR_2_USERNAME,
        password = BEHANDELAAR_2_PASSWORD,
        displayName = BEHANDELAAR_2_NAME,
        email = BEHANDELAAR_2_EMAIL
    )
} else {
    TestUser(
        username = OLD_IAM_TEST_USER_DOMEIN_TEST_2_USERNAME,
        password = OLD_IAM_TEST_USER_DOMEIN_TEST_2_PASSWORD,
        displayName = OLD_IAM_TEST_USER_DOMEIN_TEST_2_NAME,
        email = OLD_IAM_TEST_USER_DOMEIN_TEST_2_EMAIL
    )
}
