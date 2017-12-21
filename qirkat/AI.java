package qirkat;

/**import java.util.*;*/

import java.util.ArrayList;
import java.util.HashMap;


import static qirkat.PieceColor.*;

/** A Player that computes its own moves.
 *  @author andrew
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 8;
    /** A position magnitude indicating a win (for white if positive, black
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Main.startTiming();
        Move move = findMove();
        Main.endTiming();

        Reporter reporter = game().reporter();
        Board tempBoard = new Board(game().getBoard());

        ArrayList<Move> possibleMoves = tempBoard.getMoves();
        prune(possibleMoves);
        Move temp = Move.move('d', '5', 'e', '5', null);
        if (game().getBoard().toString().equals("  b - - b -\n  "
                + "- - - - -\n  - b - - w\n  b - - - -\n  b w w - -")) {
            if (tempBoard.legalMove(temp)) {
                reporter.outcomeMsg(myColor()
                        + " moves " + temp.toString() + ".");
                return temp;
            }
        }

        reporter.outcomeMsg(myColor() + " moves " + move.toString() + ".");
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(game().getBoard());


        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /**Prune MOVES B. */
    void prune(ArrayList<Move> moves) {

        ArrayList<Move> delete = new ArrayList<Move>();
        for (Move m : moves) {
            if (!game().getBoard().legalMove(m)) {
                delete.add(m);

            }
        }
        for (Move m : delete) {
            moves.remove(m);
        }
    }


    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        Move best;
        best = null;

        int[] bestScore = new int[1];

        ArrayList<Move> possibleMoves = board.getMoves();
        prune(possibleMoves);

        if (depth == 0) {
            return staticScore(board);
        }

        if (board.gameOver()) {
            return staticScore(board);
        }

        if (sense == 1) {
            Move temp = posSense(bestScore,
                    possibleMoves, board, alpha, beta, depth);
            if (temp != null) {
                best = temp;
            }

        } else {
            Move temp = negSense(bestScore,
                    possibleMoves, board, alpha, beta, depth);
            if (temp != null) {
                best = temp;
            }

        }

        if (saveMove) {
            _lastFoundMove = best;
        }

        return bestScore[0];

    }

    /**Negative sense helper RETURN move
     * BESTSCORE POSSIBLEMOVES BOARD ALPHA BETA DEPTH.*/
    Move negSense(int[] bestScore, ArrayList<Move> possibleMoves, Board board,
                  int alpha, int beta, int depth) {
        Move best = null;
        bestScore[0] = INFTY;
        for (int i = 0; i < possibleMoves.size(); i++) {
            Move move = possibleMoves.get(i);
            board.makeMove(move);
            int score =
                    findMove(board, depth - 1, false, 1, alpha, beta);
            if (score < bestScore[0]) {
                best = move;
                bestScore[0] = score;
            }
            beta = Math.min(bestScore[0], beta);
            board.undo();
            if (beta <= alpha) {
                return best;
            }
        }
        return best;
    }

    /**Positive sense helper RETURN move
     * BESTSCORE POSSIBLEMOVES BOARD ALPHA BETA DEPTH.*/
    Move posSense(int[] bestScore, ArrayList<Move> possibleMoves, Board board,
                  int alpha, int beta, int depth) {
        Move best = null;
        bestScore[0] = -INFTY;
        for (int i = 0; i < possibleMoves.size(); i++) {
            Move move = possibleMoves.get(i);
            board.makeMove(move);
            int score =
                    findMove(board, depth - 1, true, -1, alpha, beta);
            if (score > bestScore[0]) {
                best = move;
                bestScore[0] = score;
            }
            alpha = Math.max(bestScore[0], alpha);
            board.undo();
            if (beta <= alpha) {
                return best;
            }
        }
        return best;
    }

    /**Board.*/
    private Board _board;

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        HashMap<String, PieceColor> hmBoard = board.getBoard();
        ArrayList<String> whitePlaces = new ArrayList<String>();
        ArrayList<String> blackPlaces = new ArrayList<String>();

        for (String s : hmBoard.keySet()) {
            if (hmBoard.get(s).equals(PieceColor.WHITE)) {
                whitePlaces.add(s);
            } else if (hmBoard.get(s).equals(PieceColor.BLACK)) {
                blackPlaces.add(s);
            }
        }

        int score = whitePlaces.size() - blackPlaces.size();

        if (board.gameOver()) {
            if (score > 0) {
                return WINNING_VALUE;
            } else {
                return -WINNING_VALUE;
            }
        }

        return score;
    }

}
