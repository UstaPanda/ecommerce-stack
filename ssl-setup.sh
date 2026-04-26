#!/bin/bash

# Load .env file properly
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

if [ -z "$DOMAIN_NAME" ] || [ "$DOMAIN_NAME" == "localhost" ]; then
    echo "Error: Please set a valid DOMAIN_NAME in your .env file."
    exit 1
fi

echo "--- Starting SSL Certificate Request for $DOMAIN_NAME ---"

# 1. Start gateway (Nginx) - it might be running with dummy certs
docker-compose up -d gateway

# 2. Check if the current certificate is a "Dummy" cert
# Certbot fails if a directory exists but wasn't created by Certbot.
# We will remove the directory to give Certbot a clean slate.
if [ -d "./certbot/conf/live/$DOMAIN_NAME" ]; then
    echo "Existing certificate directory found. Cleaning up for a fresh Let's Encrypt issuance..."
    # We remove the live and archive folders for this domain
    sudo rm -rf "./certbot/conf/live/$DOMAIN_NAME"
    sudo rm -rf "./certbot/conf/archive/$DOMAIN_NAME"
    sudo rm -rf "./certbot/conf/renewal/$DOMAIN_NAME.conf"
fi

# 3. Request the real certificate
echo "Requesting real certificate from Let's Encrypt..."
docker-compose run --rm certbot certonly --webroot \
    --webroot-path=/var/www/certbot \
    --email "$SSL_EMAIL" \
    --agree-tos \
    --no-eff-email \
    --non-interactive \
    -d "$DOMAIN_NAME"

# 4. Reload Nginx to use the real certificate
echo "Reloading Nginx with the new certificate..."
docker-compose exec gateway nginx -s reload

echo "--- SSL Setup Complete! ---"
echo "Your site should now be live at https://$DOMAIN_NAME"
