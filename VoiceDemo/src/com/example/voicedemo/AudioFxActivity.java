package com.example.voicedemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.media.audiofx.Visualizer.OnDataCaptureListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class AudioFxActivity extends Activity {

	private static final String TAG = "Tag";
	private static final float VISUALIZER_HEIGHT_DIP = 160f;
	private MediaPlayer mMediaPlayer;
	private Visualizer mVisualizer;
	private Equalizer mEqualizer;

	private LinearLayout mLinearLayout;
	private VisualizerView mVisualizerView;
	private TextView mStatusTextView;
	private TextView mInfoView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mStatusTextView = new TextView(this);
		mLinearLayout = new LinearLayout(this);
		mLinearLayout.setOrientation(LinearLayout.VERTICAL);
		mLinearLayout.addView(mStatusTextView);

		setContentView(mLinearLayout);

		mMediaPlayer = MediaPlayer.create(this, R.raw.music1);
		Log.d("Tag", "MediaPlayer audio session ID:" + mMediaPlayer.getAudioSessionId());

		setupVisualizerFxAndUI();
		setupEqualizerFxAndUI();

		mVisualizer.setEnabled(true);

		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				mVisualizer.setEnabled(false);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				setVolumeControlStream(AudioManager.STREAM_SYSTEM);
				mStatusTextView.setText("音乐播放完毕");
			}
		});

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mMediaPlayer.start();
		mStatusTextView.setText("播放音乐中...");
	}

	private void setupEqualizerFxAndUI() {
		mEqualizer = new Equalizer(0, mMediaPlayer.getAudioSessionId());
		//控制何时采集频谱数据，在功能结束后，要设为false
		mEqualizer.setEnabled(true);

		TextView eqTextView = new TextView(this);
		eqTextView.setText("均衡器");
		mLinearLayout.addView(eqTextView);

		short bands = mEqualizer.getNumberOfBands();

		final short minEQLevel = mEqualizer.getBandLevelRange()[0];
		final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

		for (short i = 0; i < bands; i++) {
			final short band = i;

			TextView freqTextView = new TextView(this);
			freqTextView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);
			freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + "Hz");
			mLinearLayout.addView(freqTextView);

			LinearLayout row = new LinearLayout(this);
			row.setOrientation(LinearLayout.HORIZONTAL);

			TextView minDbTextView = new TextView(this);
			minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			minDbTextView.setText((minEQLevel / 100) + "dB");

			TextView maxDbTextView = new TextView(this);
			maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			maxDbTextView.setText((maxEQLevel / 100) + "dB");

			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			layoutParams.weight = 1;
			SeekBar bar = new SeekBar(this);
			bar.setLayoutParams(layoutParams);
			bar.setMax(maxEQLevel - minEQLevel);
			bar.setProgress(mEqualizer.getBandLevel(band));

			bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {

				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					mEqualizer.setBandLevel(band, (short) (progress + minEQLevel));
				}
			});

			row.addView(minDbTextView);
			row.addView(bar);
			row.addView(maxDbTextView);

			mLinearLayout.addView(row);
		}
	}
	//波形图
	private void setupVisualizerFxAndUI() {
		mVisualizerView = new VisualizerView(this);
		mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT, (int) (VISUALIZER_HEIGHT_DIP * getResources()
						.getDisplayMetrics().density)));
		mLinearLayout.addView(mVisualizerView);

		mInfoView = new TextView(this);
		String infoStr = "";

		int[] csr = Visualizer.getCaptureSizeRange();
		if (csr != null) {
			String csrStr = "CaptureSizeRange:";
			for (int i = 0; i < csr.length; i++) {
				csrStr += csr[i];
				csrStr += " ";
			}
			infoStr += csrStr;
		}
		final int maxCR = Visualizer.getMaxCaptureRate();
		infoStr = infoStr + "\nMaxCaptrueRate: " + maxCR;

		mInfoView.setText(infoStr);
		mLinearLayout.addView(mInfoView);

		mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
		//设置每次捕获频谱的大小
		mVisualizer.setCaptureSize(256);
		//参数：listener、采集的频率、是否采集波形、是否采集频率
		mVisualizer.setDataCaptureListener(new OnDataCaptureListener() {

			@Override
			public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform,
					int samplingRate) {
				mVisualizerView.updateVisualizer(waveform);
				Log.d(TAG, "调用onWaveFormDataCapture");
			}

			@Override
			public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
				mVisualizerView.updateVisualizer(fft);
				Log.i(TAG, "调用onWaveFormDataCapture");
			}
		}, maxCR / 2, false, true);

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isFinishing() && mMediaPlayer != null) {
			mVisualizer.release();
			mEqualizer.release();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	class VisualizerView extends View {

		private byte[] mBytes;
		private float[] mPoints;
		private Rect mRect = new Rect();
		
		private Paint mForePaint = new Paint();
		private int mSpectrumNum = 48;
		private boolean mFirst = true;
		
		public VisualizerView(Context context) {
			super(context);
			init();
		}

		private void init() {
			mBytes = null;
			mForePaint.setStrokeWidth(8f);
			mForePaint.setAntiAlias(true);
			mForePaint.setColor(Color.rgb(0, 128, 255));
		}

		public void updateVisualizer(byte[] fft) {
			if(mFirst){
				mInfoView.setText(mInfoView.getText().toString()+"\nCaptureSize:"+fft.length);
				mFirst = false;
			}
			byte[] mode1 = new byte[fft.length/2+1];
			mode1[0] = (byte) Math.abs(fft[0]);
			for (int i = 2,j=1; j < mSpectrumNum*2;) {
				//Math.hypot()返回 sqrt(x2 +y2)
				mode1[j] = (byte) Math.hypot(fft[i], fft[i+1]);
				i+=2;
				j++;
			}
			mBytes = mode1;
			invalidate();
		}
		@Override
		protected void onDraw(Canvas canvas) {
			Log.d(TAG, "调用onDraw");
			super.onDraw(canvas);
			if(mBytes==null){
				return;
			}
			if(mPoints==null||mPoints.length<mBytes.length*4){
				//mPoints用来存储画直线的2个坐标（x,y）
				mPoints = new float[mBytes.length*4];
			}
			mRect.set(0,0,getWidth(),getHeight());
			//baseX是每个刻度长度
			final int baseX = mRect.width()/mSpectrumNum;
			final int height = mRect.height();
			for (int i = 0; i < mSpectrumNum; i++) {
				if(mBytes[i]<0){
					mBytes[i] = 127;
				}
				final int xi = baseX*i+baseX/2;
				mPoints[i*4] = xi;
				mPoints[i*4+1] = height;
				mPoints[i*4+2] = xi;
				mPoints[i*4+3] = height-mBytes[i];
			}
			canvas.drawLines(mPoints, mForePaint);
		}

	}
	

}
