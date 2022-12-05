FROM twgorg/do-base-image-java-11:75f94dae9958cf830c47a8012e5b17d1fc77754e

ARG artifactId
ARG artifactVersion

LABEL artifactId="${artifactId}"
LABEL artifactVersion="${artifactVersion}"

COPY target/${artifactId}-${artifactVersion}.jar /app/app.jar
RUN echo "${artifactId}-${artifactVersion}"

ENV JAVA_OPTS='-XshowSettings:all -XX:NativeMemoryTracking=summary -XX:+PrintFlagsFinal -XX:MinRAMPercentage=50 -XX:MaxRAMPercentage=80'
USER application:application

CMD ["java", "-jar", "/app/app.jar"]
