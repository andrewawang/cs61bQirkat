package qirkat;

import ucb.gui2.Pad;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.util.Observer;
import java.util.Observable;

import java.awt.event.MouseEvent;

import static qirkat.PieceColor.*;

/** Widget for displaying a Qirkat board.
 *  @author skeleton
 */
class BoardWidget extends Pad implements Observer {

    /** Length of side of one square, in pixels. */
    static final int SQDIM = 150;
    /** Number of squares on a side. */
    static final int SIDE = Move.SIDE;
    /** Radius of circle representing a piece. */
    static final int PIECE_RADIUS = 15;

    /** Color of white pieces. */
    private static final Color WHITE_COLOR = Color.WHITE;
    /** Color of "phantom" white pieces. */
    /** Color of black pieces. */
    private static final Color BLACK_COLOR = Color.BLACK;
    /** Color of painted lines. */
    private static final Color LINE_COLOR = Color.BLACK;
    /** Color of blank squares. */
    private static final Color BLANK_COLOR = new Color(100, 100, 100);

    /** Stroke for lines.. */
    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);

    /** Stroke for outlining pieces. */
    private static final BasicStroke OUTLINE_STROKE = LINE_STROKE;

    /** Model being displayed. */
    private static Board _model;

    /** A new widget displaying MODEL. */
    BoardWidget(Board model) {
        _model = model;
        setMouseHandler("click", this::readMove);
        _model.addObserver(this);
        _dim = SQDIM * SIDE;
        setPreferredSize(_dim + 2 * SQDIM, _dim + 2 * SQDIM);
    }

    /** Indicate that the squares indicated by MOV are the currently selected
     *  squares for a pending move. */
    void indicateMove(Move mov) {
        _selectedMove = mov;
        repaint();
    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, _dim + 2 * SQDIM, _dim + 2 * SQDIM);


        g.setColor(Color.BLUE);

        g.drawLine(SQDIM, SQDIM, _dim + SQDIM, SQDIM);
        g.drawLine(_dim + SQDIM, SQDIM, _dim + SQDIM, _dim + SQDIM);
        g.drawLine(SQDIM, SQDIM, SQDIM, _dim + SQDIM);
        g.drawLine(SQDIM, _dim + SQDIM, _dim + SQDIM, _dim + SQDIM);


        g.drawLine(0 + SQDIM, 0 + SQDIM, _dim + SQDIM, _dim + SQDIM);
        g.drawLine(0 + SQDIM, _dim + SQDIM, _dim + SQDIM, 0 + SQDIM);
        g.drawLine(0 + SQDIM, _dim / 2 + SQDIM, _dim / 2 + SQDIM, 0 + SQDIM);
        g.drawLine(_dim + SQDIM, _dim / 2 + SQDIM,
                _dim / 2 + SQDIM, _dim + SQDIM);

        g.drawLine(_dim / 2 + SQDIM, 0 + SQDIM, _dim + SQDIM, _dim / 2 + SQDIM);
        g.drawLine(0 + SQDIM, _dim / 2 + SQDIM, _dim / 2 + SQDIM, _dim + SQDIM);


        g.drawLine(0 + SQDIM, _dim / 4 + SQDIM, _dim + SQDIM, _dim / 4 + SQDIM);
        g.drawLine(0 + SQDIM, _dim / 2 + SQDIM, _dim + SQDIM, _dim / 2 + SQDIM);
        g.drawLine(0 + SQDIM, _dim * 3 / 4 + SQDIM,
                _dim + SQDIM, _dim * 3 / 4 + SQDIM);

        g.drawLine(_dim / 4 + SQDIM, 0 + SQDIM, _dim / 4 + SQDIM, _dim + SQDIM);
        g.drawLine(_dim / 2 + SQDIM, 0 + SQDIM, _dim / 2 + SQDIM, _dim + SQDIM);
        g.drawLine(_dim * 3 / 4 + SQDIM, 0 + SQDIM,
                _dim * 3 / 4 + SQDIM, _dim + SQDIM);

        updatePieces(g);

    }
    /**Delta.*/
    private int delta = SQDIM / 2 + 1;


    /**G.*/
    void updatePieces(Graphics2D g) {
        for (String s : _model.getBoard().keySet()) {
            char col = s.charAt(0);
            char row = s.charAt(1);
            int colPixel = charToPixel(col);
            int rowPixel = charToPixel(row);



            if (_model.getBoard().get(s).equals(PieceColor.BLACK)) {
                g.setColor(Color.BLACK);
                g.drawOval(colPixel, rowPixel, delta, delta);

                g.fillOval(colPixel, rowPixel, delta, delta);
            }
            if (_model.getBoard().get(s).equals(PieceColor.WHITE)) {
                g.setColor(Color.WHITE);
                g.drawOval(colPixel, rowPixel, delta, delta);

                g.fillOval(colPixel, rowPixel, delta, delta);
            }
        }
    }

    /**Return CR to pixel.*/
    int charToPixel(char cr) {
        if (cr == 'e' || cr == '1') {
            return 5 * SQDIM + delta + delta / 2;
        }
        if (cr == 'd' || cr == '2') {
            return 4 * SQDIM + delta;
        }
        if (cr == 'c' || cr == '3') {
            return 3 * SQDIM + delta / 2;
        }
        if (cr == 'b' || cr == '4') {
            return 2 * SQDIM;
        }
        if (cr == 'a' || cr == '5') {
            return SQDIM - delta / 2;
        }
        return 0;

    }

    /** Notify observers of mouse's current position from click event WHERE. */
    private void readMove(String unused, MouseEvent where) {
        int x = where.getX(), y = where.getY();
        char mouseCol, mouseRow;
        if (where.getButton() == MouseEvent.BUTTON1) {
            mouseCol = (char) (x / SQDIM + 'a');
            mouseRow = (char) ((SQDIM * SIDE - y) / SQDIM + '1');
            if (mouseCol >= 'a' && mouseCol <= 'e'
                && mouseRow >= '1' && mouseRow <= '5') {
                setChanged();
                notifyObservers("" + mouseCol + mouseRow);
            }
        }
    }

    @Override
    public synchronized void update(Observable model, Object arg) {
        repaint();
    }



    /** Dimension of current drawing surface in pixels. */
    private int _dim;

    /** A partial Move indicating selected squares. */
    private Move _selectedMove;
}
