package org.scid.android;

import org.scid.android.gamelogic.Position;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class MoveIndicator extends View {
	private Context context;

	public MoveIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		this.invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// canvas.drawColor(Color.WHITE);
		setMoveIndicator(canvas);
	}

	/**
	 * Set the move indicators (who's move is it?) for white and black
	 */
	private void setMoveIndicator(Canvas canvas) {
		Position position = ((ScidApplication) context.getApplicationContext())
				.getPosition();
		Bitmap moveBitmap;
		if (position == null) {
			moveBitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.black_moveindicator);
		} else {
			if (position.whiteMove) {
				moveBitmap = BitmapFactory.decodeResource(getResources(),
						R.drawable.white_moveindicator);
			} else {
				moveBitmap = BitmapFactory.decodeResource(getResources(),
						R.drawable.black_moveindicator);
			}
		}
		canvas.drawBitmap(moveBitmap, 0, 0, null);
	}
}
