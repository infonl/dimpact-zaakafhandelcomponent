## 1. Fix annotation placement

- [x] 1.1 In `RestReferenceTable.kt`: change `@field:Valid var waarden: List<RestReferenceTableValue>` to `var waarden: List<@Valid RestReferenceTableValue>` and remove unused `import jakarta.validation.Valid` if no longer needed
- [x] 1.2 In `RestReferenceTableUpdate.kt`: apply the same annotation-placement fix and clean up unused import if needed

## 2. Verify

- [x] 2.1 Run `./gradlew compileKotlin` to confirm no compilation errors
- [ ] 2.2 Start ZAC and confirm `HV000271` warning no longer appears in logs
