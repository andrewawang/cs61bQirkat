package qirkat;

import static java.lang.Math.abs;

import java.util.Formatter;
import java.util.HashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashSet;


/** Represents a Qirkat move. There is one Move object created for
 *  each distinct Move.  A "vestigial" move represents a single board
 *  position, as opposed to a move (its starting and ending rows are
 *  equal, likewise columns).
 *  @author andrew
 */
class Move {

    /** Size of a side of the board. */
    static final int SIDE = 5;

    /** Maximum linearized index. */
    static final int MAX_INDEX = SIDE * SIDE - 1;

    /** Constants used to compute linearized indices. */
    private static final int
        STEP_C = 1,
        STEP_R = 5,
        INDEX_ORIGIN = -('a' * STEP_C + '1' * STEP_R);

    /** Pattern for valid move input. */
    private static final Pattern MOVE_PATTERN =
        Pattern.compile("(?:.*-)?([a-e])([1-5])-([a-e])([1-5])$");

    /* Moves get generated profligately during the calculations of an AI,
     * so it's a good idea to make that operation efficient.  Instead of
     * relying on a Move constructor, which does a memory allocation with
     * each use of 'new', we use a "Move factory": Move.move,
     * a static method that returns a Move, but not necessarily a new
     * one. Moves themselves are immutable, and for any possible move,
     * there is exactly one object of type Move. */

    /* To avoid creating Move objects that are not needed, we maintain a
     * a static variable, _staged containing a Move object.  The move
     * factory methods then sets the fields of this object before looking to
     * to see if there is already a Move object with the same parameters.
     * If there is, the move methods will simply return it, thus allowing
     * _staged to be reused on the next call without having to create a
     * new Move object.  Otherwise, we use the _staged object itself as the
     * new Move, and set the variable to null so that we create a new
     * Move on the next call to move. There is a drawback: since there
     * is at most one _staged object at any time, at most one call to move
     * may execute simultaneously.  Otherwise, two the methods may attempt
     * to use the same Move object for two different Moves, which clearly
     * will not work.  Therefore, we indicate in the comments that a number
     * of the operations are not "thread safe". Fortunately, you'll not run
     * into this problem as long as you don't do anything fancy (such as
     * trying to make your AI a parallel program).
     */

    /** The move constructor, made private to prevent its use except in
     *  this class. */
    private Move() {
    }

    /** A factory method that returns a Move from COL0 ROW0 to COL1 ROW1,
     *  followed by NEXTJUMP, if this move is a jump. Assumes the column
     *  and row designations are valid and that NEXTJUMP is null for a
     *  non-capturing move.  Not thread-safe. */
    static Move move(char col0, char row0, char col1, char row1,
                     Move nextJump) {
        if (_staged == null) {
            _staged = new Move();
        }
        _staged.set(col0, row0, col1, row1, nextJump);
        if (_staged.isJump() && nextJump != null && !nextJump.isJump()) {
            throw new IllegalArgumentException("bad jump");
        } else if (!_staged.isJump() && nextJump != null) {
            throw new IllegalArgumentException("bad jump");
        }
        Move result = _internedMoves.computeIfAbsent(_staged, IDENTITY);
        if (result == _staged) {
            _staged = null;
        }
        return result;
    }

    /** Return a single move or jump from (COL0, ROW0) to (COL1, ROW1).
     *  Not thread-safe. */
    static Move move(char col0, char row0, char col1, char row1) {
        return move(col0, row0, col1, row1, null);
    }

    /** Return a vestigial Move consisting only of starting square
     *  COL0 ROW0. */
    static Move move(char col0, char row0) {
        return move(col0, row0, col0, row0);
    }

    /** Return the concatenation MOVE0 followed by MOVE1.  Either may be
     *  null, in which case the result is the other.  A vestigial move
     *  is equivalent to a position and extends a move on either end by
     *  one square. */
    static Move move(Move move0, Move move1) {
        if (move0 == null) {
            return move1;
        }
        if (move1 == null) {
            return move0;
        }
        if (move0.isVestigial()) {
            return move1;
        }
        if (move0.jumpTail() == null) {
            Move temp;
            if (move1.isJump()) {
                int i;
            }
            return null;
        } else {
            Move jump = move1._nextJump;
            return null;

        }

    }

