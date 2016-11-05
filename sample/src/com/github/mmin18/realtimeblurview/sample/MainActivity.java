package com.github.mmin18.realtimeblurview.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mmin18.widget.RealtimeBlurView;

/**
 * Created by mmin18 on 3/5/16.
 */
public class MainActivity extends Activity {
	RealtimeBlurView blurView;
	SeekBar blurRadius;
	TextView blurRadiusText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		blurView = (RealtimeBlurView) findViewById(R.id.blur_view);
		((ListView) findViewById(R.id.list)).setAdapter(new MyListAdapter(this));

		blurRadius = (SeekBar) findViewById(R.id.blur_radius);
		blurRadius.setProgress(10);
		blurRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				updateRadius();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		blurRadiusText = (TextView) findViewById(R.id.blur_radius_val);
		updateRadius();

		findViewById(R.id.drag).setOnTouchListener(touchListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Popup Window");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		showPopup();
		return true;
	}

	private void showPopup() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.popup_layout, null);
		DisplayMetrics dm = getResources().getDisplayMetrics();
		PopupWindow pw = new PopupWindow(layout, dm.widthPixels / 2, dm.heightPixels / 2, true);
		pw.showAtLocation(layout, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
	}

	private void updateRadius() {
		blurView.setBlurRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, blurRadius.getProgress(), getResources().getDisplayMetrics()));
		blurRadiusText.setText(blurRadius.getProgress() + "dp");
	}

	private View.OnTouchListener touchListener = new View.OnTouchListener() {
		float dx, dy;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			View target = findViewById(R.id.blur_frame);
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				dx = target.getX() - event.getRawX();
				dy = target.getY() - event.getRawY();
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				target.setX(event.getRawX() + dx);
				target.setY(event.getRawY() + dy);
			}
			return true;
		}
	};

	private boolean slideUp;

	public void doSlide(View v) {
		final View view = findViewById(R.id.blur_frame);
		view.animate().translationYBy((slideUp ? -1 : 1) * view.getHeight()).setDuration(1000).start();
		slideUp = !slideUp;
	}
}
