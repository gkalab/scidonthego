package org.scid.android;

import java.util.ArrayList;

import org.scid.database.DataBaseView;
import org.scid.database.GameFilter;
import org.scid.database.SearchHeaderRequest;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

public class SearchHeaderActivity extends SearchActivityBase {
    private DataBaseView dbv;

    private class FilterableNames extends BaseAdapter implements Filterable {
        private int nameType;
        private int[] names = new int[0];
        FilterableNames(int nameType){
            this.nameType = nameType;
        }
        public int getCount() { return names.length; }
        public String getItem(int position) {
            int[] n = names; // local copy to prevent TOCTOU
            if (position >= 0 && position < n.length) {
                return dbv.getName(nameType, n[position]);
            } else {
                return null;
            }
        }

        public long getItemId(int position) {
            int[] n = names; // local copy to prevent TOCTOU
            if (position >= 0 && position < n.length) {
                return n[position];
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

    private class ExactSetter implements AdapterView.OnItemClickListener, OnKeyListener {
        private int checkBoxId;
        ExactSetter(int checkBoxId){
            this.checkBoxId = checkBoxId;
        }
        private void set(boolean checked){
            ((CheckBox) findViewById(checkBoxId)).setChecked(checked);
        }
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            set(true);
        }
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            set(false);
            return false;
        }
    }

    private void bindActv(int actvId, int nameId, int exactId) {
        AutoCompleteTextView actv = findViewById(actvId);
        actv.setAdapter(new FilterableNames(nameId));
        ExactSetter es = new ExactSetter(exactId);
        actv.setOnItemClickListener(es); // set "exact" on auto-complete
        actv.setOnKeyListener(es); // clear "exact" on keys
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_header);
        Tools.setKeepScreenOn(this, true);
        addSpinner();

        dbv = ((ScidApplication) getApplicationContext()).getDataBaseView();

        bindActv(R.id.search_white, DataBaseView.NAME_PLAYER, R.id.search_white_exact);
        bindActv(R.id.search_black, DataBaseView.NAME_PLAYER, R.id.search_black_exact);
        bindActv(R.id.search_event, DataBaseView.NAME_EVENT,  R.id.search_event_exact);
        bindActv(R.id.search_site,  DataBaseView.NAME_SITE,   R.id.search_site_exact);
        bindActv(R.id.search_round, DataBaseView.NAME_ROUND,  R.id.search_round_exact);

        findViewById(R.id.search_header_next)
        .setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return onNextLongClick(v);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // the default implementation complains with IllegalStateException:
        //  Cannot perform this action after onSaveInstanceState
        setResult(RESULT_CANCELED);
        finish();
    }


    /** list of previous requests */
    private static ArrayList<Bundle> requestHistory = new ArrayList<>();
    private int requestIndex;
    private Bundle savedRequest; // saved on the first "Previous" click
    private Bundle currentState() {
        Bundle result = new Bundle();
        onSaveInstanceState(result);
        return result;
    }
    private void restoreState(Bundle bundle) {
        onRestoreInstanceState(bundle);
        findViewById(R.id.search_header_layout).requestFocus(); // un-focus AutoCompleteTextView
    }
    public void onPreviousClick(View view) {
        if (requestHistory.isEmpty()) {
            Toast.makeText(this, getString(R.string.search_no_history), Toast.LENGTH_LONG).show();
        } else  if (savedRequest == null) { // there is history but "Previous" was not yet touched
            savedRequest = currentState();
            requestIndex = requestHistory.size() - 1;
            restoreState(requestHistory.get(requestIndex));
        } else  if (requestIndex == 0){ // last time we showed the first request, rewind
            Toast.makeText(this, getString(R.string.search_history_done), Toast.LENGTH_LONG).show();
            requestIndex = requestHistory.size();
            restoreState(savedRequest);
        } else {
            restoreState(requestHistory.get(--requestIndex));
        }
    }
    public void onNextClick(View view) {
        if (requestHistory.isEmpty()) {
            Toast.makeText(this, getString(R.string.search_no_history), Toast.LENGTH_LONG).show();
        } else  if (savedRequest == null || requestIndex == requestHistory.size()) {
            Toast.makeText(this, getString(R.string.search_nothing_to_redo), Toast.LENGTH_LONG).show();
        } else  if (requestIndex == requestHistory.size() - 1){ // last redo
            requestIndex = requestHistory.size();
            restoreState(savedRequest);
        } else {
            restoreState(requestHistory.get(++requestIndex));
        }
    }
    public boolean onNextLongClick(View view) {
        if (savedRequest == null) { // let onNextClick explain that there is no history
            return false;
        } else {
            cancelHistoryNavigation();
            return true;
        }
    }
    /** cancel history navigation and return to the state before history was touched */
    private void cancelHistoryNavigation() {
        restoreState(savedRequest);
        savedRequest = null;
        Toast.makeText(this, getString(R.string.search_redo), Toast.LENGTH_LONG).show();
    }

