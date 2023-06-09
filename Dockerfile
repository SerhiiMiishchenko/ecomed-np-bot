FROM amazoncorretto:17
LABEL authors="Serhii"
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve
COPY src ./src
CMD ["./mvnw", "spring-boot:run"]
ENV FLY_API_HOSTNAME="https://api.machines.dev"


