/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

// these groups have functional roles for which mappings need to exist in the PABC
val GROUP_RAADPLEGERS_TEST_1 = TestGroup(
    name = "raadplegers-test-1",
    description = "Test group raadplegers domein test 1"
)
val GROUP_RAADPLEGERS_TEST_2 = TestGroup(
    name = "raadplegers-test-2",
    description = "Test group raadplegers domein test 2"
)
val GROUP_BEHANDELAARS_TEST_1 = TestGroup(
    name = "behandelaars-test-1",
    description = "Test group behandelaars domein test 1"
)
val GROUP_BEHANDELAARS_TEST_2 = TestGroup(
    name = "behandelaars-test-2",
    description = "Test group behandelaars domein test 2"
)
val GROUP_COORDINATORS_TEST_1 = TestGroup(
    name = "coordinators-test-1",
    description = "Test group coordinators domein test 1"
)
val GROUP_COORDINATORS_TEST_2 = TestGroup(
    name = "coordinators-test-2",
    description = "Test group coordinators domein test 2"
)
val GROUP_RECORDMANAGERS_TEST_1 = TestGroup(
    name = "recordmanagers-test-1",
    description = "Test group recordmanagers domein test 1"
)
val GROUP_RECORDMANAGERS_TEST_2 = TestGroup(
    name = "recordmanagers-test-2",
    description = "Test group recordmanagers domein test 2"
)
val GROUP_BEHEERDERS_ELK_DOMEIN = TestGroup(
    name = "beheerders-elk-domein",
    description = "Test group beheerders elk domein"
)
val GROUP_INACTIVE_TEST_1 = TestGroup(
    name = "inactive-group-test-1",
    description = "Test group inactive"
)

// these BPMN test assignees and groups are also defined in the BPMN integration test process and BPMN form.io task forms
val BPMN_TEST_BEHANDELAAR_1 = TestGroup(name = "test-behandelaar-1", description = "BPMN test behandelaar 1")
val BPMN_TEST_GROUP_1 = TestGroup(name = "test-group-1", description = "BPMN test group 1")
