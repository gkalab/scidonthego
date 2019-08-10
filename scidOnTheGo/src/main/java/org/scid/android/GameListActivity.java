package org.scid.android;

import org.scid.database.DataBaseView;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

    private static final int[] resultStringId = {
        R.string.result_none, R.string.result_white_wins,
        R.string.result_black_wins, R.string.result_draw };

    private class GameListAdapter extends BaseAdapter {
        public int getCount() { return dbv.getCount(); }
        public Object getItem(int position) { return Integer.valueOf(position); }
        public long getItemId(int position) { return position; }
        public boolean hasStableIds() { return true; }

        /** put player info to TextView */
        private void ptv(View view, int tvId, String name, int elo){
            TextView tv = (TextView) view.findViewById(tvId);
            if (tv != null) {
                StringBuilder b = new StringBuilder();
                b.append(name);
                if(elo != 0) {
                    b.append(" (");
                    b.append(elo);
                    b.append(')');
                }
                tv.setText(b.toString());
            }
        }
        /** put text to TextView */
        private void ttv(View view, int tvId, String text){
            TextView tv = (TextView) view.findViewById(tvId);
            if (tv != null)
                tv.setText(text);
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater vi = (LayoutInflater) GameListActivity.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.gamelist_item, null);
            }
            if (dbv.moveToPosition(position, true)) {
                ptv(view, R.id.item_white, dbv.getWhite(), dbv.getWhiteElo());
                ptv(view, R.id.item_black, dbv.getBlack(), dbv.getBlackElo());
                ttv(view, R.id.item_result, getString(resultStringId[dbv.getResult()]));
                ttv(view, R.id.item_event, dbv.getEvent());
                ttv(view, R.id.item_site, dbv.getSite());
                ttv(view, R.id.item_round, dbv.getRound());
                ttv(view, R.id.item_date, dbv.getDate());

                RatingBar favorite = (RatingBar) view.findViewById(R.id.item_favorite);
                if (favorite != null) {
                    favorite.setRating(dbv.isFavorite() ? 1 : 0);
                    if(dbv.isDeleted())
                        favorite.setBackgroundColor(Color.RED);
                }
            }
            return view;
        }
    }
}
