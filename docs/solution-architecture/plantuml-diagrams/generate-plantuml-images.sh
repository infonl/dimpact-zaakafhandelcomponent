#!/bin/sh

#
# SPDX-FileCopyrightText: 2023 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# Script to generate PlantUML diagrams as PNG images from .puml files.
# It assumes that PlantUML is installed and available in the system PATH.
# See README.md for installation instructions.

plantuml *.puml --output-dir ../images
