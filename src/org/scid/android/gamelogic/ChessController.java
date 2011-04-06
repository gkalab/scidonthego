package org.scid.android.gamelogic;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.scid.android.GUIInterface;
import org.scid.android.GameMode;
import org.scid.android.PGNOptions;
import org.scid.android.engine.ComputerPlayer;
import org.scid.android.gamelogic.Game.GameState;

import android.util.Log;

/**
 * The glue between the chess engine and the GUI.
 * 
 * @author petero
 */
public class ChessController {
	private ComputerPlayer computerPlayer = null;
	private PgnToken.PgnTokenReceiver gameTextListener = null;
	private String bookFileName = "";
	private Game game;
	private GUIInterface gui;
	private GameMode gameMode;
	private PGNOptions pgnOptions;
	private Thread computerThread;
	private Thread analysisThread;

	private int timeControl;
	private int movesPerSession;
	private int timeIncrement;

	class SearchListener implements org.scid.android.gamelogic.SearchListener {
		private int currDepth = 0;
		private int currMoveNr = 0;
		private String currMove = "";
		private int currNodes = 0;
		private int currNps = 0;
		private int currTime = 0;

		private int pvDepth = 0;
		private int pvScore = 0;
		private boolean pvIsMate = false;
		private boolean pvUpperBound = false;
		private boolean pvLowerBound = false;
		private String bookInfo = "";
		private String pvStr = "";
		private List<Move> pvMoves = null;
		private List<Move> bookMoves = null;
		private boolean whiteMove = true;

		public final void clearSearchInfo() {
			pvDepth = 0;
			currDepth = 0;
			bookInfo = "";
			pvMoves = null;
			bookMoves = null;
			setSearchInfo();
		}

		private final void setSearchInfo() {
			StringBuilder buf = new StringBuilder();
			if (pvDepth > 0) {
				buf.append(String.format("[%d] ", pvDepth));
				boolean negateScore = !whiteMove;
				if (pvUpperBound || pvLowerBound) {
					boolean upper = pvUpperBound ^ negateScore;
					buf.append(upper ? "<=" : ">=");
				}
				int score = negateScore ? -pvScore : pvScore;
				if (pvIsMate) {
					buf.append(String.format("m%d", score));
				} else {
					buf.append(String.format("%.2f", score / 100.0));
				}
				buf.append(pvStr);
				buf.append("\n");
			}
			if (currDepth > 0) {
				buf.append(String.format("d:%d %d:%s t:%.2f n:%d nps:%d",
						currDepth, currMoveNr, currMove, currTime / 1000.0,
						currNodes, currNps));
			}
			final String newPV = buf.toString();
			final String newBookInfo = bookInfo;
			final SearchStatus localSS = ss;
			gui.runOnUIThread(new Runnable() {
				public void run() {
					if (!localSS.searchResultWanted && (bookMoves != null))
						return;
					gui.setThinkingInfo(newPV, newBookInfo, pvMoves, bookMoves);
				}
			});
		}

		public void notifyDepth(int depth) {
			currDepth = depth;
			setSearchInfo();
		}

		public void notifyCurrMove(Position pos, Move m, int moveNr) {
			currMove = TextIO.moveToString(pos, m, false);
			currMoveNr = moveNr;
			setSearchInfo();
		}

		public void notifyPV(Position pos, int depth, int score, int time,
				int nodes, int nps, boolean isMate, boolean upperBound,
				boolean lowerBound, ArrayList<Move> pv) {
			pvDepth = depth;
			pvScore = score;
			currTime = time;
			currNodes = nodes;
			currNps = nps;
			pvIsMate = isMate;
			pvUpperBound = upperBound;
			pvLowerBound = lowerBound;
			whiteMove = pos.whiteMove;

			StringBuilder buf = new StringBuilder();
			Position tmpPos = new Position(pos);
			UndoInfo ui = new UndoInfo();
			for (Move m : pv) {
				buf.append(String.format(" %s", TextIO.moveToString(tmpPos, m,
						false)));
				tmpPos.makeMove(m, ui);
			}
			pvStr = buf.toString();
			pvMoves = pv;
			setSearchInfo();
		}

		public void notifyStats(int nodes, int nps, int time) {
			currNodes = nodes;
			currNps = nps;
			currTime = time;
			setSearchInfo();
		}

		@Override
		public void notifyBookInfo(String bookInfo, List<Move> moveList) {
			this.bookInfo = bookInfo;
			bookMoves = moveList;
			setSearchInfo();
		}

		public void prefsChanged() {
			setSearchInfo();
		}
	}

	SearchListener listener;

