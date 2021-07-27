FROM twgorg/twg-base-image-java-11:latest

ARG artifactId
ARG artifactVersion

LABEL artifactId="${artifactId}"
LABEL artifactVersion="${artifactVersion}"

COPY target/${artifactId}-${artifactVersion}.jar /app/app.jar
RUN echo "${artifactId}-${artifactVersion}"

ENV JAVA_OPTS='-XshowSettings:all -XX:NativeMemoryTracking=summary -XX:+PrintFlagsFinal -XX:MinRAMPercentage=50 -XX:MaxRAMPercentage=80'
USER twgapplication:twgapplication

CMD ["java", "-jar", "/app/app.jar"]