    /** Return true iff (C, R) is a valid square designation. */
    static boolean validSquare(char c, char r) {
        return 'a' <= c && c <= 'e' && '1' <= r && r <= '5';
    }

    /** Return true iff K is a valid linearized index. */
    static boolean validSquare(int k) {
        return 0 <= k && k <= MAX_INDEX;
    }

    /** Return the linearized index of square C R. */
    static int index(char c, char r) {
        int k = c * STEP_C + r * STEP_R + INDEX_ORIGIN;
        assert 0 <= k && k <= MAX_INDEX;
        return k;
    }

    /** Return the column letter of linearized index K. */
    static char col(int k) {
        return (char) (k % STEP_R + 'a');
    }

    /** Return the row digit of linearized index K. */
    static char row(int k) {
        return (char) (k / STEP_R + '1');
    }

    /** Return true iff this is a capturing move (a jump). */
    boolean isJump() {
        return _isJump;
    }

    /** Return true iff this is a vestigial Move consisting only of a single
     *  position. */
    boolean isVestigial() {
        return _col0 == _col1 && _row0 == _row1 && _nextJump == null;
    }

    /** Return true iff this is a horizontal, non-capturing move to
     *  the left. */
    boolean isLeftMove() {
        if (isJump()) {
            return false;
        } else if (row0() != row1()) {
            return false;
        } else if (col0() - col1() == 1) {
            return true;
        }
        return false;
    }

    /** Return true iff this is a horizontal, non-capturing move
     *  to the right. */
    boolean isRightMove() {
        if (isJump()) {
            return false;
        } else if (row0() != row1()) {
            return false;
        } else if (col1() - col0() == 1) {
            return true;
        }
        return false;
    }

    /** Returns the source column. */
    char col0() {
        return _col0;
    }

    /** Returns the source row. */
    char row0() {
        return _row0;
    }

    /** Returns the destination column. */
    char col1() {
        return _col1;
    }

    /** Returns the destination row. */
    char row1() {
        return _row1;
    }

    /** For a jump, returns the row of the jumped-over square for the
     *  first leg of the jump.  For a non-capturing move, same as row1(). */
    char jumpedRow() {
        char result;
        if (isJump()) {
            if (row0() == row1()) {
                result = row1();
            } else {
                if (row0() > row1()) {
                    result = row1();
                    result++;
                } else {
                    result = row0();
                    result++;
                }
            }
        } else {
            result = row1();
        }
        return result;

    }

    /** For a jump, returns the column of the jumped-over square for the
     *  first leg of the jump.  For a non-capturing move, same as col1(). */
    char jumpedCol() {
        char result;
        if (isJump()) {
            if (col0() == col1()) {
                result = col1();
            } else {
                if (col0() > col1()) {
                    result = col1();
                    result++;
                } else {
                    result = col0();
                    result++;
                }
            }
        } else {
            result = col1();
        }
        return result;

    }

    /** Return the linearized index of my source square. */
    int fromIndex() {
        return _fromIndex;
    }

    /** Return The linearized index of my destination square. */
    int toIndex() {
        return _toIndex;
    }

    /** Return the linearized index of (jumpedCol(), jumpedRow()). */
    int jumpedIndex() {
        return index(jumpedCol(), jumpedRow());
    }

    /** Return the second and subsequent jumps comprising this jump, or null
     *  for a single jump. */
    Move jumpTail() {
        return _nextJump;
    }

    @Override
    public int hashCode() {
        return (_fromIndex << 5) | _toIndex;
    }

    @Override
    public boolean equals(Object obj) {
        /* NOTE: Depends on there being no more than one Move object for
         * each distinct move, so that pointer equality of _nextJump
         * is valid. */
        Move m = (Move) obj;
        return _fromIndex == m._fromIndex && _nextJump == m._nextJump
            && _toIndex == m._toIndex;
    }

    /** Return the non-vestigial Move denoted by STR. */
    static Move parseMove(String str) {
        Matcher mat = MOVE_PATTERN.matcher(str);
        int end;

        Move result;
        result = null;
        end = str.length();

        while (end > 2) {
            mat.region(0, end);
            if (!mat.matches()) {
                throw new IllegalArgumentException("bad move denotation");
            }

            result = move(mat.group(1).charAt(0), mat.group(2).charAt(0),
                          mat.group(3).charAt(0), mat.group(4).charAt(0),
                          result);
            end = mat.end(2);
        }
        if (result == null) {
            throw new IllegalArgumentException("bad move denotation");
        }
        return result;
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        toString(out);
        return out.toString();
    }

