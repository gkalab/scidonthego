package org.scid.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.scid.android.chessok.ImportChessOkActivity;
import org.scid.android.dialog.MoveListDialog;
import org.scid.android.dialog.PromoteDialog;
import org.scid.android.engine.EngineManager;
import org.scid.android.engine.EngineManager.EngineChangeEvent;
import org.scid.android.gamelogic.ChessController;
import org.scid.android.gamelogic.ChessParseError;
import org.scid.android.gamelogic.Move;
import org.scid.android.gamelogic.Position;
import org.scid.android.gamelogic.TextIO;
import org.scid.android.twic.ImportTwicActivity;
import org.scid.database.DataBase;
import org.scid.database.DataBaseView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ScidAndroidActivity extends Activity implements GUIInterface,
		IClipboardChangedListener, IDownloadCallback {

	private static final String ANALYSIS_ENGINE = "analysisEngine";
	private static final String TAG = ScidAndroidActivity.class.getSimpleName();
	private ChessBoard cb;
	private EngineManager engineManager;
	private ChessController ctrl = null;
	private boolean mShowThinking;
	private boolean mShowBookHints;
	private int maxNumArrows;
	private GameMode gameMode = new GameMode(GameMode.REVIEW_MODE);

	private RatingBar favoriteRating;
	private TextView status;
	private ScrollView moveListScroll;
	private TextView moveList;
	private TextView whitePlayer;
	private TextView blackPlayer;
	private TextView gameNo;

	SharedPreferences preferences;

	private float scrollSensitivity = 3;
	private boolean invertScrollDirection = false;

	private PGNOptions pgnOptions = new PGNOptions();

	PgnScreenText gameTextListener;
	private String myPlayerNames = "";
	private String lastWhitePlayerName = "";
	private String lastBlackPlayerName = "";
	// remember the last position a "end of variation" message was shown
	private String lastEndOfVariation = null;

	private int studyOrientation = 0;
	private static final int SO_MOVING_SIDE = 0, SO_OPPOSITE_SIDE = 1, SO_MY_PLAYER = 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		File scidFileDir = new File(Tools.getScidDirectory());
		if (!scidFileDir.exists()) {
			scidFileDir.mkdirs();
		}
		engineManager = new EngineManager(this);
		checkUciEngine();
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				readPrefs();
				setGameMode();
			}
		});

		timeIsUp = MediaPlayer.create(this, R.raw.time_is_up);

		// do not use custom titles on Honeycomb and above - don't work with
		// the Holo Theme
		initUI(Integer.valueOf(android.os.Build.VERSION.SDK) < 11);
		if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 11) {
			VersionHelper.registerClipChangedListener(this);
			if (!ScreenTools.isLargeScreenLayout(this)) {
				// remove Icon from ActionBar if it's a phone and >= Honeycomb
				VersionHelper.removeIconFromActionbar(this);
			}
		}

		gameTextListener = new PgnScreenText(pgnOptions);
		ctrl = new ChessController(this, gameTextListener, pgnOptions);
		getScidAppContext().setController(ctrl);
		ctrl.newGame(new GameMode(GameMode.REVIEW_MODE));
		readPrefs();
		ctrl.newGame(gameMode);

		DataBaseView dbv = setDataBaseViewFromFile();
		if (dbv != null)
			restoreCurrentGameId();

		byte[] data = null;
		if (savedInstanceState != null) {
			data = savedInstanceState.getByteArray("gameState");
		} else {
			String dataStr = preferences.getString("gameState", null);
			if (dataStr != null) {
				data = strToByteArr(dataStr);
			}
		}
		if (data != null) {
			ctrl.fromByteArray(data);
		}

		ctrl.setGuiPaused(true);
		ctrl.setGuiPaused(false);
		ctrl.startGame();
		setFavoriteRating();
		handleIncomingContent();
	}

	private void handleIncomingContent() {
		Uri data = getIntent().getData();
		if (data == null) {
			String pgn = null;
			if (Intent.ACTION_SEND.equals(getIntent().getAction())
					&& isXChessType()) {
				pgn = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			}
			if (pgn != null) {
				// PGN or FEN content via share
				try {
					getScidAppContext().setDataBaseView(null);
					ctrl.setFENOrPGN(pgn);
				} catch (ChessParseError e) {
	                // If FEN corresponds to illegal chess position, go into edit board mode.
	                try {
	                    TextIO.readFEN(pgn);
	                } catch (ChessParseError e2) {
	                    if (e2.pos != null) {
	                    	editBoard(pgn);
	                    } else {
	    					Log.i(TAG, "ChessParseError", e);
	    					Toast.makeText(getApplicationContext(), e.getMessage(),
	    							Toast.LENGTH_SHORT).show();
	                    }
	                }
				}
			}
		} else {
			// Support the registered *.pgn extension
			Tools.processUri(this, data, RESULT_PGN_IMPORT);
			// the data was handled, set it to null to not enter this again
			getIntent().setData(null);
		}
	}

	private boolean isXChessType() {
		return "application/x-chess-pgn".equals(getIntent().getType())
				|| "application/x-chess-fen".equals(getIntent().getType());
	}

	private void checkUciEngine() {
		// check if engine exists in the files directory
		File engine = new File(engineManager.getDefaultEngine()
				.getExecutablePath());
		if (engine.exists()) {
			try {
				String cmd[] = { "chmod", "744", engine.getAbsolutePath() };
				Process process = Runtime.getRuntime().exec(cmd);
				try {
					process.waitFor();
					Log.d(TAG, "chmod 744 " + engine.getAbsolutePath());
				} catch (InterruptedException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		} else {
			Log.d(TAG, "Engine is missing from data. Intializing...");
			try {
				String nativeLibraryDir = Tools.getNativeLibraryDir(this);
				InputStream istream = new FileInputStream(nativeLibraryDir
								+ File.separator
								+ EngineManager.INTERNAL_ENGINE_FILE_NAME);
				FileOutputStream fout = new FileOutputStream(
						engine.getAbsolutePath());
				byte[] b = new byte[1024];
				int noOfBytes = 0;
				while ((noOfBytes = istream.read(b)) != -1) {
					fout.write(b, 0, noOfBytes);
				}
				istream.close();
				fout.close();
				Log.d(TAG, engine.getName()
						+ " copied to files directory");
				try {
					String cmd[] = { "chmod", "744", engine.getAbsolutePath() };
					Process process = Runtime.getRuntime().exec(cmd);
					try {
						process.waitFor();
						Log.d(TAG, "chmod 744 " + engine.getAbsolutePath());
					} catch (InterruptedException e) {
						Log.e(TAG, e.getMessage(), e);
					}
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	private static Random generator = new Random();
	private void randomGame() {
		DataBaseView dbv = getDataBaseView();
		if (dbv != null) {
			int oldPosition = dbv.getPosition();
			int totalGames = dbv.getCount();
			if (totalGames > 1) {
				int newPosition = generator.nextInt(totalGames - 1); // newGameIndex < totalGames-1
				if (newPosition >= oldPosition)
					++newPosition;
				// assert newPosition < totalGames && newPosition != oldPosition
				dbv.moveToPosition(newPosition);
				setPgnFromDataBaseView();
			}
		}
	}

	public void onNextGameClick(View view) {
		if (hasNoDataBaseViewOpened())
			return;
		nextOrRandomGame();
	}

	private void nextOrRandomGame() {
		if (preferences.getBoolean("nextGameIsRandom", false)) {
			randomGame();
		} else {
			nextGame();
		}
	}

	private void setupTimersForNewGame(){
		if (gameMode.studyMode()) {
			resetHumanThinkingTimer();
			if (ctrl.canRedoMove()) {
				int studyAutoMoveDelay = Integer.valueOf(preferences.getString("studyAutoMoveDelay", "0"));
				if (studyAutoMoveDelay != 0) {
					scheduleAutoplay(studyAutoMoveDelay * 1000, false);
				}
			} else { // notify the user that there is no moves to think about
				Toast.makeText(this, getText(R.string.end_of_variation), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private Timer autoMoveTimer = new Timer();
	private TimerTask autoMoveTask;
	/** returns true if something was canceled */
	private boolean cancelAutoMove() {
		if (autoMoveTask == null)
			return false;
		autoMoveTask.cancel();
		autoMoveTask = null;
		return autoMoveTimer.purge() > 0;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
			if (cancelAutoMove()) {
				Toast.makeText(this, getText(R.string.autoplay_stopped), Toast.LENGTH_SHORT).show();
				if (gameMode.studyMode()) {
					// human were waiting for a computer move, but canceled it and start thinking
					resetHumanThinkingTimer();
				}
				return true; // we already consumed the touch
			}
		}
		return super.dispatchTouchEvent(event);
	}

	private void scheduleAutoplay(long timeInMs, final boolean isRepeated) {
		cancelAutoMove();
		cancelHumanThinkingTimer(); // human waits for the move instead of thinking
		autoMoveTask = new TimerTask() { @Override
		public void run() {
			runOnUiThread(new Runnable() { @Override
			public void run() {
				if (ctrl.canRedoMove()) {
					ctrl.redoMove();
					if (!isRepeated) {
						// computer auto-played a single move (due to studyAutoMoveDelay)
						resetHumanThinkingTimer();
					}
				} else { // no more moves
					Toast.makeText(ScidAndroidActivity.this, getText(R.string.end_of_variation), Toast.LENGTH_SHORT).show();
					cancelAutoMove();
				}
			}});
		}};
		if (isRepeated) {
			autoMoveTimer.scheduleAtFixedRate(autoMoveTask, 0, timeInMs);
		} else {
			autoMoveTimer.schedule(autoMoveTask, timeInMs);
		}
	}

	/** If the NextMove button is long-clicked we either start auto play or go to the end of variation */
	private void onNextMoveLongClick() {
		if (!ctrl.canRedoMove()) {
			Toast.makeText(this, getText(R.string.end_of_variation), Toast.LENGTH_SHORT).show();
			return;
		}
		int autoPlayInterval = 0;
		try {
			autoPlayInterval = Integer.valueOf(preferences.getString(
					"autoPlayInterval", "0"));
		} catch (NumberFormatException e) {
			// ignore
		}
		if (autoPlayInterval != 0) {
			scheduleAutoplay(autoPlayInterval * 1000, true);
			Toast.makeText(this, getText(R.string.autoplay_started), Toast.LENGTH_LONG).show();
		} else {
			ctrl.gotoMove(9999);
			lastEndOfVariation = null;
		}
	}

	private void previousGame() {
		DataBaseView dbv = getDataBaseView();
		if (dbv != null) {
			int position = dbv.getPosition();
			if (position > 0 && dbv.moveToPosition(position-1)) {
				setPgnFromDataBaseView();
			} else {
				Toast.makeText(this, R.string.err_no_prev_game, Toast.LENGTH_SHORT).show();
			}
			lastEndOfVariation = null;
		}
	}

	private void nextGame() {
		DataBaseView dbv = getDataBaseView();
		if (dbv != null) {
			int position = dbv.getPosition();
			if (position < dbv.getCount() && dbv.moveToPosition(position+1)) {
				setPgnFromDataBaseView();
			} else {
				Toast.makeText(this, R.string.err_no_next_game, Toast.LENGTH_SHORT).show();
			}
			lastEndOfVariation = null;
		}
	}

	private void setPgnFromDataBaseView(int moveNo) {
		DataBaseView dbv = getDataBaseView();
		if (dbv == null)
			return;
		String pgn = dbv.getPGN();
		if (pgn != null && pgn.length() > 0) {
			try {
				ctrl.setPGN(pgn);
				if (moveNo < 0)
					moveNo = dbv.getCurrentPly();
				if (moveNo > 0)
					ctrl.gotoHalfMove(moveNo);
				if (gameMode.studyMode()) {
					switch(studyOrientation){
					case SO_MOVING_SIDE:
						setFlipped(!cb.pos.whiteMove);
						break;
					case SO_OPPOSITE_SIDE:
						setFlipped(cb.pos.whiteMove);
						break;
					// case SO_MY_PLAYER is processed by setGameInformation
					}
				}
				saveGameState();
			} catch (ChessParseError e) {
				Log.i(TAG, "ChessParseError", e);
				Toast.makeText(getApplicationContext(),
						"Parse error " + e.getMessage(), Toast.LENGTH_SHORT)
						.show();
			}
		}
		setFavoriteRating();
		saveCurrentGameId();
		setupTimersForNewGame();
		refreshMenu();
	}

	private void setPgnFromDataBaseView() {
		setPgnFromDataBaseView(-1);
	}

	private void setFavoriteRating() {
		favoriteRating.setRating(this.getScidAppContext().isFavorite() ? 1 : 0);
	}

	/**
	 * Check white and black player name if it corresponds to myPlayerNames and
	 * flip board if necessary
	 */
	private void flipBoardForPlayerNames(String white, String black) {
		if (!white.equalsIgnoreCase(this.lastWhitePlayerName)
				|| !black.equalsIgnoreCase(this.lastBlackPlayerName)) {
			this.lastWhitePlayerName = white;
			this.lastBlackPlayerName = black;
			String[] names = myPlayerNames.split("\\n");
			for (int i = 0; i < names.length; i++) {
				String playerName = names[i].trim();
				if (playerName.length() > 0) {
					if (white.equalsIgnoreCase(playerName)) {
						setFlipped(false);
						break;
					} else if (black.equalsIgnoreCase(playerName)) {
						setFlipped(true);
						break;
					}
				}
			}
		}
	}

	public void onPreviousGameClick(View view) {
		if (hasNoDataBaseViewOpened())
			return;
		previousGame();
	}

	public void onNextMoveClick(View view) {
		if (ctrl.canRedoMove()) {
			ctrl.redoMove();
			if (gameMode.studyMode())
				resetHumanThinkingTimer();
		} else {
			String currentPosition = ctrl.getFEN();
			if (lastEndOfVariation == null
					|| !lastEndOfVariation.equals(currentPosition)) {
				if (preferences.getBoolean("cruiseMode", false)) {
					nextOrRandomGame();
				} else {
					lastEndOfVariation = currentPosition;
					Toast.makeText(this, getText(R.string.end_of_variation), Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	public void onPreviousMoveClick(View view) {
		if (ctrl.canUndoMove()) {
			ctrl.undoMove();
			if (gameMode.studyMode())
				resetHumanThinkingTimer();
			lastEndOfVariation = null;
		} else {
			Toast.makeText(this, getText(R.string.start_of_game), Toast.LENGTH_SHORT).show();
		}
	}

	public void onFlipBoardClick(View view) {
		setFlipped(!cb.flipped);
	}

	private final byte[] strToByteArr(String str) {
		int nBytes = str.length() / 2;
		byte[] ret = new byte[nBytes];
		for (int i = 0; i < nBytes; i++) {
			int c1 = str.charAt(i * 2) - 'A';
			int c2 = str.charAt(i * 2 + 1) - 'A';
			ret[i] = (byte) (c1 * 16 + c2);
		}
		return ret;
	}

	private final String byteArrToString(byte[] data) {
		StringBuilder ret = new StringBuilder(32768);
		int nBytes = data.length;
		for (int i = 0; i < nBytes; i++) {
			int b = data[i];
			if (b < 0) {
				b += 256;
			}
			char c1 = (char) ('A' + (b / 16));
			char c2 = (char) ('A' + (b & 15));
			ret.append(c1);
			ret.append(c2);
		}
		return ret.toString();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ChessBoard oldCB = cb;
		String statusStr = status.getText().toString();
		if (statusStr.startsWith("DELETED ")) {
			statusStr = statusStr.substring(8);
		}
		initUI(false);
		readPrefs();
		cb.cursorX = oldCB.cursorX;
		cb.cursorY = oldCB.cursorY;
		cb.cursorVisible = oldCB.cursorVisible;
		cb.setPosition(oldCB.pos);
		setFlipped(oldCB.flipped);
		cb.oneTouchMoves = oldCB.oneTouchMoves;
		setSelection(oldCB.selectedSquare);
		updateThinkingInfo();
		setStatusString(statusStr);
		moveListUpdated();
		setFavoriteRating();
	}

	private final void initUI(boolean initTitle) {
		if (initTitle) {
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		}
		setContentView(R.layout.main);
		if (initTitle) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
					R.layout.title);
			whitePlayer = (TextView) findViewById(R.id.white_player);
			blackPlayer = (TextView) findViewById(R.id.black_player);
			gameNo = (TextView) findViewById(R.id.gameNo);
		}
		favoriteRating = (RatingBar) findViewById(R.id.favorite);
		favoriteRating.setOnTouchListener(new OnTouchListener() {
			// it is tricky to intercept onClick of RatingBar, let us use touch instead
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ((event.getAction() & MotionEvent.ACTION_MASK) // TODO: use getActionMasked (API Level 8)
						== MotionEvent.ACTION_UP) {
					invertIsFavorite();
				}
				return true;
			}
		});
		status = (TextView) findViewById(R.id.status);
		status.setFocusable(false);
		status.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if ((e.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP
						&& preferences.getBoolean("oneTouchModeChange", true)) {
					float p = (e.getX() - v.getLeft()) / v.getWidth(); // p in [0,1]
					int mode = (int)(3*p-0.01); // mode in {0,1,2}
					setMode(mode);
				}
				return true;
			}
		});
		moveListScroll = (ScrollView) findViewById(R.id.moveListScrollView);
		moveList = (TextView) findViewById(R.id.moveList);
		moveListScroll.setFocusable(false);
		moveList.setMovementMethod(LinkMovementMethod.getInstance());
		moveList.setFocusable(false);
		// disable all other text colors (e.g. pressed) for move list
		moveList.setTextColor(moveList.getTextColors().getDefaultColor());
		moveList.setLinkTextColor(moveList.getTextColors().getDefaultColor());
		moveList.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (!gameMode.studyMode() && !gameMode.analysisMode()) {
					removeDialog(MOVELIST_MENU_DIALOG);
					showDialog(MOVELIST_MENU_DIALOG);
				}
				return true;
			}
		});

		cb = (ChessBoard) findViewById(R.id.chessboard);
		cb.setFocusable(true);
		cb.requestFocus();
		cb.setClickable(true);

		final GestureDetector gd = new GestureDetector(
				new GestureDetector.SimpleOnGestureListener() {
					private float scrollX = 0;
					private float scrollY = 0;

					@Override
					public boolean onDown(MotionEvent e) {
						scrollX = 0;
						scrollY = 0;
						return false;
					}

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						cb.cancelLongPress();
						if (invertScrollDirection) {
							distanceX = -distanceX;
							distanceY = -distanceY;
						}
						if (scrollSensitivity > 0) {
							scrollX += distanceX;
							scrollY += distanceY;
							float scrollUnit = cb.sqSize * scrollSensitivity;
							if (Math.abs(scrollX) < Math.abs(scrollY)) {
								// Next/previous variation
								int varDelta = 0;
								while (scrollY > scrollUnit) {
									varDelta++;
									scrollY -= scrollUnit;
								}
								while (scrollY < -scrollUnit) {
									varDelta--;
									scrollY += scrollUnit;
								}
								if (varDelta != 0) {
									scrollX = 0;
								}
								ctrl.changeVariation(varDelta);
							}
						}
						return true;
					}

					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						cb.cancelLongPress();
						handleClick(e);
						return true;
					}

					@Override
					public boolean onDoubleTapEvent(MotionEvent e) {
						if (e.getAction() == MotionEvent.ACTION_UP) {
							handleClick(e);
						}
						return true;
					}

					private final void handleClick(MotionEvent e) {
						int sq = cb.eventToSquare(e);
						Move m = cb.mousePressed(sq);
						makeHumanMove(m);
					}
				});
		cb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gd.onTouchEvent(event);
			}
		});
		cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
			@Override
			public void onTrackballEvent(MotionEvent event) {
				Move m = cb.handleTrackballEvent(event);
				makeHumanMove(m);
			}
		});
		cb.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				removeDialog(CLIPBOARD_DIALOG);
				showDialog(CLIPBOARD_DIALOG);
				return true;
			}
		});
		// add long click listeners to buttons
		ImageButton nextButton = (ImageButton) findViewById(R.id.next_move);
		nextButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				onNextMoveLongClick();
				return true;
			}
		});
		ImageButton previousButton = (ImageButton) findViewById(R.id.previous_move);
		previousButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				ctrl.gotoMove(0);
				lastEndOfVariation = null;
				moveListScroll.fullScroll(View.FOCUS_UP);
				return true;
			}
		});
		ImageButton nextGameButton = (ImageButton) findViewById(R.id.next_game);
		nextGameButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (hasNoDataBaseViewOpened())
					return true;
				DataBaseView dbv = getDataBaseView();
				int position = dbv.getPosition(), total = dbv.getCount();
				if (position != total-1 && dbv.moveToPosition(total-1)) {
					setPgnFromDataBaseView();
				}
				return true;
			}
		});
		ImageButton previousGameButton = (ImageButton) findViewById(R.id.previous_game);
		previousGameButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (hasNoDataBaseViewOpened())
					return true;
				DataBaseView dbv = getDataBaseView();
				if (dbv.moveToFirst()) {
					setPgnFromDataBaseView();
				}
				return true;
			}
		});
	}

	/** track time needed for human move */
	private long humanStartThinkingNanoTime;
	/** alarm if human thinks for too long */
	private Timer humanThinkingTimer = new Timer();
	private TimerTask humanThinkingTimerTask;
	private MediaPlayer timeIsUp;
	private void cancelHumanThinkingTimer() {
		if (humanThinkingTimerTask != null) {
			humanThinkingTimerTask.cancel();
			humanThinkingTimerTask = null;
		}
	}
	private void resetHumanThinkingTimer() {
		humanStartThinkingNanoTime = System.nanoTime();
		cancelHumanThinkingTimer();
		int t = Integer.valueOf(preferences.getString("humanThinkingAlarmInterval", "0"));
		if (t != 0) {
			humanThinkingTimerTask = new TimerTask() { @Override
			public void run() {
				runOnUiThread(new Runnable() { @Override
				public void run() {
					Toast.makeText(ScidAndroidActivity.this, getText(R.string.time_is_up), Toast.LENGTH_LONG).show();
					if (preferences.getBoolean("humanThinkingAlarmSound", false))
						timeIsUp.start();
				}});
			}};
			humanThinkingTimer.schedule(humanThinkingTimerTask, t*1000);
		}
	}
	private double getHumanThinkingTime() {
		return (System.nanoTime() - humanStartThinkingNanoTime) / 1e9;
	}

	private void makeHumanMove(Move m) {
		if (m == null)
			return;
		if (!gameMode.studyMode()) {
			ctrl.makeHumanMove(m);
			return;
		}
		// the rest only in study mode

		if (!ctrl.canRedoMove()) { // human tried to guess a move while there is none
			Toast.makeText(this, getText(R.string.end_of_variation), Toast.LENGTH_SHORT).show();
			return;
		}

		invalidMoveReported = false;
		ctrl.makeHumanMove(m); // updates invalidMoveReported
		if (invalidMoveReported) {
			if (preferences.getBoolean("makeFavoriteOnWrongMove", false)
					&& !getScidAppContext().isFavorite()) {
				invertIsFavorite(); // that is set it
			}
		} else { // correct move
			if (preferences.getBoolean("reportThinkingTime", false)) {
				Toast.makeText(this, String.format(getString(R.string.your_time), getHumanThinkingTime()),
						Toast.LENGTH_LONG).show();
			}
			resetHumanThinkingTimer();
			if (!ctrl.canRedoMove()) { // that was the last move in variation
				switch(Integer.parseInt(preferences.getString("endOfVariationInStudy", "0"))) {
				case 0: default:
					Toast.makeText(this, getText(R.string.end_of_variation), Toast.LENGTH_SHORT).show();
					break;
				case 1:
					nextOrRandomGame();
					break;
				case 2:
					setMode(GameMode.REVIEW_MODE);
					break;
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (ctrl != null) {
			byte[] data = ctrl.toByteArray();
			outState.putByteArray("gameState", data);
		}
	}

	@Override
	protected void onResume() {
		// recover from preempted DataBase
		setDataBaseViewFromFile();

		if (ctrl != null) {
			ctrl.setGuiPaused(false);
		}
		if (gameMode.analysisMode())
			startAnalysis();
		if (gameMode.studyMode())
			resetHumanThinkingTimer();
		super.onResume();
	}

	@Override
	protected void onPause() {
		cancelAutoMove();
		cancelHumanThinkingTimer();
		if (ctrl != null) {
			ctrl.setGuiPaused(true);
			saveGameState();
			ctrl.shutdownEngine();
		}
		super.onPause();
	}

	private void saveGameState() {
		byte[] data = ctrl.toByteArray();
		Editor editor = preferences.edit();
		String dataStr = byteArrToString(data);
		editor.putString("gameState", dataStr);
		editor.commit();
	}

	@Override
	protected void onDestroy() {
		if (ctrl != null) {
			ctrl.shutdownEngine();
		}
		super.onDestroy();
	}

	private final void readPrefs() {
		this.myPlayerNames = preferences.getString("playerNames", "");
		setFlipped(preferences.getBoolean("boardFlipped", false));
		studyOrientation = Integer.parseInt(preferences.getString("studyOrientation", "0"));
		cb.oneTouchMoves = preferences.getBoolean("oneTouchMoves", false);
		mShowThinking = preferences.getBoolean("showThinking", false);
		String tmp = preferences.getString("thinkingArrows", "6");
		maxNumArrows = Integer.parseInt(tmp);
		mShowBookHints = preferences.getBoolean("bookHints", false);
		gameMode = new GameMode(preferences.getInt("gameMode",
				GameMode.REVIEW_MODE));
		ctrl.setTimeLimit(300000, 60, 0);

		setFullScreenMode(true);

		tmp = preferences.getString("fontSize", "12");
		int fontSize = Integer.parseInt(tmp);
		status.setTextSize(fontSize);
		moveList.setTextSize(fontSize);

		pgnOptions.view.variations = true;
		pgnOptions.view.comments = true;
		pgnOptions.view.nag = true;
		pgnOptions.view.headers = false;
		pgnOptions.view.allMoves = true;
		String moveDisplayString = preferences.getString("moveDisplay", "0");
		int moveDisplay = Integer.parseInt(moveDisplayString);
		if (moveDisplay != 0) {
			pgnOptions.view.allMoves = false;
		}
		pgnOptions.imp.variations = true;
		pgnOptions.imp.comments = true;
		pgnOptions.imp.nag = true;
		pgnOptions.exp.variations = true;
		pgnOptions.exp.comments = true;
		pgnOptions.exp.nag = true;
		pgnOptions.exp.playerAction = false;
		pgnOptions.exp.clockInfo = false;

		ColorTheme.instance().readColors(preferences);
		cb.setColors();

		String engineName = preferences.getString(ANALYSIS_ENGINE, engineManager
				.getDefaultEngine().getName());
		engineManager.setCurrentEngineName(engineName);

		String currentScidFile = preferences.getString("currentScidFile", "");
		if (currentScidFile.length() > 0) {
			getScidAppContext().setCurrentFileName(currentScidFile);
		}

		gameTextListener.clear();
		ctrl.prefsChanged();
	}

	private final void setFullScreenMode(boolean fullScreenMode) {
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
		if (fullScreenMode) {
			attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
		} else {
			attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		getWindow().setAttributes(attrs);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (ScreenTools.isLargeScreenLayout(this)) {
			getMenuInflater().inflate(R.menu.options_menu_tablet, menu);
		} else {
			getMenuInflater().inflate(R.menu.options_menu, menu);
		}
		return true;
	}

	static private final int RESULT_EDITBOARD = 0;
	static private final int RESULT_PREFERENCES = 1;
	static private final int RESULT_SEARCH = 2;
	static private final int RESULT_PGN_FILEDIALOG = 3;
	static private final int RESULT_GAMELIST = 4;
	static private final int RESULT_PGN_IMPORT = 5;
	static private final int RESULT_TWIC_IMPORT = 6;
	static private final int RESULT_LOAD_SCID_FILE = 7;
	static private final int RESULT_ADD_ENGINE = 8;
	static private final int RESULT_REMOVE_ENGINE = 9;
	static private final int RESULT_SAVE_GAME = 10;
	static private final int RESULT_PGN_FILE_IMPORT = 11;
	static private final int RESULT_GET_FEN = 12;
	static private final int RESULT_EXPORT_PGN = 13;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_open_file:
			Intent intent = new Intent(ScidAndroidActivity.this,
					SelectFileActivity.class);
			intent.setAction(".si4");
			startActivityForResult(intent, RESULT_LOAD_SCID_FILE);
			refreshMenu();
			return true;
		case R.id.item_preferences: {
			Intent i = new Intent(ScidAndroidActivity.this, Preferences.class);
			i.putExtra(Preferences.DATA_ENGINE_NAMES,
					engineManager.getEngineNames(true));
			i.putExtra(Preferences.DATA_ENGINE_NAME,
					engineManager.getCurrentEngineName());
			startActivityForResult(i, RESULT_PREFERENCES);
			return true;
		}
		case R.id.item_create_database: {
			showDialog(SELECT_CREATE_DATABASE_DIALOG);
			refreshMenu();
			return true;
		}
		case R.id.item_goto_game: {
			showDialog(SELECT_GOTO_GAME_DIALOG);
			return true;
		}
		case R.id.item_random_game: {
			randomGame();
			return true;
		}
		case R.id.item_search: {
			showDialog(SEARCH_DIALOG);
			return true;
		}
		case R.id.item_review_mode: {
			setMode(GameMode.REVIEW_MODE);
			return true;
		}
		case R.id.item_analysis_mode: {
			setMode(GameMode.ANALYSIS_MODE);
			return true;
		}
		case R.id.item_study_mode: {
			setMode(GameMode.STUDY_MODE);
			return true;
		}
		case R.id.item_mode: {
			int id;
			switch (gameMode.getMode()) {
			default: Log.e("SAA", "Impossible game mode"); // no break
			case GameMode.REVIEW_MODE: id = R.id.item_review_mode; break;
			case GameMode.ANALYSIS_MODE: id = R.id.item_analysis_mode; break;
			case GameMode.STUDY_MODE: id = R.id.item_study_mode; break;
			}
			item.getSubMenu().findItem(id).setChecked(true);
			return true;
		}
		case R.id.item_new_game: {
			setDataBaseViewFromFile(true);
			newGame();
			return true;
		}
		case R.id.item_retrieve_position: {
			getFen();
			return true;
		}
		case R.id.item_save_game: {
			saveGame();
			refreshMenu();
			return true;
		}
		case R.id.item_game_deleted:
			invertIsDeleted();
			return true;
		case R.id.item_game_isfavorite:
			invertIsFavorite();
			return true;
		case R.id.item_import_pgn: {
			removeDialog(IMPORT_PGN_DIALOG);
			showDialog(IMPORT_PGN_DIALOG);
			refreshMenu();
			return true;
		}
		case R.id.item_export_pgn: {
			exportPgn();
			return true;
		}
		case R.id.item_gamelist: {
			showGameList();
			return true;
		}
		case R.id.item_paste_clipboard: {
			pasteFromClipboard();
			return true;
		}
		case R.id.item_copy_game_clipboard: {
			copyGameToClipboard();
			return true;
		}
		case R.id.item_copy_position_clipboard: {
			copyPositionToClipboard();
			return true;
		}
		case R.id.item_share_game: {
			shareGame();
			return true;
		}
		case R.id.item_share_position: {
			sharePosition();
			return true;
		}
		case R.id.item_edit_board: {
			editBoard(ctrl.getFEN());
			return true;
		}
		case R.id.item_strip_moves: {
			ctrl.removeSubTree();
			return true;
		}
		case R.id.item_variation_up: {
			ctrl.moveVariation(-1);
			return true;
		}
		case R.id.item_variation_down: {
			ctrl.moveVariation(1);
			return true;
		}
		case R.id.item_add_engine: {
			addEngine();
			return true;
		}
		case R.id.item_remove_engine: {
			removeEngine();
			return true;
		}
		case R.id.item_about: {
			showAboutDialog();
			return true;
		}
		}
		return false;
	}

	private void exportPgn() {
		final EditText input = new EditText(this);
		input.setText(Tools.getScidDirectory() + File.separator + "export.pgn");
		new AlertDialog.Builder(this)
				.setTitle(getText(R.string.export_pgn_title))
				.setMessage(getText(R.string.export_pgn_filename))
				.setView(input)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								String fileName = input.getText().toString()
										.trim();
								File file = new File(new File(fileName)
										.getParent());
								file.mkdirs();
								Intent i = new Intent(ScidAndroidActivity.this,
										ExportPgnActivity.class);
								i.setAction(fileName);
								startActivityForResult(i, RESULT_EXPORT_PGN);
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.create().show();
	}

	private void invertIsDeleted() {
		if (hasNoDataBaseViewOpened())
			return;
		boolean newValue = !getScidAppContext().isDeleted();
		Toast.makeText(this,
				getString(newValue ? R.string.delete_game_success
								   : R.string.undelete_game_success),
				Toast.LENGTH_SHORT).show();
		getScidAppContext().setDeleted(newValue);
		refreshMenu();
		updateStatus();
	}

	private void invertIsFavorite() {
		if (hasNoDataBaseViewOpened())
			return;
		boolean newValue = !getScidAppContext().isFavorite();
		Toast.makeText(this,
				getString(newValue ? R.string.add_favorites_success
								   : R.string.remove_favorites_success),
				Toast.LENGTH_SHORT).show();
		getScidAppContext().setFavorite(newValue);
		setFavoriteRating();
		refreshMenu();
	}

	private void refreshMenu() {
		if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 11) {
			VersionHelper.refreshActionBarMenu(this);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// dynamically change enabled state of some items in game menu
		boolean isSaveEnabled = getScidAppContext().getCurrentFileName()
				.length() > 0;
		boolean isRestOfGameMenuEnabled = currentGameIsValid();
		SubMenu gameMenu = menu.findItem(R.id.item_game).getSubMenu();
		gameMenu.findItem(R.id.item_save_game).setEnabled(isSaveEnabled);
		gameMenu.findItem(R.id.item_game_deleted).setChecked(getScidAppContext().isDeleted());
		gameMenu.findItem(R.id.item_game_deleted).setEnabled(isRestOfGameMenuEnabled);
		gameMenu.findItem(R.id.item_game_isfavorite).setChecked(getScidAppContext().isFavorite());
		gameMenu.findItem(R.id.item_game_isfavorite).setEnabled(isRestOfGameMenuEnabled);
		gameMenu.findItem(R.id.item_goto_game).setEnabled(isRestOfGameMenuEnabled);
		gameMenu.findItem(R.id.item_random_game).setEnabled(isRestOfGameMenuEnabled);
		boolean hasFenProvider = Tools.hasFenProvider(getPackageManager());
		gameMenu.findItem(R.id.item_retrieve_position).setVisible(hasFenProvider);
		if (hasFenProvider) {
			gameMenu.findItem(R.id.item_retrieve_position).setEnabled(
					getScidAppContext().getNoGames() > 0);
		}
		// adapt edit menu
		SubMenu editMenu = menu.findItem(R.id.item_edit).getSubMenu();
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		editMenu.findItem(R.id.item_paste_clipboard).setEnabled(
				clipboard.hasText());
		boolean enableVariationMenuItems = ctrl.numVariations() > 1;
		editMenu.findItem(R.id.item_variation_up).setEnabled(
				enableVariationMenuItems);
		editMenu.findItem(R.id.item_variation_down).setEnabled(
				enableVariationMenuItems);

		return super.onPrepareOptionsMenu(menu);
	}

	private boolean currentGameIsValid() {
		return getScidAppContext()
				.getGameId() >= 0
				&& getScidAppContext().getNoGames() > 0;
	}

	private final void getFen() {
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType("application/x-chess-fen");
		try {
			startActivityForResult(i, RESULT_GET_FEN);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(getApplicationContext(), e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	private void saveGame() {
		Intent i = new Intent(ScidAndroidActivity.this, SaveGameActivity.class);
		startActivityForResult(i, RESULT_SAVE_GAME);
	}

	private void setMode(int mode) {
		if (gameMode.getMode() == mode) {
			Toast.makeText(this, R.string.mode_preserved, Toast.LENGTH_SHORT).show();
			return;
		}
		if (gameMode.analysisMode()) { // clear analysis mode
			ctrl.shutdownEngine();
			gameMode = new GameMode(GameMode.REVIEW_MODE);
			moveListUpdated();
			setGameMode();
			Tools.setKeepScreenOn(this, false);
		}
		if (gameMode.studyMode())
			cancelHumanThinkingTimer();

		gameMode = new GameMode(mode);
		switch (mode) {
		case GameMode.REVIEW_MODE:
			postSetMode(getString(R.string.review_mode_enabled));
			break;
		case GameMode.ANALYSIS_MODE:
			startAnalysis(); // eventually will also call postSetMode(...analysis...)
			break;
		case GameMode.STUDY_MODE:
			resetHumanThinkingTimer();
			postSetMode(getString(R.string.study_mode_enabled));
			break;
		}
	}

	private void postSetMode(String msg) {
		refreshMenu();
		updateThinkingInfo();
		moveListUpdated();
		setGameMode();
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	private void startAnalysis() {
		if (!ctrl.hasEngineStarted()) {
			moveList.setText(R.string.initializing_engine);
			new StartEngineTask().execute(this, ctrl,
					engineManager.getCurrentEngine());
		} else {
			onFinishStartAnalysis();
		}
	}

	protected void onFinishStartAnalysis() {
		postSetMode(getString(R.string.analysis_mode_enabled, engineManager.getCurrentEngineName()));
		ctrl.startGame();
		Tools.setKeepScreenOn(this, true);
	}

	private boolean setGameMode() {
		boolean changed = false;
		if (ctrl.setGameMode(gameMode)) {
			changed = true;
			Editor editor = preferences.edit();
			editor.putInt("gameMode", gameMode.getMode());
			editor.commit();
		}
		return changed;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RESULT_LOAD_SCID_FILE:
			if (resultCode == RESULT_OK && data != null) {
				String fileName = data.getAction();
				if (fileName != null)
					loadScidFile(fileName);
			}
			break;
		case RESULT_PREFERENCES:
			readPrefs();
			String theme = preferences.getString("colorTheme", "0");
			ColorTheme.instance().setTheme(preferences, Integer.parseInt(theme));
			cb.setColors();
			setGameMode();
			break;
		case RESULT_EDITBOARD:
			if (resultCode == RESULT_OK && data != null) {
				try {
					String fen = data.getAction();
					ctrl.setFENOrPGN(fen);
				} catch (ChessParseError e) {
					Log.d(TAG, "ChessParseError", e);
				}
			}
			break;
		case RESULT_SEARCH:
			switch (resultCode) {
			case SearchActivityBase.RESULT_CANCELED:
				// search gives nothing, so keep the previous state
				break;
			case SearchActivityBase.RESULT_SHOW_LIST_AND_KEEP_OLD_GAME:
				// many games found, the current is among them
				showGameList();
				ctrl.updateGUI();
				break;
			case SearchActivityBase.RESULT_SHOW_LIST_AND_GOTO_NEW:
				// many games found, but the current one is not among them
				showGameList();
				setPgnFromDataBaseView();
				break;
			case SearchActivityBase.RESULT_SHOW_SINGLE_NEW:
				// single game was found
				int foundPly = data.getIntExtra("foundPly", -1);
				setPgnFromDataBaseView(foundPly);
				break;
			}
			break;
		case RESULT_GAMELIST:
			if (resultCode == RESULT_OK && data != null) {
				try {
					int gameNo = Integer.parseInt(data.getAction());
					DataBaseView dbv = getDataBaseView();
					if (dbv != null && dbv.moveToPosition(gameNo)) {
						setPgnFromDataBaseView();
					}
				} catch (NumberFormatException nfe) {
					Toast.makeText(getApplicationContext(),
							R.string.invalid_number_format, Toast.LENGTH_SHORT)
							.show();
				}
			}
			break;
		case RESULT_PGN_FILEDIALOG:
			// the result of the file dialog for the pgn import
			if (resultCode == RESULT_OK && data != null) {
				String pgnFileName = data.getAction();
				if (pgnFileName != null) {
					Tools.importPgn(this, pgnFileName, RESULT_PGN_FILE_IMPORT);
				}
			}
			break;
		case RESULT_PGN_FILE_IMPORT:
			// the result after importing the pgn file
			if (resultCode == RESULT_OK && data != null) {
				String pgnFileName = data.getAction();
				if (pgnFileName != null) {
					// open the successfully created scid database
					String scidFileName = Tools.stripExtension(pgnFileName)
							+ ".si4";
					loadScidFile(scidFileName);
				}
			}
			break;
		case RESULT_TWIC_IMPORT:
			// the result of the file dialog for the pgn import after TWIC
			// download
			// twic import has it's own result to delete the pgn file after
			// successful import
			if (resultCode == RESULT_OK && data != null) {
				String pgnFileName = data.getAction();
				if (pgnFileName != null) {
					Tools.importPgn(this,
							Tools.getFullScidFileName(pgnFileName),
							RESULT_PGN_IMPORT);
				}
			}
			break;
		case RESULT_PGN_IMPORT:
			// the result after importing the pgn file
			if (resultCode == RESULT_OK && data != null) {
				String pgnFileName = data.getAction();
				if (pgnFileName != null) {
					new File(pgnFileName).delete();
				}
				String scidFileName = Tools.stripExtension(pgnFileName)
						+ ".si4";
				loadScidFile(scidFileName);
			}
			break;
		case RESULT_GET_FEN:
			// the result when getting FEN from other called chess program
			if (resultCode == RESULT_OK) {
				receivedFen(data);
			}
			break;
		case RESULT_EXPORT_PGN:
			String message;
			if (resultCode == RESULT_OK) {
				message = getString(R.string.pgn_export_success,
						getDataBaseView().getCount());
			} else {
				message = getString(R.string.pgn_export_failure);
			}
			new AlertDialog.Builder(this)
					.setTitle(getString(R.string.export_pgn_title))
					.setPositiveButton(android.R.string.ok, null)
					.setMessage(message).show();
			break;
		case RESULT_ADD_ENGINE:
			if (resultCode == AddEngineActivity.RESULT_EXECUTABLE_EXISTS
					&& data != null) {
				String engineName = data
						.getStringExtra(AddEngineActivity.DATA_ENGINE_NAME);
				String executable = data
						.getStringExtra(AddEngineActivity.DATA_ENGINE_EXECUTABLE);
				String enginePackage = data.getStringExtra(AddEngineActivity.DATA_ENGINE_PACKAGE);
				int engineVersion = data.getIntExtra(AddEngineActivity.DATA_ENGINE_VERSION, 0);
				boolean makeCurrentEngine = data.getBooleanExtra(
						AddEngineActivity.DATA_MAKE_CURRENT_ENGINE, false);
				if (executable == null) {
					Toast.makeText(getApplicationContext(),
							getText(R.string.no_engine_selected),
							Toast.LENGTH_LONG).show();
				} else {
					addNewEngine(engineName, executable, enginePackage, engineVersion,
							makeCurrentEngine, false);
				}
			}
			break;
		case RESULT_REMOVE_ENGINE:
			if (resultCode == RESULT_OK && data != null) {
				final String _engineName = data
						.getStringExtra(RemoveEngineActivity.DATA_ENGINE_NAME);
				if (_engineName != null && _engineName.length() > 0) {
					// Update preferences if removing current analysis engine
					String analysisEngine = preferences.getString(
							ANALYSIS_ENGINE, null);
					if (analysisEngine != null
							&& analysisEngine.equals(_engineName)) {
						final EngineManager.EngineChangeListener _listener = new EngineManager.EngineChangeListener() {

							@Override
							public void engineChanged(EngineChangeEvent event) {
								if (_engineName.equals(event.getEngineName())
										&& event.getChangeType() == EngineChangeEvent.REMOVE_ENGINE
										&& event.getSuccess()) {
									Editor editor = preferences.edit();
									editor.putString(ANALYSIS_ENGINE,
											engineManager.getDefaultEngine()
													.getName());
									editor.commit();
								}
								engineManager.removeEngineChangeListener(this);
							}
						};
						engineManager.addEngineChangeListener(_listener);

					}
					engineManager.removeEngine(_engineName);
				}
			}
			break;
		case RESULT_SAVE_GAME:
			if (resultCode == RESULT_OK && data != null) {
				String gameNoString = data.getAction();
				if (gameNoString != null) {
					int gameNo = Integer.parseInt(gameNoString);
					if (gameNo == -1) {
						// a new game was added
						// TODO: reset dbv for now - saving should be done
						// within the
						// reset data provider and dbv
						DataBaseView dbv = setDataBaseViewFromFile(true);
						// move to newly added game
						if (dbv != null && dbv.moveToLast()) {
							setPgnFromDataBaseView();
						}
					}
				}
			}
			break;
		}
	}

	private void receivedFen(Intent data) {
		String fen = data.getStringExtra(Intent.EXTRA_TEXT);
		if (fen == null) {
			String pathName = getFilePathFromUri(data.getData());
			if (pathName != null) {
				InputStream is = null;
				try {
					is = new FileInputStream(pathName);
					fen = Tools.readFromStream(is);
				} catch (FileNotFoundException e) {
					Toast.makeText(getApplicationContext(), e.getMessage(),
							Toast.LENGTH_LONG).show();
				} finally {
					if (is != null)
						try {
							is.close();
						} catch (IOException e) {
						}
				}
			}
		}
		if (fen != null) {
			try {
				ctrl.setFENOrPGN(fen);
			} catch (ChessParseError e) {
				// If FEN corresponds to illegal chess position, go into edit
				// board mode.
				try {
					TextIO.readFEN(fen);
				} catch (ChessParseError e2) {
					if (e2.pos != null)
						editBoard(fen);
				}
			}
		}
	}

	private String getFilePathFromUri(Uri uri) {
		if (uri == null)
			return null;
		return uri.getPath();
	}

	private void loadScidFile(String fileName) {
		Editor editor = preferences.edit();
		editor.putString("currentScidFile", fileName);
		editor.commit();
		getScidAppContext().setCurrentFileName(fileName);

		DataBaseView dbv = setDataBaseViewFromFile();
		if (dbv != null && restoreCurrentGameId()) {
			setPgnFromDataBaseView();
		} else {
			newGame();
		}
	}

	private void addNewEngine(final String engineName, String executable,
			final String enginePackage, final int engineVersion, boolean makeCurrentEngine, boolean copied) {
		if (makeCurrentEngine) {
			final EngineManager.EngineChangeListener _listener = new EngineManager.EngineChangeListener() {

				@Override
				public void engineChanged(EngineChangeEvent event) {
					if (engineName.equals(event.getEngineName())
							&& event.getChangeType() == EngineChangeEvent.ADD_ENGINE
							&& event.getSuccess()) {
						Editor editor = preferences.edit();
						editor.putString(ANALYSIS_ENGINE, engineName);
						editor.putString("analysisEnginePackage", enginePackage);
						editor.putInt("analysisEngineVersion", engineVersion);
						editor.commit();
						engineManager.setCurrentEngineName(engineName);
					}
					engineManager.removeEngineChangeListener(this);
				}
			};
			engineManager.addEngineChangeListener(_listener);
		}
		engineManager.addEngine(engineName, executable, enginePackage, engineVersion);
	}

	private final void setFlipped(boolean flipped) {
		if(flipped != cb.flipped){
			cb.setFlipped(flipped);
			ImageButton flip_button = (ImageButton)findViewById(R.id.flip_board);
			flip_button.setImageResource(flipped ? R.drawable.flip_flipped : R.drawable.flip_normal);
			Editor editor = preferences.edit();
			editor.putBoolean("boardFlipped", flipped);
			editor.commit();
		}
	}

	@Override
	public void setSelection(int sq) {
		cb.setSelection(sq);
	}

	@Override
	public void setFromSelection(int sq) {
		cb.setFromSelection(sq);
	}

	@Override
	public void setStatusString(final String str) {
		String prefix = "";
		if (getScidAppContext().isDeleted()) {
			prefix = "<font color='red'><b>DELETED</b></font> ";
		}
		status.setText(Html.fromHtml(prefix + str),
				TextView.BufferType.SPANNABLE);
	}

	private void updateStatus() {
		String statusStr = status.getText().toString();
		if (statusStr.startsWith("DELETED ")) {
			statusStr = statusStr.substring(8);
		}
		setStatusString(statusStr);
	}

	@Override
	public void moveListUpdated() {
		if (gameMode.studyMode()) {
			moveList.setText("");
		} else if (!gameMode.analysisMode()) {
			if (pgnOptions.view.allMoves) {
				moveList.setText(gameTextListener.getSpannableData());
			} else {
				boolean whiteMove = false;
				Position position = this.getScidAppContext().getPosition();
				if (position != null) {
					whiteMove = !position.whiteMove;
				}
				moveList.setText(gameTextListener
						.getCurrentSpannableData(whiteMove));
			}
			Tools.bringPointtoView(moveList, moveListScroll,
					gameTextListener.getCurrentPosition());
		}
		if (gameTextListener.atEnd()) {
			moveListScroll.fullScroll(View.FOCUS_DOWN);
		}
	}

	@Override
	public void setPosition(Position pos, String variantInfo,
			List<Move> variantMoves) {
		variantStr = variantInfo;
		this.variantMoves = variantMoves;
		cb.setPosition(pos);
		((ScidApplication) getApplicationContext()).setPosition(pos);
		View view = findViewById(R.id.moveindicator);
		if (view != null) {
			view.invalidate();
		}
		updateThinkingInfo();
	}

	private String thinkingStr = "";
	private String bookInfoStr = "";
	private String variantStr = "";
	private List<Move> pvMoves = null;
	private List<Move> bookMoves = null;
	private List<Move> variantMoves = null;

	@Override
	public void setThinkingInfo(String pvStr, String bookInfo,
			List<Move> pvMoves, List<Move> bookMoves) {
		thinkingStr = pvStr;
		bookInfoStr = bookInfo;
		this.pvMoves = pvMoves;
		this.bookMoves = bookMoves;
		updateThinkingInfo();
	}

	private final void updateThinkingInfo() {
		boolean thinkingEmpty = true;
		{
			String s = "";
			if (mShowThinking || gameMode.analysisMode()) {
				s = thinkingStr;
			}
			if (s.length() > 0) {
				thinkingEmpty = false;
				moveList.setText(s, TextView.BufferType.SPANNABLE);
			}
		}
		if (mShowBookHints && (bookInfoStr.length() > 0)) {
			String s = "";
			if (!thinkingEmpty) {
				s += "<br>";
			}
			s += "<b>Book:</b>" + bookInfoStr;
			moveList.append(Html.fromHtml(s));
			thinkingEmpty = false;
		}
		if (variantStr.indexOf(' ') >= 0 && !gameMode.studyMode()) {
			String s = "";
			if (!thinkingEmpty) {
				s += "<br>";
			}
			s += "<b>Var:</b> " + variantStr;
			moveList.append(Html.fromHtml(s));
		}

		List<Move> hints = null;
		if (!gameMode.studyMode()) {
			if (mShowThinking) {
				hints = pvMoves;
			}
			if ((variantMoves != null) && variantMoves.size() > 1) {
				hints = variantMoves;
			}
			if ((hints != null) && (hints.size() > maxNumArrows)) {
				hints = hints.subList(0, maxNumArrows);
			}
		}
		cb.setMoveHints(hints);
	}

	static final int PROMOTE_DIALOG = 0;
	static final int CLIPBOARD_DIALOG = 1;
	static final int SELECT_GOTO_GAME_DIALOG = 3;
	static final int SELECT_CREATE_DATABASE_DIALOG = 4;
	static final int SEARCH_DIALOG = 5;
	static final int IMPORT_PGN_DIALOG = 6;
	static final int MOVELIST_MENU_DIALOG = 9;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case MOVELIST_MENU_DIALOG:
			MoveListDialog dialog = new MoveListDialog(this);
			return dialog.create(ctrl);
		case PROMOTE_DIALOG:
			PromoteDialog promoteDialog = new PromoteDialog(this);
			return promoteDialog.create(ctrl);
		case SEARCH_DIALOG:
			return createSearchDialog();
		case CLIPBOARD_DIALOG:
			return createClipboardDialog();
		case SELECT_GOTO_GAME_DIALOG:
			return createGotoGameDialog();
		case SELECT_CREATE_DATABASE_DIALOG:
			return createCreateDatabaseDialog();
		case IMPORT_PGN_DIALOG:
			return createImportDialog();
		}
		return null;
	}

	private Dialog createGotoGameDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.goto_game_title);
		builder.setMessage(R.string.goto_game_number);
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		builder.setView(input);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						try {
							int gameNo = Integer.parseInt(input.getText()
									.toString()) - 1;
							DataBaseView dbv = setDataBaseViewFromFile();
							if (dbv != null && dbv.moveToPosition(gameNo)) {
								setPgnFromDataBaseView();
							}
						} catch (NumberFormatException nfe) {
							Toast.makeText(getApplicationContext(),
									R.string.invalid_number_format,
									Toast.LENGTH_SHORT).show();
						}
					}
				});
		builder.setNegativeButton(android.R.string.cancel, null);
		AlertDialog alert = builder.create();
		return alert;
	}

	private void showAboutDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.app_name).setMessage(R.string.about_info);
		AlertDialog dialog = builder.create();
		dialog.show();
		// Make the textview clickable. Must be called after show()
		((TextView) dialog.findViewById(android.R.id.message))
				.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private AlertDialog createCreateDatabaseDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		builder.setTitle(getText(R.string.create_db_title));
		builder.setMessage(getText(R.string.create_db_message));
		builder.setView(input);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						createDatabase(value);
					}
				});

		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		return alert;
	}

	private boolean hasNoDataBaseViewOpened() {
		if (getScidAppContext().getDataBaseView() == null) {
			Toast.makeText(ScidAndroidActivity.this, R.string.err_no_db,
					Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	private AlertDialog createSearchDialog() {
		final int RESET_FILTER = 0;
		final int SEARCH_CURRENT_BOARD = 1;
		final int SEARCH_HEADER = 2;
		final int SHOW_FAVORITES = 3;
		List<CharSequence> lst = new ArrayList<CharSequence>();
		List<Integer> actions = new ArrayList<Integer>();
		lst.add(getString(R.string.reset_filter));
		actions.add(RESET_FILTER);
		lst.add(getString(R.string.search_current_board));
		actions.add(SEARCH_CURRENT_BOARD);
		lst.add(getString(R.string.search_header));
		actions.add(SEARCH_HEADER);
		lst.add(getString(R.string.favorites));
		actions.add(SHOW_FAVORITES);
		final List<Integer> finalActions = actions;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.search);
		builder.setItems(lst.toArray(new CharSequence[lst.size()]),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {
						if (hasNoDataBaseViewOpened())
							return;
						switch (finalActions.get(item)) {
						case RESET_FILTER: {
							resetFilter();
							break;
						}
						case SEARCH_CURRENT_BOARD: {
							Intent i = new Intent(ScidAndroidActivity.this,
									SearchBoardActivity.class);
							i.setAction(ctrl.getFEN());
							startActivityForResult(i, RESULT_SEARCH);
							break;
						}
						case SEARCH_HEADER: {
							Intent i = new Intent(ScidAndroidActivity.this,
									SearchHeaderActivity.class);
							startActivityForResult(i, RESULT_SEARCH);
							break;
						}
						case SHOW_FAVORITES: {
							Intent i = new Intent(ScidAndroidActivity.this,
									SearchFavoritesActivity.class);
							startActivityForResult(i, RESULT_SEARCH);
							break;
						}
						}
					}
				});
		AlertDialog alert = builder.create();
		return alert;
	}

	private AlertDialog createClipboardDialog() {
		final int COPY_GAME = 0;
		final int COPY_POSITION = 1;
		final int PASTE = 2;
		final int EDIT_BOARD = 3;
		final int ADD_FAVORITES = 4;
		final int REMOVE_FAVORITES = 5;
		final int SAVE_GAME = 6;
		final int DELETE_GAME = 7;
		final int UNDELETE_GAME = 8;
		final int GET_FEN = 9;

		List<CharSequence> lst = new ArrayList<CharSequence>();
		List<Integer> actions = new ArrayList<Integer>();
		// check if "add to favorites" or "remove from favorites" is needed
		if (currentGameIsValid()) {
			if (!getScidAppContext().isFavorite()) {
				lst.add(getString(R.string.add_favorites));
				actions.add(ADD_FAVORITES);
			} else {
				lst.add(getString(R.string.remove_favorites));
				actions.add(REMOVE_FAVORITES);
			}
		}
		if (getScidAppContext().getCurrentFileName().length() > 0) {
			lst.add(getString(R.string.save_game));
			actions.add(SAVE_GAME);
		}
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		if (clipboard.hasText()) {
			lst.add(getString(R.string.paste));
			actions.add(PASTE);
		}
		lst.add(getString(R.string.copy_game));
		actions.add(COPY_GAME);
		lst.add(getString(R.string.copy_position));
		actions.add(COPY_POSITION);
		lst.add(getString(R.string.edit_board));
		actions.add(EDIT_BOARD);
		if (currentGameIsValid()) {
			if (!getScidAppContext().isDeleted()) {
				lst.add(getString(R.string.delete_game));
				actions.add(DELETE_GAME);
			} else {
				lst.add(getString(R.string.undelete_game));
				actions.add(UNDELETE_GAME);
			}
		}
		if (Tools.hasFenProvider(getPackageManager())) {
			lst.add(getString(R.string.get_fen));
			actions.add(GET_FEN);
		}
		final List<Integer> finalActions = actions;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.tools_menu);
		builder.setItems(lst.toArray(new CharSequence[lst.size()]),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {
						switch (finalActions.get(item)) {
						case GET_FEN:
							getFen();
							break;
						case SAVE_GAME:
							saveGame();
							break;
						case EDIT_BOARD:
							editBoard(ctrl.getFEN());
							break;
						case COPY_GAME:
							copyGameToClipboard();
							break;
						case COPY_POSITION:
							copyPositionToClipboard();
							break;
						case PASTE:
							pasteFromClipboard();
							break;
						case ADD_FAVORITES:
						case REMOVE_FAVORITES:
							invertIsFavorite();
							break;
						case DELETE_GAME:
						case UNDELETE_GAME:
							invertIsDeleted();
							break;
						}
					}
				});
		AlertDialog alert = builder.create();
		return alert;
	}

	private AlertDialog createImportDialog() {
		final int IMPORT_PGN_FILE = 0;
		final int IMPORT_TWIC = 1;
		final int IMPORT_CHESSOK = 2;

		List<CharSequence> lst = new ArrayList<CharSequence>();
		List<Integer> actions = new ArrayList<Integer>();
		lst.add(getString(R.string.import_pgn_file));
		actions.add(IMPORT_PGN_FILE);
		lst.add(getString(R.string.import_twic));
		actions.add(IMPORT_TWIC);
		lst.add(getString(R.string.import_chessok));
		actions.add(IMPORT_CHESSOK);
		final List<Integer> finalActions = actions;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.import_pgn_title);
		builder.setItems(lst.toArray(new CharSequence[lst.size()]),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {
						switch (finalActions.get(item)) {
						case IMPORT_PGN_FILE:
							importPgnFile();
							break;
						case IMPORT_TWIC:
							importTwic();
							break;
						case IMPORT_CHESSOK:
							importChessOk();
							break;
						}
					}
				});
		AlertDialog alert = builder.create();
		return alert;
	}

	private void editBoard(String fen) {
		Intent i = new Intent(ScidAndroidActivity.this, EditBoard.class);
		i.setAction(fen);
		startActivityForResult(i, RESULT_EDITBOARD);
	}

	private void showGameList() {
		if (hasNoDataBaseViewOpened())
			return;
		Intent i = new Intent(this, GameListActivity.class);
		startActivityForResult(i, RESULT_GAMELIST);
	}

	private void pasteFromClipboard() {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		if (clipboard.hasText()) {
			String fenPgn = clipboard.getText().toString()
					.replaceAll("\n|\r|\t", " ");
			try {
				ctrl.setFENOrPGN(fenPgn);
			} catch (ChessParseError e) {
				Log.i(TAG, "ChessParseError", e);
				Toast.makeText(getApplicationContext(), e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void copyPositionToClipboard() {
		String fen = ctrl.getFEN() + "\n";
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		clipboard.setText(fen);
	}

	private void copyGameToClipboard() {
		String pgn = ctrl.getPGN();
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		clipboard.setText(pgn);
	}

	private void shareGame() {
		String data = ctrl.getPGN();
		if (data != null) {
			Sharer sharer = new Sharer(this, getComponentName());
			sharer.createShareIntent("application/x-chess-pgn", data);
			if (sharer.getIntent() != null) {
				startActivity(sharer.getIntent());
			}
		}
	}

	private void sharePosition() {
		Sharer sharer = new Sharer(this, getComponentName());
		sharer.createShareIntent("application/x-chess-fen", ctrl.getFEN());
		if (sharer.getIntent() != null) {
			startActivity(sharer.getIntent());
		}
	}

	private void createDatabase(String fileName) {
		String scidFileName = Tools.getFullScidFileName(fileName);
		if (new File(scidFileName + ".si4").exists()) {
			Toast.makeText(
					getApplicationContext(),
					String.format(getString(R.string.create_db_exists),
							fileName), Toast.LENGTH_LONG).show();
		} else {
			String result = DataBase.create(scidFileName);
			if (result.length() > 0) {
				Toast.makeText(getApplicationContext(), result,
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getApplicationContext(),
						getString(R.string.create_db_success),
						Toast.LENGTH_SHORT).show();
				Editor editor = preferences.edit();
				editor.putString("currentScidFile", scidFileName);
				editor.commit();
				getScidAppContext().setCurrentFileName(scidFileName);
				setDataBaseViewFromFile();
				newGame();
			}
		}
	}

	private void newGame() {
		refreshMenu();
		saveCurrentGameId();
		ctrl.newGame(gameMode);
		getScidAppContext().setFavorite(false);
		getScidAppContext().setDeleted(false);
		ctrl.setGuiPaused(true);
		ctrl.setGuiPaused(false);
		ctrl.startGame();
		setFavoriteRating();
	}

	/** Restores gameId for current file from preferences or go to the first game
	 *	if the id in preferences is invalid. Assumes that dbv is not null. */
	private boolean restoreCurrentGameId() {
		DataBaseView dbv = getDataBaseView();
		int gameId = preferences.getInt("currentGameId " + getScidAppContext().getCurrentFileName(), 0);
		return dbv.moveToId(gameId) || dbv.moveToFirst();
	}

	private void saveCurrentGameId() {
		int gameId = getScidAppContext().getGameId();
		if (gameId < 0)
			return;
		Editor editor = preferences.edit();
		editor.putInt("currentGameId " + getScidAppContext().getCurrentFileName(), gameId);
		editor.commit();
	}

	private void resetFilter() {
		final String fileName = getScidAppContext().getCurrentFileName();
		if (fileName.length() != 0) {
			DataBaseView dbv = setDataBaseViewFromFile();
			if (dbv != null) {
				dbv.setFilter(null, true);
				setPgnFromDataBaseView();
			}
		} else {
			getScidAppContext().setDataBaseView(null);
		}
	}

	private void importPgnFile() {
		Intent i = new Intent(ScidAndroidActivity.this,
				SelectFileActivity.class);
		i.setAction(".pgn");
		startActivityForResult(i, RESULT_PGN_FILEDIALOG);
	}

	private void importTwic() {
		Intent i = new Intent(ScidAndroidActivity.this,
				ImportTwicActivity.class);
		startActivityForResult(i, RESULT_TWIC_IMPORT);
	}

	private void importChessOk() {
		Intent i = new Intent(ScidAndroidActivity.this,
				ImportChessOkActivity.class);
		startActivityForResult(i, RESULT_TWIC_IMPORT);
	}

	private void addEngine() {
		Intent i = new Intent(ScidAndroidActivity.this, AddEngineActivity.class);
		startActivityForResult(i, RESULT_ADD_ENGINE);
	}

	private void removeEngine() {
		Intent i = new Intent(ScidAndroidActivity.this,
				RemoveEngineActivity.class);
		String[] engineNames = engineManager.getEngineNames(false);
		i.putExtra(RemoveEngineActivity.DATA_ENGINE_NAMES, engineNames);
		startActivityForResult(i, RESULT_REMOVE_ENGINE);
	}

	private DataBaseView getDataBaseView() {
		DataBaseView dbv = getScidAppContext().getDataBaseView();
		if (dbv == null) {
			dbv = setDataBaseViewFromFile();
		}
		return dbv;
	}

	private DataBaseView setDataBaseViewFromFile() {
		return setDataBaseViewFromFile(false);
	}

	/**
	 * Set the database view from the current scid file
	 * @param alwaysResetDbView if set to true always reset the dbv,
	 *	   if set to false only reset the dbv if the file name changes or the current dbv is null
	 */
	private DataBaseView setDataBaseViewFromFile(boolean alwaysResetDbView) {
		final String currentScidFile = preferences.getString("currentScidFile", "");
		if (currentScidFile.length() == 0) {
			return null;
		}
		String scidFileName = Tools.stripExtension(currentScidFile);
		DataBaseView dbv = getScidAppContext().getDataBaseView();
		if (alwaysResetDbView || dbv == null
				|| !scidFileName.equals(dbv.getFileName())) {
			dbv = new DataBaseView(scidFileName);
		} else { // The file is already loaded
			if (!dbv.reloadFile())
				dbv = null;
		}
		getScidAppContext().setDataBaseView(dbv);
		return dbv;
	}

	@Override
	public void requestPromotePiece() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showDialog(PROMOTE_DIALOG);
			}
		});
	}

	private boolean invalidMoveReported;
	@Override
	public void reportInvalidMove(Move m) {
		String msg = "";
		if (gameMode.studyMode()) {
			msg = String.format(getString(R.string.wrong_move),
					TextIO.squareToString(m.from), TextIO.squareToString(m.to));
		} else {
			msg = String.format(getString(R.string.invalid_move),
					TextIO.squareToString(m.from), TextIO.squareToString(m.to));
		}
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		invalidMoveReported = true;
	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		runOnUiThread(runnable);
	}

	@Override
	public void setGameInformation(String white, String black, String gameNo) {
		if (!gameMode.studyMode() || studyOrientation == SO_MY_PLAYER) {
			flipBoardForPlayerNames(white, black);
		}
		if (whitePlayer != null) {
			whitePlayer.setText(white);
			blackPlayer.setText(black);
			this.gameNo.setText(gameNo);
		} else {
			setTitle(gameNo + "	  " + white + " - " + black);
		}
	}

	@Override
	public ScidApplication getScidAppContext() {
		return (ScidApplication) getApplicationContext();
	}

	@Override
	public boolean onSearchRequested() {
		showDialog(SEARCH_DIALOG);
		return true;
	}

	/** Report a move made that is a candidate for GUI animation. */
	@Override
	public void setAnimMove(Position sourcePos, Move move, boolean forward) {
		if (move != null) {
			cb.setAnimMove(sourcePos, move, forward,
					Double.valueOf(preferences.getString("animationSpeed", "10")));
		}
	}

	@Override
	public void clipboardChanged() {
		refreshMenu();
	}

	@Override
	public void downloadSuccess(File pgnFile) {
		Tools.importPgnFile(ScidAndroidActivity.this, pgnFile,
				RESULT_PGN_IMPORT);
	}

	@Override
	public void downloadFailure(String message) {
		Tools.showErrorMessage(this, this.getText(R.string.download_error)
				+ " (" + message + ")");
	}
}