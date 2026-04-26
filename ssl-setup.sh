#!/bin/bash

# Load .env file
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

if [ -z "$DOMAIN_NAME" ] || [ "$DOMAIN_NAME" == "localhost" ]; then
    echo "Error: Please set a valid DOMAIN_NAME in your .env file."
    exit 1
fi

if [ -z "$SSL_EMAIL" ]; then
    echo "Error: Please set SSL_EMAIL in your .env file."
    exit 1
fi

echo "--- Starting SSL Certificate Request for $DOMAIN_NAME ---"

# 1. Start gateway to handle ACME challenge
docker-compose up -d gateway

# 2. Request certificate
docker-compose run --rm certbot certonly --webroot --webroot-path=/var/www/certbot \
    --email $SSL_EMAIL --agree-tos --no-eff-email \
    -d $DOMAIN_NAME

# 3. Reload Nginx to use the new certificate
echo "Reloading Nginx..."
docker-compose exec gateway nginx -s reload

echo "--- SSL Setup Complete! ---"
echo "Your site should now be accessible at https://$DOMAIN_NAME"
