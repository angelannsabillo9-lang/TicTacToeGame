
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

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
                String css = Files.readString(
                    Path.of("style.css"),
                    StandardCharsets.UTF_8
                );

                byte[] data = css.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                    "Content-Type", "text/css; charset=UTF-8"
                );

                exchange.sendResponseHeaders(200, data.length);

                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }
            } catch (IOException e) {
                sendResponse(exchange, 500, "Could not load CSS.");
            }
        });

        server.createContext("/", exchange -> {
            try {
                if ("POST".equalsIgnoreCase(
                        exchange.getRequestMethod())) {

                    handleMove(exchange);
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

                byte[] data = html.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                    "Content-Type", "text/html; charset=UTF-8"
                );

                exchange.sendResponseHeaders(200, data.length);

                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }

            } catch (IOException e) {
                sendResponse(exchange, 500,
                    "Error loading the game. Check your project files.");
            }
        });

        server.setExecutor(null);
        server.start();

        System.out.println("Tic-Tac-Toe is running!");
        System.out.println("Open http://localhost:8080");
        System.out.println("Press Ctrl+C to stop the server.");
    }

    static void handleMove(HttpExchange exchange) throws IOException {

        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> form = parseForm(body);

        if (form.containsKey("reset")) {
            resetBoard();
            status = "Player X's turn!";
        } else if (form.containsKey("cell") && !gameOver) {

            try {
                int cell = Integer.parseInt(form.get("cell"));

                if (cell >= 0 && cell < 9 && board[cell] == ' ') {

                    board[cell] = currentPlayer;

                    if (hasWinner()) {
                        gameOver = true;

                        if (currentPlayer == 'X') {
                            scoreX++;
                        } else {
                            scoreO++;
                        }

                        status = "Player " + currentPlayer + " wins!";
                    } else if (isBoardFull()) {
                        gameOver = true;
                        draws++;
                        status = "It's a draw!";
                    } else {
                        currentPlayer =
                            (currentPlayer == 'X') ? 'O' : 'X';

                        status = "Player " + currentPlayer + "'s turn!";
                    }
                }

            } catch (NumberFormatException e) {
                status = "Invalid move.";
            }
        }

        exchange.getRequestBody().close();
    }

    static Map<String, String> parseForm(String body) {

        Map<String, String> result = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8
                );

                String value = URLDecoder.decode(
                    parts[1], StandardCharsets.UTF_8
                );

                result.put(key, value);
            }
        }

        return result;
    }

    static String createBoardHTML() {

        StringBuilder html = new StringBuilder();

        for (int i = 0; i < 9; i++) {

            html.append("<form class=\"cell-form\" method=\"POST\" action=\"/\">");

            html.append("<button class=\"cell\" type=\"submit\" name=\"cell\" value=\"")
                .append(i).append("\"");

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
            } else {
                html.append("<span class=\"empty-mark\">+</span>");
            }

            html.append("</button>");
            html.append("</form>");
        }

        return html.toString();
    }

    static boolean hasWinner() {

        int[][] wins = {
            {0, 1, 2},
            {3, 4, 5},
            {6, 7, 8},
            {0, 3, 6},
            {1, 4, 7},
            {2, 5, 8},
            {0, 4, 8},
            {2, 4, 6}
        };

        for (int[] line : wins) {
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

    static void resetBoard() {

        for (int i = 0; i < board.length; i++) {
            board[i] = ' ';
        }

        currentPlayer = 'X';
        gameOver = false;
        status = "Player X's turn!";
    }

    static void sendResponse(
        HttpExchange exchange, int code, String message
    ) throws IOException {

        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(code, data.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }
}
