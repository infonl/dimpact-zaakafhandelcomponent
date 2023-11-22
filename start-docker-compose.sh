#!/bin/sh

#
# SPDX-FileCopyrightText: 2023 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

Help()
{
   echo "Starts the ZAC Docker Compose environment using the 1Password CLI tools to retrieve secrets."
   echo
   echo "Syntax: start-docker-compose.sh [-d|z|h]"
   echo "options:"
   echo "-d     Delete local Docker volume data before starting Docker Compose."
   echo "-z     Also start the ZAC Docker container as part of the Docker Compose environment."
   echo "-h     Print this Help."
   echo
}

volumeDataFolder="./scripts/docker-compose/volume-data"
startZac=false

while getopts ':dhz' OPTION; do
  case $OPTION in
    d)
      echo "Deleting local Docker volume data folder: '$volumeDataFolder'.."
      rm -rf $volumeDataFolder
      echo "Done"
      ;;
    h)
      Help
      exit;;
    z)
      # Do a pull first to make sure we use the latest ZAC Docker Image
      echo "Pulling latest ZAC Docker Image.."
      startZac=true
      docker compose pull zac
      ;;
    \?)
      echo "Error: Invalid option"
      Help
      exit;;
  esac
done

# Uses the 1Password CLI tools to set up the environment variables for running Docker Compose and ZAC in IntelliJ.
# Please see docs/INSTALL.md for details on how to use this script.
if [ "$startZac" = true ] ; then
  echo "Starting Docker Compose environment with ZAC.."
  export APP_ENV=devlocal && op run --env-file="./.env.tpl" --no-masking -- docker compose --profile zac --project-name zac up -d
else
  echo "Starting Docker Compose environment without ZAC.."
  export APP_ENV=devlocal && op run --env-file="./.env.tpl" --no-masking -- docker compose --project-name zac up -d
fi
