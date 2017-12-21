package qirkat;

/* Author: P. N. Hilfinger */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import java.util.ArrayList;

import static qirkat.PieceColor.*;
import static qirkat.Game.State.*;
import static qirkat.Command.Type.*;
import static qirkat.GameException.error;

/** Controls the play of the game.
 *  @author andrew
 */
class Game {

    /** States of play. */
    static enum State {
        SETUP, PLAYING;
    }

    /** A new Game, using BOARD to play on, reading initially from
     *  BASESOURCE and using REPORTER for error and informational messages. */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _constBoard = _board.constantView();
        _reporter = reporter;
    }
    /**Return reporter.*/
    Reporter reporter() {
        return _reporter;
    }

    /**Returns inputs.*/
    CommandSource inputs() {
        return _inputs;
    }

    /** Run a session of Qirkat gaming. */
    void process() {
        Player white, black;
        white = black = null;
        doClear(null);
        _whiteIsManual = true;
        _blackIsManual = false;
        Boolean textGUI = false;
        while (true) {
            _state = SETUP;
            while (_state == SETUP) {
                doCommand();
            }
            while (_state != SETUP && !_board.gameOver()) {
                Move move;
                move = null;

                if (_whiteIsManual) {
                    white = new Manual(this, PieceColor.WHITE);
                } else {
                    white = new AI(this, PieceColor.WHITE);
                }
                if (_blackIsManual) {
                    black = new Manual(this, PieceColor.BLACK);
                } else {
                    black = new AI(this, PieceColor.BLACK);
                }
                if (_board.getMoves().size() == 0) {
                    _state = SETUP;
                    break;
                }
                if (textGUI) {
                    System.out.println(_board.toString());
                    System.out.println(_board.getMoves());
                }
                if (_board.whoseMove().equals(PieceColor.WHITE)) {
                    move = white.myMove();
                    if (move == null) {
                        break;
                    }
                } else if (_board.whoseMove().equals(PieceColor.BLACK)) {
                    move = black.myMove();
                    if (move == null) {
                        break;
                    }
                }
                if (!canMove(_board.whoseMove())) {
                    _state = SETUP;
                }
                if (_state == PLAYING) {
                    _board.makeMove(move);
                }
            }
            if (_state != PLAYING) {
                if (_board.getMoves().size() == 0) {
                    reportWinner();
                }
            }
        }
    }
    /**Return board.*/
    Board getBoard() {
        return _board;
    }

    /**Returns if PLAYER can move.*/
    boolean canMove(PieceColor player) {


        HashMap<String, PieceColor> b = _board.board();
        ArrayList<Move> moves = new ArrayList<Move>();
        for (String s : b.keySet()) {
            if (b.get(s).equals(player)) {
                char c = s.charAt(0);
                char r = s.charAt(1);
                int index = Move.index(c, r);
                if (_board.jumpPossible()) {
                    _board.getJumps(moves, index);
                } else {
                    _board.getMoves(moves, index);
                }
                if (moves.size() > 0) {
                    return true;
                }

            }
        }

        return false;
    }

    /** Return a read-only view of my game board. */
    Board board() {
        return _constBoard;
    }

    /** Perform the next command from our input source. */
    void doCommand() {
        try {
            Command cmnd =
                Command.parseCommand(_inputs.getLine("qirkat: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /** Read and execute commands until encountering a move or until
     *  the game leaves playing state due to one of the commands. Return
     *  the terminating move command, or null if the game first drops out
     *  of playing mode. If appropriate to the current input source, use
     *  PROMPT to prompt for input. */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                switch (cmnd.commandType()) {
                case PIECEMOVE:
                    return cmnd;
                default:
                    _commands.get(cmnd.commandType()).accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /** Return random integer between 0 (inclusive) and MAX>0 (exclusive). */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /** Report a move, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /** Report an error, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /** Perform the command 'auto OPERANDS[0]'. */
    void doAuto(String[] operands) {
        _state = SETUP;
        String player = operands[0];
        player = player.toUpperCase();
        if (player.equals("BLACK")) {
            _blackIsManual = false;
        } else if (player.equals("WHITE")) {
            _whiteIsManual = false;
        }

    }

    /** Perform a 'help' command. */
    void doHelp(String[] unused) {
        InputStream helpIn =
            Game.class.getClassLoader().getResourceAsStream("qirkat/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                    = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /** Perform the command 'load OPERANDS[0]'. */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);

            ArrayList<String> allLines = new ArrayList<String>();
            try (BufferedReader br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    allLines.add(line);
                }
            }
            for (String line : allLines) {
                try {
                    Command cmnd = Command.parseCommand(line);
                    _commands.get(cmnd.commandType()).accept(cmnd.operands());
                } catch (GameException excp) {
                    _reporter.errMsg(excp.getMessage());
                }
            }

        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /** Perform the command 'manual OPERANDS[0]'. */
    void doManual(String[] operands) {
        _state = SETUP;
        String player = operands[0];
        player = player.toUpperCase();
        if (player.equals("BLACK")) {
            _blackIsManual = true;
        } else if (player.equals("WHITE")) {
            _whiteIsManual = true;
        }

    }

    /** Exit the program. */
    void doQuit(String[] unused) {
        Main.reportTotalTimes();
        System.exit(0);
    }

    /** Perform the command 'start'. */
    void doStart(String[] unused) {
        _state = PLAYING;
    }

    /** Perform the move OPERANDS[0]. */
    void doMove(String[] operands) {

        String moveStr = operands[0];
        Move currMove = Move.parseMove(moveStr);
        if (!_board.legalMove(currMove)) {
            _reporter.outcomeMsg("Illegal move.");
        } else {
            _board.makeMove(currMove);
        }
    }

    /** Perform the command 'clear'. */
    void doClear(String[] unused) {

        _board = new Board();
        _state = SETUP;
        _whiteIsManual = true;
        _blackIsManual = false;
    }

    /** Perform the command 'set OPERANDS[0] OPERANDS[1]'. */
    void doSet(String[] operands) {
        doClear(null);
        String player = operands[0];
        player = player.toUpperCase();
        PieceColor nextMove = PieceColor.EMPTY;
        if (player.equals("WHITE")) {
            nextMove = PieceColor.WHITE;
        } else if (player.equals("BLACK")) {
            nextMove = PieceColor.BLACK;
        }
        String board = operands[1];
        _board.setPieces(board, nextMove);

    }

    /** Perform the command 'dump'. */
    void doDump(String[] unused) {

        String msg;
        msg = "===\n";
        msg += _board.toString();
        msg += "\n===";
        _reporter.outcomeMsg(msg);
    }

    /** Execute 'seed OPERANDS[0]' command, where the operand is a string
     *  of decimal digits. Silently substitutes another value if
     *  too large. */
    void doSeed(String[] operands) {
        try {
            _randoms.setSeed(Long.parseLong(operands[0]));
        } catch (NumberFormatException e) {
            _randoms.setSeed(Long.MAX_VALUE);
        }
    }
    /**Undo a move.*/
    void doUndo(String[] unused) {
        if (_whiteIsManual && _blackIsManual) {
            _board.undo();
        } else {
            _board.undo();
            _board.undo();
        }

    }

    /** Execute the artificial 'error' command. */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /** Report the outcome of the current game. */
    void reportWinner() {
        String msg;
        msg = _board.whoseMove().opposite() + " wins.";
        _reporter.outcomeMsg(msg);
    }

    /** Mapping of command types to methods that process them. */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
        new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(SETBOARD, this::doSet);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
        _commands.put(UNDO, this::doUndo);
    }

    /** Input source. */
    private final CommandSources _inputs = new CommandSources();

    /** My board and its read-only view. */
    private Board _board, _constBoard;
    /** Indicate which players are manual players (as opposed to AIs). */
    private boolean _whiteIsManual, _blackIsManual;
    /** Current game state. */
    private State _state;
    /** Used to send messages to the user. */
    private Reporter _reporter;
    /** Source of pseudo-random numbers (used by AIs). */
    private Random _randoms = new Random();
}
