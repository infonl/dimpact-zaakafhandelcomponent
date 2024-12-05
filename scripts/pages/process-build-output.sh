#!/bin/sh
#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Copy source directory content into destination directory
copy_dir() {
  src_dir="$1"
  dest_dir="$2"
  mkdir -p "$dest_dir"
  cp -r "$src_dir"/* "$dest_dir"
}

# Copy source directory content into destination directory
copy_file() {
  src_file="$1"
  dest_file="$2"
  dest_dir="$(dirname "${dest_file}")"
  mkdir -p "$dest_dir"
  cp "$src_file" "$dest_file"
}

# Add a passed header to a source file into the destination file
# If source and destination files are the same, the file will be replaced
add_header_to() {
  src_file="$1"
  dest_file="$2"
  header_content="$3"

  if [ -f "$dest_file" ]; then
    # Replace existing file
    echo "$header_content\n$(cat "$src_file")" > "$dest_file.tmp"
    rm -f "$dest_file"
    mv "$dest_file.tmp" "$dest_file"
  else
    # Write file directly
    echo "$header_content\n$(cat "$src_file")" > "$dest_file"
  fi
}

# Determine the script location dir
script_dir="$(cd "$(dirname "$0")" && pwd)"
# Determine the project root dir
root_dir="$(cd "$script_dir/../.." && pwd)"
# Set the project sources dir
sources_dir="$root_dir/src"
# Set the project sources dir
pages_sources_dir="$sources_dir/pages"
# Set the project build dir
build_dir="$root_dir/build"

# Ensure the pages build directories exists
build_pages_dir="$build_dir/pages"
build_reports_dir="$build_pages_dir/_reports"
mkdir -p "$build_reports_dir"
build_docs_dir="$build_pages_dir/_docs"
mkdir -p "$build_docs_dir"

# Copy the pages sources to the pages build directory
cp -r "$pages_sources_dir"/* "$build_pages_dir"
# For the CI build, we need to skip the _config.yml file.
# That will be processed later with some replacements
# For local testing it will be copied
# And the github-mock-data will be appended
config_file="$build_pages_dir/_config.yml"
if [ -n "$CI" ]; then
    if [ -f "$config_file" ]; then
        rm "$config_file"
    fi
else
    # Perform manual variable replacement in _config.yml
    if [ -f "$config_file" ]; then
        # Replace information that will be pulled
        sed -i '' 's|{{PROJECT_TITLE}}|Dimpact Zaakafhandelcomponent (ZAC)|g' "$config_file"
        sed -i '' 's|{{REPO_URL}}|https://github.com/infonl/dimpact-zaakafhandelcomponent|g' "$config_file"
        sed -i '' 's|{{SOFTWARE_VERSION}}|dev-version|g' "$config_file"
        sed -i '' "s|{{REPO_CREATED_AT}}|$(date)|g" "$config_file"
        sed -i '' 's|{{DOCKER_IMAGE}}|ghcr.io/infonl/zaakafhandelcomponent:1.11.55|g' "$config_file"

        github_mock_data="$script_dir/github-mock-data.yml"
        echo "load mock github data from: $github_mock_data"
        if [ -f "$github_mock_data" ]; then
          cat "$github_mock_data" >> "$config_file"
        fi
    fi
fi

# Add the Detekt Report
detekt_report_src_file="$build_dir/reports/detekt/detekt.md"
detekt_header_file="$script_dir/detekt-header.md"
if [ -f "$detekt_header_file" ]; then
    header_content=$(cat "$detekt_header_file")
    add_header_to "$detekt_report_src_file" "$build_reports_dir/detekt.md" "$header_content"
fi

# Add the Backend Test HTML reports
copy_dir "$build_dir/reports/tests/test" "$build_reports_dir/backend-unit-tests/"

# Add the Backend Integration Test HTML reports
copy_dir "$build_dir/reports/tests/itest" "$build_reports_dir/backend-integration-tests/"

# Add the Frontend Unit Test HTML reports
copy_file "$sources_dir/main/app/reports/test-report.html" "$build_reports_dir/frontend-unit-tests/index.html"

# Add the Backend Test Jacoco HTML reports
copy_dir "$build_dir/reports/jacoco/test/html" "$build_reports_dir/backend-unit-tests-lcov/"

# Add the Frontend Lcov HTML reports
copy_dir "$sources_dir/main/app/coverage/lcov-report" "$build_reports_dir/frontend-unit-tests-lcov/"

# Add the End-to_end HTML report from GitHub branch, and trim the file tree
build_reports_e2e_dir="$build_reports_dir/end-to-end-tests"
git clone --depth 1 --no-checkout --branch "gh-pages/dimpact-e2e-test-report" "$(git remote get-url origin)" "$build_reports_e2e_dir"
echo "Trim file tree: $build_reports_e2e_dir"
rm -rf "$build_reports_e2e_dir"/.git
rm -rf "$build_reports_e2e_dir"/202*
rm -f "$build_reports_e2e_dir"/.nojekyll"
rm -f "$build_reports_e2e_dir/*.json

manual_header="$script_dir"/manual-header.md
# Add the project configuration manual documentation
copy_dir "$root_dir/docs/manuals/inrichting-zaakafhandelcomponent" "$build_docs_dir"
# Read the header template, replace placeholders, and set in the file
inrichting_file="$build_docs_dir"/inrichting-zaakafhandelcomponent.md
inrichting_header_content=$(sed -e "s|{{title}}|Inrichting zaakafhandelcomponent|" -e "s|{{link}}|inrichting-zaakafhandelcomponent|" "$manual_header")
add_header_to "$inrichting_file" "$inrichting_file" "$inrichting_header_content"


# Add the project users manual documentation
copy_dir "$root_dir/docs/manuals/ZAC-gebruikershandleiding" "$build_docs_dir"
gebruikershandleiding_file="$build_docs_dir"/ZAC-gebruikershandleiding.md
gebruikershandleiding_header_content=$(sed -e "s|{{title}}|ZAC gebruikershandleiding|" -e "s|{{link}}|zac-gebruikershandleiding|" "$manual_header")
add_header_to "$gebruikershandleiding_file" "$gebruikershandleiding_file" "$gebruikershandleiding_header_content"

