#!/bin/bash
cd ../..


# Shows available actions
#act --list

# Run the default (`push`) event
act --job create-release --verbose --dryrun
