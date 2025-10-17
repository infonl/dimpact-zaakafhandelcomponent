#!/usr/bin/env bash

set -e

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

help()
{
   echo "Builds the ZAC Docker image"
   echo
   echo "Syntax: $0 [-v|b|c|t|h]"
   echo "options:"
   echo "-v     Version number"
   echo "-b     Branch name"
   echo "-c     Commit hash"
   echo "-t     Docker Image tag"
   echo "-h     Print this Help"
   echo
}

echoerr() {
  echo 1>&2;
  echo "$@" 1>&2;
  echo 1>&2;
}

while getopts "v:b:c:t:h" option; do
   case "$option" in
       v) versionNumber=${OPTARG};;
       b) branchName=${OPTARG};;
       c) commitHash=${OPTARG};;
       t) tag=${OPTARG};;
       h)
           help
           exit;;
       \?)
           echoerr "Error: Invalid option"
           help
           exit;;
   esac
done

docker build --build-arg versionNumber=$versionNumber --build-arg branchName=$branchName --build-arg commitHash=$commitHash -t $tag .
