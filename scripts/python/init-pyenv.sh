#!/bin/bash
#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Function to display help
show_help() {
    echo "Usage: $0 [-h] [-v] [-r] [-c] [-p path_to_python3]"
    echo "Options:"
    echo "  -h    Show help"
    echo "  -v    Enable verbose mode"
    echo "  -r    Re-install virtual environment"
    echo "  -c    Clean virtual environment"
    echo "  -p    Specify path to local Python3 instance"
}

# Parse command-line options
VERBOSE=0
REINSTALL=0
CLEAN=0
PYTHON_PATH="python3"
while getopts "hvrcp:" opt; do
    case ${opt} in
        h )
            show_help
            exit 0
            ;;
        v )
            VERBOSE=1
            ;;
        r )
            REINSTALL=1
            ;;
        c )
            CLEAN=1
            ;;
        p )
            PYTHON_PATH=$OPTARG
            ;;
        \? )
            show_help
            exit 1
            ;;
    esac
done

# Verbose mode
if [ $VERBOSE -eq 1 ]; then
    echo "Verbose mode enabled"
    set -x
fi

# Clean virtual environment
if [ $CLEAN -eq 1 ]; then
    echo "Cleaning virtual environment"
    rm -rf .venv
    exit 0
fi

# Re-install virtual environment
if [ $REINSTALL -eq 1 ]; then
    echo "Re-installing virtual environment"
    rm -rf .venv
fi

# Create a virtual environment for Python
if [ -d .venv ]; then
    echo "Virtual environment already exists."
    echo "To re-create the virtual environment, use:"
    echo "$0 -r"
elif ! command -v $PYTHON_PATH &> /dev/null; then
    echo "Python3 could not be found. Please install Python3 to proceed."
    echo "To specify the Python3 location, use:"
    echo "$0 -p path_to_python3"
    exit 1
else
    echo "Create virtual environment"
    $PYTHON_PATH -m venv .venv
fi

# Activate the virtual environment and install the dependencies
source .venv/bin/activate

# Verify that the virtual environment has been activated
if [ -z "$VIRTUAL_ENV" ]; then
    echo "Failed to activate the virtual environment"
    exit 1
else
    echo "Virtual environment activated successfully"
fi

pip install --upgrade pip
pip install -r requirements.txt
