package org.scid.android.chessok;

import org.scid.android.Link;
import org.scid.android.R;
import org.scid.android.R.id;
import org.scid.android.R.layout;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class LinkListArrayAdapter extends ArrayAdapter<Link> {

	private Context context;

	public LinkListArrayAdapter(Context context, int textViewResourceId) {
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
		Link item = this.getItem(position);
		if (item != null) {
			TextView title = (TextView) view.findViewById(R.id.item_title);
			TextView details = (TextView) view.findViewById(R.id.item_details);
			if (title != null) {
				title.setText(item.getDescription());
			}
			if (details != null) {
				String text = item.getLink();
				details.setText(Html.fromHtml(text));
			}
		}
		return view;
	}
}
