package qirkat;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests of the Board class.
 *  @author
 */
public class BoardTest {

    private static final String INIT_BOARD =
        "  b b b b b\n  b b b b b\n  b b - w w\n  w w w w w\n  w w w w w";

    private static final String[] GAME1 =
    { "c2-c3", "c4-c2",
      "c1-c3", "a3-c1",
      "c3-a3", "c5-c4",
      "a3-c5-c3",
    };

    private static final String[] GAME2 = { "c2-c3", "c4-c2",
        "c1-c3", "a3-c1", "c3-a3", "c5-c4", "a3-c5-c3", "d4-b2", "a1-c3",
        "c1-a1-a3"};
    private static final String[] GAME3 = { "d3-c3", "b3-d3", "e3-c3",
        "a3-b3", "c3-a3", "b4-c3", "d2-b4", "b5-b3", "a3-c3", "e4-e3",
        "e2-e4", "e5-e3", "c3-e5", "c4-d4", "e5-c3", "d5-d4",
        "c3-e5", "c5-d5", "e5-c5", "e3-e2", "e1-e3", "a4-b4",
        "c5-a3", "a5-b5"};


    private static final String GAME1_BOARD =
        "  b b - b b\n  b - - b b\n  - - w w w\n  w - - w w\n  w w b w w";

    private static final String GAME2_BOARD =
            "  b b - b b\n  b - - - b\n  b - w w w\n  - - - w w\n  - - - w w";
    private static final String GAME3_BOARD =
            "  - b - - -\n  - - - - -\n  w - - - w\n  w w w - -\n  w w w w -";

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(Move.parseMove(s));
        }
    }

    @Test
    public void testInit1() {

        Board b0 = new Board();
        assertEquals(INIT_BOARD, b0.toString());
        assertEquals(4, b0.getMoves().size());

    }

    @Test
    public void testMoves1() {

        Board b0 = new Board();
        makeMoves(b0, GAME1);
        assertEquals(GAME1_BOARD, b0.toString());
    }

    @Test
    public void testUndo() {

        Board b0 = new Board();
        Board b1 = new Board(b0);
        makeMoves(b0, GAME1);
        Board b2 = new Board(b0);
        for (int i = 0; i < GAME1.length; i += 1) {
            b0.undo();
        }

        assertEquals("failed to return to start", b1, b0);
        makeMoves(b0, GAME1);
        assertEquals("second pass failed to reach same position", b2, b0);
    }

    @Test
    public void testGetMoves() {
        Board b0 = new Board();
        makeMoves(b0, GAME2);
        assertEquals(GAME2_BOARD, b0.toString());

        assertEquals(8, b0.getMoves().size());

        Board b1 = new Board();
        makeMoves(b1, GAME3);
        assertEquals(GAME3_BOARD, b1.toString());
        assertEquals(13, b1.getMoves().size());

    }

    @Test
    public void end() {
        Board b0 = new Board();
        b0.setPieces("----- ----- ----- ----- -----", PieceColor.WHITE);

        assertEquals(0, b0.getMoves().size());

        b0.setPieces("----- ----- --w-- ----- -----", PieceColor.WHITE);

        assertEquals(5, b0.getMoves().size());
    }


    @Test
    public void testttt() {
        Board b = new Board();
        b.setPieces("----- wb-b- ----- ----- -----", PieceColor.WHITE);
        assertEquals(false, b.legalMove(Move.parseMove("a2-c2")));



        Board b1 = new Board();
        b1.setPieces("----- wb-b- ----b ---b- -----", PieceColor.WHITE);

    }

    @Test
    public void testGetMoves1() {
        Board b0 = new Board();
        b0.setPieces("----- wb-b- ----- ----- -----", PieceColor.WHITE);

        assertEquals(1, b0.getMoves().size());

        assertEquals(2, Move.moveSize(b0.getMoves().get(0)));

    }

    @Test
    public void testGetMoves2() {
        Board b0 = new Board();
        b0.setPieces("----- wb-b- b-b-- ----- -----", PieceColor.WHITE);

        assertEquals(3, b0.getMoves().size());

        assertEquals(2, Move.moveSize(b0.getMoves().get(1)));

    }

    @Test
    public void testGetMoves3() {
        Board b0 = new Board();
        b0.setPieces("----- ----b wb-b- b-b-b -----", PieceColor.WHITE);

        assertEquals(4, b0.getMoves().size());

        assertEquals(3, Move.moveSize(b0.getMoves().get(2)));

    }

    @Test
    public void testGetMoves4() {
        Board b0 = new Board();
        b0.setPieces("ww-ww ww-ww bbwww bb-bb bbbbb", PieceColor.BLACK);

        assertEquals(1, b0.getMoves().size());

        assertEquals(1, Move.moveSize(b0.getMoves().get(0)));

    }

    @Test
    public void testInt() {
        Board b0 = new Board();
        b0.setPieces("bww-- b---- -b--w ----- b--b-", PieceColor.BLACK);
        Move temp = Move.move('d', '5', 'e', '5', null);
        assertEquals(b0.toString(), "  b - - b -\n  - - - - -\n  - b - - w\n"
                + "  b - - - -\n  b w w - -");
        assertTrue(b0.getMoves().contains(temp));
        b0.makeMove(temp);
    }

}
