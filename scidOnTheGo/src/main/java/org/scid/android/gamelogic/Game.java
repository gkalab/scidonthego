package org.scid.android.gamelogic;

import java.util.ArrayList;
import java.util.List;

import org.scid.android.PGNOptions;
import org.scid.android.engine.ComputerPlayer;
import org.scid.android.gamelogic.GameTree.Node;

/**
 * 
 * @author petero
 */
public class Game {
	GameTree tree;
	TimeControl timeController;
	private boolean pendingDrawOffer;
	private boolean gamePaused;
	private boolean addFirst;

	private PgnToken.PgnTokenReceiver gameTextListener;

	public Game(ComputerPlayer computerPlayer,
			PgnToken.PgnTokenReceiver gameTextListener, int timeControl,
			int movesPerSession, int timeIncrement) {
		this.gameTextListener = gameTextListener;
		tree = new GameTree(gameTextListener);
		timeController = new TimeControl();
		timeController.setTimeControl(timeControl, movesPerSession,
				timeIncrement);
		gamePaused = false;
		newGame();
	}

	final void fromByteArray(byte[] data) {
		tree.fromByteArray(data);
		updateTimeControl(true);
	}

	final void setComputerPlayer(ComputerPlayer computerPlayer) {
	}

	final void setGamePaused(boolean gamePaused) {
		if (gamePaused != this.gamePaused) {
			this.gamePaused = gamePaused;
			updateTimeControl(false);
		}
	}

	final void setPos(Position pos) {
		tree.setStartPos(new Position(pos));
		updateTimeControl(false);
	}

	final boolean readPGN(String pgn, PGNOptions options)
			throws ChessParseError {
		boolean ret = tree.readPGN(pgn, options);
		if (ret)
			updateTimeControl(false);
		return ret;
	}

	public final Position currPos() {
		return tree.currentPos;
	}

	final Position prevPos() {
		Move m = tree.currentNode.move;
		if (m != null) {
			tree.goBack();
			Position ret = new Position(currPos());
			tree.goForward(-1);
			return ret;
		} else {
			return currPos();
		}
	}

	final Move getNextMove() {
		if (canRedoMove()) {
			tree.goForward(-1);
			Move ret = tree.currentNode.move;
			tree.goBack();
			return ret;
		} else {
			return null;
		}
	}

	/**
	 * Update the game state according to move/command string from a player.
	 * 
	 * @param str
	 *            The move or command to process.
	 * @return True if str was understood, false otherwise.
	 */
	final boolean processString(String str, boolean inStudyMode) {
		if (getGameState() != GameState.ALIVE)
			return false;
		if (str.startsWith("draw ")) {
			String drawCmd = str.substring(str.indexOf(" ") + 1);
			handleDrawCmd(drawCmd, inStudyMode);
			return true;
		} else if (str.equals("resign")) {
			addToGameTree(new Move(0, 0, 0), "resign", inStudyMode);
			return true;
		}

		Move m = TextIO.UCIstringToMove(str);
		if (m != null) {
			ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(currPos());
			moves = MoveGen.removeIllegal(currPos(), moves);
			boolean legal = false;
			for (int i = 0; i < moves.size(); i++) {
				if (m.equals(moves.get(i))) {
					legal = true;
					break;
				}
			}
			if (!legal)
				m = null;
		}
		if (m == null) {
			m = TextIO.stringToMove(currPos(), str);
		}
		if (m == null) {
			return false;
		}

		return addToGameTree(m, pendingDrawOffer ? "draw offer" : "",
				inStudyMode);
	}

