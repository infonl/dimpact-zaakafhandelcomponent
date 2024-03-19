#!/bin/bash

#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
set -e

help()
{
   echo "Sends requests to ZAC to reindex zaak, taak and document data in Solr."
   echo
   echo "Syntax: $0 [-u|m|h]"
   echo "options:"
   echo "-u     Base ZAC URL, defaults to 'http://localhost:8080'."
   echo "-m     Maximum number of items to reindex; defaults to '1000'."
   echo "-h     Print this help."
   echo
}

zacBaseURL="http://localhost:8080"
maxItems=1000

while getopts 'u:m:h' OPTION; do
  case $OPTION in
    h)
      Help
      exit;;
    u)
      zacBaseURL=$OPTARG;;
    m)
      maxItems=$OPTARG;;
    \?)
      echo "Error: Invalid option"
      help
      exit;;
  esac
done

echo "Sending requests to ZAC to reindex zaak, taak and document data in Solr using ZAC base URL: '$zacBaseURL' and maximum number of items: '$maxItems'."

## Note that we first need to mark all items for re-indexation per item type
curl ${zacBaseURL}/rest/indexeren/herindexeren/ZAAK \
    && curl ${zacBaseURL}/rest/indexeren/herindexeren/TAAK \
    && curl ${zacBaseURL}/rest/indexeren/herindexeren/DOCUMENT \
    && curl ${zacBaseURL}/rest/indexeren/${maxItems}
