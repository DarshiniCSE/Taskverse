#!/bin/sh
set -e

# Render assigns a dynamic PORT environment variable at runtime.
# Default to 8080 if PORT is not set (e.g. during local testing).
PORT="${PORT:-8080}"

echo "[*] Configuring Apache Tomcat 10.1 to listen on PORT ${PORT}..."
sed -i -E "s/Connector port=\"[0-9]+\"/Connector port=\"${PORT}\"/g" /usr/local/tomcat/conf/server.xml

echo "[*] Starting TaskVerse application on port ${PORT}..."
exec catalina.sh run