	private boolean addToGameTree(Move m, String playerAction,
								  boolean inStudyMode) {
		if (m.equals(new Move(0, 0, 0))) { // Don't create more than one null
			// move at a node
			List<Move> varMoves = tree.variations();
			for (int i = varMoves.size() - 1; i >= 0; i--) {
				if (varMoves.get(i).equals(m)) {
					tree.deleteVariation(i);
				}
			}
		}

		List<Move> varMoves = tree.variations();
		boolean movePresent = false;
		int varNo;
		for (varNo = 0; varNo < varMoves.size(); varNo++) {
			if (varMoves.get(varNo).equals(m)) {
				movePresent = true;
				break;
			}
		}
		if (!movePresent) {
			if (inStudyMode) {
				return false;
			} else {
				String moveStr = TextIO.moveToUCIString(m);
				varNo = tree.addMove(moveStr, playerAction, 0, "", "");
			}
		}
        int newPos = addFirst ? 0 : varNo;
        tree.reorderVariation(varNo, newPos);
        tree.goForward(newPos);
		int remaining = timeController.moveMade(System.currentTimeMillis());
		tree.setRemainingTime(remaining);
		updateTimeControl(true);
		pendingDrawOffer = false;
		return true;
	}

	private void updateTimeControl(boolean discardElapsed) {
		int move = currPos().fullMoveCounter;
		boolean wtm = currPos().whiteMove;
		if (discardElapsed || (move != timeController.currentMove)
				|| (wtm != timeController.whiteToMove)) {
			int initialTime = timeController.getInitialTime();
			int whiteBaseTime = tree.getRemainingTime(true, initialTime);
			int blackBaseTime = tree.getRemainingTime(false, initialTime);
			timeController.setCurrentMove(move, wtm, whiteBaseTime,
					blackBaseTime);
		}
		long now = System.currentTimeMillis();
		if (gamePaused || (getGameState() != GameState.ALIVE)) {
			timeController.stopTimer(now);
		} else {
			timeController.startTimer(now);
		}
	}

	/**
	 * Get the last played move, or null if no moves played yet.
	 */
	final Move getLastMove() {
		return tree.currentNode.move;
	}

	/** Return true if there is a move to redo. */
	final boolean canRedoMove() {
		int nVar = tree.variations().size();
		if (nVar == 1) {
			// check if last move is the null move
			if (tree.variations().get(0).isNullMove()) {
				nVar = 0;
			}
		}
		return nVar > 0;
	}

	final int numVariations() {
		if (tree.currentNode == tree.rootNode)
			return 1;
		tree.goBack();
		int nChildren = tree.variations().size();
		tree.goForward(-1);
		return nChildren;
	}

	final void changeVariation(int delta) {
		if (tree.currentNode == tree.rootNode)
			return;
		tree.goBack();
		int defChild = tree.currentNode.defaultChild;
		int nChildren = tree.variations().size();
		int newChild = defChild + delta;
		newChild = Math.max(newChild, 0);
		newChild = Math.min(newChild, nChildren - 1);
		tree.goForward(newChild);
		pendingDrawOffer = false;
		updateTimeControl(true);
	}

    final void moveVariation(int delta) {
        if (tree.currentNode == tree.rootNode)
			return;
		tree.goBack();
        int varNo = tree.currentNode.defaultChild;
        int nChildren = tree.variations().size();
        int newPos = varNo + delta;
        newPos = Math.max(newPos, 0);
        newPos = Math.min(newPos, nChildren - 1);
        tree.reorderVariation(varNo, newPos);
        tree.goForward(newPos);
        pendingDrawOffer = false;
        updateTimeControl(true);
    }

    final void removeSubTree() {
        if (getLastMove() != null) {
            tree.goBack();
		int defChild = tree.currentNode.defaultChild;
		tree.deleteVariation(defChild);
        } else {
            while (canRedoMove())
                tree.deleteVariation(0);
        }
		pendingDrawOffer = false;
		updateTimeControl(true);
	}

	public enum GameState {
		ALIVE, 
		WHITE_MATE, // White mates
		BLACK_MATE, // Black mates
		WHITE_STALEMATE, // White is stalemated
		BLACK_STALEMATE, // Black is stalemated
		DRAW_REP, // Draw by 3-fold repetition
		DRAW_50, // Draw by 50 move rule
		DRAW_NO_MATE, // Draw by impossibility of check mate
		DRAW_AGREE, // Draw by agreement
		RESIGN_WHITE, // White resigns
		RESIGN_BLACK // Black resigns
	}

	/**
	 * Get the current state (draw, mate, ongoing, etc) of the game.
	 */
	private GameState getGameState() {
		return tree.getGameState();
	}

