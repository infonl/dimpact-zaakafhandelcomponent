#!/bin/bash

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

# Generate types first
echo ""
echo "🔧 Generating TypeScript types..."
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

# Now run strict linting on changed files
echo ""
echo "🔍 Running strict TypeScript checking on changed files..."

# Create a temporary tsconfig that only includes the changed files
cat > tsconfig.changed-files.json << 'EOF'
{
  "extends": "./tsconfig.json",
  "compilerOptions": {},
  "include": [],
  "exclude": []
}
EOF

# Add only the changed .ts files to the include array
if [ -n "$RELATIVE_FILES" ]; then
    # Keep only .ts files for the TypeScript compiler
    RELATIVE_TS_FILES=$(echo "$RELATIVE_FILES" | grep -E '\.ts$' || true)
    
    if [ -z "$RELATIVE_TS_FILES" ]; then
        echo "No TypeScript files to type-check"
    else
        # Convert to JSON array format
        FILES_JSON=$(echo "$RELATIVE_TS_FILES" | tr ' ' '\n' | jq -R -s -c 'split("\n") | map(select(length>0))')
        
        # Update tsconfig to include only changed .ts files
        jq --argjson files "$FILES_JSON" '.include = $files' tsconfig.changed-files.json > tsconfig.changed-files.tmp.json
        mv tsconfig.changed-files.tmp.json tsconfig.changed-files.json
        
        if ! npx tsc --project tsconfig.changed-files.json; then
            echo ""
            echo "❌ TypeScript check failed on changed files"
            echo "💡 Changed files must follow strict TypeScript standards"
            echo "💡 Tip: Temporarily enable strict mode in tsconfig.app.json to see all issues"
            rm -f tsconfig.changed-files.json
            exit 1
        fi
    fi
fi

# Clean up temporary file
rm -f tsconfig.changed-files.json

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
