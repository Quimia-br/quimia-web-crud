# Imagem de produção do painel (usada pelo Render). Duas etapas: compila numa imagem com o JDK e
# copia só o .jar para uma imagem menor, só com o JRE.

# ---- 1. compilar ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Primeiro só o que define as dependências: enquanto o pom.xml não mudar, o Docker reaproveita essa camada
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Os testes rodam no CI, não aqui: esta etapa só gera o .jar
RUN ./mvnw -B -q package -DskipTests

# ---- 2. rodar ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Não roda como root: se o app for invadido, o invasor não é administrador do container
RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build --chown=app:app /workspace/target/QuimiaWebCrud-*.jar /app/app.jar
USER app

# Fuso do Brasil: as datas gravadas pelo app (LocalDateTime.now) e o "hoje" do dashboard seguem o horário local.
# Memória: o plano pequeno do Render tem 512 MB, então a JVM usa só uma parte e o coletor mais leve.
ENV TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-Duser.timezone=America/Sao_Paulo -XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss512k"

# O Render define a variável PORT; sem ela o app usa 8080 (ver server.port no application.properties)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
