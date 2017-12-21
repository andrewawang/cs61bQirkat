package qirkat;

import java.util.ArrayList;
import java.util.Observable;
import java.util.HashMap;
import java.util.Formatter;
import java.util.Set;
import java.util.Observer;

import static qirkat.PieceColor.*;
import static qirkat.Move.*;

/** A Qirkat board.   The squares are labeled by column (a char value between
 *  'a' and 'e') and row (a char value between '1' and '5'.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (with row 0 being the bottom row)
 *  counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author andrew
 */
class Board extends Observable {
    /**Board.*/
    private HashMap<String, PieceColor> _board;
    /**Return board hashmap.*/
    public HashMap<String, PieceColor> getBoard() {
        return this._board;
    }
    /**Return Hashmap board.*/
    public HashMap<String, PieceColor> board() {
        return this._board;
    }
    /**Return the K th hash.*/
    static String linToHash(int k) {
        for (char c = 'a'; c <= 'e'; c++) {
            for (char r = '1'; r <= '5'; r++) {
                if (index(c, r) == k) {
                    return Character.toString(c) + Character.toString(r);
                }
            }
        }
        return "LINTOHASH ERROR";
    }

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new HashMap<String, PieceColor>();
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        internalCopy(b);
    }

    /** Return a constant view of me (allows any access method, but no
     *  method that modifies it). */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions. */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;

        starterBoard();

        setChanged();
        notifyObservers();
    }
    /**create starterboard.*/
    void starterBoard() {
        _board.clear();
        String key;

        PieceColor white = PIECE_VALUES[1];
        for (char c = 'a'; c <= 'e'; c++) {
            for (char r = '1'; r <= '2'; r++) {
                key = Character.toString(c) + Character.toString(r);
                _board.put(key, white);
            }
        }
        _board.put("d3", white);
        _board.put("e3", white);

        PieceColor black = PIECE_VALUES[2];
        for (char c = 'a'; c <= 'e'; c++) {
            for (char r = '4'; r <= '5'; r++) {
                key = Character.toString(c) + Character.toString(r);
                _board.put(key, black);
            }
        }
        _board.put("a3", black);
        _board.put("b3", black);

        PieceColor empty = PIECE_VALUES[0];
        _board.put("c3", empty);
    }

    /** Copy B into me. */
    void copy(Board b) {
        internalCopy(b);
    }

    /** Copy B into me. */
    private void internalCopy(Board b) {

        _board = new HashMap<String, PieceColor>(b._board);
        _whoseMove = b._whoseMove;
        if (_whoseMove == PIECE_VALUES[0]) {
            _whoseMove = PIECE_VALUES[0];
        }
        if (_whoseMove == PIECE_VALUES[1]) {
            _whoseMove = PIECE_VALUES[1];
        }
        if (_whoseMove == PIECE_VALUES[2]) {
            _whoseMove = PIECE_VALUES[2];
        }
        _gameOver = b._gameOver;
        oldMoves = b.oldMoves;
    }

    /** Set my contents as defined by STR.  STR consists of 25 characters,
     *  each of which is b, w, or -, optionally interspersed with whitespace.
     *  These give the contents of the Board in row-major order, starting
     *  with the bottom row (row 1) and left column (column a). All squares
     *  are initialized to allow horizontal movement in either direction.
     *  NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }



        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k, EMPTY);
                break;
            case 'b': case 'B':
                set(k, BLACK);
                break;
            case 'w': case 'W':
                set(k, WHITE);
                break;
            default:
                break;
            }
        }


        _whoseMove = nextMove;

        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if the current player has
     *  no moves. */
    boolean gameOver() {
        return _gameOver;
    }

    /** Return the current contents of square C R, where 'a' <= C <= 'e',
     *  and '1' <= R <= '5'.  */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);

        return _board.get(Character.toString(c) + Character.toString(r));
    }

    /** Return the current contents of the square at linearized index K. */
    PieceColor get(int k) {
        assert validSquare(k);

        String pos = linToHash(k);
        char c = pos.charAt(0);
        char r = pos.charAt(1);
        return get(c, r);

    }

    /** Set get(C, R) to V, where 'a' <= C <= 'e', and
     *  '1' <= R <= '5'. */
    private void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        String pos = Character.toString(c) + Character.toString(r);
        _board.put(pos, v);

    }

    /** Set get(K) to V, where K is the linearized index of a square. */
    private void set(int k, PieceColor v) {
        assert validSquare(k);

        String pos = linToHash(k);
        char c = pos.charAt(0);
        char r = pos.charAt(1);
        set(c, r, v);


    }



    /** Return a list of all legal moves from the current position. */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        ArrayList<Move> temp = new ArrayList<>();
        getMoves(temp);
        for (Move m : temp) {
            if (legalMove(m)) {
                result.add(m);
            }
        }
        return result;
    }

    /** Add all legal moves (move and jumps) from the current position to
     * MOVES. */
    void getMoves(ArrayList<Move> moves) {
        if (gameOver()) {

            return;
        }
        if (jumpPossible()) {

            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        } else {

            for (int k = 0; k <= MAX_INDEX; k += 1) {
                getMoves(moves, k);
            }
        }
    }


    /** Add all legal non-capturing moves from the position
     *  with linearized index K to MOVES. */
    void getMoves(ArrayList<Move> moves, int k) {
        Move.cleanGetMovesHelper(moves, k, this);

    }

    /** Add all legal captures from the position with linearized index K
     *  to MOVES. */
    void getJumps(ArrayList<Move> moves, int k) {



        ArrayList<Move> temp = new ArrayList<Move>();

        Move.finalChainJump(temp, this, k);
        for (Move m : temp) {
            if (legalMove(m)) {
                moves.add(m);
            }
        }

    }



    /** Add all legal single captures from the position with linearized index K
     *  to MOVES. */
    void getSingleJump(ArrayList<Move> moves, int k) {
        Move.cleanGetSingleJumpHelper(moves, k, this);

    }

    /** Return true iff MOV is a valid jump sequence on the current board.
     *  MOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     *  could be continued and are valid as far as they go.  */
    boolean checkJump(Move mov, boolean allowPartial) {
        if (mov == null) {
            return true;
        }
        return false;
    }

    /** Return true iff a jump is possible for a piece at position C R. */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /** Return true iff a jump is possible for a piece at position with
     *  linearized index K. */
    boolean jumpPossible(int k) {
        ArrayList<Move> allJumps = new ArrayList<Move>();
        getSingleJump(allJumps, k);
        return allJumps.size() > 0;
    }

    /** Return true iff a jump is possible from the current board. */
    boolean jumpPossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }
    /**Helper Return if move possible from K.*/
    boolean movePossible(int k) {
        ArrayList<Move> allMoves = new ArrayList<Move>();
        getMoves(allMoves, k);
        return allMoves.size() > 0;
    }
    /**Return if move possible.*/
    boolean movePossible() {
        for (int k = 0; k <= MAX_INDEX; k += 1) {
            if (movePossible(k)) {
                return true;
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Perform the move C0R0-C1R1. Assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null));
    }

    /** Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     *  Assumes the result is legal. */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next));
    }

    /** Make the Move MOV on this Board, assuming it is legal. */
    void makeMove(Move mov) {
        assert legalMove(mov);

        HashMap<String, PieceColor> temp =
                new HashMap<String, PieceColor>(_board);
        tempBoard.add(temp);

        actuallyMakeMove(mov);
        _whoseMove = _whoseMove.opposite();


        setChanged();
        notifyObservers();
    }
    /**OldMoves.*/
    private HashMap<String, String> oldMoves = new HashMap<String, String>();
    /**Actually make the MOV move.*/
    void actuallyMakeMove(Move mov) {
        char startCol = mov.col0();
        char startRow = mov.row0();
        char endCol = mov.col1();
        char endRow = mov.row1();
        String start =
                Character.toString(startCol) + Character.toString(startRow);
        String end = Character.toString(endCol) + Character.toString(endRow);

        PieceColor curr = _board.get(start);
        _board.put(start, PIECE_VALUES[0]);
        oldMoves.remove(start);
        if (!mov.isJump()) {
            _board.put(end, curr);

            oldMoves.put(end, start);
        } else {
            char middleCol = middleChar(startCol, endCol);
            char middleRow = middleChar(startRow, endRow);
            String middle = Character.toString(middleCol)
                    + Character.toString(middleRow);
            _board.put(middle, PIECE_VALUES[0]);
            _board.put(end, curr);
            if (mov.jumpTail() != null) {
                actuallyMakeMove(mov.jumpTail());


            }
        }

    }

    /**Temp board.*/
    private ArrayList<HashMap<String, PieceColor>> tempBoard =
            new ArrayList<HashMap<String, PieceColor>>();

    /** Undo the last move, if any. */
    void undo() {
        if (tempBoard.size() == 0) {
            return;
        }
        _board = tempBoard.remove(tempBoard.size() - 1);

        _whoseMove = _whoseMove.opposite();

        setChanged();
        notifyObservers();
    }
    /**Temp MOV Return.*/
    boolean legalMoved(Move mov) {
        return getMoves().contains(mov);

    }
    /** Return true iff MOV is legal on the current board. */
    boolean legalMove(Move mov) {


        if (mov == null) {
            return true;
        }
        char startCol = mov.col0();
        char startRow = mov.row0();
        String start = Character.toString(startCol)
                + Character.toString(startRow);
        if (!_board.containsKey(start)) {
            return false;
        }
        if (!get(startCol, startRow).equals(_whoseMove)) {
            return false;
        }
        char endCol = mov.col1();
        char endRow = mov.row1();
        String end = Character.toString(endCol)
                + Character.toString(endRow);
        if (!_board.containsKey(end)) {
            return false;
        }
        if (!get(endCol, endRow).equals(PIECE_VALUES[0])) {

            return false;
        }
        if (startCol == endCol && startRow == endRow) {

            return false;
        }
        if (!mov.isJump()) {
            if (jumpPossible()) {
                return false;
            }
            return notJumpHelper(mov);
        } else {
            if (mov.jumpTail() == null) {

                return singleJumpHelper(mov);
            } else {


                HashMap<String, PieceColor> temp =
                        new HashMap<String, PieceColor>(_board);
                return chainJumpHelper(mov, temp);
            }
        }
    }
    /**Return MOV TMPBOARD HELPER.*/
    boolean chainJumpHelper(Move mov, HashMap<String, PieceColor> tmpBoard) {
        if (mov.jumpTail() == null) {

            return singleJumpHelper(mov);
        }
        if (!singleJumpHelper(mov)) {
            return false;
        }
        char startCol = mov.col0();

        char startRow = mov.row0();

        char endCol = mov.col1();

        char endRow = mov.row1();


        String start = Character.toString(startCol)
                + Character.toString(startRow);
        String end = Character.toString(endCol) + Character.toString(endRow);
        PieceColor curr = tmpBoard.get(start);
        tmpBoard.put(start, PIECE_VALUES[0]);

        if (!mov.isJump()) {
            tmpBoard.put(end, curr);
            return false;
        } else {
            char middleCol = middleChar(startCol, endCol);
            char middleRow = middleChar(startRow, endRow);
            String middle = Character.toString(middleCol)
                    + Character.toString(middleRow);
            tmpBoard.put(middle, PIECE_VALUES[0]);
            tmpBoard.put(end, curr);
        }

        return chainJumpHelper(mov.jumpTail(), tmpBoard);

    }
    /**MOV RETURN HELPER SINGLE JUMP.*/
    boolean singleJumpHelper(Move mov) {
        char startCol = mov.col0();

        char startRow = mov.row0();

        char endCol = mov.col1();

        char endRow = mov.row1();


        if (!mov.isJump()) {
            return false;
        }

        if (!((Math.abs(startCol - endCol) == 2
                || Math.abs(startCol - endCol) == 0)
                && (Math.abs(startRow - endRow) == 2
                || Math.abs(startRow - endRow) == 0))) {
            return false;
        }

        Board temp = new Board(this);

        temp.actuallyMakeMove(mov);


        if (temp.jumpPossible(endCol, endRow)) {

            return false;
        }


        if (!((Math.abs(startCol - endCol) == 2)
                && (Math.abs(startRow - endRow) == 2))) {
            char middleCol = middleChar(startCol, endCol);
            char middleRow = middleChar(startRow, endRow);
            return get(middleCol, middleRow).equals(_whoseMove.opposite());



        } else {
            if ((startCol + startRow) % 2 == 1) {
                return false;
            } else {

                char middleCol = middleChar(startCol, endCol);
                char middleRow = middleChar(startRow, endRow);
                return get(middleCol, middleRow).equals(_whoseMove.opposite());


            }
        }
    }
    /**NOT JUMP HELPER RETURN MOV.*/
    boolean notJumpHelper(Move mov) {
        char startCol = mov.col0();
        char startRow = mov.row0();
        char endCol = mov.col1();
        char endRow = mov.row1();

        String start = Character.toString(startCol)
                + Character.toString(startRow);
        String end = Character.toString(endCol) + Character.toString(endRow);
        if (oldMoves.containsKey(start)) {

            if (oldMoves.get(start).equals(end)) {
                return false;
            }
        }

        if (_whoseMove == PIECE_VALUES[1]) {
            if (endRow - startRow < 0 || startRow == '5') {
                return false;
            }
        } else if (_whoseMove == PIECE_VALUES[2]) {
            if (endRow - startRow > 0 || startRow == '1') {
                return false;
            }
        } else if (mov.isLeftMove() || mov.isRightMove()) {
            return true;
        } else if ((startCol + startRow) % 2 == 1) {
            return startCol == endCol;
        }
        return true;

    }

    /**Returns the middle char assuming A and B are 0 or 2 between. */
    char middleChar(char a, char b) {
        char result;
        if (a == b) {
            result = a;
            return result;
        }
        if (a > b) {
            result = b;
            result++;
            return result;
        } else {
            result = a;
            result++;
            return result;
        }
    }



    @Override
    public int hashCode() {
        return this.toString().length();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else if (_whoseMove != ((Board) obj)._whoseMove) {
            return false;
        } else {
            HashMap<String, PieceColor> otherBoard = ((Board) obj)._board;
            if (otherBoard == _board) {
                return true;
            } else if (otherBoard.size() != _board.size()) {
                return false;
            }
            Set<String> keys = _board.keySet();

            for (String k : keys) {
                if (!otherBoard.containsKey(k)) {
                    return false;
                }
                if (_board.get(k) != otherBoard.get(k)) {
                    return false;
                }
            }

        }
        return true;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /** Return a text depiction of the board.  If LEGEND, supply row and
     *  column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        if (legend) {
            trueToString(out);
        } else {
            falseToString(out);
        }
        return out.toString();
    }
    /**No legend OUT. */
    void falseToString(Formatter out) {
        String output = "";
        String curr;
        PieceColor currPiece;
        output += " ";
        int i = 0;

        for (char r = '5'; r >= '1'; r--) {
            for (char c = 'a'; c <= 'e'; c++) {

                if (i % 5 == 0 && i != 0) {
                    output += "\n";
                    output += "  ";
                } else {
                    output += " ";
                }
                i++;

                curr = Character.toString(c) + Character.toString(r);
                currPiece = _board.get(curr);

                if (currPiece == PIECE_VALUES[0]) {
                    output += "-";
                } else if (currPiece == PIECE_VALUES[1]) {
                    output += "w";
                } else if (currPiece == PIECE_VALUES[2]) {
                    output += "b";
                }

            }
        }

        out.format(output);
    }
    /**Legend OUT.*/
    void trueToString(Formatter out) {
        String output = "";
        String curr;
        PieceColor currPiece;
        output += "5 ";
        int i = 0;

        for (char r = '5'; r >= '1'; r--) {
            for (char c = 'a'; c <= 'e'; c++) {

                if (i % 5 == 0 && i != 0) {
                    output += "\n";
                    output += Character.toString(r);
                    output += "  ";
                } else {
                    output += " ";
                }
                i++;

                curr = Character.toString(c) + Character.toString(r);
                currPiece = _board.get(curr);

                if (currPiece == PIECE_VALUES[0]) {
                    output += "-";
                } else if (currPiece == PIECE_VALUES[1]) {
                    output += "w";
                } else if (currPiece == PIECE_VALUES[2]) {
                    output += "b";
                }

            }
        }
        output += "\n   a b c d e";

        out.format(output);
    }



    /** Return true iff there is a move for the current player. */
    private boolean isMove() {
        return false;
    }


    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** Set true when game ends. */
    private boolean _gameOver;

    /** Convenience value giving values of pieces at each ordinal position. */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /** One cannot create arrays of ArrayList<Move>, so we introduce
     *  a specialized private list type for this purpose. */
    private static class MoveList extends ArrayList<Move> {
    }

    /** A read-only view of a Board. */
    private class ConstantBoard extends Board implements Observer {
        /** A constant view of this Board. */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move) {
            assert false;
        }

        /** Undo the last move. */
        @Override
        void undo() {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }
}
