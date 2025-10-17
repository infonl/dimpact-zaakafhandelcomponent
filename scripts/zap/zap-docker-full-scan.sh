#!/bin/sh
#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

ZAP_DOCKER_IMAGE=ghcr.io/zaproxy/zaproxy:stable
ZAP_CONFIG_FILE_NAME=default.config
ZAP_CONTEXT_FILE_NAME=sample.context
ZAP_REPORT_FILE_NAME=zap-report.html

SCRIPT_NAME="$0"
usage() {
    echo " "
    echo "This script is used to run a ZAP full scan from a "
    echo "docker container ($ZAP_DOCKER_IMAGE)."
    echo "It will pass a config file ($ZAP_CONFIG_FILE_NAME) "
    echo "and a context ($ZAP_CONTEXT_FILE_NAME) file to the container."
    echo " "
    echo "This script will run through the following steps:"
    echo "- Check that the given website is valid and accessible."
    echo "- Sets the working directory to the project root."
    echo "- Verify that the config and context files are available."
    echo "- Creates the reporting output location (build/reports/zap)."
    echo "- Execute the ZAP full scan in the docker container."
    echo "- Html report ($ZAP_REPORT_FILE_NAME) will be created."
    echo " "
    echo "Usage: $SCRIPT_NAME [website]"
    echo "  website  URL of the website to scan"
    exit 1
}

if ! [ $# -eq 1 ]; then
    echo "× Website is not provided."
    usage
fi
# Passed Website URL
WEBSITE="$1"

# Issue a HEAD request and capture the HTTP status code
STATUS_CODE=$(curl -s --head "$WEBSITE" | head -n 1 | grep -Eo 'HTTP/1.[01] [23]..')
if ! [ "$STATUS_CODE" ]; then
    echo "× Website ($WEBSITE) is not accessible or does not exist."
    usage
fi
echo "✓ Website $WEBSITE is accessible."

# Define the target file you're looking for
REQUIRED_FILE="build.gradle.kts"
# Initialize the current directory
PROJECT_DIR=$(pwd)

# Loop until we find the target file or reach the root directory
while [ "$PROJECT_DIR" != "/" ]; do
    if [ -f "$PROJECT_DIR/$REQUIRED_FILE" ]; then
        echo "✓ Project root directory: $PROJECT_DIR"
        break
    fi
    # Move up to the parent directory
    PROJECT_DIR=$(dirname "$PROJECT_DIR")
done

# If we reached the root directory without finding the file
if [ "$PROJECT_DIR" = "/" ]; then
    echo "× Unable to determine the project root directory!"
    usage
fi

# Here we are jumping to the Project Root directory
cd "$PROJECT_DIR" || exit

# Verify the ZAP directory
SRC_DIR="$PROJECT_DIR/scripts/zap"
if ! [ -d "$SRC_DIR" ]; then
    echo "× ZAP context and config should be available in $SRC_DIR !"
    usage
fi

# Determine the (relative) path of the Context File
CONTEXT_FILE="$SRC_DIR/$ZAP_CONTEXT_FILE_NAME"
if [ -f "$CONTEXT_FILE" ]; then
    #  Convert the full 'local' path into a 'relative' path for use in the docker container
    CONTEXT_FILE=.${CONTEXT_FILE#${PROJECT_DIR}}
else
    echo "× ZAP context file should available at $CONTEXT_FILE !"
    usage
fi

# Determine the (relative) path of the Config File
CONFIG_FILE="$SRC_DIR/$ZAP_CONFIG_FILE_NAME"
if [ -f "$CONFIG_FILE" ]; then
    #  Convert the full 'local' path into a 'relative' path for use in the docker container
    CONFIG_FILE=.${CONFIG_FILE#${PROJECT_DIR}}
else
    echo "× ZAP config file should available at $CONFIG_FILE !"
    usage
fi

# Create reports output location and remove old report
REPORTS_DIR="$PROJECT_DIR/build/reports/zap"
mkdir -p "$REPORTS_DIR"
if ! [ -d "$REPORTS_DIR" ]; then
  echo "× Report output directory ($REPORTS_DIR) invalid."
  usage
fi
rm -f $REPORTS_DIR/$ZAP_REPORT_FILE_NAME

# Full report output location
REPORT_FILE_FULL=$REPORTS_DIR/$ZAP_REPORT_FILE_NAME
# Relative report output location
REPORT_FILE=.${REPORT_FILE_FULL#${PROJECT_DIR}}


# Execute the ZAP full scan
echo "Start scan..."
docker run \
    -v "$(pwd)":/zap/wrk/:rw \
    -t $ZAP_DOCKER_IMAGE zap-full-scan.py \
    -t $WEBSITE \
    -m 1 \
    -a -j \
    -n "$CONTEXT_FILE" \
    -c "$CONFIG_FILE" \
    -r "$REPORT_FILE"

if [ -f "$REPORT_FILE_FULL" ]; then
  echo "✓ ZAP Scan Report available at $REPORT_FILE_FULL"
else
  echo "× ZAP Scan Report not available. Please check the scan output above."
fi
