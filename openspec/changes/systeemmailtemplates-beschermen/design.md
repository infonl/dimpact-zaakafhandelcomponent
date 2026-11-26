## Context

`MailtemplateBeheerRestService` (`beheer/mailtemplates`) checks only the `canBeheren` policy and passes each request
to `MailTemplateService`:

- `delete(id)` removes the template when it exists, and does nothing when it does not.
- `updateMailtemplate(id, mailTemplate)` copies the name, subject, body, `mail` and `isDefaultMailtemplate` onto the
  managed entity, merges it, and evicts the cached configuration of every zaaktype that links the template, with
  `MailTemplateKoppelingenService.listZaaktypeUuidsLinkedTo(id)`.
- `createMailtemplate(mailTemplate)` persists the template with any `mail` and any `isDefaultMailtemplate`.

The Flyway migrations V34, V35, V44 and V106 create one template with `default_mailtemplate = true` for each of the
14 `Mail` types. ZAC reads them as follows:

- `SignaleringMailHelper` sends a signalering with `readDefaultMailTemplate(mail)`, which throws
  `MailTemplateNotConfiguredException` when the default template is missing.
- `PlanItemsRestService` falls back to `readDefaultMailTemplate(mail)` when the zaaktype configuration links no
  template for a task mail.
- `MailtemplateRestService` falls back to `findDefaultMailtemplate(mail)` when the zaaktype configuration links no
  template for a zaak mail.
- `findDefaultMailtemplate` takes the first match when a `Mail` type has more than one default template.

A zaaktype configuration links a mail template with a row in `mail_template_koppelingen`. Its foreign key to
`mail_template` is `ON DELETE RESTRICT`, so deleting a linked template fails in the database, and
`RestExceptionMapper` answers 500. Each zaaktype version has its own configuration, so the configuration of an
earlier zaaktype version can still link a template that the current one no longer links.

The admin screen hides the delete button when `defaultMailtemplate` is true, and disables it when any koppeling links
the template. The edit form disables the mail type of an existing template and sends back the stored `mail` and
`defaultMailtemplate`. The form has no control for `defaultMailtemplate`; a new template is sent with `false`, and the
mail type list holds only the `LINKABLE_MAILS`.

ZAC protects reference tables the same way: `ReferenceTableAdminService.deleteReferenceTable` throws
`InputValidationFailedException` with `ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED` for a system table and
with `ERROR_CODE_REFERENCE_TABLE_IS_IN_USE_BY_ZAAKTYPE_CMMN_CONFIGURATION` for a table in use. `RestExceptionMapper`
turns that exception into a 400 response with the error code as message, and the frontend translates that code.

## Goals / Non-Goals

**Goals:**
- No REST request can delete a system mail template, or remove it in two steps by first turning it into a normal one.
- Each `Mail` type keeps exactly the system mail template that the migrations created.
- Deleting a linked mail template gives a clear 400 instead of a database error.
- The name, subject and body of a system mail template stay editable; normal templates behave as before.
- The admin screen works as before, and shows a clear message if a refused request still reaches the backend.

**Non-Goals:**
- Restoring system mail templates that an environment has already lost. ZAC keeps answering with
  `MailTemplateNotConfiguredException` there, as now.
- A database constraint for one system mail template per `Mail` type. An environment may already have a second
  default template, created through the API, and a migration that adds the constraint would then fail at startup.
- Removing `MailTemplateService.storeMailtemplate`, which only tests call. The checks below cover it, because it
  calls `createMailtemplate` and `updateMailtemplate`.

## Decisions

### D1. A system mail template is a template with `isDefaultMailtemplate = true`

The story defines it the same way. The flag fits the goal:

- Every template the migrations created has the flag, also those of the linkable mail types, such as "Algemene
  e-mail" (`ZAAK_ALGEMEEN`) and "Ontvangstbevestiging" (`TAAK_ONTVANGSTBEVESTIGING`). `PlanItemsRestService` and
  `MailtemplateRestService` fall back to these when a zaaktype configuration links no template, so losing one breaks
  a mail as well.
