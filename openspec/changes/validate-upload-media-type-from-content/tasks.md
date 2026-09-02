## 1. Dependencies

- [ ] 1.1 Add `tika = "3.3.1"` to `gradle/libs.versions.toml` and three library entries: `tika-core` (`org.apache.tika:tika-core`), `tika-parser-zip-commons` (`org.apache.tika:tika-parser-zip-commons`) and `tika-parser-microsoft-module` (`org.apache.tika:tika-parser-microsoft-module`).
- [ ] 1.2 Add the three as `implementation` dependencies in `build.gradle.kts`, with exclusions on the microsoft module for the parsing-only transitives: `org.apache.poi:poi-ooxml-full`, `org.apache.poi:poi-scratchpad`, group `org.bouncycastle`, `org.apache.commons:commons-math3`, group `com.healthmarketscience.jackcess`, group `com.pff`, group `org.jsoup` and group `org.apache.james`. Add a comment stating that these are excluded because only detection is used, and that the format-coverage test in task 5.3 is what catches a future Tika release moving a detector into an excluded artifact.
- [ ] 1.3 Do **not** add `tika-parser-audiovideo-module`. Its Matroska detector does not improve content-only detection and it pulls in `metadata-extractor` and `xmpcore` for no benefit.
- [ ] 1.4 Decide the `log4j-api` question from design.md's open questions: either add `log4j-to-slf4j` so POI's logging routes into ZAC's existing SLF4J setup, or record the decision to accept the startup error line. Verify by starting ZAC and checking the log.
- [ ] 1.5 Run `./gradlew dependencies --configuration runtimeClasspath` and confirm the resolved set matches the expectation in design.md (22 jars for the Tika subtree, 6 of which ZAC already had). Check the OWASP dependency check task still passes and add suppressions only if genuinely needed.

## 2. Media type detection

- [ ] 2.1 Add a class in `nl.info.zac.configuration` that determines a media type from a `ByteArray`. Obtain the detector once from `TikaConfig.getDefaultConfig().getDetector()` and hold it, plus the `MediaTypeRegistry`, in a `companion object` — `getDefaultConfig` is a cached singleton and `DefaultDetector` is thread-safe, so per-call construction would be waste.
- [ ] 2.2 Detect from content only: construct an empty `Metadata` and never set `TikaCoreProperties.RESOURCE_NAME_KEY` or `CONTENT_TYPE`. Document in the class KDoc *why* — the file name and the declared media type are client-supplied, and supplying either lets the caller steer the check meant to constrain it. Keep this the single call site for detection.
- [ ] 2.3 Normalize the detected media type through `MediaTypeRegistry.normalize` before comparing, so Tika's own aliases (`text/rtf` for `application/rtf`, `video/avi` and `video/msvideo` for `video/x-msvideo`) fold into the canonical type.
- [ ] 2.4 Return the base type only, discarding parameters such as `charset`, so a detected `text/plain; charset=UTF-8` compares equal to `text/plain`.

## 3. Allowlist

- [ ] 3.1 Extend `AllowedFileType` with a per-entry set of media types that content detection may legitimately yield. Default it to the entry's existing `mediaType` so only the two exceptions need a value; leave `mediaType` itself unchanged in meaning, since `RestAllowedFileType`, `/rest/configuratie/file-types` and the frontend depend on it.
- [ ] 3.2 Set the exceptions: `.mp4` also accepts `video/quicktime`, `.mkv` also accepts `application/x-matroska`. Explain in the enum KDoc that these formats share a container with a sibling and cannot be separated by content, so a `.mov` renamed to `.mp4` is accepted by design — both extensions are on the allowlist.
- [ ] 3.3 Add a content-aware `isAllowed(filename, content)`: the extension must resolve to an entry, and the detected media type must equal that entry's canonical type, be in its accepted set, or satisfy `MediaTypeRegistry.isSpecializationOf(detected, expected)`. Keep the direction one-way — a detected type that is a *parent* of the expected one must be rejected, otherwise a plain ZIP would pass as a `.docx`.
- [ ] 3.4 Keep the existing extension-only `isAllowed(filename)` if callers outside the validators still need it; otherwise remove it so there is no way to perform the weaker check by accident.

## 4. Validators and converter