    /** id of EditText =&gt; String */
    private String ets(int id) {
        return ((EditText) findViewById(id)).getText().toString().trim();
    }
    /** id of EditText =&gt; int */
    private int eti(int id, int defaultValue) {
        String s = ets(id);
        try {
            return Integer.valueOf(s);
        } catch(NumberFormatException e) {
            return defaultValue;
        }
    }
    /** set integer value to EditText */
    private void siet(int id, int value) {
        ((EditText) findViewById(id)).setText(Integer.toString(value));
    }
    /** id of CheckBox =&gt; boolean */
    private boolean cbb(int id) {
        return ((CheckBox) findViewById(id)).isChecked();
    }
    private static int toRange(int x, int min, int max){
        if (x < min)
            return min;
        if (x > max)
            return max;
        return x;
    }

    /** validate dates and put the result into EditTexts and request */
    private void prepareDates(SearchHeaderRequest r){
        int fy = toRange(eti(R.id.search_year_from, 0), // unknown year is stored as 0
                         0, SearchHeaderRequest.YEAR_MAX),
            fm = toRange(eti(R.id.search_month_from, 1), 1, 12),
            fd = toRange(eti(R.id.search_day_from, 1), 1, 31), // allow February 31
            ty = toRange(eti(R.id.search_year_to, SearchHeaderRequest.YEAR_MAX),
                         0, SearchHeaderRequest.YEAR_MAX),
            tm = toRange(eti(R.id.search_month_to, 12), 1, 12),
            td = toRange(eti(R.id.search_day_to, 31), 1, 31);
        if (ty <= fy) {
            ty = fy;
            if (tm <= fm) {
                tm = fm;
                if (td <= fd) {
                    td = fd;
                }
            }
        }

        siet(R.id.search_year_from, fy);
        siet(R.id.search_month_from, fm);
        siet(R.id.search_day_from, fd);
        siet(R.id.search_year_to, ty);
        siet(R.id.search_month_to, tm);
        siet(R.id.search_day_to, td);

        r.dateMin = SearchHeaderRequest.makeDate(fy, fm, fd);
        r.dateMax = SearchHeaderRequest.makeDate(ty, tm, td);
    }

    /** validate id range and put the result into EditTexts and request */
    private void prepareIdRange(SearchHeaderRequest r){
    	int minId = R.id.search_game_id_min, maxId = R.id.search_game_id_max;
        int total = dbv.getTotalGamesInFile();
        int minVal = toRange(eti(minId, 1), 1, total),
            maxVal = toRange(eti(maxId, total), 1, total);
        if (maxVal < minVal) {
            int tmp = maxVal; maxVal = minVal; minVal = tmp;
        }
        siet(minId, minVal);
        siet(maxId, maxVal);

        r.idMin = minVal-1;
        r.idMax = maxVal-1;
    }

