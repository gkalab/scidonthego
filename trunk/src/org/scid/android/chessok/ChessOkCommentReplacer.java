package org.scid.android.chessok;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChessOkCommentReplacer {
	static Map<String, String> replaceMap = new HashMap<String, String>() {
		{
			put("$11", "equal chances");
			put("$13", "{And position remains very unclear}");
			put("$14", "White stands slightly better");
			put("$15", "Black stands slightly better");
			put("$16", "White has the upper hand");
			put("$17", "Black has the upper hand");
			put("$18", "White has a decisive advantage");
			put("$19", "Black has a decisive advantage");
			put("$201", "An inaccuracy");
			put("$202", "An error");
			put("$203", "A blunder");
			put("$204", "The second error in a row");
			put("$205", "Not the best continuation");
			put("$206", "White fails to take advantage of the opponent's error");
			put("$207", "Black fails to take advantage of the opponent's error");
			put("$211", "Stronger is");
			put("$212", "More promising is");
			put("$213", "Preferable is");
			put("$214", "Worth attention is");
			put("$215", "An interesting alternative is");
			put("$216", "Better is");
			put("$217", "More stubborn is");
			put("$221", "retains an advantage");
			put("$222", "retains an approximate equality");
			put("$223", "gives a chance to save the game");
			put("$231", "White stands slightly better");
			put("$232", "Black stands slightly better");
			put("$233", "White's chances are slightly better");
			put("$234", "Black's chances are slightly better");
			put("$235", "White's position is slightly more promising");
			put("$236", "Black's position is slightly more promising");
			put("$237", "Chances are equal");
			put("$238", "The position is approximately equal");
			put("$239", "Chances are mutual");
			put("$240", "Double-edged play");
			put("$241", "White is winning");
			put("$242", "Black is winning");
			put("$243", "White has a decisive advantage");
			put("$244", "Black has a decisive advantage");
			put("$245", "White has an initiative");
			put("$246", "Black has an initiative");
			put("$247", "White's king is in danger");
			put("$248", "Black's king is in danger");
			put("$249", "White develops a dangerous attack");
			put("$250", "Black develops a dangerous attack");
			put("$251", "White has a pair of bishops");
			put("$252", "Black has a pair of bishops");
			put("$255", "White's king is deprived of castling");
			put("$256", "Black's king is deprived of castling");
			put("$257",
					"In the ensuing endgame White's chances are slightly better");
			put("$258",
					"In the ensuing endgame Black's chances are slightly better");
			put("$259", "A passed pawn secures an advantage for White");
			put("$260", "A passed pawn secures an advantage for White");
			put("$261", "Both sides exchange blows");
			put("$263",
					"An unpleasant pin granting a slight advantage to White");
			put("$264",
					"An unpleasant pin granting a slight advantage to Black");
			put("$265", "White has good compensation for sacrificed material");
			put("$266", "Black has good compensation for sacrificed material");
			put("$267", "White has no compensation for lacking material");
			put("$268", "Black has no compensation for lacking material");
			put("$269", "White has a material advantage");
			put("$270", "Black has a material advantage");
			put("$271", "The ensuing closed position is favorable for White");
			put("$272", "The ensuing closed position is favorable for Black");
			put("$291", "With a threat of");
			put("$292", "With the idea of");
			put("$501", "A rare move");
			put("$502", "The main continuation is");
			put("$503", "Novelty");
			put("$505", "An old move");
			put("$506", "Recently more popular is the continuation");
			put("$507", "This move has not occurred at the high level before");
			put("$508", "Leading chessplayers prefer");
			put("$509", "The more reliable continuation is");
			put("$510", "Worth attention is");
		}
	};

	final static Pattern pattern = Pattern.compile("\\$[0-9]*");

	public static String replace(final String str) {
		final Matcher m = ChessOkCommentReplacer.pattern.matcher(str);
		final StringBuffer sb = new StringBuffer();
		while (m.find()) {
			final String repl = ChessOkCommentReplacer.replaceMap
					.get(m.group());
			if (repl != null) {
				m.appendReplacement(sb, "");
				sb.append(repl);
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

}