	public ChessController(GUIInterface gui,
			PgnToken.PgnTokenReceiver gameTextListener, PGNOptions options) {
		this.gui = gui;
		this.gameTextListener = gameTextListener;
		pgnOptions = options;
		listener = new SearchListener();
	}

	public final void setBookFileName(String bookFileName) {
		if (!this.bookFileName.equals(bookFileName)) {
			this.bookFileName = bookFileName;
			if (computerPlayer != null) {
				computerPlayer.setBookFileName(bookFileName);
				if (analysisThread != null) {
					stopAnalysis();
					startAnalysis();
				}
				updateBookHints();
			}
		}
	}

	private final void updateBookHints() {
		if (computerPlayer != null && gameMode != null) {
			boolean analysis = gameMode.analysisMode();
			if (!analysis && humansTurn()) {
				ss = new SearchStatus();
				Pair<String, ArrayList<Move>> bi = computerPlayer
						.getBookHints(game.currPos());
				listener.notifyBookInfo(bi.first, bi.second);
			}
		}
	}

	private final static class SearchStatus {
		boolean searchResultWanted = true;
	}

	SearchStatus ss = new SearchStatus();

	public final void newGame(GameMode gameMode) {
		ss.searchResultWanted = false;
		stopComputerThinking();
		stopAnalysis();
		this.gameMode = gameMode;
		game = new Game(computerPlayer, gameTextListener, timeControl,
				movesPerSession, timeIncrement);
		setPlayerNames(game);
		updateGameMode();
	}

	public final void startEngine(String engineFileName) {
		if (computerPlayer == null) {
			computerPlayer = new ComputerPlayer(engineFileName);
			computerPlayer.setListener(listener);
			computerPlayer.setBookFileName(bookFileName);
			game.setComputerPlayer(computerPlayer);
			gameTextListener.clear();
		}
	}

	public final void startGame() {
		updateComputeThreads(true);
		setSelection();
		updateGUI();
		updateGameMode();
	}

	private boolean guiPaused = false;

	public final void setGuiPaused(boolean paused) {
		guiPaused = paused;
		updateGameMode();
		if (paused) {
			stopAnalysis();
			stopComputerThinking();
		} else {
			updateComputeThreads(true);
		}
	}

	private final void updateGameMode() {
		if (game != null) {
			boolean gamePaused = gameMode.analysisMode()
					|| (humansTurn() && guiPaused);
			game.setGamePaused(gamePaused);
		}
	}

	private final void updateComputeThreads(boolean clearPV) {
		boolean analysis = gameMode.analysisMode();
		boolean computersTurn = !humansTurn();
		if (!analysis)
			stopAnalysis();
		if (!computersTurn)
			stopComputerThinking();
		if (clearPV) {
			listener.clearSearchInfo();
			updateBookHints();
		}
		if (analysis && this.computerPlayer != null)
			startAnalysis();
		if (computersTurn)
			startComputerThinking();
	}

	private final synchronized void startComputerThinking() {
		if (analysisThread != null)
			return;
		if (game.getGameState() != GameState.ALIVE)
			return;
		if (computerThread == null) {
			ss = new SearchStatus();
			final Pair<Position, ArrayList<Move>> ph = game.getUCIHistory();
			final Game g = game;
			final boolean haveDrawOffer = g.haveDrawOffer();
			final Position currPos = new Position(g.currPos());
			long now = System.currentTimeMillis();
			final int wTime = game.timeController.getRemainingTime(true, now);
			final int bTime = game.timeController.getRemainingTime(false, now);
			final int inc = game.timeController.getIncrement();
			final int movesToGo = game.timeController.getMovesToTC();
			computerThread = new Thread(new Runnable() {
				public void run() {
					final String cmd = computerPlayer.doSearch(ph.first,
							ph.second, currPos, haveDrawOffer, wTime, bTime,
							inc, movesToGo);
					final SearchStatus localSS = ss;
					gui.runOnUIThread(new Runnable() {
						public void run() {
							synchronized (shutdownEngineLock) {
								if (!localSS.searchResultWanted)
									return;
								Position oldPos = new Position(g.currPos());
								g.processString(cmd, false);
								updateGameMode();
								gui.computerMoveMade();
								listener.clearSearchInfo();
								stopComputerThinking();
								stopAnalysis(); // To force analysis to restart
								// for new position
								updateComputeThreads(true);
								setSelection();
								setAnimMove(oldPos, g.getLastMove(), true);
								updateGUI();
							}
						}
					});
				}
			});
			listener.clearSearchInfo();
			computerPlayer.shouldStop = false;
			computerThread.start();
			updateGUI();
		}
	}

