# OpenZaak Create Document Integration Test Helper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `createEnkelvoudigInformatieobject` function to `OpenZaakClient` that posts a document directly to Open Zaak's DRC API, bypassing ZAC, so integration tests can simulate externally created documents.

**Architecture:** Add `performZgwApiPostRequest` to `ItestHttpClient` (mirrors the existing `performZgwApiGetRequest`, auto-applies OpenZaak JWT via port check). Then add `createEnkelvoudigInformatieobject` to `OpenZaakClient`, which loads a file from test resources, base64-encodes it, and posts a JSON body to Open Zaak's DRC API.

**Tech Stack:** Kotlin, OkHttp3, `org.json.JSONObject`, `java.util.Base64`, `java.time.LocalDate`

---

## File Map

| File | Change |
|---|---|
| `src/itest/kotlin/nl/info/zac/itest/client/ItestHttpClient.kt` | Add `performZgwApiPostRequest` |
| `src/itest/kotlin/nl/info/zac/itest/client/OpenZaakClient.kt` | Add `createEnkelvoudigInformatieobject` |

---

### Task 1: Add `performZgwApiPostRequest` to `ItestHttpClient`

**Files:**
- Modify: `src/itest/kotlin/nl/info/zac/itest/client/ItestHttpClient.kt`

- [ ] **Step 1: Add the method after `performZgwApiGetRequest` (currently ends at line 329)**

  Open `src/itest/kotlin/nl/info/zac/itest/client/ItestHttpClient.kt` and insert the following method after the closing `}` of `performZgwApiGetRequest` (after line 329, before the private functions block):

  ```kotlin
  /**
   * Performs a ZGW API POST request on the given URL with optional headers.
   *
   * @param url The URL to perform the POST request on.
   * @param requestBodyAsString The JSON body of the POST request as a string.
   * @param headers Optional headers to include in the request. Defaults to standard headers for ZGW API requests.
   * @return A [ResponseContent] containing the response body, headers, and status code.
   */
  fun performZgwApiPostRequest(
      url: String,
      requestBodyAsString: String,
      headers: Headers = buildHeaders()
  ): ResponseContent {
      logger.info { "Performing POST request on: '$url'" }
      val request = Request.Builder()
          .headers(cloneHeadersWithAuthorization(headers, url))
          .url(url)
          .post(requestBodyAsString.toRequestBody(MediaType.APPLICATION_JSON.toMediaType()))
          .build()
      return okHttpClient.newCall(request).execute().use {
          logger.info { "Received response with status code: '${it.code}'" }
          ResponseContent(it.body.string(), it.headers, it.code)
      }
  }
  ```

  Note: `cloneHeadersWithAuthorization(headers, url)` (the two-argument overload without `accessToken`) auto-generates the OpenZaak JWT token when the URL port matches `OPEN_ZAAK_EXTERNAL_PORT`. No additional auth handling is needed.

- [ ] **Step 2: Commit**

  ```bash
  git add src/itest/kotlin/nl/info/zac/itest/client/ItestHttpClient.kt
  git commit -m "chore: add performZgwApiPostRequest to ItestHttpClient"
  ```

---

### Task 2: Add `createEnkelvoudigInformatieobject` to `OpenZaakClient`

**Files:**
- Modify: `src/itest/kotlin/nl/info/zac/itest/client/OpenZaakClient.kt`

