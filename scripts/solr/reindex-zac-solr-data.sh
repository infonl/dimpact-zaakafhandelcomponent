#!/usr/bin/env bash

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -e

help()
{
   echo "Sends request(s) to ZAC to reindex zaak, taak and/or document data in Solr. By default all zaak, taak and document data is re-indexed. Note that the ZAC endpoint used requires API key authentication."
   echo
   echo "Syntax: $0 [-u|k|d|t|z|h]"
   echo "options:"
   echo "-u     Base ZAC URL. Defaults to 'http://localhost:8080'."
   echo "-k     ZAC internal endpoints API key. Defaults to 'fakeZacInternalEndpointsApiKey'."
   echo "-d     Reindex document data only."
   echo "-t     Reindex taak data only."
   echo "-z     Reindex zaak data only."
   echo "-h     Print this help."
   echo
}

echoerr() {
  echo 1>&2;
  echo "$@" 1>&2;
  echo 1>&2;
}

zacBaseURL="http://localhost:8080"
zacInternalEndpointsApiKey="fakeZacInternalEndpointsApiKey"
reindexDocuments=true
reindexTasks=true
reindexZaken=true

while getopts 'u:k:dzh' OPTION; do
  case $OPTION in
    u)
      zacBaseURL=$OPTARG
      ;;
    k)
      zacInternalEndpointsApiKey=$OPTARG
      ;;
    d)
      reindexDocuments=true
      reindexTasks=false
      reindexZaken=false
      ;;
    t)
      reindexDocuments=false
      reindexTasks=true
      reindexZaken=false
      ;;
    z)
      reindexDocuments=false
      reindexTasks=false
      reindexZaken=true
      ;;
    h)
      help
      exit;;
    \?)
      echoerr "Error: Invalid option"
      help
      exit;;
  esac
done

if [ "$reindexDocuments" = true ] ; then
    echo "Sending request to ZAC to reindex document data in Solr using ZAC base URL: '$zacBaseURL'."
    curl -v -H "X-API-KEY: ${zacInternalEndpointsApiKey}" ${zacBaseURL}/rest/internal/indexeren/herindexeren/DOCUMENT
fi
if [ "$reindexTasks" = true ] ; then
    echo "Sending request to ZAC to reindex task data in Solr using ZAC base URL: '$zacBaseURL'."
    curl -v -H "X-API-KEY: ${zacInternalEndpointsApiKey}" ${zacBaseURL}/rest/internal/indexeren/herindexeren/TAAK
fi
if [ "$reindexZaken" = true ] ; then
    echo "Sending request to ZAC to reindex zaak data in Solr using ZAC base URL: '$zacBaseURL'."
    curl -v -H "X-API-KEY: ${zacInternalEndpointsApiKey}" ${zacBaseURL}/rest/internal/indexeren/herindexeren/ZAAK
fi
echo "Finished reindexing data."