- The admin screen already hides the delete button by this flag, so the backend and the frontend use one rule.
- A beheerder can create a template of a linkable mail type with the flag off. Such a template is not needed to send
  any mail, and can be deleted when nothing links it.

The check reads the flag of the stored template, not of the request.

### D2. The checks live in `MailTemplateService`

`delete`, `updateMailtemplate` and `createMailtemplate` check before they change anything, so every caller is covered,
also `storeMailtemplate` and any future caller. This follows `ReferenceTableAdminService.deleteReferenceTable`.

`updateMailtemplate` checks before it copies the request onto the managed entity. The `InputValidationFailedException`
also rolls back the transaction, but a check after the copy would rely on that rollback to keep the entity unchanged.

`delete` checks the system flag first and the links second, so a linked system template gets the system message.

### D3. Refuse, not ignore

A request that would delete or convert a system mail template gets a 400 response; the service does not silently
keep the stored `mail` and flag. The story asks for a clear error, and a caller that sends a changed flag learns that
it was not stored. The admin screen never sends such a request, so it shows no new errors.

400, not 403: the beheerder may manage mail templates; the request itself is not allowed for this template. ZAC uses
`InputValidationFailedException` and 400 for reference tables too.

### D4. The API never sets the system flag

`createMailtemplate` refuses `isDefaultMailtemplate = true`, and `updateMailtemplate` refuses any change of the flag,
from true to false and from false to true. The story only asks for the first direction. Without the others a
beheerder can create a second system template for a `Mail` type: `findDefaultMailtemplate` then uses whichever of the
two the database returns first, and neither can be deleted any more. The admin screen sends `false` for a new
template and the stored value on an update, so it is not affected.

For a normal template `updateMailtemplate` keeps accepting a changed `mail`, as now.

### D5. A linked template is refused with `listZaaktypeUuidsLinkedTo`

`delete` refuses the template when `MailTemplateKoppelingenService.listZaaktypeUuidsLinkedTo(id)` is not empty.
`MailTemplateService` already uses this query to evict the cache after an update. It reads every row of
`mail_template_koppelingen`, so it also finds a link from the configuration of an earlier zaaktype version, which the
foreign key would refuse as well.

Alternative: catch the `PersistenceException` of the foreign key. The remove only fails at the flush, often after
`delete` has returned, and the exception does not tell which constraint failed.

### D6. Three error codes

Following the reference tables:

| `ErrorCode` | Key | nl |
|---|---|---|
| `ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_DELETED` | `msg.error.system.mailtemplate.cannot.be.deleted` | Een systeemmailtemplate kan niet worden verwijderd. |
| `ERROR_CODE_SYSTEM_MAIL_TEMPLATE_CANNOT_BE_CONVERTED` | `msg.error.system.mailtemplate.cannot.be.converted` | Een systeemmailtemplate kan niet worden omgezet naar een gewone mailtemplate of een ander mailtype, en een gewone mailtemplate kan geen systeemmailtemplate worden. |
| `ERROR_CODE_MAIL_TEMPLATE_IS_IN_USE_BY_ZAAKTYPE_CONFIGURATION` | `msg.error.mailtemplate.is.in.use.by.zaaktype-configuration` | De mailtemplate is gekoppeld aan een of meer zaaktypes en kan niet worden verwijderd. |

The second code covers both directions of D4, because both convert between a normal and a system template.

## Risks / Trade-offs

- [A script or external client creates or converts system templates through the API] → It now gets a 400. The
  scripts in `scripts/test-data/` do not call these endpoints; a new system mail template belongs in a Flyway
  migration, as in PZ-13184.
- [An environment already lost a system mail template] → This change does not restore it (non-goal). The beheerder
  sees `msg.error.mailtemplate.not.configured` when ZAC needs it, as now.
- [A link is added between the check and the commit of a delete] → The foreign key still refuses the delete, with the
  500 of today. Two beheerders would have to do this at the same moment.
- [The integration test changes a shared system mail template, and links a template to a shared zaaktype] → The test
  restores the name, subject and body, and removes the link and the template afterwards, so that the tests that
  compare the list of mail templates and koppelingen keep passing.