- [ ] 4.1 Change `ValidRestEnkelvoudigInformatieFileUploadFormValidator` and `ValidRestFileUploadFormValidator` to call the content-aware check. Preserve both existing outcomes: neither file nor name is valid (a metadata-only version update), and a name without content is invalid.
- [ ] 4.2 Return a distinct message for a content/extension mismatch using `ConstraintValidatorContext.buildConstraintViolationWithTemplate` with a new message key, keeping the existing key for a disallowed extension. Disable the default violation when building a custom one.
- [ ] 4.3 Add the new message key to `src/main/app/src/assets/i18n/nl.json` and `en.json`, next to `msg.error.document.upload.invalid`, using kebab-case for a multi-word final segment. The Dutch text should tell the user the file's content does not match its extension, not just that the file was refused.
- [ ] 4.4 In `RestInformatieobjectConverter`, write the extension's canonical `mediaType` instead of the client-supplied value at the three `formaat` assignments (document creation from `restEnkelvoudigInformatieobject.formaat`, task document from `bestand.type`, and new version from `restEnkelvoudigInformatieObjectVersieGegevens.formaat`). Write the canonical value rather than the raw detection result, so Open Zaak does not end up holding `video/quicktime` for a `.mp4` or `text/html` for a `.txt`.
- [ ] 4.5 Confirm no endpoint changes are needed: all three multipart handlers already mark their `@MultipartForm` parameter `@Valid`, so they inherit the stricter check. Verify this rather than assuming — a missing `@Valid` has silently disabled this validation before.

## 5. Tests

- [ ] 5.1 Add Kotest `BehaviorSpec` unit tests for the detector: it identifies content correctly, it returns the same result for identical bytes regardless of the file name supplied to the *validator*, and it strips media type parameters. State the behaviour in the `given`/`when`/`then` descriptions rather than in comments.
- [ ] 5.2 Add unit tests for the extended `AllowedFileType`: an extension not on the allowlist is refused; matching content is accepted; mismatched content is refused; a specialization of the expected type is accepted (HTML content under `.txt`); a parent of the expected type is refused (a ZIP under `.docx`); the `.mp4` and `.mkv` accepted sets work; and an alias of a canonical type is accepted.
- [ ] 5.3 Add a format-coverage test that asserts, for a real sample file of every one of the 26 allowlisted extensions, that content-only detection produces a media type the allowlist accepts for that extension. This is the test that fails if a Tika upgrade moves a detector into one of the artifacts excluded in task 1.2. Resolve the open question in design.md about where the sample files live before writing it.
- [ ] 5.4 Add a test asserting the name-hint constraint directly: the same bytes uploaded under a spoofed allowlisted extension must be refused, and refused with the mismatch message rather than the disallowed-extension message. This is what fails if a future change starts passing the file name to Tika, which would otherwise leave every happy-path test green.
- [ ] 5.5 Extend the integration tests for the upload endpoints with a rejected spoof and an accepted genuine file: at minimum an executable renamed to `.pdf`, a Word document renamed to `.pdf`, a plain ZIP renamed to `.docx`, and a genuine `.docx` and `.pdf` as controls. Cover all three endpoints, including the task-form upload.
- [ ] 5.6 Add an integration assertion that the document created in Open Zaak carries the canonical media type for its extension even when the request declares a different one or declares none.
- [ ] 5.7 Update the existing `AllowedFileTypeTest` for the new signature, and check whether the Bruno collection `zaakafhandelcomponent_file_upload_validation` needs new requests for the spoof cases.

## 6. Verification and cleanup

- [ ] 6.1 Run `./gradlew spotlessApply detektApply` and fix any findings.
- [ ] 6.2 Run `./gradlew test` and confirm new and existing tests pass.
- [ ] 6.3 Build the Docker image and run `./gradlew itest --info`.
- [ ] 6.4 Verify in a browser against the local stack: upload a genuine `.pdf` and a genuine `.docx` successfully, then rename an executable to `.pdf` and a `.doc` to `.pdf` and confirm each is refused with the mismatch message visible in the error dialog — not a silent form reset.
- [ ] 6.5 Confirm detection has no noticeable effect on a large upload by uploading a file close to the 80 MB limit.
- [ ] 6.6 Check the ZAC startup log for POI or Tika warnings, and confirm the resolved WAR size increase is in line with design.md.