    /** Write my string representation into OUT. */
    private void toString(Formatter out) {

        String output = "";
        output += col0();
        output += row0();
        if (isVestigial()) {
            out.format(output);
            return;
        }
        output += '-';
        output += col1();
        output += row1();
        if (jumpTail() == null) {
            out.format(output);
            return;
        } else {

            String helper = toStringHelper(jumpTail());
            output += helper;
        }

        out.format(output);

    }

    /**Return String of MOVE.*/
    String toStringHelper(Move move) {

        String output = Character.toString('-');
        output += move.col1();
        output += move.row1();
        if (move.jumpTail() != null) {

            output += toStringHelper(move.jumpTail());
        }


        return output;
    }
    /**Add move MOVES STARTCOL STARTROW ENDCOL ENDROW NEXT.*/
    static void addMove(ArrayList<Move> moves,
                        char startCol,
                        char startRow,
                        char endCol,
                        char endRow,
                        Move next) {
        Move temp = new Move();
        temp.set(startCol, startRow, endCol, endRow, next);
        moves.add(temp);
    }
    /**Helper, left is in code MOVES K B STARTCOL STARTROW ENDCOL ENDROW.*/
    static void rightHelper(ArrayList<Move> moves, int k, Board b,
                            char startCol, char startRow,
                            char endCol, char endRow) {
        if (endCol <= 'e') {
            String right = Character.toString(endCol)
                    + Character.toString(startRow);
            if (b.board().get(right).equals(PieceColor.EMPTY)) {
                addMove(moves, startCol, startRow, endCol, endRow, null);
            }
        }
    }
    /**Clean MOVES K B.*/
    static void cleanGetMovesHelper(ArrayList<Move> moves, int k, Board b) {
        String curr = Board.linToHash(k);
        if (!b.board().get(curr).equals(b.whoseMove())) {
            return;
        }
        char startCol = curr.charAt(0);
        char startRow = curr.charAt(1);
        char endCol = startCol;
        char endRow = startRow;
        endCol--;
        if (endCol >= 'a') {
            String left = Character.toString(endCol)
                    + Character.toString(startRow);
            if (b.board().get(left).equals(PieceColor.EMPTY)) {
                addMove(moves, startCol, startRow, endCol, endRow, null);
            }
        }
        endCol++;
        endCol++;
        rightHelper(moves, k, b, startCol, startRow, endCol, endRow);
        endCol--;
        if (b.whoseMove().equals(PieceColor.WHITE)) {
            endRow++;
        } else if (b.whoseMove().equals(PieceColor.BLACK)) {
            endRow--;
        }
        if (endRow >= '1' && endRow <= '5') {
            String updown = Character.toString(startCol)
                    + Character.toString(endRow);
            if (b.board().get(updown).equals(PieceColor.EMPTY)) {
                addMove(moves, startCol, startRow, endCol, endRow, null);
            }
        }
        if ((startCol + startRow) % 2 == 0) {
            endCol--;
            String topleft = Character.toString(endCol)
                    + Character.toString(endRow);
            if (endRow >= '1' && endRow <= '5') {
                if (endCol >= 'a') {
                    if (b.board().get(topleft).equals(PieceColor.EMPTY)) {
                        addMove(moves, startCol,
                                startRow, endCol, endRow, null);
                    }
                }
                endCol++;
                endCol++;
                if (endCol <= 'e') {
                    String topright = Character.toString(endCol)
                            + Character.toString(endRow);
                    if (b.board().get(topright).equals(PieceColor.EMPTY)) {
                        addMove(moves, startCol,
                                startRow, endCol, endRow, null);
                    }
                }
            }
        }
    }
    /**Single jump MOVES B STARTCOL STARTROW ENDCOL ENDROW.*/
    static void addSingleJumpIfLegal(ArrayList<Move> moves, Board b,
                                     char startCol, char startRow,
                                     char endCol, char endRow) {

        if (validSquare(endCol, endRow)) {

            char midCol = b.middleChar(startCol, endCol);
            char midRow = b.middleChar(startRow, endRow);
            String middle = Character.toString(midCol)
                    + Character.toString(midRow);
            if (!b.board().get(middle).equals(b.whoseMove().opposite())) {

                return;
            }
            String end = Character.toString(endCol)
                    + Character.toString(endRow);
            if (!b.board().get(end)
                    .equals(PieceColor.EMPTY)) {

                return;
            }
            addMove(moves, startCol, startRow, endCol, endRow, null);
        }

    }
    /**CLEAN JUMP MOVES K B.*/
    static void cleanGetSingleJumpHelper(
            ArrayList<Move> moves, int k, Board b) {
        String curr = Board.linToHash(k);
        if (!b.board().get(curr).equals(b.whoseMove())) {

            return;
        }
        char startCol = curr.charAt(0);
        char startRow = curr.charAt(1);
        char colPlus = startCol;
        colPlus++;
        colPlus++;
        char rowPlus = startRow;
        rowPlus++;
        rowPlus++;
        char colMinus = startCol;
        colMinus--;
        colMinus--;
        char rowMinus = startRow;
        rowMinus--;
        rowMinus--;

        addSingleJumpIfLegal(moves, b, startCol, startRow, startCol, rowPlus);
        addSingleJumpIfLegal(moves, b, startCol, startRow, startCol, rowMinus);
        addSingleJumpIfLegal(moves, b, startCol, startRow, colPlus, startRow);
        addSingleJumpIfLegal(moves, b, startCol, startRow, colMinus, startRow);

        if ((startCol + startRow) % 2 == 0) {
            addSingleJumpIfLegal(
                    moves, b, startCol, startRow, colPlus, rowPlus);
            addSingleJumpIfLegal(
                    moves, b, startCol, startRow, colPlus, rowMinus);
            addSingleJumpIfLegal(
                    moves, b, startCol, startRow, colMinus, rowPlus);
            addSingleJumpIfLegal(
                    moves, b, startCol, startRow, colMinus, rowMinus);

        }
    }
    /**Return size of MOV.*/
    static int moveSize(Move mov) {
        if (mov == null) {
            return 0;
        }
        return 1 + moveSize(mov._nextJump);
    }

