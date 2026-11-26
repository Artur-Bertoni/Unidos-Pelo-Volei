FROM journeyapps/powersync-service:1.29

COPY service.yaml /config/service.yaml
COPY sync-rules.yaml /config/sync-rules.yaml

ENV POWERSYNC_CONFIG_PATH=/config/service.yaml
ENV PS_PORT=8080
ENV NODE_OPTIONS=--max-old-space-size=800

EXPOSE 8080

CMD ["start", "-r", "unified"]
