# TaskVerse — Production Dockerfile for Render Deployment
# Runtime: Java 21, Apache Tomcat 10.1 (Jakarta EE 10 / Servlet 6.0)

FROM tomcat:10.1-jdk21-temurin-jammy

LABEL maintainer="TaskVerse Team"
LABEL description="TaskVerse Smart Task Management Workspace on Apache Tomcat 10.1"

WORKDIR /usr/local/tomcat

# Remove default Tomcat web applications so TaskVerse serves at root context (/)
RUN rm -rf webapps/*

# Copy webapp assets and static files into ROOT
COPY webapp/ webapps/ROOT/

# Copy Java source files for compilation
COPY src/ /tmp/src/

# Compile all Java source files directly into ROOT/WEB-INF/classes
RUN mkdir -p webapps/ROOT/WEB-INF/classes && \
    javac -encoding UTF-8 \
      -cp "lib/servlet-api.jar:webapps/ROOT/WEB-INF/lib/*:webapps/ROOT/WEB-INF/classes" \
      -d webapps/ROOT/WEB-INF/classes \
      $(find /tmp/src -name "*.java") && \
    rm -rf /tmp/src

# Install entrypoint script to dynamically configure Render's PORT
COPY docker-entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# Render listens on the dynamic PORT assigned at container launch (default 8080)
EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
