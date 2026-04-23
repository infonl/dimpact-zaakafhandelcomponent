#!/usr/bin/env bash

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
set -e

help()
{
   echo "Reindexes ZAC Solr data using a blue-green collection alias strategy."
   echo "Creates a new inactive collection, reindexes into it, atomically switches the"
   echo "'zac' alias to the new collection, then deletes the old collection."
   echo "Requires ZAC to be running and accessible, and Solr to be in SolrCloud mode."
   echo
   echo "Syntax: $0 [-u|s|k|d|t|z|l|h]"
   echo "options:"
   echo "-u     Base ZAC URL. Defaults to 'http://localhost:8080'."
   echo "-s     Base Solr URL (for collection management). Defaults to 'http://localhost:8983'."
   echo "-k     ZAC internal endpoints API key. Defaults to 'fakeZacInternalEndpointsApiKey'."
   echo "-d     Reindex document data only."
   echo "-t     Reindex taak data only."
   echo "-z     Reindex zaak data only."
   echo "-l     Legacy mode: delete all data in the existing collection and reindex in place."
   echo "       Use only when Solr is NOT in SolrCloud mode."
   echo "-h     Print this help."
   echo
}

echoerr() {
  echo 1>&2;
  echo "$@" 1>&2;
  echo 1>&2;
}

zacBaseURL="http://localhost:8080"
solrBaseURL="http://localhost:8983"
zacInternalEndpointsApiKey="fakeZacInternalEndpointsApiKey"
reindexDocuments=true
reindexTasks=true
reindexZaken=true
legacyMode=false

while getopts 'u:s:k:dtzlh' OPTION; do
  case $OPTION in
    u)
      zacBaseURL=$OPTARG
      ;;
    s)
      solrBaseURL=$OPTARG
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
    l)
      legacyMode=true
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

if [ "$legacyMode" = true ] ; then
  echo "Running in legacy mode: deleting existing data and reindexing in place."
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
  exit 0
fi

# Blue-green alias-based reindex
echo "Running in blue-green alias mode using Solr URL: '$solrBaseURL', ZAC URL: '$zacBaseURL'."

# Step 1: Determine the current active collection from the 'zac' alias
echo "Resolving current active collection from alias 'zac'..."
ALIAS_RESPONSE=$(curl -sf "${solrBaseURL}/solr/admin/collections?action=LISTALIASES&wt=json")
CURRENT_COLLECTION=$(echo "$ALIAS_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['aliases']['zac'])" 2>/dev/null || true)

if [ -z "$CURRENT_COLLECTION" ] ; then
  echoerr "Error: Solr alias 'zac' not found. Ensure Solr is running in SolrCloud mode and the alias exists."
  echoerr "To initialize the alias, start ZAC once (SolrDeployerService will create it), or use -l for legacy mode."
  exit 1
fi

echo "Current active collection: '$CURRENT_COLLECTION'"

# Step 2: Determine the inactive collection name (toggle blue/green)
if [ "$CURRENT_COLLECTION" = "zac_a" ] ; then
  NEW_COLLECTION="zac_b"
else
  NEW_COLLECTION="zac_a"
fi

echo "New (inactive) collection: '$NEW_COLLECTION'"

# Step 3: Delete new collection if it already exists (cleanup from a failed previous run)
COLLECTION_LIST=$(curl -sf "${solrBaseURL}/solr/admin/collections?action=LIST&wt=json")
COLLECTION_EXISTS=$(echo "$COLLECTION_LIST" | python3 -c "import sys,json; print('yes' if '${NEW_COLLECTION}' in json.load(sys.stdin).get('collections', []) else 'no')" 2>/dev/null || echo "no")

if [ "$COLLECTION_EXISTS" = "yes" ] ; then
  echo "Deleting stale collection '$NEW_COLLECTION' from a previous failed run..."
  curl -sf "${solrBaseURL}/solr/admin/collections?action=DELETE&name=${NEW_COLLECTION}&wt=json"
fi

# Step 4: Create the new collection
echo "Creating new collection '$NEW_COLLECTION'..."
curl -sf "${solrBaseURL}/solr/admin/collections?action=CREATE&name=${NEW_COLLECTION}&numShards=2&replicationFactor=2&wt=json"

# Step 5: Apply current schema to the new collection
echo "Applying current ZAC schema to collection '$NEW_COLLECTION'..."
curl -v -H "X-API-KEY: ${zacInternalEndpointsApiKey}" \
  "${zacBaseURL}/rest/internal/indexeren/schema/${NEW_COLLECTION}"

# Step 6: Reindex selected types into the new collection
if [ "$reindexDocuments" = true ] ; then
    echo "Reindexing document data into '$NEW_COLLECTION'..."
    curl -v -H "X-API-KEY: ${zacInternalEndpointsApiKey}" \
      "${zacBaseURL}/rest/internal/indexeren/herindexeren/DOCUMENT/${NEW_COLLECTION}"
fi
if [ "$reindexTasks" = true ] ; then
    echo "Reindexing taak data into '$NEW_COLLECTION'..."
    curl -v -H "X-API-KEY: ${zacInternalEndpointsApiKey}" \
      "${zacBaseURL}/rest/internal/indexeren/herindexeren/TAAK/${NEW_COLLECTION}"
fi
if [ "$reindexZaken" = true ] ; then
    echo "Reindexing zaak data into '$NEW_COLLECTION'..."
    curl -v -H "X-API-KEY: ${zacInternalEndpointsApiKey}" \
      "${zacBaseURL}/rest/internal/indexeren/herindexeren/ZAAK/${NEW_COLLECTION}"
fi

# Step 7: Switch alias 'zac' -> new collection (atomic)
echo "Switching alias 'zac' from '$CURRENT_COLLECTION' to '$NEW_COLLECTION'..."
curl -sf "${solrBaseURL}/solr/admin/collections?action=CREATEALIAS&name=zac&collections=${NEW_COLLECTION}&wt=json"

# Step 8: Delete old collection
echo "Deleting old collection '$CURRENT_COLLECTION'..."
curl -sf "${solrBaseURL}/solr/admin/collections?action=DELETE&name=${CURRENT_COLLECTION}&wt=json"

echo "Finished reindexing data. Alias 'zac' now points to '$NEW_COLLECTION'."