	private final synchronized void startAnalysis() {
		if (gameMode.analysisMode()) {
			if (computerThread != null)
				return;
			if (analysisThread == null) {
				ss = new SearchStatus();
				final Pair<Position, ArrayList<Move>> ph = game.getUCIHistory();
				final boolean haveDrawOffer = game.haveDrawOffer();
				final Position currPos = new Position(game.currPos());
				final boolean alive = game.tree.getGameState() == GameState.ALIVE;
				analysisThread = new Thread(new Runnable() {
					public void run() {
						if (alive)
							computerPlayer.analyze(ph.first, ph.second,
									currPos, haveDrawOffer);
					}
				});
				listener.clearSearchInfo();
				computerPlayer.shouldStop = false;
				analysisThread.start();
				updateGUI();
			}
		}
	}

	private final synchronized void stopAnalysis() {
		if (analysisThread != null) {
			computerPlayer.stopSearch();
			try {
				if (analysisThread.isAlive()) {
					analysisThread.join();
				}
			} catch (InterruptedException ex) {
				Log.e("SCID", "Could not stop analysis thread");
			}
			analysisThread = null;
			listener.clearSearchInfo();
			updateGUI();
		}
	}

	private final synchronized void stopComputerThinking() {
		if (computerThread != null) {
			computerPlayer.stopSearch();
			try {
				computerThread.join();
			} catch (InterruptedException ex) {
				Log.e("SCID", "Could not stop computer thread");
			}
			computerThread = null;
			updateGUI();
		}
	}

	/** Set game mode. */
	public final boolean setGameMode(GameMode newMode) {
		boolean changed = false;
		if (!gameMode.equals(newMode)) {
			changed = true;
			if (newMode.humansTurn(game.currPos().whiteMove))
				ss.searchResultWanted = false;
			gameMode = newMode;
			if (!gameMode.playerWhite() || !gameMode.playerBlack())
				setPlayerNames(game); // If computer player involved, set player
			// names
			updateGameMode();
			updateComputeThreads(true);
			updateGUI();
		}
		return changed;
	}

	public final void prefsChanged() {
		updateBookHints();
		updateMoveList();
		listener.prefsChanged();
	}

	private final void setPlayerNames(Game game) {
		if (game != null) {
			String white = "";
			String black = "";
			if (!gameMode.humansTurn(game.currPos().whiteMove)) {
				white = gameMode.playerWhite() ? "Player" : "Engine";
				black = gameMode.playerBlack() ? "Player" : "Engine";
			}
			game.tree.setPlayerNames(white, black);
		}
	}

	public final void fromByteArray(byte[] data) {
		game.fromByteArray(data);
	}

	public final byte[] toByteArray() {
		return game.tree.toByteArray();
	}

	public final String getFEN() {
		return TextIO.toFEN(game.tree.currentPos);
	}

	/** Convert current game to PGN format. */
	public final String getPGN() {
		return game.tree.toPGN(pgnOptions);
	}

	public final void setPGN(String pgn) throws ChessParseError {
		Game newGame = new Game(null, gameTextListener, timeControl,
				movesPerSession, timeIncrement);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss.SSS");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date d1 = new Date();
		if (!newGame.readPGN(pgn, pgnOptions)) {
			throw new ChessParseError();
		}
		Date d2 = new Date();
		Log.d("SCID", "before/after readPGN " + df.format(d1) + "/"
				+ df.format(d2));
		game = newGame;
		updateGame();
	}

	public void updateGame() {
		ss.searchResultWanted = false;
		game.setComputerPlayer(computerPlayer);
		gameTextListener.clear();
		updateGameMode();
		stopAnalysis();
		stopComputerThinking();
		if (computerPlayer != null) {
			computerPlayer.clearTT();
			updateComputeThreads(true);
		}
		gui.setSelection(-1);
		gui.setFromSelection(-1);
		updateGUI();
	}

	public boolean hasEngineStarted() {
		return (computerPlayer != null);
	}

	public final void setFENOrPGN(String fenPgn) throws ChessParseError {
		try {
			Game newGame = new Game(null, gameTextListener, timeControl,
					movesPerSession, timeIncrement);
			Position pos = TextIO.readFEN(fenPgn);
			newGame.setPos(pos);
			setPlayerNames(newGame);
			game = newGame;
			updateGame();
		} catch (ChessParseError e) {
			// Try read as PGN instead
			setPGN(fenPgn);
		}
	}

	/** True if human's turn to make a move. (True in analysis mode.) */
	public final boolean humansTurn() {
		return gameMode.humansTurn(game.currPos().whiteMove);
	}

