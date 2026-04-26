#!/bin/bash

# 1. Start gateway to handle ACME challenge
echo "Starting Nginx gateway..."
docker-compose up -d gateway

# 2. Request certificate using the command defined in docker-compose
echo "Requesting certificate from Let's Encrypt..."
docker-compose run --rm certbot

# 3. Reload Nginx to use the new certificate
echo "Reloading Nginx..."
docker-compose exec gateway nginx -s reload

echo "--- SSL Setup Attempt Finished ---"
echo "Check the logs above. If it said 'Successfully received certificate', your site is now at https://$DOMAIN_NAME"
