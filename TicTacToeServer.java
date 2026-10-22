import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TicTacToeServer {

    static char[] board = new char[9];
    static char currentPlayer = 'X';
    static boolean gameOver = false;

    static String status = "Player X's turn!";

    static int scoreX = 0;
    static int scoreO = 0;
    static int draws = 0;

    public static void main(String[] args) throws IOException {

        resetBoard();

        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", port), 0
        );

        server.createContext("/style.css", exchange -> {
            try {
                byte[] css = Files.readAllBytes(Path.of("style.css"));

                exchange.getResponseHeaders().set(
                    "Content-Type", "text/css; charset=UTF-8"
                );

                exchange.sendResponseHeaders(200, css.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(css);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.createContext("/", exchange -> {
            try {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleMove(exchange);
                } else {
                    exchange.getRequestBody().close();
                }

                String html = Files.readString(
                    Path.of("index.html"),
                    StandardCharsets.UTF_8
                );

                html = html.replace("{{BOARD}}", createBoardHTML());
                html = html.replace("{{STATUS}}", status);
                html = html.replace("{{SCORE_X}}", String.valueOf(scoreX));
                html = html.replace("{{SCORE_O}}", String.valueOf(scoreO));
                html = html.replace("{{DRAWS}}", String.valueOf(draws));

                sendResponse(exchange, html);

            } catch (Exception e) {
                e.printStackTrace();

                try {
                    sendResponse(
                        exchange,
                        "<h1>Error loading game.</h1>"
                        + "<p>Check index.html and style.css.</p>"
                    );
                } catch (IOException ignored) {
                    exchange.close();
                }
            }
        });

        server.start();

        System.out.println(
            "Tic-Tac-Toe is running on port " + port
        );
        System.out.println(
            "Open your browser at http://localhost:" + port
        );
    }

    static void handleMove(HttpExchange exchange) throws IOException {

        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> form = parseForm(body);

        if (form.containsKey("newround")) {
            resetBoard();
            return;
        }

        if (form.containsKey("reset")) {
            scoreX = 0;
            scoreO = 0;
            draws = 0;

            resetBoard();
            return;
        }

        if (form.containsKey("cell") && !gameOver) {

            int cell;

            try {
                cell = Integer.parseInt(form.get("cell"));
            } catch (NumberFormatException e) {
                return;
            }

            if (cell < 0 || cell >= 9) {
                return;
            }

            if (board[cell] == ' ') {

                board[cell] = currentPlayer;

                if (hasWinner()) {
                    status = "Player " + currentPlayer + " wins!";

                    if (currentPlayer == 'X') {
                        scoreX++;
                    } else {
                        scoreO++;
                    }

                    gameOver = true;

                } else if (isBoardFull()) {
                    status = "It's a draw!";
                    draws++;
                    gameOver = true;

                } else {
                    currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
                    status = "Player " + currentPlayer + "'s turn!";
                }
            }
        }
    }

    static void resetBoard() {

        for (int i = 0; i < board.length; i++) {
            board[i] = ' ';
        }

        currentPlayer = 'X';
        gameOver = false;
        status = "Player X's turn!";
    }

    static String createBoardHTML() {

        StringBuilder html = new StringBuilder();

        for (int i = 0; i < 9; i++) {

            html.append(
                "<form class=\"cell-form\" method=\"POST\" action=\"/\">"
            );

            html.append(
                "<button class=\"cell\" type=\"submit\" name=\"cell\" value=\""
            ).append(i).append("\"");

            if (board[i] != ' ' || gameOver) {
                html.append(" disabled");
            }

            html.append(">");

            if (board[i] != ' ') {
                html.append("<span class=\"mark ")
                    .append(board[i] == 'X' ? "x-mark" : "o-mark")
                    .append("\">")
                    .append(board[i])
                    .append("</span>");
            }

            html.append("</button>");
            html.append("</form>");
        }

        return html.toString();
    }

    static boolean hasWinner() {

        int[][] winningLines = {
            {0, 1, 2},
            {3, 4, 5},
            {6, 7, 8},
            {0, 3, 6},
            {1, 4, 7},
            {2, 5, 8},
            {0, 4, 8},
            {2, 4, 6}
        };

        for (int[] line : winningLines) {

            if (board[line[0]] != ' '
                && board[line[0]] == board[line[1]]
                && board[line[1]] == board[line[2]]) {

                return true;
            }
        }

        return false;
    }

    static boolean isBoardFull() {

        for (char cell : board) {
            if (cell == ' ') {
                return false;
            }
        }

        return true;
    }

    static Map<String, String> parseForm(String body) {

        Map<String, String> form = new HashMap<>();

        if (body == null || body.isEmpty()) {
            return form;
        }

        String[] pairs = body.split("&");

        for (String pair : pairs) {

            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                parts[0], StandardCharsets.UTF_8
            );

            String value = parts.length > 1
                ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                : "";

            form.put(key, value);
        }

        return form;
    }

    static void sendResponse(
        HttpExchange exchange, String response
    ) throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
