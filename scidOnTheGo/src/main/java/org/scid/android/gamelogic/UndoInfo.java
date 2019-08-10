/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.scid.android.gamelogic;

/**
 * Contains enough information to undo a previous move.
 * Set by makeMove(). Used by unMakeMove().
 * @author petero
 */
public class UndoInfo {
    int capturedPiece;
    int castleMask;
    int epSquare;
    int halfMoveClock;
}
