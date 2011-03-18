package org.scid.android.chessok;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.scid.android.Link;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ChessOkLinkMapActivity extends ListActivity {
	// TODO: allow downloading of all links in one category into one database

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LinkMap linkMap = (LinkMap) this.getIntent().getSerializableExtra(
				"linkmap");
		final ChessOkLinkMapActivity chessOkList = this;
		chessOkList.showList(linkMap.getLinkMap());
	}

	protected void showList(final Map<String, List<Link>> linkList) {
		final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, new Vector<String>(
						linkList.keySet()));
		setListAdapter(aa);
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				String item = aa.getItem(pos);
				Intent intent = new Intent(ChessOkLinkMapActivity.this,
						PgnLinkListActivity.class);
				LinkList list = new LinkList(linkList.get(item));
				Log.d("SCID", "linklist:" + list);
				intent.putExtra("linklist", list);
				startActivity(intent);
			}
		});

	}
}
