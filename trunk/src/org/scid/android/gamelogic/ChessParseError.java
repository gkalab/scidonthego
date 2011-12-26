package org.scid.android.gamelogic;


/**
 * Exception class to represent parse errors in FEN or algebraic notation.
 * @author petero
 */
public class ChessParseError extends Exception {
    private static final long serialVersionUID = -6051856171275301175L;

    public Position pos;
    public int resourceId = -1;

    public ChessParseError(String msg) {
        super(msg);
        pos = null;
    }
    public ChessParseError(String msg, Position pos) {
        super(msg);
        this.pos = pos;
    }

    public ChessParseError(int resourceId) {
        super("");
        pos = null;
        this.resourceId = resourceId;
    }

    public ChessParseError(int resourceId, Position pos) {
        super("");
        this.pos = pos;
        this.resourceId = resourceId;
    }
}
