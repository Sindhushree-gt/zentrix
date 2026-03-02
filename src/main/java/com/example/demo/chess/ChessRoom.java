package com.example.demo.chess;

import java.util.*;

class ChessRoom {
    String roomId;
    String whitePlayer;
    String blackPlayer;
    String status; // waiting | active | ended
    String[][] board; // 8x8, null="", e.g. "wK","bP"
    String turn; // "w" | "b"
    int[] enPassant; // null or [row,col]
    Map<String, Map<String, Boolean>> castleRights;
    int[] lastMoveFrom, lastMoveTo;
    String gameStatus; // idle | check | checkmate | stalemate
    List<String> capturedW = new ArrayList<>();
    List<String> capturedB = new ArrayList<>();
    List<Map<String, Object>> moveHistory = new ArrayList<>();

    ChessRoom(String roomId, String whitePlayer) {
        this.roomId = roomId;
        this.whitePlayer = whitePlayer;
        this.status = "waiting";
        this.turn = "w";
        this.gameStatus = "idle";
        this.board = initBoard();
        this.castleRights = Map.of(
                "w", new HashMap<>(Map.of("kside", true, "qside", true)),
                "b", new HashMap<>(Map.of("kside", true, "qside", true)));
    }

    String[][] initBoard() {
        String[][] b = new String[8][8];
        String[] back = { "R", "N", "B", "Q", "K", "B", "N", "R" };
        for (int c = 0; c < 8; c++) {
            b[0][c] = "b" + back[c];
            b[1][c] = "bP";
            b[6][c] = "wP";
            b[7][c] = "w" + back[c];
        }
        return b;
    }

    void applyMove(MoveMessage msg) {
        int fr = msg.from[0], fc = msg.from[1];
        int tr = msg.to[0], tc = msg.to[1];
        String piece = board[fr][fc];
        String captured = board[tr][tc];

        // Apply move
        board[tr][tc] = msg.promotion != null ? (piece.charAt(0) + "" + msg.promotion) : piece;
        board[fr][fc] = null;

        // En passant capture
        if (piece != null && piece.endsWith("P") && enPassant != null
                && tr == enPassant[0] && tc == enPassant[1]) {
            int capturedRow = piece.startsWith("w") ? tr + 1 : tr - 1;
            captured = board[capturedRow][tc];
            board[capturedRow][tc] = null;
        }

        // Castling rook move
        if (piece != null && piece.endsWith("K")) {
            if (tc - fc == 2) {
                board[fr][7] = null;
                board[fr][5] = (turn.equals("w") ? "wR" : "bR");
            }
            if (fc - tc == 2) {
                board[fr][0] = null;
                board[fr][3] = (turn.equals("w") ? "wR" : "bR");
            }
            castleRights.get(turn).put("kside", false);
            castleRights.get(turn).put("qside", false);
        }
        if (piece != null && piece.endsWith("R")) {
            if (fc == 7)
                castleRights.get(turn).put("kside", false);
            if (fc == 0)
                castleRights.get(turn).put("qside", false);
        }

        // Pawn promo (auto queen if no promotion specified)
        if (piece != null && piece.endsWith("P") && (tr == 0 || tr == 7)) {
            board[tr][tc] = piece.charAt(0) + "Q";
        }

        // En passant target
        enPassant = null;
        if (piece != null && piece.endsWith("P") && Math.abs(tr - fr) == 2) {
            enPassant = new int[] { (fr + tr) / 2, tc };
        }

        // Track captures
        if (captured != null) {
            if (turn.equals("w"))
                capturedW.add(captured);
            else
                capturedB.add(captured);
        }

        // Move history
        Map<String, Object> histEntry = new HashMap<>();
        histEntry.put("from", msg.from);
        histEntry.put("to", msg.to);
        histEntry.put("piece", piece);
        moveHistory.add(histEntry);

        lastMoveFrom = msg.from;
        lastMoveTo = msg.to;
        turn = turn.equals("w") ? "b" : "w";
        gameStatus = msg.gameStatus != null ? msg.gameStatus : "idle";
    }

    Map<String, Object> toStateMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("roomId", roomId);
        m.put("board", board);
        m.put("turn", turn);
        m.put("enPassant", enPassant);
        m.put("castleRights", castleRights);
        m.put("status", status);
        m.put("gameStatus", gameStatus);
        m.put("players",
                Map.of("w", whitePlayer != null ? whitePlayer : "", "b", blackPlayer != null ? blackPlayer : ""));
        m.put("lastMove", lastMoveFrom != null ? Map.of("from", lastMoveFrom, "to", lastMoveTo) : null);
        m.put("capturedW", capturedW);
        m.put("capturedB", capturedB);
        m.put("moveHistory", moveHistory);
        return m;
    }
}
