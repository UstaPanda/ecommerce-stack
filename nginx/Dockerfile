FROM nginx:alpine

# Install envsubst and openssl for dynamic configuration and dummy certs
RUN apk add --no-cache gettext openssl

# Copy template and entrypoint script
COPY nginx.conf.template /etc/nginx/nginx.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.sh

RUN chmod +x /docker-entrypoint.sh

# Default domain to localhost if not provided
ENV DOMAIN_NAME=localhost

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
