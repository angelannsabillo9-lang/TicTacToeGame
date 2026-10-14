
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY TicTacToeServer.java .
COPY index.html .
COPY style.css .

RUN javac --add-modules jdk.httpserver TicTacToeServer.java

EXPOSE 10000

CMD ["java", "--add-modules", "jdk.httpserver", "TicTacToeServer"]
