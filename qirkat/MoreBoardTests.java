package qirkat;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoreBoardTests {
    private final char[][] boardRepr = new char[][]{
        {'b', 'b', 'b', 'b', 'b'},
        {'b', 'b', 'b', 'b', 'b'},
        {'b', 'b', '-', 'w', 'w'},
        {'w', 'w', 'w', 'w', 'w'},
        {'w', 'w', 'w', 'w', 'w'}
    };

    private final PieceColor currMove = PieceColor.WHITE;

    /**
    Return string.
     */
    private String getInitialRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int i = boardRepr.length - 1; i >= 0; i--) {
            for (int j = 0; j < boardRepr[0].length; j++) {
                sb.append(boardRepr[i][j] + " ");
            }
            sb.deleteCharAt(sb.length() - 1);
            if (i != 0) {
                sb.append("\n  ");
            }
        }
        return sb.toString();
    }

    private Board getBoard() {
        Board b = new Board();
        b.setPieces(getInitialRepresentation(), currMove);
        return b;
    }

    private void resetToInitialState(Board b) {
        b.setPieces(getInitialRepresentation(), currMove);
    }


    private static final String INIT_BOARD =
            "  b b b b b\n  b b b b b\n  b b - w w\n  w w w w w\n  w w w w w";

    private static final String[] GAME2 = { "c2-c3", "c4-c2", "c1-c3", "a3-c1",
        "c3-a3", "c5-c4", "a3-c5-c3", "d4-b2", "a1-c3", "c1-a1"};
    private static final String[] GAME3 = { "d3-c3", "b3-d3", "e3-c3",
        "a3-b3", "c3-a3", "b4-c3", "d2-b4", "b5-b3", "a3-c3",
        "e4-e3", "e2-e4", "e5-e3", "c3-e5", "c4-d4", "e5-c3",
        "d5-d4", "c3-e5", "c5-d5", "e5-c5", "e3-e2", "e1-e3",
        "a4-b4", "c5-a3", "a5-b5"};
    private static final String GAME1_BOARD =
            "  b b - b b\n  b - - b b\n  - - w w w\n  w - - w w\n  w w b w w";

    private static final String GAME2_BOARD =
            "  b b - b b\n  b - - - b\n  - - w w w\n  w - - w w\n  b - - w w";
    private static final String GAME3_BOARD =
            "  - b - - -\n  - - - - -\n  w - - - w\n  w w w - -\n  w w w w -";

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(Move.parseMove(s));
        }
    }

    @Test
    public void testSomething() {
        Board b = getBoard();

        assertEquals(INIT_BOARD, b.toString());

        b.setPieces("wb-b- ----b -b-b- b---- -b-b-", currMove);
        String[] move = {"a1-c1-e1-e3-c3-a3-a5-c5-e5"};
        makeMoves(b, move);
        assertEquals("  - - - - w"
                       + "\n  - - - - -\n  - - - - -\n  - - - - -\n  - - - - -",
                b.toString());


    }
    @Test
    public void testGetMoves() {
        Board b0 = new Board();
        makeMoves(b0, GAME2);
        assertEquals(GAME2_BOARD, b0.toString());

        Board b1 = new Board();
        makeMoves(b1, GAME3);
        assertEquals(GAME3_BOARD, b1.toString());

    }
}
