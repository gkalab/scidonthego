package org.scid.android;

import org.scid.database.DataBaseView;
import org.scid.database.GameFilter;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class SearchHeaderActivity extends SearchActivityBase {
	private DataBaseView dbv;

	private class FilterableNames extends BaseAdapter implements Filterable {
		private int nameType;
		private int[] names = new int[0];
		public FilterableNames(int nameType){
			this.nameType = nameType;
		}
		public int getCount() { return names.length; }
		public String getItem(int position) {
			return dbv.getName(nameType, names[position]);
		}
		
		public long getItemId(int position) {
			if (position < names.length) {
				return names[position];
			} else {
				return -1;
			}
		}
		
		public boolean hasStableIds() { return true; }
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(android.R.layout.simple_list_item_1, null);
			}
			((TextView) view).setText(getItem(position));
			return view;
		}

		private class NamesFilter extends Filter {
			protected FilterResults performFiltering(CharSequence cs) {
	            FilterResults results = new FilterResults();
	            Log.i("SHA","Filter on " + cs);
	            int[] ids;
	            if (cs == null) {
	            	ids = new int[0];
	            } else {
	            	String prefix = cs.toString();
	            	if (prefix.equals(" ")) // type space to see the whole list
	            		prefix = "";
	            	ids = dbv.getMatchingNames(nameType, prefix);
	            }
	            results.values = ids;
	            results.count = ids.length;
	            return results;
			}
	        protected void publishResults(CharSequence constraint, FilterResults results) {
	            names = (int[]) results.values;
	            if (results.count > 0) {
	                notifyDataSetChanged();
	            } else {
	                notifyDataSetInvalidated();
	            }
	        }
		}
		public Filter getFilter() {
			return new NamesFilter();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_header);
	    Tools.setKeepScreenOn(this, true);
		addSpinner();

		dbv = ((ScidApplication) getApplicationContext()).getDataBaseView();
		FilterableNames players = new FilterableNames(DataBaseView.NAME_PLAYER);
		((AutoCompleteTextView) findViewById(R.id.search_white)).setAdapter(players);
		((AutoCompleteTextView) findViewById(R.id.search_black)).setAdapter(players);
		((AutoCompleteTextView) findViewById(R.id.search_event)).setAdapter(
				new FilterableNames(DataBaseView.NAME_EVENT));
		((AutoCompleteTextView) findViewById(R.id.search_site)).setAdapter(
				new FilterableNames(DataBaseView.NAME_SITE));
	}

    private String ets(int id){ // id of EditText => String
        return ((EditText) findViewById(id)).getText().toString().trim();
    }
    private boolean cbb(int id){ // id of CheckBox => boolean
        return ((CheckBox) findViewById(id)).isChecked();
    }
	public void onOkClick(View view) {
		final String white = ets(R.id.search_white), black = ets(R.id.search_black),
				event = ets(R.id.search_event), site = ets(R.id.search_site),
				ecoFrom = ets(R.id.search_eco_from), ecoTo = ets(R.id.search_eco_to),
				yearFrom = ets(R.id.search_year_from), yearTo = ets(R.id.search_year_to),
				idFrom = ets(R.id.search_game_id_from), idTo = ets(R.id.search_game_id_to);
		final boolean ignoreColors = cbb(R.id.ignore_colors),
				resultWhiteWins = cbb(R.id.result_white_wins),
				resultDraw = cbb(R.id.result_draw),
				resultBlackWins = cbb(R.id.result_black_wins),
				resultUnspecified = cbb(R.id.result_unspecified),
				ecoNone = cbb(R.id.eco_none);
		(new SearchTask(this){
			@Override
			protected GameFilter doInBackground(Void... params) {
				return dbv.getMatchingHeaders(filterOperation,
						white, black, ignoreColors,
						resultWhiteWins, resultDraw,
						resultBlackWins, resultUnspecified,
						event, site,
						ecoFrom, ecoTo, ecoNone,
						yearFrom, yearTo,
						idFrom, idTo,
						progress);
			}
		}).execute();
	}
}