    /** validate one ELO range and put the result into EditTexts*/
    private void fixEloRange(int minId, int maxId) {
        int minVal = toRange(eti(minId, 0), 0, SearchHeaderRequest.MAX_ELO);
        int maxVal = toRange(eti(maxId, SearchHeaderRequest.MAX_ELO), 0, SearchHeaderRequest.MAX_ELO);
        if (maxVal < minVal) {
            int tmp = maxVal; maxVal = minVal; minVal = tmp;
        }
        siet(minId, minVal);
        siet(maxId, maxVal);
    }
    /** validate all ELO ranges and put the result into EditTexts and request */
    private void prepareEloRanges(SearchHeaderRequest r) {
    	int minId, maxId;

    	// replicate code since Java has no macros and no references
    	minId = R.id.search_white_elo_min; maxId = R.id.search_white_elo_max;
    	fixEloRange(minId, maxId);
        r.whiteEloMin = eti(minId, 0); r.whiteEloMax = eti(maxId, 0);

    	minId = R.id.search_black_elo_min; maxId = R.id.search_black_elo_max;
    	fixEloRange(minId, maxId);
        r.blackEloMin = eti(minId, 0); r.blackEloMax = eti(maxId, 0);

    	minId = R.id.search_diff_elo_min; maxId = R.id.search_diff_elo_max;
    	fixEloRange(minId, maxId);
        r.diffEloMin = eti(minId, 0); r.diffEloMax = eti(maxId, 0);

    	minId = R.id.search_min_elo_min; maxId = R.id.search_min_elo_max;
    	fixEloRange(minId, maxId);
        r.minEloMin = eti(minId, 0); r.minEloMax = eti(maxId, 0);

    	minId = R.id.search_max_elo_min; maxId = R.id.search_max_elo_max;
    	fixEloRange(minId, maxId);
        r.maxEloMin = eti(minId, 0); r.maxEloMax = eti(maxId, 0);
    }

    /** validate half moves range and put the result into EditTexts and request */
    private void prepareHalfMoves(SearchHeaderRequest r){
    	int minId = R.id.search_half_moves_min, maxId = R.id.search_half_moves_max;
        int minVal = toRange(eti(minId, 0), 0, 9999),
            maxVal = toRange(eti(maxId, 9999), 0, 9999);
        if (maxVal < minVal) {
            int tmp = maxVal; maxVal = minVal; minVal = tmp;
        }
        siet(minId, minVal);
        siet(maxId, maxVal);

        r.halfMovesMin = minVal;
        r.halfMovesMax = maxVal;
    }

  public void onOkClick(View view) {
        final SearchHeaderRequest r = new SearchHeaderRequest();

        r.white = ets(R.id.search_white);
        r.black = ets(R.id.search_black);
        r.event = ets(R.id.search_event);
        r.site = ets(R.id.search_site);
        r.round = ets(R.id.search_round);
        r.whiteExact = cbb(R.id.search_white_exact);
        r.blackExact = cbb(R.id.search_black_exact);
        r.eventExact = cbb(R.id.search_event_exact);
        r.siteExact = cbb(R.id.search_site_exact);
        r.roundExact = cbb(R.id.search_round_exact);
        r.ignoreColors = cbb(R.id.ignore_colors);

        r.resultNone = cbb(R.id.result_unspecified);
        r.resultWhiteWins = cbb(R.id.result_white_wins);
        r.resultDraw = cbb(R.id.result_draw);
        r.resultBlackWins = cbb(R.id.result_black_wins);

        prepareDates(r);

        prepareEloRanges(r);
        r.allowUnknownElo = cbb(R.id.search_allow_unknown_elo);

        prepareHalfMoves(r);
        r.halfMovesEven = cbb(R.id.search_half_moves_even);
        r.halfMovesOdd = cbb(R.id.search_half_moves_odd);

        r.ecoFrom = ets(R.id.search_eco_from);
        r.ecoTo = ets(R.id.search_eco_to);
        r.allowEcoNone = cbb(R.id.eco_none);

        r.annotatedOnly = cbb(R.id.search_annotated_only);

        prepareIdRange(r);

        requestHistory.add(currentState()); // after validations
        (new SearchTask(this){ protected GameFilter doInBackground(Void... params) {
          return dbv.getMatchingHeaders(filterOperation, r, progress);
        }}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