	/** Return true if computer player is using CPU power. */
	public final boolean computerBusy() {
		if (game.getGameState() != GameState.ALIVE)
			return false;
		return gameMode.analysisMode() || !humansTurn();
	}

	private final void undoMoveNoUpdate() {
		if (game.getLastMove() != null) {
			ss.searchResultWanted = false;
			game.undoMove();
			if (!humansTurn()) {
				if (game.getLastMove() != null) {
					game.undoMove();
					if (!humansTurn()) {
						game.redoMove();
					}
				} else {
					// Don't undo first white move if playing black vs computer,
					// because that would cause computer to immediately make
					// a new move and the whole redo history will be lost.
					if (gameMode.playerWhite() || gameMode.playerBlack())
						game.redoMove();
				}
			}
		}
	}

	public final void undoMove() {
		if (game.getLastMove() != null) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			undoMoveNoUpdate();
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	private final void redoMoveNoUpdate() {
		if (game.canRedoMove()) {
			ss.searchResultWanted = false;
			game.redoMove();
			if (!humansTurn() && game.canRedoMove()) {
				game.redoMove();
				if (!humansTurn())
					game.undoMove();
			}
		}
	}

	public final boolean canRedoMove() {
		return game.canRedoMove();
	}

	public final void redoMove(boolean animate) {
		if (canRedoMove()) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			redoMoveNoUpdate();
			updateComputeThreads(true);
			setSelection();
			if (animate) {
				setAnimMove(game.prevPos(), game.getLastMove(), true);
			}
			updateGUI();
		}
	}

	public final int numVariations() {
		return game.numVariations();
	}