	/**
	 * Check if a draw offer is available.
	 * @return True if the current player has the option to accept a draw offer.
	 */
	private boolean haveDrawOffer() {
		return tree.currentNode.playerAction.equals("draw offer");
	}

	final void undoMove() {
		Move m = tree.currentNode.move;
		if (m != null) {
			tree.goBack();
			pendingDrawOffer = false;
			updateTimeControl(true);
		}
	}

	final void redoMove() {
		if (canRedoMove()) {
			tree.goForward(-1);
			pendingDrawOffer = false;
			updateTimeControl(true);
		}
	}

	private void newGame() {
		tree = new GameTree(gameTextListener);
		timeController.reset();
		pendingDrawOffer = false;
		updateTimeControl(true);
	}


	/**
     * Return the last zeroing position and a list of moves
     * to go from that position to the current position.
	 */
	final Pair<Position, ArrayList<Move>> getUCIHistory() {
		Pair<List<Node>, Integer> ml = tree.getMoveList();
		List<Node> moveList = ml.first;
		Position pos = new Position(tree.startPos);
		ArrayList<Move> mList = new ArrayList<>();
		Position currPos = new Position(pos);
		UndoInfo ui = new UndoInfo();
		int nMoves = ml.second;
		for (int i = 0; i < nMoves; i++) {
			Node n = moveList.get(i);
			mList.add(n.move);
			currPos.makeMove(n.move, ui);
			if (currPos.halfMoveClock == 0) {
				pos = new Position(currPos);
				mList.clear();
			}
		}
		return new Pair<>(pos, mList);
	}

	private void handleDrawCmd(String drawCmd, boolean inStudyMode) {
		Position pos = tree.currentPos;
		if (drawCmd.startsWith("rep") || drawCmd.startsWith("50")) {
			boolean rep = drawCmd.startsWith("rep");
			Move m = null;
			String ms = null;
			int firstSpace = drawCmd.indexOf(" ");
			if (firstSpace >= 0) {
				ms = drawCmd.substring(firstSpace + 1);
				if (ms.length() > 0) {
					m = TextIO.stringToMove(pos, ms);
				}
			}
			boolean valid;
			if (rep) {
				valid = false;
				UndoInfo ui = new UndoInfo();
				int repetitions = 0;
				Position posToCompare = new Position(tree.currentPos);
				if (m != null) {
					posToCompare.makeMove(m, ui);
					repetitions = 1;
				}
				Pair<List<Node>, Integer> ml = tree.getMoveList();
				List<Node> moveList = ml.first;
				Position tmpPos = new Position(tree.startPos);
				if (tmpPos.drawRuleEquals(posToCompare))
					repetitions++;
				int nMoves = ml.second;
				for (int i = 0; i < nMoves; i++) {
					Node n = moveList.get(i);
					tmpPos.makeMove(n.move, ui);
					TextIO.fixupEPSquare(tmpPos);
					if (tmpPos.drawRuleEquals(posToCompare))
						repetitions++;
				}
				if (repetitions >= 3)
					valid = true;
			} else {
				Position tmpPos = new Position(pos);
				if (m != null) {
					UndoInfo ui = new UndoInfo();
					tmpPos.makeMove(m, ui);
				}
				valid = tmpPos.halfMoveClock >= 100;
			}
			if (valid) {
				String playerAction = rep ? "draw rep" : "draw 50";
				if (m != null)
					playerAction += " " + TextIO.moveToString(pos, m, false);
				addToGameTree(new Move(0, 0, 0), playerAction, inStudyMode);
			} else {
				pendingDrawOffer = true;
				if (m != null) {
					processString(ms, inStudyMode);
				}
			}
		} else if (drawCmd.startsWith("offer ")) {
			pendingDrawOffer = true;
			String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
			if (TextIO.stringToMove(pos, ms) != null) {
				processString(ms, inStudyMode);
			}
		} else if (drawCmd.equals("accept")) {
			if (haveDrawOffer())
				addToGameTree(new Move(0, 0, 0), "draw accept", inStudyMode);
		}
	}
}
