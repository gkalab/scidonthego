package org.scid.android;

import org.scid.android.gamelogic.GameTree.Node;

import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

/**
 * Class for creating a movable click area to be able to jump to the selected node
 * @author GKalab
 */
public class MoveClickableSpan extends ClickableSpan {

	private Node node;

	public MoveClickableSpan(Node node) {
		this.node = node;
	}

	@Override
	public void onClick(View widget) {
		Log.d("SCID", "clicked on node " + node);
		ScidApplication appContext = (ScidApplication)widget.getContext().getApplicationContext();
		appContext.getController().gotoNode(node);
	}
}
