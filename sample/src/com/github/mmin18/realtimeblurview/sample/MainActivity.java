package com.github.mmin18.realtimeblurview.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

/**
 * Created by mmin18 on 3/5/16.
 */
public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((ListView) findViewById(R.id.list)).setAdapter(new MyListAdapter(this));
	}
}
