#!/bin/bash
#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# To setup a python virtual environment, with required libraries, run this script with the following command:
# source ./init-pyenv.sh
#


# Global variables
SOURCED=1
PYTHON_PATH="python3"

# Display the script's help
show_help() {
    echo "Usage: source $0 [-h] [-v] [-r] [-c] [-p path_to_python3]"
    echo "Options:"
    echo "  -h    Show this help"
    echo "  -v    Enable verbose mode"
    echo "  -r    Re-install the virtual environment"
    echo "  -c    Clean the virtual environment"
    echo "  -p    Specify the path to your local Python3 instance"
    echo ""
    echo "Note: execute this script using 'source' to ensure the current terminal session"
    echo "      will be initialised with the python virtual environment, so it can be used"
    echo "      immediately to execute the python scripts. If that is not used, the"
    echo "      virtual will have to be activate manually."
}

# Enable verbose output
enable_verbose() {
    echo "Verbose mode enabled"
    set -x
}

# Disable verbose output
disable_verbose() {
    set +x
}

# Clean (remove) the virtual environment
clean_virtual_environment() {
    echo "Clean virtual environment"
    rm -rf .venv
    echo "Don't forget to deactivate the virtual environment if you won't be using it again by running:"
    echo ""
    echo "deactivate"
    echo ""
}

# If there is no virtual environment available, then
# check if there is a python3 available to c
# create a virtual environment
create_virtual_environment() {
      if [ -d .venv ]; then
          echo "Virtual environment already exists."
          echo "To re-create the virtual environment, use:"
          echo "$0 -r"
          echo ""
      elif ! command -v "$PYTHON_PATH" &> /dev/null; then
          echo "Python3 could not be found. Please install Python3 to proceed."
          echo "To specify the Python3 location manually, use:"
          echo "$0 -p path_to_python3"
          echo ""
          exit 1
      else
          echo "Create virtual environment"
          $PYTHON_PATH -m venv .venv
      fi
}

# Activate the virtual environment, and
# verify that it has been activated
activate_virtual_environment() {
    source .venv/bin/activate
    if [ -z "$VIRTUAL_ENV" ]; then
        echo "Failed to activate the virtual environment"
        exit 1
    else
        echo "Virtual environment activated successfully"
    fi
}

# Upgrade the pip version, and
# install the required libraries from requirements.txt
update_virtual_environment() {
    pip install --upgrade pip
    pip install -r requirements.txt
}

# Check if the script is being sourced.
# If not show a message indicating that the virtual environment will have to be manually activated.
check_virtual_environment() {
    echo "Check virtual environment:"
    if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
        echo ""
        echo "The python virtual environment has been created and required libraries have been"
        echo "installed, but it will not be active in the current terminal session."
        echo "To activate the virtual environment run:"
        echo ""
        echo "source .venv/bin/activate"
        echo ""
    fi
}

# create the virtual environment,
# activate the virtual environment,
# update the virtual environment, and
# check if the virtual environment is usable
install_virtual_environment() {
      create_virtual_environment
      activate_virtual_environment
      update_virtual_environment
      check_virtual_environment
}

# Parse command-line options
while getopts "hvrcp:" opt; do
    case ${opt} in
        h )
            show_help
            exit 0
            ;;
        v )
            enable_verbose
            ;;
        r )
            clean_virtual_environment
            ;;
        c )
            clean_virtual_environment
            exit 0
            ;;
        p )
            # Store passed python path
            PYTHON_PATH=$OPTARG
            ;;
        \? )
            show_help
            exit 1
            ;;
    esac
done

install_virtual_environment

disable_verbose