    /**Not used RESULT BOARD K.*/
    static void cchainJump(ArrayList<Move> result, Board board, int k) {

        ArrayList<Move> temp = new ArrayList<Move>();
        ArrayList<Move> singleJump = new ArrayList<Move>();
        Board tempBoard = new Board(board);

        cleanGetSingleJumpHelper(singleJump, k, tempBoard);

        for (Move m : singleJump) {
            if (board.legalMove(m)) {
                tempBoard = new Board(board);
                tempBoard.actuallyMakeMove(m);
                if (!tempBoard.jumpPossible()) {
                    result.add(m);
                } else {
                    temp.add(m);
                }
            }

        }

    }
    /**Returns list of possible next jumps from B K.*/
    static ArrayList<Move> nextJump(Board b, int k) {
        ArrayList<Move> result = new ArrayList<Move>();

        ArrayList<Move> singleJump = new ArrayList<Move>();
        cleanGetSingleJumpHelper(singleJump, k, b);
        for (Move m : singleJump) {
            result.add(m);
        }

        return result;
    }

    /**Final chain jump RESULT BOARD K.*/
    static void finalChainJump(ArrayList<Move> result, Board board, int k) {
        ArrayList<Move> jumps = finalChainJumpHelper(board, k);
        for (Move m : jumps) {
            result.add(m);
        }
    }

    /**Unused helper RESULT K B.*/
    static void finalSingleJumpHelper(ArrayList<Move> result, int k, Board b) {
        if (!b.jumpPossible()) {
            return;
        }
        char col = col(k);
        char row = row(k);


    }

    /**Final chain jump helper Return arraylist based off B K.*/
    static ArrayList<Move> finalChainJumpHelper(Board b, int k) {
        ArrayList<Move> result = new ArrayList<Move>();

        if (!b.jumpPossible()) {
            result.add(null);
            return result;
        } else {
            ArrayList<Move> jump = new ArrayList<Move>();

            cleanGetSingleJumpHelper(jump, k, b);

            for (Move m : jump) {
                Board temp = new Board(b);
                temp.actuallyMakeMove(m);
                int nextIndex = index(m.col1(), m.row1());
                ArrayList<Move> next = finalChainJumpHelper(temp, nextIndex);
                if (next.size() == 0) {

                    next.add(null);
                }
                for (Move nextMove : next) {
                    result.add(move(m.col0(), m.row0(), m.col1(),
                            m.row1(), nextMove));
                }
            }

            return result;
        }
    }