- [ ] **Step 1: Replace the full contents of `OpenZaakClient.kt` with the updated version**

  The current file has only one function (`getRolesForZaak`). Add `createEnkelvoudigInformatieobject` and the required imports:

  ```kotlin
  /*
   * SPDX-FileCopyrightText: 2024 INFO.nl
   * SPDX-License-Identifier: EUPL-1.2+
   */
  package nl.info.zac.itest.client

  import com.auth0.jwt.JWT
  import com.auth0.jwt.algorithms.Algorithm.HMAC256
  import nl.info.zac.itest.config.ItestConfiguration.BRON_ORGANISATIE
  import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
  import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
  import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
  import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
  import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
  import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
  import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_ID
  import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_CLIENT_SECRET
  import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_EXTERNAL_URI
  import org.json.JSONObject
  import java.io.File
  import java.net.URLDecoder
  import java.time.LocalDate
  import java.util.Base64
  import java.util.Date
  import java.util.UUID

  class OpenZaakClient(
      val itestHttpClient: ItestHttpClient
  ) {
      fun getRolesForZaak(zaakUUID: UUID): ResponseContent =
          itestHttpClient.performZgwApiGetRequest(
              url = "$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/rollen?zaak=$OPEN_ZAAK_EXTERNAL_URI/zaken/api/v1/zaken/$zaakUUID"
          )

      /**
       * Creates an enkelvoudig informatieobject directly in Open Zaak's DRC API,
       * bypassing ZAC. Use this to simulate externally created documents in integration tests.
       *
       * The file is loaded from test resources and base64-encoded for the [inhoud] field.
       *
       * @param fileName Name of the file in test resources (e.g. "fäkeTestDocument.pdf")
       * @param title Document title; defaults to [DOCUMENT_FILE_TITLE]
       * @param informatieobjectTypeUUID UUID of the informatieobjecttype in Open Zaak;
       *   defaults to the "bijlage" type ([INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID])
       * @param vertrouwelijkheidaanduiding Confidentiality level;
       *   defaults to [DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK]
       * @return [ResponseContent] with the Open Zaak API response (HTTP 201 on success)
       */
      fun createEnkelvoudigInformatieobject(
          fileName: String,
          title: String = DOCUMENT_FILE_TITLE,
          informatieobjectTypeUUID: UUID = UUID.fromString(INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID),
          vertrouwelijkheidaanduiding: String = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
      ): ResponseContent {
          val file = Thread.currentThread().contextClassLoader.getResource(fileName).let {
              File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
          }
          val encodedContent = Base64.getEncoder().encodeToString(file.readBytes())
          val requestBody = JSONObject(
              mapOf(
                  "bronorganisatie" to BRON_ORGANISATIE,
                  "creatiedatum" to LocalDate.now().toString(),
                  "titel" to title,
                  "auteur" to FAKE_AUTHOR_NAME,
                  "taal" to "dut",
                  "informatieobjecttype" to
                      "$OPEN_ZAAK_BASE_URI/catalogi/api/v1/informatieobjecttypen/$informatieobjectTypeUUID",
                  "inhoud" to encodedContent,
                  "bestandsnaam" to fileName,
                  "bestandsomvang" to file.length(),
                  "vertrouwelijkheidaanduiding" to vertrouwelijkheidaanduiding,
                  "status" to DOCUMENT_STATUS_IN_BEWERKING
              )
          ).toString()
          return itestHttpClient.performZgwApiPostRequest(
              url = "$OPEN_ZAAK_EXTERNAL_URI/documenten/api/v1/enkelvoudiginformatieobjecten",
              requestBodyAsString = requestBody
          )
      }
  }

  /**
   * Generates a JWT token for OpenZaak client authentication from our integration tests.
   * Note that no user claims are added, as this is not required for these requests from
   * our integration tests.
   */
  fun generateOpenZaakJwtToken(): String =
      JWT.create().withIssuer(OPEN_ZAAK_CLIENT_ID)
          .withIssuedAt(Date())
          .withHeader(mapOf("client_identifier" to OPEN_ZAAK_CLIENT_ID))
          .withClaim("client_id", OPEN_ZAAK_CLIENT_ID)
          .sign(HMAC256(OPEN_ZAAK_CLIENT_SECRET))
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add src/itest/kotlin/nl/info/zac/itest/client/OpenZaakClient.kt
  git commit -m "feat: add createEnkelvoudigInformatieobject to OpenZaakClient"
  ```

---

### Task 3: Verify the build compiles

- [ ] **Step 1: Run the itest compilation check**

  ```bash
  ./gradlew compileItestKotlin
  ```

  Expected: `BUILD SUCCESSFUL`

  If you see import errors, double-check that:
  - `BRON_ORGANISATIE`, `DOCUMENT_FILE_TITLE`, `DOCUMENT_STATUS_IN_BEWERKING`, `FAKE_AUTHOR_NAME`, `INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID`, `OPEN_ZAAK_BASE_URI` are all defined in `ItestConfiguration.kt` (they are, verified during design)
  - `org.json.JSONObject` is on the itest classpath (it is — already used in `DocumentHelper.kt`)

- [ ] **Step 2: Commit if any compile fixes were needed**

  Only commit if Step 1 required a fix:
  ```bash
  git add -p
  git commit -m "fix: resolve compile errors in OpenZaakClient"
  ```
