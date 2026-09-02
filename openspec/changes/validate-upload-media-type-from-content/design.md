## Context

Three multipart endpoints let a user put a file into ZAC, and all three already funnel through two class-level bean validation constraints:

- `EnkelvoudigInformatieObjectRestService.createEnkelvoudigInformatieobjectAndUploadFile` and `updateEnkelvoudigInformatieobjectAndUploadFile` take subclasses of `RestEnkelvoudigInformatieFileUpload`, annotated `@ValidRestEnkelvoudigInformatieFileUploadForm`.
- `TaskRestService.uploadFile` takes `RestFileUpload`, annotated `@ValidRestFileUploadForm`.

Both validators today call `AllowedFileType.isAllowed(filename)`, which resolves the extension against a 26-entry enum and returns whether it is present. Nothing reads the file's content. Both form classes hold the upload as `file: ByteArray?`, so the complete content is already in memory at the moment validation runs — content inspection needs no change to streaming, temporary files, or endpoint signatures.

`AllowedFileType` also carries one canonical `mediaType` per extension, currently used only to tell the frontend which media type belongs to an extension. Its KDoc records why the client-reported media type is not validated: it comes from the client OS and varies per machine. A previous attempt to validate it caused silent upload failures for `.avi` and other formats with several MIME aliases. That value is nevertheless written straight into Open Zaak (`RestInformatieobjectConverter`, at the `formaat` assignments for document creation, task documents, and new versions), so the media type Open Zaak stores is client-supplied.

The spike (PZ-1637) measured Apache Tika 3.3.1 against real sample files for all 26 allowlisted extensions plus a Windows executable and a plain ZIP archive, and measured the JDK alternatives named in the ticket. The measurements below are the basis for the decisions and were reproduced in a throwaway project; they are not estimates.

## Goals / Non-Goals

**Goals:**
- Reject an upload whose real file type is not on the allowlist, regardless of the extension it was given.
- Reject an upload whose real file type does not match its extension, with a 4xx the user can understand.
- Keep the change confined to the allowlist enum, the two validators, and the converter — no new endpoints, no changes to endpoint signatures, no frontend logic.
- Keep the added dependency weight as small as detection allows, and record what was excluded and why.
- Remove ZAC's dependence on the client-reported media type, both for validation and for what is stored in Open Zaak.

**Non-Goals:**
- Judging whether a file is safe. A genuinely malicious PDF that really is a PDF passes. That is a virus scanner's job, and the story says so explicitly.
- Validating documents that enter PodiumD outside ZAC (Open Formulieren, SmartDocuments). ZAC trusts what is already stored.
- Turning the browser-side extension filter into a security control. Browsers cannot reliably determine content type, and a client-side check is not a boundary.
- Making the allowlist configurable again. The enum stays the single source of truth; `ADDITIONAL_ALLOWED_FILE_TYPES` was removed deliberately and is not coming back.
- Full Tika parsing (text extraction, metadata). Only detection is added.

## Decisions

### Decision: Apache Tika, because the JDK alternatives cannot do this

The ticket suggests `Files.probeContentType` may be sufficient. It is not, and this was measured rather than assumed: an executable renamed to `.pdf` and a Word document renamed to `.pdf` are both reported as `application/pdf`, on macOS and inside a Linux container matching the ZAC runtime. The method resolves the extension against the platform's MIME table and never reads the content. `URLConnection.guessContentTypeFromStream` returned no answer at all for a PDF, a Word document or an executable — it knows only a small set of signatures. Neither can satisfy either acceptance criterion.

### Decision: `tika-core` alone is not enough; the container-aware modules are required

With only `tika-core` on the classpath, detection from content is too coarse to be usable:

- a genuine `.docx` is reported as `application/zip`, and `.xlsx` / `.pptx` as `application/x-tika-ooxml`
- `.doc`, `.ppt`, `.msg` and `.vsd` all collapse into `application/x-tika-msoffice`

Legitimate uploads of every current Office format would be refused. Adding `tika-parser-zip-commons` (which looks inside ZIP containers) and `tika-parser-microsoft-module` (which reads the OLE2 directory and the OPC package) makes content-only detection correct for all 26 extensions except the two container families handled below. Both modules register their detectors through the service loader, so they are picked up by `DefaultDetector` purely by being on the classpath; no Tika configuration file is needed, and this works from `WEB-INF/lib` in the WAR.

### Decision: never pass the file name to the detector

