package org.scid.android;

import android.os.Bundle;
import android.view.MenuItem;

public class Preferences extends AppCompatPreferenceActivity {
    public static final String DATA_ENGINE_NAME = "org.scid.android.engine.name";
    public static final String DATA_ENGINE_NAMES = "org.scid.android.engine.names";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
