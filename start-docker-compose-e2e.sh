#!/bin/sh

# ... (previous comments and headers)

export APP_ENV=devlocal
op run --env-file="./.env.tpl" -- sh -c "docker-compose -f docker-compose-e2e.yaml rm -sfv && docker-compose -f docker-compose-e2e.yaml --profile zac up -d"