    /**chain jump RESULT BOARD K.*/
    static void cChainJump(ArrayList<Move> result, Board board, int k) {
        ArrayList<Move> nextJump = nextJump(board, k);
        ArrayList<Move> temp = new ArrayList<Move>(nextJump);
        for (Move m : temp) {
            if (!board.legalMove(m)) {
                nextJump.remove(m);
            }
        }

        for (Move m : nextJump) {
            Board tempBoard = new Board(board);
            tempBoard.makeMove(m);
            if (!tempBoard.jumpPossible()) {
                result.add(m);
            } else {
                char col = m.col1();
                char row = m.row1();
                int index = index(col, row);
                ArrayList<Move> nextNextJump = nextJump(tempBoard, index);
                for (Move m1 : nextNextJump) {
                    Board tempBoard1 = new Board(tempBoard);
                    tempBoard.makeMove(m1);
                    if (!tempBoard1.jumpPossible()) {
                        Move toAdd = move(m.col0(), m.row0(),
                                m.col1(), m.row1(), m1);
                        System.out.println(toAdd);
                        result.add(toAdd);
                    } else {
                        int i;
                    }
                }
            }
        }
    }

    /**Chain jump RESULT BOARD I.*/
    static void chainJump(ArrayList<Move> result, Board board, int i) {
        if (!board.jumpPossible()) {
            return;
        }
        PieceColor currPlayer = board.whoseMove();
        ArrayList<Move> singleJumps = new ArrayList<Move>();
        String pos = board.linToHash(i);
        if (board.board().get(pos).equals(currPlayer)) {
            char col = pos.charAt(0);
            char row = pos.charAt(1);
            int k = index(col, row);
            cleanGetSingleJumpHelper(singleJumps, k, board);
        }

        if (singleJumps.size() == 0) {
            return;
        }

        result.addAll(singleJumps);

        for (Move mov : singleJumps) {
            Board temp = new Board(board);
            temp.actuallyMakeMove(mov);
            char endCol = mov.col1();
            char endRow = mov.row1();
            int k = index(endCol, endRow);
            ArrayList<Move> nextSingleJump = new ArrayList<Move>();
            cleanGetSingleJumpHelper(nextSingleJump, k, temp);

            if (nextSingleJump.size() > 0) {
                for (Move nextMove : nextSingleJump) {
                    result.add(move(mov.col0(), mov.row0(),
                            mov.col1(), mov.row1(), nextMove));
                    chainJumpHelper(result, temp);
                }
            }
        }

    }
    /**Helper for chainjumps RESULT B.*/
    static void chainJumpHelper(ArrayList<Move> result, Board b) {
        ArrayList<Move> resultTemp = new ArrayList<Move>(result);
        for (Move m : resultTemp) {
            Move temp = getLastJump(m);

            Board tempB = new Board(b);
            tempB.actuallyMakeMove(temp);
            char endCol = temp.col1();
            char endRow = temp.row1();
            int k = index(endCol, endRow);
            ArrayList<Move> nextSingleJump = new ArrayList<Move>();
            cleanGetSingleJumpHelper(nextSingleJump, k, tempB);

            if (nextSingleJump.size() > 0) {
                for (Move nextMove : nextSingleJump) {
                    result.add(move(temp.col0(), temp.row0(),
                            temp.col1(), temp.row1(), nextMove));
                    chainJumpHelper(result, tempB);
                }
            }

        }
    }

    /**Return true last jump of MOV.*/
    static Move getLastJump(Move mov) {
        Move temp = mov;
        while (temp != null && temp._nextJump != null) {
            temp = temp._nextJump;
        }
        return temp;
    }

