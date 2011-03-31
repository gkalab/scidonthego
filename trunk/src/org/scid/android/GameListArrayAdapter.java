package org.scid.android;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

public class GameListArrayAdapter extends ArrayAdapter<GameInfo> {

	private Context context;

	public GameListArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater vi = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = vi.inflate(R.layout.gamelist_item, null);
		}
		GameInfo item = this.getItem(position);
		if (item != null) {
			TextView title = (TextView) view.findViewById(R.id.item_title);
			TextView details = (TextView) view.findViewById(R.id.item_details);
			if (title != null) {
				title.setText(item.getTitle());
			}
			if (details != null) {
				String text = item.getDetails();
				details.setText(Html.fromHtml(text));
			}
			RatingBar favorite = (RatingBar) view
					.findViewById(R.id.item_favorite);
			if (favorite != null) {
				favorite.setRating(item.getFavorite());
			}
		}
		return view;
	}
}
