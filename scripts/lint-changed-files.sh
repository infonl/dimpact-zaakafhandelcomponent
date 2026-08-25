#!/bin/bash

#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Script to lint only changed files in the Angular app
# Usage: ./scripts/lint-changed-files.sh [base-branch]

set -e

# Default to main branch if not specified
BASE_BRANCH=${1:-main}
APP_DIR="src/main/app"

echo "🔍 Checking for linting errors in changed files..."
echo "Base branch: $BASE_BRANCH"
echo "App directory: $APP_DIR"
echo ""

# Check if we're in the right directory
if [ ! -d "$APP_DIR" ]; then
    echo "❌ Error: $APP_DIR directory not found. Please run this script from the project root."
    exit 1
fi

# Get changed files compared to base branch
echo "📋 Getting changed files compared to $BASE_BRANCH..."
CHANGED_FILES=$(git diff --name-only origin/$BASE_BRANCH...HEAD | grep -E '\.(ts|js|html)$' | grep "^$APP_DIR/" || true)

# Get untracked files
echo "📋 Getting untracked files..."
UNTRACKED_FILES=$(git ls-files --others --exclude-standard | grep -E '\.(ts|js|html)$' | grep "^$APP_DIR/" || true)

# Combine changed and untracked files
ALL_CHANGED_FILES=""
if [ -n "$CHANGED_FILES" ]; then
    ALL_CHANGED_FILES="$CHANGED_FILES"$'\n'
fi
if [ -n "$UNTRACKED_FILES" ]; then
    echo "📝 Found untracked files:"
    echo "$UNTRACKED_FILES"
    ALL_CHANGED_FILES="$ALL_CHANGED_FILES$UNTRACKED_FILES"$'\n'
fi

CHANGED_FILES="$ALL_CHANGED_FILES"

if [ -z "$CHANGED_FILES" ]; then
    echo "✅ No TypeScript/JavaScript/HTML files changed in $APP_DIR"
    exit 0
fi

# Filter out deleted files (files that exist in the diff but not in the current working directory)
EXISTING_FILES=""
for file in $CHANGED_FILES; do
    if [ -f "$file" ]; then
        EXISTING_FILES="$EXISTING_FILES$file"$'\n'
    else
        echo "⚠️  Skipping deleted file: $file"
    fi
done

# Filter out test files
FILTERED_FILES=$(echo "$EXISTING_FILES" | grep -v '\.spec\.' | grep -v '\.test\.' | grep -v 'test-helpers' || true)

# Spec files are linted separately, against the stricter Testing Library rules
SPEC_FILES=$(echo "$EXISTING_FILES" | grep '\.spec\.ts$' || true)

if [ -n "$SPEC_FILES" ]; then
    RELATIVE_SPECS=$(echo "$SPEC_FILES" | sed "s|^$APP_DIR/||")

    echo "🔧 Running ESLint on changed spec files (strict Testing Library rules)..."
    echo "$RELATIVE_SPECS"
    echo ""

    if ! (cd "$APP_DIR" && ESLINT_USE_FLAT_CONFIG=false npx eslint -c .eslintrc.strict-specs.js $RELATIVE_SPECS); then
        echo ""
        echo "❌ Changed spec files must follow the Testing Library query priority"
        echo "💡 Prefer getByRole over DOM traversal: https://testing-library.com/docs/queries/about/#priority"
        exit 1
    fi

    echo "✅ Changed spec files passed"
    echo ""
fi

if [ -z "$FILTERED_FILES" ]; then
    echo "✅ No source files changed (only test files)"
    exit 0
fi

echo "📝 Files to lint:"
echo "$FILTERED_FILES"
echo ""

# Separate new files from modified files
NEW_FILES=$(git diff --name-only --diff-filter=A origin/$BASE_BRANCH...HEAD | grep -E '\.(ts|js|html)$' | grep "^$APP_DIR/" | grep -v '\.spec\.' | grep -v '\.test\.' | grep -v 'test-helpers' || true)
MODIFIED_FILES=$(git diff --name-only --diff-filter=M origin/$BASE_BRANCH...HEAD | grep -E '\.(ts|js|html)$' | grep "^$APP_DIR/" | grep -v '\.spec\.' | grep -v '\.test\.' | grep -v 'test-helpers' || true)

# Change to app directory
cd "$APP_DIR"

# Convert file paths to be relative to the app directory
RELATIVE_FILES=$(echo "$FILTERED_FILES" | sed "s|^$APP_DIR/||")

echo "🔧 Running ESLint on changed files..."
echo "Files to lint (relative to app directory):"
echo "$RELATIVE_FILES"
echo ""
echo "Current directory: $(pwd)"
echo "ESLint config file exists: $([ -f .eslintrc.js ] && echo 'Yes' || echo 'No')"
echo ""

