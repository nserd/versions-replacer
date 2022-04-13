FROM alpine:3.13.6
RUN apk --no-cache add openjdk11-jre-headless --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community
COPY ./artifacts/versions-replacer-1.0.jar /opt/versions-replacer.jar
WORKDIR /opt
ENTRYPOINT ["java", "-jar", "versions-replacer.jar"]
