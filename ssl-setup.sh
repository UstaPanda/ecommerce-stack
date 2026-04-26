#!/bin/bash

# Load .env file properly
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

if [ -z "$DOMAIN_NAME" ] || [ "$DOMAIN_NAME" == "localhost" ]; then
    echo "Error: Please set a valid DOMAIN_NAME in your .env file (e.g. DOMAIN_NAME=zorlukurt.com)."
    exit 1
fi

if [ -z "$SSL_EMAIL" ]; then
    echo "Error: Please set SSL_EMAIL in your .env file (e.g. SSL_EMAIL=admin@gmail.com)."
    exit 1
fi

echo "--- Starting SSL Certificate Request for $DOMAIN_NAME ---"

# 1. Start gateway to handle ACME challenge
echo "Starting Nginx gateway..."
docker-compose up -d gateway

# 2. Request certificate using certbot
echo "Requesting certificate from Let's Encrypt for $DOMAIN_NAME..."
docker-compose run --rm certbot certonly --webroot \
    --webroot-path=/var/www/certbot \
    --email "$SSL_EMAIL" \
    --agree-tos \
    --no-eff-email \
    --non-interactive \
    -d "$DOMAIN_NAME"

# 3. Reload Nginx to use the new certificate
echo "Reloading Nginx..."
docker-compose exec gateway nginx -s reload

echo "--- SSL Setup Complete! ---"
echo "If the command above succeeded, your site is now at https://$DOMAIN_NAME"
echo "If it failed, check that your VPS firewall allows port 80 and your DNS points to this IP."
