package org.scid.android;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.scid.android.gamelogic.ChessController;
import org.scid.android.gamelogic.ChessParseError;
import org.scid.android.gamelogic.Move;
import org.scid.android.gamelogic.Position;
import org.scid.android.gamelogic.TextIO;
import org.scid.database.ScidProviderMetaData;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Html;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ScidAndroidActivity extends Activity implements GUIInterface {

	private ChessBoard cb;
	private ChessController ctrl = null;
	private boolean mShowThinking;
	private boolean mShowBookHints;
	private int maxNumArrows;
	private boolean inStudyMode = false;
	private GameMode gameMode = new GameMode(GameMode.TWO_PLAYERS);
	private boolean boardFlipped = false;
	private boolean autoSwapSides = false;

	private TextView status;
	private ScrollView moveListScroll;
	private TextView moveList;
	private TextView whitePlayer;
	private TextView blackPlayer;
	private TextView gameNo;

	SharedPreferences settings;

	private float scrollSensitivity = 2;
	private boolean invertScrollDirection = false;

	private final String scidDir = "scid";
	private PGNOptions pgnOptions = new PGNOptions();

	PgnScreenText gameTextListener;
	private String myPlayerNames = "";
	private String lastWhitePlayerName = "";
	private String lastBlackPlayerName = "";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		File scidFileDir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + scidDir);
		if (!scidFileDir.exists()) {
			scidFileDir.mkdirs();
		}

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings
				.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
					@Override
					public void onSharedPreferenceChanged(
							SharedPreferences sharedPreferences, String key) {
						readPrefs();
						ctrl.setGameMode(gameMode);
					}
				});

		initUI(true);

		gameTextListener = new PgnScreenText(pgnOptions);
		ctrl = new ChessController(this, gameTextListener, pgnOptions);
		ctrl.newGame(new GameMode(GameMode.TWO_PLAYERS));
		readPrefs();
		ctrl.newGame(gameMode);

		int gameNo = settings.getInt("currentGameNo", 0);
		Cursor cursor = getCursor();
		if (cursor != null && cursor.moveToPosition(gameNo)) {
			this.getScidAppContext().setCurrentGameNo(gameNo);
		}

		byte[] data = null;
		if (savedInstanceState != null) {
			data = savedInstanceState.getByteArray("gameState");
		} else {
			String dataStr = settings.getString("gameState", null);
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
	}

	public void onNextGameClick(View view) {
		Cursor cursor = this.getScidAppContext().getGamesCursor();
		if (cursor == null) {
			cursor = this.getCursor();
		}
		if (cursor != null) {
			startManagingCursor(cursor);
			if (cursor.isBeforeFirst()) {
				cursor.moveToFirst();
			}
			if (cursor.moveToNext()) {
				setPgnFromCursor(cursor);
			}
		}
	}

	private void setPgnFromCursor(Cursor cursor) {
		this.getScidAppContext().setCurrentGameNo(
				cursor.getInt(cursor.getColumnIndex("_id")));
		Editor editor = settings.edit();
		editor.putInt("currentGameNo", this.getScidAppContext()
				.getCurrentGameNo());
		editor.commit();
		String pgn = cursor.getString(cursor
				.getColumnIndex(ScidProviderMetaData.ScidMetaData.PGN));
		if (pgn != null && pgn.length() > 0) {
			try {
				ctrl.setFENOrPGN(pgn);
				int moveNo = cursor
						.getInt(cursor
								.getColumnIndex(ScidProviderMetaData.ScidMetaData.CURRENT_PLY));
				if (moveNo > 0) {
					ctrl.gotoHalfMove(moveNo);
				}
				saveGameState();
			} catch (ChessParseError e) {
				Toast.makeText(getApplicationContext(), e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
		}
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
				String playerName = names[i];
				if (white.equalsIgnoreCase(playerName) && boardFlipped) {
					boardFlipped = !boardFlipped;
					setBoardFlip();
					break;
				} else if (black.equalsIgnoreCase(playerName) && !boardFlipped) {
					boardFlipped = !boardFlipped;
					setBoardFlip();
					break;
				}
			}
		}
	}

	public void onPreviousGameClick(View view) {
		Cursor cursor = this.getScidAppContext().getGamesCursor();
		if (cursor == null) {
			cursor = this.getCursor();
		}
		if (cursor != null) {
			startManagingCursor(cursor);
			if (cursor.isAfterLast()) {
				cursor.moveToLast();
			}
			if (cursor.moveToPrevious()) {
				setPgnFromCursor(cursor);
			}
		}
	}

	public void onNextMoveClick(View view) {
		if (ctrl.canRedoMove()) {
			ctrl.redoMove();
		}
	}

	public void onPreviousMoveClick(View view) {
		ctrl.undoMove();
	}

	public void onFlipBoardClick(View view) {
		boardFlipped = !boardFlipped;
		Editor editor = settings.edit();
		editor.putBoolean("boardFlipped", boardFlipped);
		editor.commit();
		setBoardFlip();
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
		initUI(false);
		readPrefs();
		cb.cursorX = oldCB.cursorX;
		cb.cursorY = oldCB.cursorY;
		cb.cursorVisible = oldCB.cursorVisible;
		cb.setPosition(oldCB.pos);
		cb.setFlipped(oldCB.flipped);
		cb.oneTouchMoves = oldCB.oneTouchMoves;
		setSelection(oldCB.selectedSquare);
		updateThinkingInfo();
		setStatusString(statusStr);
		moveListUpdated();
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
		status = (TextView) findViewById(R.id.status);
		moveListScroll = (ScrollView) findViewById(R.id.scrollView);
		moveList = (TextView) findViewById(R.id.moveList);
		status.setFocusable(false);
		moveListScroll.setFocusable(false);
		moveList.setFocusable(false);

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
						if (ctrl.humansTurn()) {
							int sq = cb.eventToSquare(e);
							Move m = cb.mousePressed(sq);
							if (m != null) {
								ctrl.makeHumanMove(m);
							}
						}
					}
				});
		cb.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gd.onTouchEvent(event);
			}
		});
		cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
			@Override
			public void onTrackballEvent(MotionEvent event) {
				if (ctrl.humansTurn()) {
					Move m = cb.handleTrackballEvent(event);
					if (m != null) {
						ctrl.makeHumanMove(m);
					}
				}
			}
		});
		cb.setOnLongClickListener(new OnLongClickListener() {
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
				ctrl.gotoMove(999);
				return true;
			}
		});
		ImageButton previousButton = (ImageButton) findViewById(R.id.previous_move);
		previousButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				ctrl.gotoMove(0);
				return true;
			}
		});
		ImageButton nextGameButton = (ImageButton) findViewById(R.id.next_game);
		nextGameButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Cursor cursor = getScidAppContext().getGamesCursor();
				if (cursor == null) {
					cursor = getCursor();
				}
				if (cursor != null) {
					startManagingCursor(cursor);
					if (cursor.moveToLast()) {
						setPgnFromCursor(cursor);
					}
				}
				return true;
			}
		});
		ImageButton previousGameButton = (ImageButton) findViewById(R.id.previous_game);
		previousGameButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Cursor cursor = getScidAppContext().getGamesCursor();
				if (cursor == null) {
					cursor = getCursor();
				}
				if (cursor != null) {
					startManagingCursor(cursor);
					if (cursor.moveToFirst()) {
						setPgnFromCursor(cursor);
					}
				}
				return true;
			}
		});
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
		if (ctrl != null) {
			ctrl.setGuiPaused(false);
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (ctrl != null) {
			ctrl.setGuiPaused(true);
			saveGameState();
		}
		super.onPause();
	}

	private void saveGameState() {
		byte[] data = ctrl.toByteArray();
		Editor editor = settings.edit();
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
		boardFlipped = settings.getBoolean("boardFlipped", false);
		this.myPlayerNames = settings.getString("playerNames", "");
		setBoardFlip();
		cb.oneTouchMoves = settings.getBoolean("oneTouchMoves", false);
		mShowThinking = settings.getBoolean("showThinking", false);
		String tmp = settings.getString("thinkingArrows", "2");
		maxNumArrows = Integer.parseInt(tmp);
		mShowBookHints = settings.getBoolean("bookHints", false);
		inStudyMode = settings.getBoolean("inStudyMode", false);

		ctrl.setTimeLimit(300000, 60, 0);

		setFullScreenMode(true);

		tmp = settings.getString("fontSize", "12");
		int fontSize = Integer.parseInt(tmp);
		status.setTextSize(fontSize);
		moveList.setTextSize(fontSize);

		updateThinkingInfo();

		pgnOptions.view.variations = true;
		pgnOptions.view.comments = true;
		pgnOptions.view.nag = true;
		pgnOptions.view.headers = false;
		pgnOptions.view.allMoves = true;
		String moveDisplayString = settings.getString("moveDisplay", "0");
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

		ColorTheme.instance().readColors(settings);
		cb.setColors();

		final String currentScidFile = settings
				.getString("currentScidFile", "");
		if (currentScidFile.length() > 0) {
			this.getScidAppContext().setCurrentFileName(
					getFullScidFileName(currentScidFile));
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
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}

	static private final int RESULT_EDITBOARD = 0;
	static private final int RESULT_SETTINGS = 1;
	static private final int RESULT_SEARCH = 2;
	static private final int RESULT_TWIC_IMPORT = 3;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_open_file:
			removeDialog(SELECT_SCID_FILE_DIALOG);
			showDialog(SELECT_SCID_FILE_DIALOG);
			return true;
			/*
			 * TODO enable when saving to scid database case R.id.item_new_game:
			 * if (autoSwapSides && (gameMode.playerWhite() !=
			 * gameMode.playerBlack())) { int gameModeType; if
			 * (gameMode.playerWhite()) { gameModeType = GameMode.PLAYER_BLACK;
			 * } else { gameModeType = GameMode.PLAYER_WHITE; } Editor editor =
			 * settings.edit(); String gameModeStr = String.format("%d",
			 * gameModeType); editor.putString("gameMode", gameModeStr);
			 * editor.commit(); gameMode = new GameMode(gameModeType); }
			 * ctrl.newGame(gameMode); ctrl.startGame(); return true;
			 */
		case R.id.item_editboard: {
			Intent i = new Intent(ScidAndroidActivity.this, EditBoard.class);
			i.setAction(ctrl.getFEN());
			startActivityForResult(i, RESULT_EDITBOARD);
			return true;
		}
		case R.id.item_settings: {
			Intent i = new Intent(ScidAndroidActivity.this, Preferences.class);
			startActivityForResult(i, RESULT_SETTINGS);
			return true;
		}
		case R.id.item_goto_move: {
			showDialog(SELECT_MOVE_DIALOG);
			return true;
		}
		case R.id.item_goto_game: {
			showDialog(SELECT_GOTO_GAME_DIALOG);
			return true;
		}
		case R.id.item_search: {
			showDialog(SEARCH_DIALOG);
			return true;
		}
		case R.id.item_study_mode: {
			setStudyMode();
			return true;
		}
		case R.id.item_import_pgn: {
			removeDialog(IMPORT_PGN_DIALOG);
			showDialog(IMPORT_PGN_DIALOG);
			return true;
		}
		case R.id.item_import_twic: {
			importTwic();
			return true;
		}
			/*
			 * TODO enable when saving to scid database case R.id.item_draw: {
			 * if (ctrl.humansTurn()) { if (!ctrl.claimDrawIfPossible()) {
			 * Toast.makeText(getApplicationContext(), R.string.offer_draw,
			 * Toast.LENGTH_SHORT).show(); } } return true; } case
			 * R.id.item_resign: { if (ctrl.humansTurn()) { ctrl.resignGame(); }
			 * return true; }
			 */
		case R.id.item_about:
			showDialog(ABOUT_DIALOG);
			return true;
		}
		return false;
	}

	private void importTwic() {
		Intent i = new Intent(ScidAndroidActivity.this,
				ImportTwicActivity.class);
		startActivityForResult(i, RESULT_TWIC_IMPORT);
	}

	private void setStudyMode() {
		inStudyMode = !inStudyMode;
		Editor editor = settings.edit();
		editor.putBoolean("inStudyMode", inStudyMode);
		editor.commit();
		updateThinkingInfo();
		moveListUpdated();
		Toast.makeText(
				getApplicationContext(),
				inStudyMode ? R.string.study_mode_enabled
						: R.string.study_mode_disabled, Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RESULT_SETTINGS:
			readPrefs();
			String theme = settings.getString("colorTheme", "0");
			ColorTheme.instance().setTheme(settings, Integer.parseInt(theme));
			cb.setColors();
			ctrl.setGameMode(gameMode);
			break;
		case RESULT_EDITBOARD:
			if (resultCode == RESULT_OK) {
				try {
					String fen = data.getAction();
					ctrl.setFENOrPGN(fen);
				} catch (ChessParseError e) {
				}
			}
			break;
		case RESULT_SEARCH:
			if (resultCode == RESULT_OK) {
				Cursor cursor = this.getScidAppContext().getGamesCursor();
				if (cursor == null) {
					cursor = this.getCursor();
				}
				if (cursor != null) {
					startManagingCursor(cursor);
					setPgnFromCursor(cursor);
				}
			}
			break;
		case RESULT_TWIC_IMPORT:
			if (resultCode == RESULT_OK) {
				String pgnFileName = data.getAction();
				if (pgnFileName != null) {
					importPgn(getFullScidFileName(pgnFileName));
				}
			}
			break;
		}
	}

	private final void setBoardFlip() {
		// check player names
		boolean flipped = boardFlipped;
		if (autoSwapSides) {
			if (gameMode.analysisMode()) {
				flipped = !cb.pos.whiteMove;
			} else if (gameMode.playerWhite() && gameMode.playerBlack()) {
				flipped = !cb.pos.whiteMove;
			} else if (gameMode.playerWhite()) {
				flipped = false;
			} else if (gameMode.playerBlack()) {
				flipped = true;
			} else { // two computers
				flipped = !cb.pos.whiteMove;
			}
		}
		cb.setFlipped(flipped);
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
	public void setStatusString(String str) {
		status.setText(str);
	}

	@Override
	public void moveListUpdated() {
		if (inStudyMode) {
			moveList.setText("");
		} else {
			if (pgnOptions.view.allMoves) {
				moveList.setText(gameTextListener.getSpannableData());
			} else {
				moveList.setText(gameTextListener.getCurrentSpannableData());
			}
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
		setBoardFlip();
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
				status.setText(s, TextView.BufferType.SPANNABLE);
			}
		}
		if (mShowBookHints && (bookInfoStr.length() > 0)) {
			String s = "";
			if (!thinkingEmpty) {
				s += "<br>";
			}
			s += "<b>Book:</b>" + bookInfoStr;
			status.append(Html.fromHtml(s));
			thinkingEmpty = false;
		}
		if (variantStr.indexOf(' ') >= 0 && !inStudyMode) {
			String s = "";
			if (!thinkingEmpty) {
				s += "<br>";
			}
			s += "<b>Var:</b> " + variantStr;
			status.append(Html.fromHtml(s));
		}

		List<Move> hints = null;
		if (!inStudyMode) {
			if (mShowThinking || gameMode.analysisMode()) {
				hints = pvMoves;
			}
			if ((hints == null) && mShowBookHints) {
				hints = bookMoves;
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
	static final int ABOUT_DIALOG = 2;
	static final int SELECT_MOVE_DIALOG = 3;
	static final int SELECT_GOTO_GAME_DIALOG = 4;
	static final int SELECT_SCID_FILE_DIALOG = 5;
	static final int SEARCH_DIALOG = 6;
	static final int IMPORT_PGN_DIALOG = 7;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROMOTE_DIALOG: {
			final CharSequence[] items = { getString(R.string.queen),
					getString(R.string.rook), getString(R.string.bishop),
					getString(R.string.knight) };
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.promote_pawn_to);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					ctrl.reportPromotePiece(item);
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case SEARCH_DIALOG: {
			final int RESET_FILTER = 0;
			final int SEARCH_CURRENT_BOARD = 1;
			final int SEARCH_HEADER = 2;
			List<CharSequence> lst = new ArrayList<CharSequence>();
			List<Integer> actions = new ArrayList<Integer>();
			lst.add(getString(R.string.reset_filter));
			actions.add(RESET_FILTER);
			lst.add(getString(R.string.search_current_board));
			actions.add(SEARCH_CURRENT_BOARD);
			lst.add(getString(R.string.search_header));
			actions.add(SEARCH_HEADER);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.search);
			builder.setItems(lst.toArray(new CharSequence[lst.size()]),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (finalActions.get(item)) {
							case RESET_FILTER: {
								final String fileName = getScidAppContext()
										.getCurrentFileName();
								if (fileName.length() != 0) {
									Cursor cursor = getContentResolver()
											.query(
													Uri
															.parse("content://org.scid.database.scidprovider/games"),
													null, fileName, null, null);
									getScidAppContext().setGamesCursor(cursor);
									getScidAppContext().setNoGames(cursor);
									startManagingCursor(cursor);
									cursor.moveToPosition(getScidAppContext()
											.getCurrentGameNo());
								} else {
									getScidAppContext().setGamesCursor(null);
								}
								break;
							}
							case SEARCH_CURRENT_BOARD: {
								Intent i = new Intent(ScidAndroidActivity.this,
										SearchCurrentBoardActivity.class);
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
							}
						}
					});
			AlertDialog alert = builder.create();
			return alert;
		}
		case CLIPBOARD_DIALOG: {
			final int COPY_GAME = 0;
			final int COPY_POSITION = 1;
			final int PASTE = 2;
			final int REMOVE_VARIATION = 3;

			List<CharSequence> lst = new ArrayList<CharSequence>();
			List<Integer> actions = new ArrayList<Integer>();
			lst.add(getString(R.string.copy_game));
			actions.add(COPY_GAME);
			lst.add(getString(R.string.copy_position));
			actions.add(COPY_POSITION);
			lst.add(getString(R.string.paste));
			actions.add(PASTE);
			if (ctrl.numVariations() > 1) {
				lst.add(getString(R.string.remove_variation));
				actions.add(REMOVE_VARIATION);
			}
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.tools_menu);
			builder.setItems(lst.toArray(new CharSequence[lst.size()]),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (finalActions.get(item)) {
							case COPY_GAME: {
								String pgn = ctrl.getPGN();
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								clipboard.setText(pgn);
								break;
							}
							case COPY_POSITION: {
								String fen = ctrl.getFEN() + "\n";
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								clipboard.setText(fen);
								break;
							}
							case PASTE: {
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								if (clipboard.hasText()) {
									String fenPgn = clipboard.getText()
											.toString();
									try {
										ctrl.setFENOrPGN(fenPgn);
									} catch (ChessParseError e) {
										Toast.makeText(getApplicationContext(),
												e.getMessage(),
												Toast.LENGTH_SHORT).show();
									}
								}
								break;
							}
							case REMOVE_VARIATION:
								ctrl.removeVariation();
								break;
							}
						}
					});
			AlertDialog alert = builder.create();
			return alert;
		}
		case ABOUT_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_name).setMessage(R.string.about_info);
			AlertDialog alert = builder.create();
			return alert;
		}
		case SELECT_MOVE_DIALOG: {
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.select_move_number);
			dialog.setTitle(R.string.goto_move);
			final EditText moveNrView = (EditText) dialog
					.findViewById(R.id.selmove_number);
			Button ok = (Button) dialog.findViewById(R.id.selmove_ok);
			Button cancel = (Button) dialog.findViewById(R.id.selmove_cancel);
			moveNrView.setText("1");
			final Runnable gotoMove = new Runnable() {
				public void run() {
					try {
						int moveNr = Integer.parseInt(moveNrView.getText()
								.toString());
						ctrl.gotoMove(moveNr);
						dialog.cancel();
					} catch (NumberFormatException nfe) {
						Toast.makeText(getApplicationContext(),
								R.string.invalid_number_format,
								Toast.LENGTH_SHORT).show();
					}
				}
			};
			moveNrView.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if ((event.getAction() == KeyEvent.ACTION_DOWN)
							&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
						gotoMove.run();
						return true;
					}
					return false;
				}
			});
			ok.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					gotoMove.run();
				}
			});
			cancel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			return dialog;
		}
		case SELECT_GOTO_GAME_DIALOG: {
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.select_game_number);
			dialog.setTitle(R.string.goto_game);
			final EditText gameNoView = (EditText) dialog
					.findViewById(R.id.selgame_number);
			Button ok = (Button) dialog.findViewById(R.id.selgame_ok);
			Button cancel = (Button) dialog.findViewById(R.id.selgame_cancel);
			gameNoView.setText("1");
			final Runnable gotoGame = new Runnable() {
				public void run() {
					try {
						int gameNo = Integer.parseInt(gameNoView.getText()
								.toString()) - 1;
						Cursor cursor = getCursor();
						if (cursor != null && cursor.moveToPosition(gameNo)) {
							setPgnFromCursor(cursor);
						}
						dialog.cancel();
					} catch (NumberFormatException nfe) {
						Toast.makeText(getApplicationContext(),
								R.string.invalid_number_format,
								Toast.LENGTH_SHORT).show();
					}
				}
			};
			gameNoView.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if ((event.getAction() == KeyEvent.ACTION_DOWN)
							&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
						gotoGame.run();
						return true;
					}
					return false;
				}
			});
			ok.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					gotoGame.run();
				}
			});
			cancel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			return dialog;
		}
		case SELECT_SCID_FILE_DIALOG: {
			final String[] fileNames = findFilesInDirectory(scidDir, ".si4");
			final int numFiles = fileNames.length;
			if (numFiles == 0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.app_name).setMessage(
						R.string.no_scid_files);
				AlertDialog alert = builder.create();
				return alert;
			}
			int defaultItem = 0;
			final String currentScidFile = settings.getString(
					"currentScidFile", "");
			for (int i = 0; i < numFiles; i++) {
				if (currentScidFile.equals(fileNames[i])) {
					defaultItem = i;
					break;
				}
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_pgn_file);
			builder.setSingleChoiceItems(fileNames, defaultItem,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							String scidFile = fileNames[item].toString();
							int gameNo = 0;
							if (scidFile.equals(currentScidFile)) {
								gameNo = settings.getInt("currentGameNo", 0);
							} else {
								Editor editor = settings.edit();
								editor.putString("currentScidFile", scidFile);
								editor.commit();
								getScidAppContext().setCurrentFileName(
										getFullScidFileName(scidFile));
							}
							Cursor cursor = getCursor();
							if (cursor.moveToPosition(gameNo)
									|| cursor.moveToFirst()) {
								setPgnFromCursor(cursor);
							}
							dialog.dismiss();
						}
					});
			AlertDialog alert = builder.create();
			return alert;
		}
		case IMPORT_PGN_DIALOG: {
			final String[] fileNames = findFilesInDirectory(scidDir, ".pgn");
			final int numFiles = fileNames.length;
			if (numFiles == 0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.app_name).setMessage(
						R.string.no_pgn_files);
				AlertDialog alert = builder.create();
				return alert;
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_pgn_file);
			builder.setSingleChoiceItems(fileNames, 0,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							String baseName = getFullScidFileName(fileNames[item]
									.toString());
							importPgn(baseName);
							dialog.dismiss();
						}
					});
			AlertDialog alert = builder.create();
			return alert;
		}
		}
		return null;
	}

	private void startPgnImport(String pgnFileName) {
		Intent i = new Intent(ScidAndroidActivity.this, ImportPgnActivity.class);
		i.setAction(pgnFileName);
		startActivity(i);
	}

	private void importPgn(String baseName) {
		final String pgnFileName = baseName + ".pgn";
		String scidFileName = baseName + ".si4";
		File scidFile = new File(scidFileName);
		if (scidFile.exists()) {
			final AlertDialog fileExistsDialog = new AlertDialog.Builder(
					ScidAndroidActivity.this).create();
			fileExistsDialog.setTitle("Database exists");
			String message = String.format(
					getString(R.string.pgn_import_db_exists), scidFile
							.getName());
			fileExistsDialog.setMessage(message);
			fileExistsDialog.setIcon(android.R.drawable.ic_dialog_alert);
			fileExistsDialog.setButton(getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							startPgnImport(pgnFileName);
						}
					});
			fileExistsDialog.setButton2(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Toast.makeText(getApplicationContext(),
									getString(R.string.pgn_import_cancel),
									Toast.LENGTH_SHORT).show();
						}
					});
			fileExistsDialog.show();
		} else {
			startPgnImport(pgnFileName);
		}
	}

	private Cursor getCursor() {
		final String currentScidFile = settings
				.getString("currentScidFile", "");
		if (currentScidFile.length() == 0) {
			return null;
		}
		String scidFileName = getFullScidFileName(currentScidFile);
		Cursor cursor = getContentResolver().query(
				Uri.parse("content://org.scid.database.scidprovider/games"),
				null, scidFileName, null, null);
		((ScidApplication) this.getApplicationContext()).setGamesCursor(cursor);
		((ScidApplication) this.getApplicationContext()).setNoGames(cursor);

		startManagingCursor(cursor);
		return cursor;
	}

	private String getFullScidFileName(final String fileName) {
		String pathName = getFullFileName(fileName);
		String scidFileName = pathName.substring(0, pathName.indexOf("."));
		return scidFileName;
	}

	private String getFullFileName(final String fileName) {
		String sep = File.separator;
		String pathName = Environment.getExternalStorageDirectory() + sep
				+ scidDir + sep + fileName;
		return pathName;
	}

	private final String[] findFilesInDirectory(String dirName,
			final String extension) {
		File extDir = Environment.getExternalStorageDirectory();
		String sep = File.separator;
		File dir = new File(extDir.getAbsolutePath() + sep + dirName);
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isFile()
						&& (pathname.getAbsolutePath().endsWith(extension));
			}
		});
		if (files == null) {
			files = new File[0];
		}
		final int numFiles = files.length;
		String[] fileNames = new String[numFiles];
		for (int i = 0; i < files.length; i++) {
			fileNames[i] = files[i].getName();
		}
		Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
		return fileNames;
	}

	@Override
	public void requestPromotePiece() {
		runOnUIThread(new Runnable() {
			public void run() {
				showDialog(PROMOTE_DIALOG);
			}
		});
	}

	@Override
	public void reportInvalidMove(Move m) {
		String msg = String.format("Invalid move %s-%s", TextIO
				.squareToString(m.from), TextIO.squareToString(m.to));
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void computerMoveMade() {
		// do nothing
		// TODO: possibly re-enable sound
	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		runOnUiThread(runnable);
	}

	@Override
	public void setGameInformation(String white, String black, String gameNo) {
		flipBoardForPlayerNames(white, black);
		whitePlayer.setText(white);
		blackPlayer.setText(black);
		this.gameNo.setText(gameNo);
	}

	public ScidApplication getScidAppContext() {
		return (ScidApplication) getApplicationContext();
	}

	@Override
	public boolean onSearchRequested() {
		showDialog(SEARCH_DIALOG);
		return true;
	}
}