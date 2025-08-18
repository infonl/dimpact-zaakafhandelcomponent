# Linting Strategy for TypeScript Migration

This document describes the linting strategy used to gradually migrate the codebase to strict TypeScript while ensuring new code follows best practices.

## Current State

The application currently has TypeScript strict mode disabled in `tsconfig.app.json` to allow the existing codebase to build without errors. However, we want to ensure that:

1. **New code** follows strict TypeScript standards
2. **Modified code** doesn't introduce new strict mode violations
3. **Existing code** can be gradually migrated over time

## Overview

The linting strategy uses a two-step approach:

1. **ESLint**: Checks all changed files (`.ts`, `.js`, `.html`) using the base configuration
2. **TypeScript Compiler**: Checks only changed `.ts` files using the base `tsconfig.json` (which has strict mode enabled)

## GitHub Actions

### Linting Check (`lint-changed-files.yml`)

This workflow runs on pull requests when files in `src/main/app/**/*.{ts,js,html}` are changed and:

- Identifies changed TypeScript/JavaScript/HTML files compared to the base branch
- Filters out deleted files and test files (`.spec.ts`, `.test.ts`, `test-helpers`)
- Runs ESLint on all changed files using the base `.eslintrc.js` configuration
- Runs TypeScript compiler with strict mode enabled only on changed `.ts` files
- Uses the base `tsconfig.json` (which has `strict: true`) for type checking
- Provides helpful feedback when linting fails

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

1. **Identify changed files** compared to the base branch (committed/staged changes)
2. **Identify untracked files** in the app directory
3. **Filter out deleted files** and test files
4. **Run ESLint** on all changed and untracked files using the base configuration
5. **Run TypeScript checking** only on changed `.ts` files using the base `tsconfig.json`
6. **Provide a summary** of what was checked

### What the Script Checks

- **ESLint**: All changed files (`.ts`, `.js`, `.html`) using `.eslintrc.js`
- **TypeScript**: Only `.ts` files using `tsconfig.json` (strict mode enabled)
- **File types**: Excludes `.spec.ts`, `.test.ts`, and `test-helpers` files
- **Untracked files**: Automatically includes new untracked files in the app directory

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

- `.eslintrc.js`: Base configuration for the entire codebase (used by both local script and CI)

### TypeScript Configuration

- `tsconfig.json`: Base configuration with strict mode enabled (used for type checking)
- `tsconfig.app.json`: Application configuration with strict mode disabled (used for building)
- `tsconfig.changed-files.json`: Temporary configuration used by the script to check only changed `.ts` files

## How It Works

### File Detection

1. **Changed files**: Uses `git diff --name-only origin/$BASE_BRANCH...HEAD`
2. **Untracked files**: Uses `git ls-files --others --exclude-standard`
3. **File filtering**: Excludes test files and deleted files
4. **Path filtering**: Only includes files in `src/main/app/`

### TypeScript Checking

1. Creates a temporary `tsconfig.changed-files.json` that extends the base `tsconfig.json`
2. Sets the `include` array to only the changed `.ts` files
3. Runs `npx tsc --project tsconfig.changed-files.json`
4. Cleans up the temporary file

### ESLint Checking

1. Uses the base `.eslintrc.js` configuration
2. Runs ESLint on all changed files (`.ts`, `.js`, `.html`)
3. Uses `--no-config-lookup` to avoid conflicts with other config files

## Migration Strategy

### Phase 1: Prevent New Issues (Current)

- ✅ Single GitHub Action checks all changed code for strict compliance
- ✅ Local script available for developers with support for untracked files
- ✅ Uses base `tsconfig.json` (strict mode) for type checking
- ✅ Uses base `.eslintrc.js` for linting
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

1. **Property initialization**:

   ```typescript
   // ❌ Bad
   class Component {
     private map: ol.Map;
   }

   // ✅ Good
   class Component {
     private map!: ol.Map; // Definite assignment assertion
   }
   ```

2. **Strict null checks**:

   ```typescript
   // ❌ Bad
   const value = obj.property;

   // ✅ Good
   const value = obj.property ?? defaultValue;
   ```

3. **Type safety**:

   ```typescript
   // ❌ Bad
   function processData(data: any) {}

   // ✅ Good
   function processData(data: unknown) {}
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
2. **Fix linting errors** in new and modified code immediately
3. **Use TypeScript strict mode** when creating new files (enforced by the script)
4. **Document any exceptions** to the strict rules
5. **Gradually migrate** existing code when touching it
6. **Include untracked files** in your linting checks (the script handles this automatically)

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

1. Check if the rule is in the base configuration
2. Consider if the rule should be adjusted for your use case
3. Document exceptions in the ESLint configuration

### No Files Found

If the script reports "No TypeScript/JavaScript/HTML files changed":

1. Make sure you're running from the project root
2. Check that your files are in the `src/main/app/` directory
3. Verify that your files have the correct extensions (`.ts`, `.js`, `.html`)
4. Ensure your files are not test files (`.spec.ts`, `.test.ts`)

## Example Output

```
🔍 Checking for linting errors in changed files...
Base branch: main
App directory: src/main/app

📋 Getting changed files compared to main...
📋 Getting untracked files...
📝 Found untracked files:
src/main/app/src/app/new-component.ts

📝 Files to lint:
src/main/app/src/app/modified-component.ts
src/main/app/src/app/new-component.ts

🔧 Running ESLint on changed files...
Files to lint (relative to app directory):
src/app/modified-component.ts
src/app/new-component.ts

Running regular lint command to check basic issues...
All files pass linting.

🔍 Running strict TypeScript checking on changed files...
Running TypeScript check on changed .ts files only...
src/app/modified-component.ts:15:7 - error TS2564: Property 'data' has no initializer...

❌ TypeScript check failed on changed files
💡 Changed files must follow strict TypeScript standards
```
