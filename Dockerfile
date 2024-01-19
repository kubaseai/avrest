FROM ubuntu:22.04
RUN apt-get update && apt-get upgrade -y && apt-get install openjdk-17-jre clamav clamdscan clamav-daemon -y && freshclam --show-progress
RUN echo "OnAccessMaxFileSize 25M" >> /etc/clamav/clamd.conf; echo "OnAccessPrevention yes" >> /etc/clamav/clamd.conf; echo "OnAccessIncludePath /tmp/av-scan" >> /etc/clamav/clamd.conf; echo "OnAccessExcludeUname clamav" >> /etc/clamav/clamd.conf; echo "OnAccessDisableDDD yes" >> /etc/clamav/clamd.conf
COPY target/api-av-*.jar /
EXPOSE 8080
EXPOSE 1344
ENTRYPOINT id; mkdir -p /var/run/clamav; chown -R clamav:clamav /var/run/clamav; /usr/sbin/clamd || echo "ERROR: clamd"; freshclam || echo "ERROR: av signatures update"; mkdir /tmp/av-scan; clamonacc -v --log /var/log/clamav/onaccess.log --remove && java -jar /api*.jar
HEALTHCHECK --interval=60s --timeout=15s --start-period=180s CMD curl --fail -k -u pam:access https://localhost:8080/rest/api/1.0/av-scan/files/ || exit 1