package org.scid.android;

import org.scid.database.DataBaseView;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

public class GameListActivity extends ListActivity {
	private DataBaseView dbv;
	private int oldPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbv = ((ScidApplication) getApplicationContext()).getDataBaseView();
		if (dbv == null){ // TODO: make sure the function can assume this never happens
			finish();
			return;
		}
		oldPosition = dbv.getPosition();
		setListAdapter(new GameListAdapter());
		ListView lv = getListView();
		lv.setFastScrollEnabled(true);
		lv.setSelectionFromTop(oldPosition, 0);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				oldPosition = position;
				setResult(RESULT_OK, (new Intent()).setAction("" + position));
				finish();
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		dbv.moveToPosition(oldPosition);
	}

	private class GameListAdapter extends BaseAdapter {
		public int getCount() { return dbv.getCount(); }
		public Object getItem(int position) { return Integer.valueOf(position); }
		public long getItemId(int position) { return position; }
		public boolean hasStableIds() { return true; }

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater vi =	(LayoutInflater) GameListActivity.this
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.gamelist_item, null);
			}
			dbv.moveToPosition(position);
			GameInfo item = new GameInfo(dbv.getGameInfo());
			if (item != null) {
				TextView title = (TextView) view.findViewById(R.id.item_title);
				TextView details = (TextView) view.findViewById(R.id.item_details);
				if (title != null) {
					title.setText(item.getTitle());
				}
				if (details != null) {
					String text = item.getDetails();
					if (item.isDeleted()) {
						text = "<font color='red'><b>DELETED</b></font> " + text;
					}
					details.setText(Html.fromHtml(text));
				}
				RatingBar favorite = (RatingBar) view.findViewById(R.id.item_favorite);
				if (favorite != null) {
					favorite.setRating(item.isFavorite() ? 1 : 0);
				}
			}
			return view;
		}
	}
}
