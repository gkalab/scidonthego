package org.scid.android;

import java.util.HashMap;

import org.scid.android.gamelogic.PgnToken;
import org.scid.android.gamelogic.GameTree.Node;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;

/**
 * PngTokenReceiver implementation that renders PGN data for screen display.
 */
class PgnScreenText implements PgnToken.PgnTokenReceiver {
	private SpannableStringBuilder sb = new SpannableStringBuilder();
	private int prevType = PgnToken.EOF;
	private int nestLevel = 0;
	private boolean col0 = true;
	private Node currNode = null;
	private final int indentStep = 15;
	private int currPos = 0, endPos = 0;
	private boolean upToDate = false;
	private PGNOptions options;

	private static class NodeInfo {
		int l0, l1;

		NodeInfo(int ls, int le) {
			l0 = ls;
			l1 = le;
		}
	}

	HashMap<Node, PgnScreenText.NodeInfo> nodeToCharPos;

	PgnScreenText(PGNOptions options) {
		nodeToCharPos = new HashMap<Node, PgnScreenText.NodeInfo>();
		this.options = options;
	}

	public final SpannableStringBuilder getSpannableData() {
		return sb;
	}

	/**
	 * Get spannable data of current move
	 * 
	 * @param whiteMove
	 * 
	 * @return sb of current move
	 */
	public final SpannableStringBuilder getCurrentSpannableData(
			boolean whiteMove) {
		PgnScreenText.NodeInfo ni = nodeToCharPos.get(currNode);
		if (ni != null) {
			return new SpannableStringBuilder(currNode.getMoveString(whiteMove));
		}
		return new SpannableStringBuilder();
	}

	public final boolean atEnd() {
		return currPos >= endPos - 10;
	}

	public boolean isUpToDate() {
		return upToDate;
	}

	int paraStart = 0;
	int paraIndent = 0;
	boolean paraBold = false;

	private final void newLine() {
		synchronized (sb) {
			if (!col0) {
				if (paraIndent > 0) {
					int paraEnd = sb.length();
					int indent = paraIndent * indentStep;
					sb.setSpan(new LeadingMarginSpan.Standard(indent),
							paraStart, paraEnd,
							Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if (paraBold) {
					int paraEnd = sb.length();
					sb.setSpan(new StyleSpan(Typeface.BOLD), paraStart,
							paraEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				sb.append('\n');
				paraStart = sb.length();
				paraIndent = nestLevel;
				paraBold = false;
			}
			col0 = true;
		}
	}

	boolean pendingNewLine = false;

	public void processToken(Node node, int type, String token) {
		synchronized (sb) {
			if ((prevType == PgnToken.RIGHT_BRACKET)
					&& (type != PgnToken.LEFT_BRACKET)) {
				if (options.view.headers) {
					col0 = false;
					newLine();
				} else {
					sb.clear();
					paraBold = false;
				}
			}
			if (pendingNewLine) {
				if (type != PgnToken.RIGHT_PAREN) {
					newLine();
					pendingNewLine = false;
				}
			}
			switch (type) {
			case PgnToken.STRING:
				sb.append(" \"");
				sb.append(token);
				sb.append('"');
				break;
			case PgnToken.INTEGER:
				if ((prevType != PgnToken.LEFT_PAREN)
						&& (prevType != PgnToken.RIGHT_BRACKET) && !col0) {
					sb.append(' ');
				}
				sb.append(token);
				col0 = false;
				break;
			case PgnToken.PERIOD:
				sb.append('.');
				col0 = false;
				break;
			case PgnToken.ASTERISK:
				sb.append(" *");
				col0 = false;
				break;
			case PgnToken.LEFT_BRACKET:
				sb.append('[');
				col0 = false;
				break;
			case PgnToken.RIGHT_BRACKET:
				sb.append("]\n");
				col0 = false;
				break;
			case PgnToken.LEFT_PAREN:
				nestLevel++;
				if (col0) {
					paraIndent++;
				}
				newLine();
				sb.append('(');
				col0 = false;
				break;
			case PgnToken.RIGHT_PAREN:
				sb.append(')');
				nestLevel--;
				pendingNewLine = true;
				break;
			case PgnToken.NAG:
				sb.append(Node.nagStr(Integer.parseInt(token)));
				col0 = false;
				break;
			case PgnToken.SYMBOL: {
				if ((prevType != PgnToken.RIGHT_BRACKET)
						&& (prevType != PgnToken.LEFT_BRACKET) && !col0) {
					sb.append(' ');
				}
				int l0 = sb.length();
				sb.append(token);
				int l1 = sb.length();
				nodeToCharPos.put(node, new NodeInfo(l0, l1));
				if (endPos < l0) {
					endPos = l0;
				}
				col0 = false;
				if (nestLevel == 0) {
					paraBold = true;
				}
				break;
			}
			case PgnToken.COMMENT:
				if (prevType == PgnToken.RIGHT_BRACKET) {
				} else if (nestLevel == 0) {
					nestLevel++;
					newLine();
					nestLevel--;
				} else {
					if ((prevType != PgnToken.LEFT_PAREN) && !col0) {
						sb.append(' ');
					}
				}
				sb.append(token.replaceAll("[ \t\r\n]+", " ").trim());
				col0 = false;
				if (nestLevel == 0) {
					newLine();
				}
				break;
			case PgnToken.EOF:
				newLine();
				upToDate = true;
				break;
			}
			prevType = type;
		}
	}

	@Override
	public void clear() {
		synchronized (sb) {
			sb.clear();
		}
		prevType = PgnToken.EOF;
		nestLevel = 0;
		col0 = true;
		currNode = null;
		currPos = 0;
		endPos = 0;
		nodeToCharPos.clear();
		paraStart = 0;
		paraIndent = 0;
		paraBold = false;
		pendingNewLine = false;

		upToDate = false;
	}

	BackgroundColorSpan bgSpan = new BackgroundColorSpan(0xff888888);

	@Override
	public void setCurrent(Node node) {
		synchronized (sb) {
			sb.removeSpan(bgSpan);
			PgnScreenText.NodeInfo ni = nodeToCharPos.get(node);
			if (ni != null) {
				if (options.view.allMoves) {
					int color = ColorTheme.instance().getColor(
							ColorTheme.CURRENT_MOVE);
					bgSpan = new BackgroundColorSpan(color);
					sb.setSpan(bgSpan, ni.l0, ni.l1,
							Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				currPos = ni.l0;
			}
			currNode = node;
		}
	}

	public int getCurrentPosition() {
		return currPos;
	}
}