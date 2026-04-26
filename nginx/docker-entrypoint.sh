#!/bin/sh
set -e

# Substitute environment variables in the nginx configuration template
envsubst '${DOMAIN_NAME}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

# Check if SSL certificates exist
if [ ! -f "/etc/letsencrypt/live/${DOMAIN_NAME}/fullchain.pem" ]; then
    echo "SSL certificates not found for ${DOMAIN_NAME}. Creating dummy certificates to allow Nginx to start..."
    mkdir -p "/etc/letsencrypt/live/${DOMAIN_NAME}"
    openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
        -keyout "/etc/letsencrypt/live/${DOMAIN_NAME}/privkey.pem" \
        -out "/etc/letsencrypt/live/${DOMAIN_NAME}/fullchain.pem" \
        -subj "/CN=localhost"
fi

exec "$@"