This is the single decision that determines whether the check works at all. Tika's `DefaultDetector` combines content-based magic with the resource name, and uses the name to refine a generic answer into a specific one. The name is client-supplied, so passing it hands the attacker the outcome.

Measured, with the container modules present: a plain ZIP archive named `upload.docx` is reported as `application/vnd.openxmlformats-officedocument.wordprocessingml.document` when the name is supplied — and would be accepted — versus `application/zip` when it is not, and rejected. With `tika-core` only, an Excel sheet named `upload.docx` is likewise accepted when the name is supplied.

The detection call therefore constructs an empty `Metadata` and must never set `TikaCoreProperties.RESOURCE_NAME_KEY`, nor `CONTENT_TYPE` from the client's `formaat` / `type`. This is a correctness constraint that is invisible in the happy path, so it needs a test that fails if a future change starts passing the name — see tasks.

### Decision: per-extension accepted media types on `AllowedFileType`, keeping `mediaType` as the canonical one

Each enum entry gains a set of media types that content detection may legitimately yield for that extension. For 24 of 26 entries this set is exactly `{mediaType}`. Two need an addition, both measured:

- `.mp4` also accepts `video/quicktime`. MP4 and QuickTime share the ISO base media file format; a genuine MP4 — including one with the `isom` brand — is determined to be `video/quicktime` from content. Tika only reaches `video/mp4` with the file name as a hint, which is exactly what must not be used.
- `.mkv` also accepts `application/x-matroska`. Content detection yields the generic Matroska type; the video-specific `video/x-matroska` again requires the name hint.

The existing `mediaType` property stays the canonical value and keeps its current meaning, so `RestAllowedFileType`, the `/rest/configuratie/file-types` endpoint and the frontend are untouched.

The residual effect is that a `.mov` renamed to `.mp4` is accepted. Both extensions are on the allowlist and both hold the same kind of content, so this is not a security weakness — it is the limit of what content inspection can establish, and the spec states it as intended behaviour rather than leaving it implicit.

### Decision: accept a specialization of the expected media type, not only an exact match

Tika keeps a media type hierarchy in its `MediaTypeRegistry`. The acceptance rule is: the detected type, after `registry.normalize`, is accepted when it equals the expected type, is in the extension's accepted set, or `registry.isSpecializationOf(detected, expected)` holds.

Normalizing first folds in Tika's own aliases, measured examples being `text/rtf` for `application/rtf` and `video/avi` / `video/msvideo` for `video/x-msvideo` — the same alias problem that previously caused silent `.avi` upload failures, now solved at the detection layer instead of by hand-maintaining variant lists.

The specialization rule matters for text: a `.txt` containing HTML markup is detected as `text/html`, and one containing XML as `application/xml`, and Tika registers both as children of `text/plain`. Without the rule these legitimate files would be refused. Measured for the reverse direction, the rule does not weaken anything: `application/zip` is not a specialization of the Word document type, `application/x-tika-msoffice` is not a specialization of `application/msword`, and `application/vnd.ms-excel` is not a specialization of `application/msword` — all three remain rejections.

This was an explicit choice by the requester over strict equality: it accepts that HTML content can be stored under a `.txt` name, in exchange for not refusing text files that contain markup. Note the rule is deliberately one-directional. It does not accept a detected type that is a *parent* of the expected one, because a generic container is no evidence that the specific format is inside it — that is precisely what keeps the ZIP-named-`.docx` case a rejection, and it is why `.mp4` and `.mkv` need explicit sets rather than being handled by relaxing the direction.

### Decision: keep the check in the existing bean validators

The two validators already receive the bytes and already produce the 4xx. Detection costs well under a millisecond for ordinary files, so there is no reason to move the work elsewhere or to introduce a service layer for it. The detector itself is obtained from `TikaConfig.getDefaultConfig()`, which is a cached singleton, and `DefaultDetector` is thread-safe; it must be held in a `companion object` rather than constructed per validation.

Distinguishing the two rejection reasons in the message requires `ConstraintValidatorContext.buildConstraintViolationWithTemplate` with a second message key, since a class-level constraint otherwise carries a single fixed message. The frontend needs nothing new: `FoutAfhandelingService` already translates the violation's `message` through `translateService.instant`, and every mutation reports its own failures through it.

### Decision: trim the dependency to the artifacts detection needs

