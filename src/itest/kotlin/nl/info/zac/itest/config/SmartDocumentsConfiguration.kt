/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.config

import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID

const val SMART_DOCUMENTS_MOCK_BASE_URI = "http://smartdocuments-wiremock:8080"

/**
 * Constants used in the SmartDocuments WireMock template response
 */
const val SMART_DOCUMENTS_ROOT_GROUP_ID = "D5037631FF6748269059B353699EFA0C"
const val SMART_DOCUMENTS_ROOT_GROUP_NAME = "root"
const val SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID = "445E1A2C5D964A33961CA46679AB51CF"
const val SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME = "root template 1"
const val SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID = "8CCCF38A7757473CB5F5F2B8E5D7484D"
const val SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME = "root template 2"
const val SMART_DOCUMENTS_GROUP_1_ID = "0E18B04EDF9646C0A2D04E651DC4C6FF"
const val SMART_DOCUMENTS_GROUP_1_NAME = "group 1"
const val SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID = "7B7857BB9959470C82974037304E433D"
const val SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME = "group 1 template 1"
const val SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID = "273C2707E5A844699B653C87ACFD618E"
const val SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME = "group 1 template 2"
const val SMART_DOCUMENTS_GROUP_2_ID = "348097107FA346DC9AEBBE33A5500B79"
const val SMART_DOCUMENTS_GROUP_2_NAME = "group 2"
const val SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID = "7B7857BB9959470C82974037304E433D"
const val SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME = "group 2 template 1"
const val SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID = "273C2707E5A844699B653C87ACFD618E"
const val SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME = "group 2 template 2"

const val SMART_DOCUMENTS_FILE_ID = "fakeFileId"
const val SMART_DOCUMENTS_FILE_TITLE = "Smart Documents file"

val SMART_DOCUMENTS_TEMPLATE_MAPPINGS = """
            [
              {
                "id": "$SMART_DOCUMENTS_ROOT_GROUP_ID",
                "name": "$SMART_DOCUMENTS_ROOT_GROUP_NAME",
                "groups": [
                  {
                    "groups": [],
                    "id": "$SMART_DOCUMENTS_GROUP_1_ID",
                    "name": "$SMART_DOCUMENTS_GROUP_1_NAME",
                    "templates": [
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      },
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      }
                    ]
                  },
                  {
                    "groups": [],
                    "id": "$SMART_DOCUMENTS_GROUP_2_ID",
                    "name": "$SMART_DOCUMENTS_GROUP_2_NAME",
                    "templates": [
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      },
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      }
                    ]
                  }
                ],
                "templates": [
                  {
                    "id": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID",
                    "name": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME",
                    "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                  },
                  {
                    "id": "$SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID",
                    "name": "$SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME",
                    "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                  }
                ]
              }
            ]
""".trimIndent()
