package qirkat;


import static qirkat.Command.Type;


/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author andrew
 */
class Manual extends Player {
    /**Game.*/
    private Game _gam;

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {

        super(game, myColor);
        _gam = game;
        _prompt = myColor + ": ";
    }


    @Override
    Move myMove() {

        Move mov;
        Reporter reporter = _gam.reporter();

        while (true) {

            Command cmd = game().getMoveCmnd(_prompt);
            if (cmd == null) {
                return null;
            } else if (cmd.commandType()
                    .equals(Command.Type.PIECEMOVE)) {
                mov = Move.parseMove(cmd.operands()[0]);

                if (game().getBoard().legalMove(mov)) {
                    return mov;
                } else {
                    reporter.outcomeMsg("that move is illegal.");
                }
            }
        }
    }

    /**Not actually in spec or used CMD REPORTER.*/
    void manualHelper(Command cmd, Reporter reporter) {
        Type type = cmd.commandType();
        if (type.equals(Type.AUTO)) {
            reporter.outcomeMsg("'auto' command is not allowed now.");
        }
    }

    /** Identifies the player serving as a source of input commands. */
    private String _prompt;
}

