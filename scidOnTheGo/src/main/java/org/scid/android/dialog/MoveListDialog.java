package org.scid.android.dialog;

import java.util.ArrayList;
import java.util.List;

import org.scid.android.R;
import org.scid.android.gamelogic.ChessController;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class MoveListDialog {

	private Context context;

	public MoveListDialog(Context context) {
		this.context = context;
	}

	public AlertDialog create(final ChessController ctrl) {
		final int REMOVE_SUBTREE = 0;
		final int MOVE_VAR_UP = 1;
		final int MOVE_VAR_DOWN = 2;

		List<CharSequence> lst = new ArrayList<>();
		List<Integer> actions = new ArrayList<>();
		lst.add(context.getString(R.string.truncate_gametree));
		actions.add(REMOVE_SUBTREE);
		if (ctrl.numVariations() > 1) {
			lst.add(context.getString(R.string.move_var_up));
			actions.add(MOVE_VAR_UP);
			lst.add(context.getString(R.string.move_var_down));
			actions.add(MOVE_VAR_DOWN);
		}
		final List<Integer> finalActions = actions;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.edit_game);
		builder.setItems(lst.toArray(new CharSequence[lst.size()]),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switch (finalActions.get(item)) {
						case REMOVE_SUBTREE:
							ctrl.removeSubTree();
							break;
						case MOVE_VAR_UP:
							ctrl.moveVariation(-1);
							break;
						case MOVE_VAR_DOWN:
							ctrl.moveVariation(1);
							break;
						}
					}
				});
		return builder.create();
	}
}
