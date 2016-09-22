package com.github.mmin18.realtimeblurview.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ListView;
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
	SeekBar blurDownsampling;
	TextView blurDownsamplingText;

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

		blurDownsampling = (SeekBar) findViewById(R.id.blur_downsampling);
		blurDownsampling.setProgress(3);
		blurDownsampling.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				updateDownsampling();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		blurDownsamplingText = (TextView) findViewById(R.id.blur_downsampling_val);
		updateDownsampling();
	}

	private void updateRadius() {
		blurView.setBlurRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, blurRadius.getProgress(), getResources().getDisplayMetrics()));
		blurRadiusText.setText(blurRadius.getProgress() + "dp");
	}

	private void updateDownsampling() {
		blurView.setDownsampleFactor(1 + blurDownsampling.getProgress());
		blurDownsamplingText.setText(String.valueOf(1 + blurDownsampling.getProgress()));
	}
}
