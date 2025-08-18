# Linting Strategy for TypeScript Migration

This document describes the linting strategy used to gradually migrate the codebase to strict TypeScript while ensuring new code follows best practices.

## Current State

The application currently has TypeScript strict mode disabled in `tsconfig.app.json` to allow the existing codebase to build without errors. However, we want to ensure that:

1. **New code** follows strict TypeScript standards
2. **Modified code** doesn't introduce new strict mode violations
3. **Existing code** can be gradually migrated over time

## GitHub Actions

### 1. Basic Linting Check (`lint-changed-files.yml`)

This workflow runs on pull requests and:

- Identifies changed TypeScript/JavaScript/HTML files
- Runs ESLint on changed files using the current configuration
- Runs TypeScript compiler with strict mode enabled only on changed files
- Provides helpful feedback when linting fails

### 2. Strict Linting for New Code (`strict-lint-new-code.yml`)

This workflow provides more granular control:

- **New files**: Must follow strict TypeScript standards
- **Modified files**: Must not introduce new strict mode violations
- Uses different ESLint and TypeScript configurations for each case
- Provides detailed feedback about which files failed and why

## Local Development

### Using the Local Script

Run the local linting script to check your changes before committing:

```bash
# Check against main branch
./scripts/lint-changed-files.sh

# Check against a specific branch
./scripts/lint-changed-files.sh develop
```

The script will:

1. Identify changed files compared to the base branch
2. Run ESLint on changed files
3. Run strict TypeScript checking on new files
4. Provide a summary of what was checked

### Manual Strict Checking

To manually check your code with strict TypeScript settings:

1. Temporarily enable strict mode in `tsconfig.app.json`:

   ```json
   {
     "compilerOptions": {
       "strict": true
     },
     "angularCompilerOptions": {
       "strictTemplates": true
     }
   }
   ```

2. Run the TypeScript compiler:

   ```bash
   cd src/main/app
   npx tsc --noEmit
   ```

3. Fix any errors that appear
4. Revert the `tsconfig.app.json` changes

## Configuration Files

### ESLint Configuration

- `.eslintrc.js`: Current configuration for the entire codebase
- `.eslintrc.strict.js`: Stricter configuration used for new files in CI

### TypeScript Configuration

- `tsconfig.json`: Base configuration with strict mode enabled
- `tsconfig.app.json`: Application configuration with strict mode disabled
- `tsconfig.strict-*.json`: Temporary configurations used in CI for strict checking

## Migration Strategy

### Phase 1: Prevent New Issues (Current)

- ✅ GitHub Actions check new code for strict compliance
- ✅ Local script available for developers
- ✅ Existing code continues to work

### Phase 2: Gradual Migration (Future)

- 🔄 Enable strict mode for specific modules/components
- 🔄 Fix existing strict mode violations incrementally
- 🔄 Update CI to check more files over time

### Phase 3: Full Strict Mode (Future)

- 🔄 Enable strict mode for the entire application
- 🔄 Remove temporary configurations
- 🔄 Standardize on single TypeScript configuration

## Common Issues and Solutions

### TypeScript Strict Mode Errors

1. **Implicit any types**:

   ```typescript
   // ❌ Bad
   function processData(data) {}

   // ✅ Good
   function processData(data: unknown) {}
   ```

2. **Strict null checks**:

   ```typescript
   // ❌ Bad
   const value = obj.property;

   // ✅ Good
   const value = obj.property ?? defaultValue;
   ```

3. **Strict template checking**:

   ```typescript
   // ❌ Bad
   <div>{{ user.name }}</div>

   // ✅ Good
   <div>{{ user?.name }}</div>
   ```

### ESLint Errors

1. **Unused variables**:

   ```typescript
   // ❌ Bad
   const unused = "value";

   // ✅ Good
   const _unused = "value"; // Prefix with underscore
   ```

2. **Missing return types**:

   ```typescript
   // ❌ Bad
   function getData() {
     return fetch("/api/data");
   }

   // ✅ Good
   function getData(): Promise<Response> {
     return fetch("/api/data");
   }
   ```

## Best Practices

1. **Always run the local script** before committing changes
2. **Fix linting errors** in new code immediately
3. **Use TypeScript strict mode** when creating new files
4. **Document any exceptions** to the strict rules
5. **Gradually migrate** existing code when touching it

## Troubleshooting

### Script Fails with "jq not found"

Install jq on your system:

- **macOS**: `brew install jq`
- **Ubuntu/Debian**: `sudo apt-get install jq`
- **Windows**: Download from https://stedolan.github.io/jq/

### TypeScript Errors in Existing Code

If you're modifying existing code and getting strict mode errors:

1. Fix the errors in the modified code
2. Consider if the changes affect other parts of the file
3. Document any remaining issues for future migration

### ESLint Configuration Issues

If ESLint rules seem too strict:

1. Check if the rule is in the strict configuration only
2. Consider if the rule should be adjusted for your use case
3. Document exceptions in the ESLint configuration
