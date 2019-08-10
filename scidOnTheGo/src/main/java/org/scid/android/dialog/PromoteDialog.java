package org.scid.android.dialog;

import org.scid.android.R;
import org.scid.android.gamelogic.ChessController;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class PromoteDialog {

	private Context context;

	public PromoteDialog(Context context) {
		this.context = context;
	}

	public AlertDialog create(final ChessController ctrl) {
		final CharSequence[] items = { context.getString(R.string.queen),
				context.getString(R.string.rook),
				context.getString(R.string.bishop),
				context.getString(R.string.knight) };
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.promote_pawn_to);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				ctrl.reportPromotePiece(item);
			}
		});
		return builder.create();
	}
}