	public final void changeVariation(int delta) {
		if (game.numVariations() > 1) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			game.changeVariation(delta);
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	public final void removeVariation() {
		if (game.numVariations() > 1) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			game.removeVariation();
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	public final void gotoMove(int moveNr) {
		gotoHalfMove(moveNr * 2);
	}

	public final void gotoHalfMove(int moveNr) {
		boolean needUpdate = false;
		while (game.currPos().halfMoveCounter > moveNr) { // Go backward
			int before = game.currPos().halfMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			undoMoveNoUpdate();
			int after = game.currPos().halfMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			if (after >= before)
				break;
			needUpdate = true;
		}
		while (game.currPos().halfMoveCounter < moveNr) { // Go forward
			int before = game.currPos().halfMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			redoMoveNoUpdate();
			int after = game.currPos().halfMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			if (after <= before)
				break;
			needUpdate = true;
		}
		if (needUpdate) {
			ss.searchResultWanted = false;
			stopAnalysis();
			stopComputerThinking();
			updateComputeThreads(true);
			setSelection();
			updateGUI();
		}
	}

	public final void makeHumanMove(Move m) {
		if (humansTurn()) {
			if (doMove(m)) {
				ss.searchResultWanted = false;
				stopAnalysis();
				stopComputerThinking();
				updateComputeThreads(true);
				updateGUI();
				if (gameMode.studyMode()) {
					redoMove(true);
				}
			} else {
				gui.setSelection(-1);
			}
		}
	}

	Move promoteMove;

	public final void reportPromotePiece(int choice) {
		final boolean white = game.currPos().whiteMove;
		int promoteTo;
		switch (choice) {
		case 1:
			promoteTo = white ? Piece.WROOK : Piece.BROOK;
			break;
		case 2:
			promoteTo = white ? Piece.WBISHOP : Piece.BBISHOP;
			break;
		case 3:
			promoteTo = white ? Piece.WKNIGHT : Piece.BKNIGHT;
			break;
		default:
			promoteTo = white ? Piece.WQUEEN : Piece.BQUEEN;
			break;
		}
		promoteMove.promoteTo = promoteTo;
		Move m = promoteMove;
		promoteMove = null;
		makeHumanMove(m);
	}

	/**
	 * Move a piece from one square to another.
	 * 
	 * @return True if the move was legal, false otherwise.
	 */
	final private boolean doMove(Move move) {
		Position pos = game.currPos();
		ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
		moves = MoveGen.removeIllegal(pos, moves);
		int promoteTo = move.promoteTo;
		for (Move m : moves) {
			if ((m.from == move.from) && (m.to == move.to)) {
				if ((m.promoteTo != Piece.EMPTY) && (promoteTo == Piece.EMPTY)) {
					promoteMove = m;
					gui.requestPromotePiece();
					return false;
				}
				if (m.promoteTo == promoteTo) {
					String strMove = TextIO.moveToString(pos, m, false);
					boolean isCorrect = game.processString(strMove, gameMode
							.studyMode());
					if (!isCorrect && this.gameMode.studyMode()) {
						gui.setSelection(-1);
						gui.reportInvalidMove(m);
						return false;
					}
					return true;
				}
			}
		}
		gui.reportInvalidMove(move);
		return false;
	}

	final private void updateGUI() {
		updateStatus();
		updateMoveList();

		StringBuilder sb = new StringBuilder();
		if (game.tree.currentNode != game.tree.rootNode) {
			game.tree.goBack();
			Position pos = game.currPos();
			List<Move> prevVarList = game.tree.variations();
			for (int i = 0; i < prevVarList.size(); i++) {
				if (i > 0)
					sb.append(' ');
				if (i == game.tree.currentNode.defaultChild)
					sb.append("<b>");
				sb.append(TextIO.moveToString(pos, prevVarList.get(i), false));
				if (i == game.tree.currentNode.defaultChild)
					sb.append("</b>");
			}
			game.tree.goForward(-1);
		}
		gui.setPosition(game.currPos(), sb.toString(), game.tree.variations());
		int noGames = gui.getScidAppContext().getNoGames();
		String gameNo = "";
		if (noGames != 0) {
			gameNo = "" + (gui.getScidAppContext().getCurrentGameNo() + 1)
					+ "/" + noGames;
		}
		gui.setGameInformation(game.tree.white, game.tree.black, gameNo);
	}

	private void updateStatus() {
		String str = "";
		String result = game.tree.getResult();
		if (!result.equals("*")) {
			str += result + " ";
		}
		String date = game.tree.date.replaceAll("\\.\\?\\?", "");
		if (!date.equals("?") && !date.equals("????")) {
			str += date + " ";
		}
		if (!game.tree.site.equals("?")) {
			str += game.tree.site + ": ";
		}
		if (!game.tree.event.equals("?")) {
			str += game.tree.event + " ";
		}
		if (!game.tree.round.equals("?")) {
			str += "(" + game.tree.round + ")";
		}
		gui.setStatusString(str);
	}

	final private void updateMoveList() {
		if (game != null) {
			if (!gameTextListener.isUpToDate()) {
				PGNOptions tmpOptions = new PGNOptions();
				tmpOptions.exp.variations = pgnOptions.view.variations;
				tmpOptions.exp.comments = pgnOptions.view.comments;
				tmpOptions.exp.nag = pgnOptions.view.nag;
				tmpOptions.exp.playerAction = false;
				tmpOptions.exp.clockInfo = false;
				tmpOptions.exp.moveNrAfterNag = false;
				gameTextListener.clear();
				game.tree.pgnTreeWalker(tmpOptions, gameTextListener);
			}
			gameTextListener.setCurrent(game.tree.currentNode);
			gui.moveListUpdated();
		}
	}

	final private void setSelection() {
		Move m = game.getLastMove();
		int fromSq = (m != null) ? m.from : -1;
		gui.setFromSelection(fromSq);
		int sq = (m != null) ? m.to : -1;
		gui.setSelection(sq);
	}

	public final synchronized void setTimeLimit(int time, int moves, int inc) {
		timeControl = time;
		movesPerSession = moves;
		timeIncrement = inc;
		if (game != null)
			game.timeController.setTimeControl(timeControl, movesPerSession,
					timeIncrement);
	}

	public final void stopSearch() {
		if (computerThread != null) {
			computerPlayer.stopSearch();
		}
	}

	private Object shutdownEngineLock = new Object();

	public final void shutdownEngine() {
		synchronized (shutdownEngineLock) {
			gameMode = new GameMode(GameMode.TWO_PLAYERS);
			ss.searchResultWanted = false;
			stopComputerThinking();
			stopAnalysis();
			if (computerPlayer != null) {
				computerPlayer.shutdownEngine();
			}
		}
	}

	/**
	 * Help human to claim a draw by trying to find and execute a valid draw
	 * claim.
	 */
	public final boolean claimDrawIfPossible() {
		if (!findValidDrawClaim())
			return false;
		updateGUI();
		return true;
	}

	private final boolean findValidDrawClaim() {
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw accept", false);
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw rep", false);
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw 50", false);
		if (game.getGameState() != GameState.ALIVE)
			return true;
		return false;
	}

	public final void resignGame() {
		if (game.getGameState() == GameState.ALIVE) {
			game.processString("resign", false);
			updateGUI();
		}
	}

	private void setAnimMove(Position sourcePos, Move move, boolean forward) {
		gui.setAnimMove(sourcePos, move, forward);
	}
}
