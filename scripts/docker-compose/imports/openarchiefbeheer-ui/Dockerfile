FROM nginx

COPY nginx.conf /etc/nginx/conf.d/openarchiefbeheer-ui.conf

COPY --from=maykinmedia/open-archiefbeheer:0.1.0-beta.39@sha256:79f3c59248c6e7143606a096b0885e2acc2b1545a4ec1fb004c2d31bc6b61ca4 /app /usr/share/nginx/html
COPY --from=maykinmedia/open-archiefbeheer:0.1.0-beta.39@sha256:79f3c59248c6e7143606a096b0885e2acc2b1545a4ec1fb004c2d31bc6b61ca4 /replace-envvars.sh /replace-envvars.sh

RUN ln -s /usr/share/nginx/html /app

CMD /replace-envvars.sh && nginx -g 'daemon off;'
