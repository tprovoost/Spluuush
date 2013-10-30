package com.tprovoost.spluuush;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.purplebrain.adbuddiz.sdk.AdBuddiz;
import com.purplebrain.adbuddiz.sdk.AdBuddizDelegate;
import com.purplebrain.giftiz.sdk.GiftizSDK;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class SpluuushActivity extends Activity implements AdBuddizDelegate {

	private Renderer	view;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_fullscreen);

		view = (Renderer) findViewById(R.id.renderer);
		Engine engine = new Engine(this, view);
		view.setEngine(engine);

		final AdBuddiz adBuddiz = AdBuddiz.getInstance();
		adBuddiz.onStart(this);
		adBuddiz.cacheAds(this); // start ads caching
		adBuddiz.setDelegate(this); // optional
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onPause() {
		super.onPause();
		GiftizSDK.onPauseMainActivity(this);
		if (view != null)
			view.setPause(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		GiftizSDK.onResumeMainActivity(this);
		if (view != null)
			view.setPause(false);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	public void failToLoadAd(String placementId, AdBuddizFailToDisplayCause cause) {
		// Toast.makeText(this, cause.name(), Toast.LENGTH_SHORT).show();
		// Log.e("TestActivity", placementId+": "+cause);
	}
}