    /** Checks if this is base case, otherwise chains more jumps.
     * MOVES K B STARTCOL STARTROW ENDCOL ENDROW*/
    static void chainJumpHelper(ArrayList<Move> moves, int k, Board b,
                          char startCol, char startRow,
                          char endCol, char endRow) {


        Board copy = new Board(b);
        ArrayList<Move> nextMoves = new ArrayList<Move>();
        int next = index(endCol, endRow);
        cleanGetSingleJumpHelper(nextMoves, next, copy);
        if (nextMoves.size() == 0) {
            cleanGetSingleJumpHelper(moves, k, b);
        } else {
            int i;
        }
    }
    /**Chain jump STARTCOL STARTROW ENDCOL ENDROW RESULT B.*/
    static void resultChainJumpHelper(char startCol, char startRow,
                                      char endCol, char endRow,
                                      ArrayList<Move> result, Board b) {
        if (endCol >= 'a' && endCol <= 'e' && endRow >= '1' && endRow <= '5') {
            Board copyBoard = new Board(b);

            char middleCol = copyBoard.middleChar(startCol, endCol);
            char middleRow = copyBoard.middleChar(startRow, endRow);
            String middle = Character.toString(middleCol)
                    + Character.toString(middleRow);
            if (!copyBoard.board().get(middle)
                    .equals(copyBoard.whoseMove().opposite())) {
                return;
            }
            String end = Character.toString(endCol)
                    + Character.toString(endRow);
            if (!copyBoard.board().get(end).equals(PieceColor.EMPTY)) {
                return;
            }
            copyBoard.board().put(middle, PieceColor.EMPTY);
            copyBoard.board().put(end, b.whoseMove());
            ArrayList<Move> future =
                    generateChainJumps(endCol, endRow, copyBoard);
            if (future.size() == 0) {
                future.add(null);
            }
            for (Move m : future) {
                Move curr = new Move();
                curr.set(startCol, startRow, endCol, endRow, m);
                result.add(curr);
            }
        }
    }
    /**Get jumps MOVES K B.*/
    static void testGetJumps(ArrayList<Move> moves, int k, Board b) {
        testGetJumpsHelper(moves, k, b, 12);


    }
    /**Get jumps MOVES K B I.*/
    static void testGetJumpsHelper(
            ArrayList<Move> moves, int k, Board b, int i) {
        if (i == 0) {
            moves.add(null);
            return;
        }
        String curr = Board.linToHash(k);
        char startCol = curr.charAt(0);
        char startRow = curr.charAt(1);
        char colPlus = startCol;
        colPlus++;
        colPlus++;
        char rowPlus = startRow;
        rowPlus++;
        rowPlus++;
        char colMinus = startCol;
        colMinus--;
        colMinus--;
        char rowMinus = startRow;
        rowMinus--;
        rowMinus--;
        if (!b.board().get(curr).equals(b.whoseMove())) {
            return;
        }

        char[] columns = {colMinus, startCol, colPlus};
        char[] rows = {rowMinus, startRow, rowPlus};

        for (char c: columns) {
            for (char r: rows) {

                if (c >= 'a' && c <= 'e' && r >= '1' && r <= '5') {
                    ArrayList<Move> temp = new ArrayList<Move>();

                    System.out.println(b.toString());
                    testGetJumpsHelper(temp, index(c, r), b, i - 1);
                    for (Move m : temp) {
                        if (b.legalMove(m)) {
                            addMove(moves, startCol, startRow, c, r, m);
                        }
                    }
                }
            }
        }
    }
    /**Chain jumps STARTCOL STARTROW B RETURN ArrayList.*/
    static ArrayList<Move> generateChainJumps(
            char startCol, char startRow, Board b) {
        ArrayList<Move> result = new ArrayList<Move>();

        char colPlus = startCol;
        colPlus++;
        colPlus++;
        char rowPlus = startRow;
        rowPlus++;
        rowPlus++;
        char colMinus = startCol;
        colMinus--;
        colMinus--;
        char rowMinus = startRow;
        rowMinus--;
        rowMinus--;

        resultChainJumpHelper(
                startCol, startRow, startCol, rowPlus, result, b);
        resultChainJumpHelper(
                startCol, startRow, startCol, rowMinus, result, b);
        resultChainJumpHelper(
                startCol, startRow, colPlus, startRow, result, b);
        resultChainJumpHelper(
                startCol, startRow, colMinus, startRow, result, b);



        if ((startCol + startRow) % 2 == 0) {
            resultChainJumpHelper(
                    startCol, startRow, colPlus, rowPlus, result, b);
            resultChainJumpHelper(
                    startCol, startRow, colPlus, rowMinus, result, b);
            resultChainJumpHelper(
                    startCol, startRow, colMinus, rowPlus, result, b);
            resultChainJumpHelper(
                    startCol, startRow, colMinus, rowMinus, result, b);
        }
        return result;
    }
    /**Clean chain jump MOVES K B.*/
    static void cleanChainJumpHelper(ArrayList<Move> moves, int k, Board b) {
        String curr = Board.linToHash(k);
        if (!b.board().get(curr).equals(b.whoseMove())) {

            return;
        }

        char startCol = curr.charAt(0);
        char startRow = curr.charAt(1);
        char colPlus = startCol;
        colPlus++;
        colPlus++;
        char rowPlus = startRow;
        rowPlus++;
        rowPlus++;
        char colMinus = startCol;
        colMinus--;
        colMinus--;
        char rowMinus = startRow;
        rowMinus--;
        rowMinus--;
    }

