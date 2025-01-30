FROM openjdk:17-jdk-slim

RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*




# Copia la carpeta del repositorio local de Maven con el artefacto específico que necesitas
COPY ./1.4.1-SNAPSHOT /root/.m2/repository/es/tid/netphony/network-protocols/1.4.1-SNAPSHOT
COPY PCEServerConfiguration.xml /usr/src/app/PCEServerConfiguration.xml


COPY . /usr/src/app
WORKDIR /usr/src/app


# RUN mvn clean package -X -P generate-autojar-PCE -f /usr/src/app/pom.xml
# Si no hay modificaciones no hace falta crear el jar en cada instancia
RUN mvn clean package -P generate-autojar-PCE -DskipTests



WORKDIR /usr/src/app/

EXPOSE 4189
EXPOSE 6666
EXPOSE 10060

# RUN java -Dlog4j.configurationFile=log4j2.xml -jar target/PCE-jar-with-dependencies.jar PCEServerConfiguration.xml 

# Comando para ejecutar la aplicación con los parámetros necesarios
CMD ["java", "-Dlog4j.configurationFile=log4j2.xml", "-jar", "target/PCE-jar-with-dependencies.jar", "PCEServerConfiguration.xml"]