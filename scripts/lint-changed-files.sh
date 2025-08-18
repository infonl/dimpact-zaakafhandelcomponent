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

# Run regular linting first to check basic issues
echo "Running regular lint command to check basic issues..."
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
  "compilerOptions": {
    "noEmit": true,
    "skipLibCheck": true
  },
  "include": [],
  "exclude": ["src/test-helpers.ts", "src/**/*.spec.ts", "src/**/*.test.ts"]
}
EOF

# Add only the changed files to the include array
if [ -n "$RELATIVE_FILES" ]; then
    # Convert to JSON array format
    FILES_JSON=$(echo "$RELATIVE_FILES" | tr ' ' '\n' | jq -R -s -c 'split("\n")[:-1]')
    
    # Update tsconfig to include only changed files
    jq --argjson files "$FILES_JSON" '.include = $files' tsconfig.changed-files.json > tsconfig.changed-files.tmp.json
    mv tsconfig.changed-files.tmp.json tsconfig.changed-files.json
    
    echo "Running TypeScript check on changed files only..."
    if ! npx tsc --project tsconfig.changed-files.json; then
        echo ""
        echo "❌ TypeScript check failed on changed files"
        echo "💡 Changed files must follow strict TypeScript standards"
        echo "💡 Tip: Temporarily enable strict mode in tsconfig.app.json to see all issues"
        rm -f tsconfig.changed-files.json
        exit 1
    fi
fi

# Clean up temporary file
rm -f tsconfig.changed-files.json
    echo ""
    echo "❌ ESLint found errors in changed files"
    echo "💡 Tip: Run 'npm run lint' (in the app directory) to see all linting issues"
    exit 1
fi

# Check if we have new files that need strict checking
if [ -n "$NEW_FILES" ]; then
    echo ""
    echo "🆕 New files detected - running strict TypeScript check..."
    
    # Create temporary strict tsconfig for new files
    cat > tsconfig.strict-temp.json << 'EOF'
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "strict": true,
    "noEmit": true,
    "skipLibCheck": true,
    "noImplicitAny": true,
    "noImplicitReturns": true,
    "noImplicitThis": true
  },
  "angularCompilerOptions": {
    "strictTemplates": true,
    "strictInjectionParameters": true
  },
  "include": [],
  "exclude": ["src/test-helpers.ts", "src/**/*.spec.ts", "src/**/*.test.ts"]
}
EOF

    # Add new files to include array
    if [ -n "$NEW_FILES" ]; then
        # Convert file paths to be relative to the app directory
        RELATIVE_NEW_FILES=$(echo "$NEW_FILES" | sed "s|^$APP_DIR/||")
        
        # Convert to JSON array format
        FILES_JSON=$(echo "$RELATIVE_NEW_FILES" | jq -R -s -c 'split("\n")[:-1]')
        
        # Update tsconfig to include only new files
        jq --argjson files "$FILES_JSON" '.include = $files' tsconfig.strict-temp.json > tsconfig.strict-temp.tmp.json
        mv tsconfig.strict-temp.tmp.json tsconfig.strict-temp.json
        
        echo "Running strict TypeScript check on new files..."
        if ! npx tsc --project tsconfig.strict-temp.json; then
            echo ""
            echo "❌ TypeScript strict check failed on new files"
            echo "💡 New files must follow strict TypeScript standards"
            echo "💡 Tip: Temporarily enable strict mode in tsconfig.app.json to see all issues"
            rm -f tsconfig.strict-temp.json
            exit 1
        fi
    fi
    
    # Clean up temporary file
    rm -f tsconfig.strict-temp.json
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
