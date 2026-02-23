/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import nl.info.zac.itest.config.ItestConfiguration.FEATURE_FLAG_PABC_INTEGRATION

// new IAM test groups; these groups have functional roles for which mappings need to exist in the PABC
val GROUP_RAADPLEGERS_TEST_1 = TestGroup(
    name = "raadplegers-test-1",
    description = "Test group raadplegers domein test 1 - new IAM"
)
val GROUP_RAADPLEGERS_TEST_2 = TestGroup(
    name = "raadplegers-test-2",
    description = "Test group raadplegers domein test 2 - new IAM"
)
val GROUP_BEHANDELAARS_TEST_1 = TestGroup(
    name = "behandelaars-test-1",
    description = "Test group behandelaars domein test 1 - new IAM"
)
val GROUP_BEHANDELAARS_TEST_2 = TestGroup(
    name = "behandelaars-test-2",
    description = "Test group behandelaars domein test 2 - new IAM"
)
val GROUP_COORDINATORS_TEST_1 = TestGroup(
    name = "coordinators-test-1",
    description = "Test group coordinators domein test 1 - new IAM"
)
val GROUP_COORDINATORS_TEST_2 = TestGroup(
    name = "coordinators-test-2",
    description = "Test group coordinators domein test 2 - new IAM"
)
val GROUP_RECORDMANAGERS_TEST_1 = TestGroup(
    name = "recordmanagers-test-1",
    description = "Test group recordmanagers domein test 1 - new IAM"
)
val GROUP_RECORDMANAGERS_TEST_2 = TestGroup(
    name = "recordmanagers-test-2",
    description = "Test group recordmanagers domein test 2 - new IAM"
)
val GROUP_BEHEERDERS_ELK_DOMEIN = TestGroup(
    name = "beheerders-elk-domein",
    description = "Test group beheerders elk domein - new IAM"
)

// old IAM test groups; will be removed in the future; do not use these to test new IAM functionality
val OLD_IAM_TEST_GROUP_A = TestGroup(
    name = "test-group-a",
    description = "Test group A - old IAM"
)
val OLD_IAM_TEST_GROUP_RAADPLEGERS = TestGroup(
    name = "test-group-rp",
    description = "Test group raadplegers - old IAM"
)
val OLD_IAM_TEST_GROUP_BEHANDELAARS = TestGroup(
    name = "test-group-bh",
    description = "Test group behandelaars - old IAM"
)
val OLD_IAM_TEST_GROUP_COORDINATORS = TestGroup(
    name = "test-group-co",
    description = "Test group coordinators - old IAM"
)
val OLD_IAM_TEST_GROUP_RECORD_MANAGERS = TestGroup(
    name = "test-group-rm",
    description = "Test group recordmanagers - old IAM"
)
val OLD_IAM_TEST_GROUP_FUNCTIONAL_ADMINS = TestGroup(
    name = "test-group-fb",
    description = "Test group functional admins - old IAM"
)
val OLD_IAM_GROUP_DOMEIN_TEST_1 = TestGroup(
    name = "test-group-domein-test-1",
    description = "Test group which has access to domein_test_1 only - old IAM"
)
val OLD_IAM_GROUP_DOMEIN_TEST_2 = TestGroup(
    name = "test-group-domein-test-2",
    description = "Test group which has access to domein_test_2 only - old IAM"
)

// group constants that switch between old and new IAM test groups based on the PABC feature flag value
val RAADPLEGERS_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) GROUP_RAADPLEGERS_TEST_1 else OLD_IAM_TEST_GROUP_RAADPLEGERS
val BEHANDELAARS_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) GROUP_BEHANDELAARS_TEST_1 else OLD_IAM_TEST_GROUP_BEHANDELAARS
val COORDINATORS_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) GROUP_COORDINATORS_TEST_1 else OLD_IAM_TEST_GROUP_COORDINATORS
val RECORDMANAGERS_DOMAIN_TEST_1 = if (FEATURE_FLAG_PABC_INTEGRATION) GROUP_RECORDMANAGERS_TEST_1 else OLD_IAM_TEST_GROUP_RECORD_MANAGERS

// these BPMN test groups are also defined in the BPMN integration test process and BPMN form.io task forms
val BPMN_TEST_GROUP_1 = TestGroup(name = "test-group-1", description = "BPMN Test Group 1")
val BPMN_TEST_GROUP_2 = TestGroup(name = "test-group-2", description = "BPMN Test Group 2")