    /**All single moves RETURN ArrayList.*/
    static ArrayList<Move> generateAllSingleJumpMoves() {

        ArrayList<Move> result = new ArrayList<Move>();
        for (char r = '1'; r <= '5'; r++) {
            for (char c = 'a'; c <= 'e'; c++) {
                char rMinus = r;
                rMinus--;
                rMinus--;
                char cMinus = c;
                cMinus--;
                cMinus--;
                char rPlus = r;
                rPlus++;
                rPlus++;
                char cPlus = c;
                cPlus++;
                cPlus++;

                gASJMHelper(result, c, r, rMinus, rPlus, cMinus, cPlus);

            }
        }
        return result;
    }

    /**Helper for above.
     * RESULT C R RMINUS RPLUS CMINUS CPLUS*/
    static void gASJMHelper(ArrayList<Move> result, char c, char r,
                       char rMinus, char rPlus,
                       char cMinus, char cPlus) {
        Move temp = new Move();
        try {
            temp.set(c, r, c, rMinus, null);
        } catch (AssertionError e) {
            int i;
        }
        result.add(temp);
        temp = new Move();
        try {
            temp.set(c, r, cMinus, r, null);
        } catch (AssertionError e) {
            int i;
        }
        result.add(temp);
        temp = new Move();
        try {
            temp.set(c, r, c, rPlus, null);
        } catch (AssertionError e) {
            int i;
        }
        result.add(temp);
        temp = new Move();
        try {
            temp.set(c, r, cPlus, r, null);
        } catch (AssertionError e) {
            int i;
        }
        result.add(temp);
        if ((c + r) % 2 == 0) {
            temp = new Move();
            try {
                temp.set(c, r, cMinus, rMinus, null);
            } catch (AssertionError e) {
                int i;
            }
            result.add(temp);
            temp = new Move();
            try {
                temp.set(c, r, cMinus, rPlus, null);
            } catch (AssertionError e) {
                int i;
            }
            result.add(temp);
            temp = new Move();
            try {
                temp.set(c, r, cPlus, rMinus, null);
            } catch (AssertionError e) {
                int i;
            }
            result.add(temp);
            temp = new Move();
            try {
                temp.set(c, r, cPlus, rPlus, null);
            } catch (AssertionError e) {
                int i;
            }
            result.add(temp);
        }
    }


    /**Generate all Multi Jumps from POS RETURN. */
    static ArrayList<Move> generateAllMultJumps2(int pos) {
        HashSet<Move> result = new HashSet<Move>();
        return new ArrayList<>(generateAllMultJumps2(3, result, pos));
    }

    /**Return hashset of all multijumps from I and RESULT at POS.*/
    static HashSet<Move> generateAllMultJumps2(int i,
                                               HashSet<Move> result, int pos) {
        if (i == 0) {
            return new HashSet<>(generateAllSingleJumpMoves());
        }
        char c = col(pos);
        char r = row(pos);

        char rMinus = r;
        rMinus--;
        rMinus--;
        char cMinus = c;
        cMinus--;
        cMinus--;
        char rPlus = r;
        rPlus++;
        rPlus++;
        char cPlus = c;
        cPlus++;
        cPlus++;

        gAMJHelper(i, result, r, c, rMinus, rPlus, cMinus, cPlus);


        return result;
    }


    /**Generate all Multi Jumps RETURN. */
    static ArrayList<Move> generateAllMultJumps() {
        HashSet<Move> result = new HashSet<Move>();
        return new ArrayList<>(generateAllMultJumps(2, result));
    }

