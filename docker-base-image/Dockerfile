# AlpineLinux with a glibc-2.29-r0 and OpenJDK Java 8
FROM alpine:3.11
MAINTAINER dbuschman <dbuschman@concertohealthai.com>

RUN  set -ex \
  && apk -U upgrade \
  && apk add --upgrade libstdc++ ca-certificates bash java-cacerts openjdk11-jre=11.0.5_p10-r0  gnupg \
  && rm -vfr /var/cache/apk/* \
  && echo "securerandom.source=file:/dev/urandom" >> /usr/lib/jvm/default-jvm/jre/lib/security/java.security \
  && java -version

ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk 

# EOF