Declared plainly, `tika-parser-microsoft-module` resolves to 38 jars and 45.5 MB, dominated by POI's full schema jar, BouncyCastle, `commons-math3` and Jackcess — all needed for *parsing* documents, none for detecting them. Excluding `poi-ooxml-full`, `poi-scratchpad`, `org.bouncycastle`, `commons-math3`, Jackcess, `java-libpst`, `jsoup` and mime4j brings this to 22 jars and 13.5 MB, of which ZAC already ships 6 (`commons-io`, `commons-lang3`, `commons-collections4`, `commons-codec`, `commons-logging`, `slf4j-api`) — roughly 10.5 MB and 16 jars new. Detection results across all 28 sample files, and the rejection of all spoofing attempts, were verified to be byte-for-byte identical to the untrimmed set.

`tika-parser-audiovideo-module` is deliberately *not* added. It exists to provide the Matroska detector, but that detector does not improve content-only detection — `.mkv` still resolves to the generic type, which the accepted-media-types set handles — and it drags in `metadata-extractor` and `xmpcore` for no benefit.

The exclusions are a maintenance obligation: a future Tika upgrade could move a detector into one of the excluded artifacts, and the symptom would be legitimate Office uploads starting to fail. The format-coverage test described in tasks is what catches that.

### Decision: write the detected media type to Open Zaak

Once detection runs, the media type is a server-determined value, so there is no reason to keep forwarding the client's. `RestInformatieobjectConverter` writes the detected value at the three points where it currently assigns `formaat` from `restEnkelvoudigInformatieobject.formaat`, `bestand.type` and `restEnkelvoudigInformatieObjectVersieGegevens.formaat`.

The value written is the extension's canonical `mediaType`, not the raw detection result. Two reasons: it keeps what Open Zaak stores consistent with what `/rest/configuratie/file-types` advertises for that extension, and it avoids storing `video/quicktime` for a file the user uploaded as `.mp4` or `text/html` for a file uploaded as `.txt`. Validation has already established that the content genuinely belongs to that extension, so the canonical value is accurate.

## Risks / Trade-offs

- **The name-hint constraint is invisible.** Passing the file name to the detector makes the check silently useless while every happy-path test still passes. Mitigated by a test that asserts identical detection for identical bytes under a spoofed name, and by keeping detection in one small class with a single call site.
- **POI enlarges the vulnerability surface.** POI and XMLBeans are frequent CVE subjects and the project runs the OWASP dependency check. This is the real cost of the change and is the trade-off the architecture board is accepting; there is no smaller way to tell a `.doc` from an `.xls`.
- **`log4j-api` arrives via POI.** Only the API, so no Log4Shell exposure, but with no provider on the classpath it prints an error line at startup. Needs `log4j-to-slf4j` to route into ZAC's existing SLF4J setup, or an explicit decision to accept the line.
- **HTML under a `.txt` name is accepted.** A consequence of the specialization rule, chosen knowingly. `.txt` documents are not rendered by ZAC, which limits the impact, but it is a real widening compared to strict equality.
- **Sibling formats within a container remain interchangeable.** `.mp4` and `.mov` cannot be separated. Both are allowlisted, so the effect is cosmetic.
- **ZIP-based formats are opened during detection.** Detection reads the central directory and content types rather than inflating entries, and a 78 MB Word document was measured at 3 ms, so a compression bomb has little to work with. Worth a look during review rather than a mitigation up front.
- **Files that are valid but unusual may now be refused where they previously passed.** Any file whose bytes do not match its extension is by definition now rejected — that is the point of the change — but it does mean a genuine upload that previously worked can start failing. The format-coverage test over real sample files is the guard, and the distinct error message is what makes such a case diagnosable in production instead of appearing as an unexplained refusal.

## Migration Plan

No data migration and no configuration change. The stricter validation takes effect for uploads made after deployment; documents already stored in Open Zaak are untouched and are never revalidated. Nothing needs to be coordinated with Open Zaak, Open Notificaties or any other component, and the change is revertible by removing the dependency and restoring the extension-only `isAllowed`.

## Open Questions

- Should `log4j-to-slf4j` be added as part of this change, or is the startup error line acceptable until logging is looked at separately? Leaning towards adding the bridge, since it is one line and the alternative is a permanent error in the logs.
- The sample files used in the spike come from Apache Tika's own test corpus, with a few small ones synthesised for formats the corpus lacks. Where should the sample set used by the format-coverage test live, given its size — checked in under `src/test/resources`, or generated during the test run?