    /**Return hashset of all multijumps from I and RESULT.*/
    static HashSet<Move> generateAllMultJumps(int i, HashSet<Move> result) {
        if (i == 0) {
            return new HashSet<>(generateAllSingleJumpMoves());
        }
        for (char r = '1'; r <= '5'; r++) {
            for (char c = 'a'; c <= 'e'; c++) {
                char rMinus = r;
                rMinus--;
                rMinus--;
                char cMinus = c;
                cMinus--;
                cMinus--;
                char rPlus = r;
                rPlus++;
                rPlus++;
                char cPlus = c;
                cPlus++;
                cPlus++;

                gAMJHelper(i, result, r, c, rMinus, rPlus, cMinus, cPlus);

            }
        }
        return result;
    }
    /** Helper for the function right above.
     * I RESULT R C RMINUS RPLUS CMINUS CPLUS.*/
    static void gAMJHelper(int i, HashSet<Move> result,
                           char r, char c,
                           char rMinus, char rPlus,
                           char cMinus, char cPlus) {
        ArrayList<Move> old =
                new ArrayList<>(generateAllMultJumps(i - 1, result));
        for (Move m : old) {
            Move temp = new Move();
            if (m.col0() == c && m.row0() == rMinus) {
                temp.set(c, r, c, rMinus, m);
                result.add(temp);
            }
            temp = new Move();
            if (m.col0() == cMinus && m.row0() == r) {
                temp.set(c, r, cMinus, r, null);
                result.add(temp);
            }
            temp = new Move();
            if (m.col0() == c && m.row0() == rPlus) {
                temp.set(c, r, c, rPlus, null);
                result.add(temp);
            }
            temp = new Move();
            if (m.col0() == cPlus && m.row0() == r) {
                temp.set(c, r, cPlus, r, null);
                result.add(temp);
            }
            if ((c + r) % 2 == 0) {
                temp = new Move();
                if (m.col0() == cMinus && m.row0() == rMinus) {
                    temp.set(c, r, cMinus, rMinus, null);
                    result.add(temp);
                }
                temp = new Move();
                if (m.col0() == cMinus && m.row0() == rPlus) {
                    temp.set(c, r, cMinus, rPlus, null);
                    result.add(temp);
                }
                temp = new Move();
                if (m.col0() == cPlus && m.row0() == rMinus) {
                    temp.set(c, r, cPlus, rMinus, null);
                    result.add(temp);
                }
                temp = new Move();
                if (m.col0() == cPlus && m.row0() == rPlus) {
                    temp.set(c, r, cPlus, rPlus, null);
                    result.add(temp);
                }
            }
        }
    }

    /** Set me to COL0 ROW0 - COL1 ROW1 - NEXTJUMP. */
    private void set(char col0, char row0, char col1, char row1,
                     Move nextJump) {
        assert col0 >= 'a' && row0 >= '1' && col1 >= 'a' && row1 >= '1'
            && col0 <= 'e' && row0 <= '5' && col1 <= 'e' &&  row1 <= '5';
        _col0 = col0;
        _row0 = row0;
        _col1 = col1;
        _row1 = row1;
        _fromIndex = (byte) index(col0, row0);
        _toIndex = (byte) index(col1, row1);
        _isJump = abs(col0 - col1) > 1 || abs(row0 - row1) > 1;
        _nextJump = nextJump;
        assert (_isJump
                && (nextJump == null
                    || (nextJump.isJump()
                        && col1 == nextJump.col0()
                        && row1 == nextJump.row0())))
            || (!_isJump && nextJump == null);
    }

    /** Linearized indices. */
    private byte _fromIndex, _toIndex;

    /** True iff move is a jump. */
    private boolean _isJump;

    /** From and to squares. */
    private char _col0, _row0, _col1, _row1;

    /** For a jump, the Move representing the jumps following the
     *  initial jump. */
    private Move _nextJump;

    /* Used for the Move factory. */

    /** Holds the next Move object to be added to _internedMoves.
     *  The factory method move tentatively fills it in, and then returns
     *  it if it is unique (resetting _staged to null). */
    private static Move _staged;

    /** The set of all distinct moves generated so far. */
    private static HashMap<Move, Move> _internedMoves = new HashMap<>();

    /** The identity function on Moves. */
    static final Function<Move, Move> IDENTITY = k -> k;

}
