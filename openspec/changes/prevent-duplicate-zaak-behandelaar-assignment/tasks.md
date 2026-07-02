## 1. Add per-zaak striped lock to ZaakService

- [x] 1.1 Add `private val zaakAssignmentLocks = Array(64) { ReentrantLock() }` field to `ZaakService` and a private helper `fun lockForZaak(uuid: UUID) = zaakAssignmentLocks[Math.floorMod(uuid.hashCode(), zaakAssignmentLocks.size)]` (import `java.util.concurrent.locks.ReentrantLock`)
- [x] 1.2 Wrap the body of `ZaakService.assignZaak()` with `lockForZaak(zaak.uuid).withLock { ... }` (import `kotlin.concurrent.withLock`)

## 2. Defensive cleanup of duplicate Behandelaar roles in assignUser

- [x] 2.1 In `ZaakService.assignUser()`, before calling `zrcClientService.updateRol(zaak, bepaalRolMedewerker(...), reason)`, add a call to list all existing MEDEWERKER roles for the zaak and log a WARNING if more than one is found (include zaak UUID and count in log message)
- [x] 2.2 Replace the `updateRol` call in `ZaakService.assignUser()` with: first `zrcClientService.deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, reason)` to purge all existing, then `zrcClientService.createRol(bepaalRolMedewerker(user, zaak), reason)` to create exactly one — removing the stale-read dependency

## 3. Unit tests for ZaakService

- [x] 3.1 Add a test scenario in `ZaakServiceTest` verifying that when `assignZaak` is called and an existing MEDEWERKER role is present, `deleteRol` is called before `createRol` (no stale update)
- [x] 3.2 Add a test scenario verifying that when `assignZaak` is called with `userName = null`, only `deleteRol` is called and no MEDEWERKER role is created
- [x] 3.3 Add a test scenario verifying that when multiple concurrent `assignZaak` calls are made for the same zaak UUID (simulate with threads), only one MEDEWERKER role creation call reaches `zrcClientService` in the end

## 4. Integration / verification

- [x] 4.1 Run `./gradlew spotlessApply detektApply` to ensure code style compliance
- [x] 4.2 Run `./gradlew test --tests "nl.info.zac.zaak.ZaakServiceTest"` and confirm all tests pass
