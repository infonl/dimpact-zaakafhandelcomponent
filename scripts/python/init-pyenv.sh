#!/bin/bash
#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Create a virtual environment for Python
if [ -d .venv ]; then
    echo "Virtual environment already exists"
else
    echo "Create virtual environment"
    python3 -m venv .venv
fi

# Activate the virtual environment and install the dependencies
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