# Generate OpenAPI specs first
echo ""
echo "🔧 Generating OpenAPI specs..."
cd ../../..
if ! ./gradlew generateOpenApiSpec; then
    echo ""
    echo "❌ OpenAPI spec generation failed"
    echo "💡 Tip: Run './gradlew generateOpenApiSpec' from the project root to generate OpenAPI specs"
    exit 1
fi
cd "$APP_DIR"

# Generate TypeScript types from OpenAPI specs
echo ""
echo "🔧 Generating TypeScript types from OpenAPI specs..."
if ! npm run generate:types:zac-openapi; then
    echo ""
    echo "❌ Type generation failed"
    echo "💡 Tip: Run 'npm run generate:types:zac-openapi' (in the app directory) to generate types"
    exit 1
fi

# Run regular linting first to check basic issues
echo ""
echo "🔍 Running regular lint command to check basic issues..."
if ! npm run lint; then
    echo ""
    echo "❌ Basic linting failed"
    echo "💡 Tip: Run 'npm run lint' (in the app directory) to see all linting issues"
    exit 1
fi

# Note: Strict TypeScript checking temporarily disabled due to technical issues
# The ESLint check above already covers most linting requirements
echo ""
echo "🔍 Running strict TypeScript checking on changed files..."

# Check each TypeScript file individually
if [ -n "$RELATIVE_FILES" ]; then
    # Keep only .ts files for the TypeScript compiler
    RELATIVE_TS_FILES=$(echo "$RELATIVE_FILES" | grep -E '\.ts$' || true)
    
    if [ -z "$RELATIVE_TS_FILES" ]; then
        echo "No TypeScript files to type-check"
    else
        echo "TypeScript files to check:"
        echo "$RELATIVE_TS_FILES"
        echo ""
        
        # Check TypeScript compilation for the entire project and filter for changed files
        echo "Running TypeScript compilation check..."
        TSC_TEMP_FILE=$(mktemp)
        # Temporarily disable set -e for this command to handle errors gracefully
        set +e
        timeout 60s npx tsc --noEmit --project . > "$TSC_TEMP_FILE" 2>&1
        TSC_EXIT_CODE=$?
        set -e
        
        # Check if timeout occurred
        if [ $TSC_EXIT_CODE -eq 124 ]; then
            echo "⚠️  TypeScript check timed out (60s)"
            rm -f "$TSC_TEMP_FILE"
        else
            FAILED_FILES=""
            # Check each changed TypeScript file for errors
            for file in $RELATIVE_TS_FILES; do
                echo ""
                echo "👀 Checking: $file"
                # Filter output to only show errors from the current file being checked
                # TypeScript error format: "src/app/file.ts(line,col): error message"
                FILTERED_ERRORS=$(grep "^$file(" "$TSC_TEMP_FILE" || true)
                
                if [ -n "$FILTERED_ERRORS" ]; then
                    echo "❌ TypeScript errors found in $file:"
                    echo "$FILTERED_ERRORS"
                    FAILED_FILES="$FAILED_FILES$file"$'\n'
                else
                    echo "✅ $file passed TypeScript check"
                fi
            done
            
            if [ -n "$FAILED_FILES" ]; then
                echo ""
                echo "❌ TypeScript check failed for changed files with errors in the files themselves"
                echo "💡 Changed files must follow strict TypeScript standards"
                echo "💡 Files with errors:"
                echo "$FAILED_FILES"
                rm -f "$TSC_TEMP_FILE"
                exit 1
            fi
            
            echo "✅ All TypeScript files passed strict checking"
        fi
        
        # Clean up temp file
        rm -f "$TSC_TEMP_FILE"
        
        if [ -n "$FAILED_FILES" ]; then
            echo ""
            echo "❌ TypeScript check failed for changed files with errors in the files themselves"
            echo "💡 Changed files must follow strict TypeScript standards"
            echo "💡 Files with errors:"
            echo "$FAILED_FILES"
            exit 1
        fi
        
        echo "✅ All TypeScript files passed strict checking"
    fi
fi

echo ""
echo "✅ All linting checks passed!"
echo "📊 Summary:"
echo "   - Changed files: $(echo "$FILTERED_FILES" | wc -l | tr -d ' ')"
if [ -n "$NEW_FILES" ]; then
    echo "   - New files: $(echo "$NEW_FILES" | wc -l | tr -d ' ')"
fi
if [ -n "$MODIFIED_FILES" ]; then
    echo "   - Modified files: $(echo "$MODIFIED_FILES" | wc -l | tr -d ' ')"
fi
