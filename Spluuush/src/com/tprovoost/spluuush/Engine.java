package com.tprovoost.spluuush;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;

import com.purplebrain.adbuddiz.sdk.AdBuddiz;
import com.purplebrain.giftiz.sdk.GiftizSDK;

public class Engine {

	static enum GameStatus {
		GAME_IDLE, GAME_START_SCREEN, GAME_RUNNING, GAME_SHOW_RESULTS, GAME_OVER, GAME_PAUSE, GAME_QUIT
	};

	// Game Internals
	private static int			nbGames					= 0;
	private Random				rand					= new Random();
	private SpluuushActivity	context;
	private int					bombsLeft;
	private int					boatsLeft;
	private int					highscore;
	private boolean				muted					= false;
	private SoundPool			soundPoolVoice;
	private SoundPool			soundPoolGlobal;
	private int[]				sounds					= new int[15];
	private long				timeSpeak				= 0L;
	private GameStatus			status					= GameStatus.GAME_START_SCREEN;
	private Sprite				muteButton;
	private Sprite				pauseButton;
	private Sprite				salvatore;
	private int					lastSoundVoicePlayed	= -1;
	private int					lastSoundGlobalPlayed	= -1;
	// private View giftizButton;

	/** Constant containing the duration of an animation in ms. */
	public static final long	ANIM_DURATION			= 3000L;

	/** Shows an ad every 5 game */
	private static final int	ADS_EVERY				= 5;

	// AUDIO RESSOURCES REFERENCES
	public static final int		SOUND_BOMB_SUCCESS		= 0;
	public static final int		SOUND_BOMB_FAIL			= 1;
	public static final int		SOUND_SHIP_DOWN			= 2;
	public static final int		SOUND_GAME_LOST			= 3;
	public static final int		SOUND_GAME_WIN			= 4;
	public static final int		SOUND_SALV_SIREN		= 5;
	public static final int		SOUND_SALV_LAUGH_PIRATE	= 6;
	public static final int		SOUND_SALV_HOORAY		= 7;
	public static final int		SOUND_SALV_HEY			= 8;
	public static final int		SOUND_SALV_HUH			= 9;
	public static final int		SOUND_SALV_SLEEP		= 10;
	public static final int		SOUND_SALV_SLEEP_2		= 11;
	public static final int		SOUND_SALV_SLEEP_3		= 12;
	public static final int		SOUND_SALV_SLEEP_4		= 13;

	// MAP CONSTANTS
	static final int			SIZE_MAP				= 8;
	static final int			NB_BOMBS				= 24;
	static final byte			VOID					= 0;
	static final byte			BOAT					= 1;
	static final byte			VOID_MISS				= 2;
	static final byte			BOAT_TOUCHED			= 3;

	// INTERNAL MAP
	private byte[]				map						= new byte[SIZE_MAP * SIZE_MAP];
	private ArrayList<Boat>		boats					= new ArrayList<Boat>();

	// DRAW POSITION
	private float				x						= -1;
	private float				y						= -1;
	private int					size					= -1;

	// GRAPHICS RESOURCES
	private Bitmap				bSalvatore;
	private Bitmap				bSalvatoreSpeak;
	private Bitmap				bSoundOn;
	private Bitmap				bSoundOff;
	private Bitmap				bPause;

	// OTHER RESOURCES
	private String				txtShareHead;
	private String				txtShareBody;
	private String				txtShareBody2;

	// PREFERENCES
	private SharedPreferences	prefs;
	// private Sprite buttonPlay;
	// private Sprite buttonRestart;
	// private Sprite buttonQuit;
	// private Sprite buttonPause;
	private Renderer			render;
	private int					width;
	private int					height;
	private final String		link					= "http://goo.gl/D5PN1C";
	private boolean				blocked					= false;
	private static final String	PREF_HIGH_SCORE			= "highscore";
	private static final String	SPLUUUSH_PREFERENCES	= "spluuush_preferences";
	private static final String	PREF_MUTED				= "muted";
	private static final int	ERROR					= 15;
	private static final int	MISSION_COMPLETE_SCORE	= 20;

	public Engine(SpluuushActivity context, Renderer render) {
		this.context = context;
		this.render = render;
		bombsLeft = NB_BOMBS;

		// AUDIO LOADING
		soundPoolVoice = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
		soundPoolGlobal = new SoundPool(5, AudioManager.STREAM_MUSIC, 100);
		sounds[SOUND_BOMB_FAIL] = soundPoolVoice.load(context, R.raw.sploosh, 1);
		sounds[SOUND_BOMB_SUCCESS] = soundPoolVoice.load(context, R.raw.kerboom, 1);
		sounds[SOUND_GAME_LOST] = soundPoolVoice.load(context, R.raw.wuhahahaha, 1);
		sounds[SOUND_GAME_WIN] = soundPoolVoice.load(context, R.raw.hooray, 1);
		sounds[SOUND_SALV_HEY] = soundPoolVoice.load(context, R.raw.hey, 1);
		sounds[SOUND_SALV_SIREN] = soundPoolVoice.load(context, R.raw.siren, 1);
		sounds[SOUND_SHIP_DOWN] = soundPoolGlobal.load(context, R.raw.boat_down, 1);

		// getting preferences
		prefs = context.getSharedPreferences(SPLUUUSH_PREFERENCES, Context.MODE_PRIVATE);
		highscore = prefs.getInt(PREF_HIGH_SCORE, -1);
		muted = prefs.getBoolean(PREF_MUTED, false);
		txtShareBody = context.getResources().getString(R.string.sharebody);
		txtShareBody2 = context.getResources().getString(R.string.sharebody2);
		txtShareHead = context.getResources().getString(R.string.sharehead);
	}

	public void init() {
		width = render.getWidth();
		height = render.getHeight();
		createSalvatore();
		createMuteButton();
		createMenuButton();
	}

	private void createSalvatore() {
		salvatore = new Sprite() {

			@Override
			public void touched() {
				timeSpeak = System.nanoTime() + 1200000000L;
				playSound(sounds[SOUND_SALV_HEY], true);
			}

			@Override
			public Bitmap getBitmap() {
				if (isSpeaking())
					return bitmaps.get(1);
				return bitmaps.get(0);
			}
		};

		int idealSizeSalvatoreW = (int) (width * Renderer.RIGHT_PART_SIZE);
		int idealSizeSalvatoreH = (int) ((height * (1 - Renderer.RIGHT_PART_SIZE) / 2));

		if (bSalvatore == null) {
			bSalvatore = decodeSampledBitmapFromResource(render.getResources(), R.drawable.salvatore_stand, idealSizeSalvatoreW, idealSizeSalvatoreH);
			bSalvatoreSpeak = decodeSampledBitmapFromResource(render.getResources(), R.drawable.salvatore_stand_speak, idealSizeSalvatoreW, idealSizeSalvatoreH);
		}
		salvatore.addBitmap(bSalvatore);
		salvatore.addBitmap(bSalvatoreSpeak);
		salvatore.setX(width * (1 - Renderer.RIGHT_PART_SIZE) + 10);
		salvatore.setY(height - idealSizeSalvatoreH - height * Renderer.MARGIN);
	}

	private void createMuteButton() {
		muteButton = new Sprite() {

			@Override
			public void touched() {
				muted = !muted;
				Editor editor = prefs.edit();
				editor.putBoolean(PREF_MUTED, muted);
				editor.commit();

				if (muted) {
					soundPoolVoice.stop(lastSoundVoicePlayed);
					soundPoolGlobal.stop(lastSoundGlobalPlayed);
				}
				nextAnim();
			}
		};
		bSoundOn = BitmapFactory.decodeResource(render.getResources(), R.drawable.sound_on);
		bSoundOff = BitmapFactory.decodeResource(render.getResources(), R.drawable.sound_off);
		int idealW = (int) (width * Renderer.MARGIN);
		if (bSoundOn.getWidth() != idealW) {
			bSoundOn = Bitmap.createScaledBitmap(bSoundOn, idealW, idealW, true);
			bSoundOff = Bitmap.createScaledBitmap(bSoundOff, idealW, idealW, true);
		}
		muteButton.addBitmap(bSoundOn);
		muteButton.addBitmap(bSoundOff);
		muteButton.setX(width - idealW - 5);
		muteButton.setY(5);
		if (muted)
			muteButton.setIdxAnim(1);
	}

	private void createMenuButton() {
		pauseButton = new Sprite() {

			@Override
			public void touched() {
				status = GameStatus.GAME_PAUSE;
			}
		};
		bPause = BitmapFactory.decodeResource(render.getResources(), R.drawable.pause);
		int idealW = (int) (width * Renderer.MARGIN);
		if (bPause.getWidth() != idealW) {
			bPause = Bitmap.createScaledBitmap(bPause, idealW, idealW, true);
		}
		pauseButton.addBitmap(bPause);
		pauseButton.setX(width - idealW * 2 - 5 * 2);
		pauseButton.setY(5);
	}

	public ArrayList<Sprite> getSprites() {
		ArrayList<Sprite> sprites = new ArrayList<Sprite>();
		sprites.add(salvatore);
		sprites.add(muteButton);
		if (status == GameStatus.GAME_RUNNING || status == GameStatus.GAME_PAUSE)
			sprites.add(pauseButton);
		return sprites;
	}

	public byte[] getMap() {
		return map;
	}

	public void setMap(byte[] map) {
		this.map = map;
	}

	public ArrayList<Boat> getBoats() {
		return boats;
	}

	public void setBoats(ArrayList<Boat> boats) {
		this.boats = boats;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	private void generateBoats() {
		for (int i = 2; i <= 4; ++i) {
			Boat b = new Boat(i);
			while (!b.positionBoat())
				;
			b.assignToMap();
			boats.add(b);
			// Log.d("engine", "pos: " + b.coords[0][0] + " / vert: " +
			// b.vertical);
		}
		boatsLeft = boats.size();

		// DEBUG VERSION
		// Boat b = new Boat(2);
		// b.vertical = false;
		// b.coords[0][0] = 0;
		// b.coords[0][0] = 0;
		//
		// b.coords[1][0] = 0;
		// b.coords[1][0] = 1;
		// map[14] = BOAT;
		// map[15] = BOAT;
		//
		// map[3 + 1 * SIZE_MAP] = BOAT;
		// map[3 + 2 * SIZE_MAP] = BOAT;
		// map[3 + 3 * SIZE_MAP] = BOAT;
		//
		// map[1 + 7 * SIZE_MAP] = BOAT;
		// map[2 + 7 * SIZE_MAP] = BOAT;
		// map[3 + 7 * SIZE_MAP] = BOAT;
		// map[4 + 7 * SIZE_MAP] = BOAT;
	}

	public boolean contains(float ex, float ey) {
		return ex > x && ex < x + size && ey > y && ey < y + size;
	}

	public void touched(float ex, float ey) {
		boolean consumed = false;
		if (isBlocked())
			return;
		if (status == GameStatus.GAME_RUNNING) {
			if (contains(ex, ey)) {
				int posX = (int) ((ex - x) / (size / SIZE_MAP));
				int posY = (int) ((ey - y) / (size / SIZE_MAP));
				int idx = posX + posY * SIZE_MAP;
				if (map[idx] == VOID) {
					playSound(sounds[SOUND_BOMB_FAIL], true);
					setBombsLeft(bombsLeft - 1);
					map[idx] = VOID_MISS;
					timeSpeak = System.nanoTime() + 1200000000L;
				} else if (map[idx] == BOAT) {
					playSound(sounds[SOUND_BOMB_SUCCESS], true);
					map[idx] = BOAT_TOUCHED;
					Boat b = getBoat(posX, posY);
					if (b == null) {
						// Log.d("engine", "Should not be null ! : " + posX +
						// " , " + posY);
					} else {
						if (b.isDestoyed()) {
							boatsLeft--;
							if (boatsLeft <= 0) {
								int score = NB_BOMBS - bombsLeft + 1;
								if (highscore == -1 || score < highscore) {
									highscore = score;
									Editor editor = prefs.edit();
									editor.putInt(PREF_HIGH_SCORE, score);
									editor.commit();
								}
								if (score <= MISSION_COMPLETE_SCORE) {
									GiftizSDK.missionComplete(context);
								}
								playSound(sounds[SOUND_GAME_WIN], true);
								status = GameStatus.GAME_SHOW_RESULTS;
								ThreadUtil.bgRun(new Runnable() {

									@Override
									public void run() {
										ThreadUtil.sleep(1000);
										status = GameStatus.GAME_OVER;
									}
								});
							} else
								playSound(sounds[SOUND_SHIP_DOWN], false);
						}
					}
					setBombsLeft(bombsLeft - 1);
					timeSpeak = System.nanoTime() + 1200000000L;
				}
				if (bombsLeft <= 0 && status != GameStatus.GAME_OVER) {
					status = GameStatus.GAME_SHOW_RESULTS;
					ThreadUtil.bgRun(new Runnable() {

						@Override
						public void run() {
							ThreadUtil.sleep(1000);
							status = GameStatus.GAME_OVER;
						}
					});
					playSound(sounds[SOUND_GAME_LOST], true);
				}
				consumed = true;
			}
		} else if (status == GameStatus.GAME_OVER) {
			consumed = determineActionGameOver(ex, ey);
		} else if (status == GameStatus.GAME_START_SCREEN) {
			consumed = determineActionStart(ex, ey);
		} else if (status == GameStatus.GAME_PAUSE) {
			consumed = determineActionPause(ex, ey);
		}
		if (!consumed) {
			for (Sprite s : getSprites()) {
				if (s.contains(ex, ey)) {
					s.touched();
					consumed = true;
				}
			}
		}
	}

	private boolean determineActionGameOver(float ex, float ey) {
		// get the size of the interline spacing
		Paint paint = new Paint(render.getPaintStroke());
		float originalSize = paint.getTextSize();
		float textHeight = paint.getFontMetrics(null);
		paint.setTextSize(originalSize);

		Rect rectTouched = new Rect((int) (ex - ERROR), (int) (ey - ERROR), (int) (ex + ERROR), (int) (ey + ERROR));
		Rect rect = new Rect();

		if (boatsLeft == 0) {

			String txtRestart = render.getResources().getString(R.string.restart);
			paint.getTextBounds(txtRestart.toCharArray(), 0, txtRestart.length(), rect);
			rect.offsetTo(width / 3 - rect.width() / 2, (int) (height / 2 + textHeight * 1.5f - rect.height() / 2));
			if (rect.intersect(rectTouched)) {
				restart();
				return true;
			}

			String txtShare = render.getResources().getString(R.string.share);
			paint.getTextBounds(txtShare.toCharArray(), 0, txtShare.length(), rect);
			rect.offsetTo(2 * width / 3 - rect.width() / 2, (int) (height / 2 + textHeight * 1.5f - rect.height() / 2));
			if (rect.intersect(rectTouched)) {
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				String shareBody = txtShareBody + " " + (NB_BOMBS - bombsLeft) + " " + txtShareBody2 + " " + link;
				shareIntent.putExtra(Intent.EXTRA_SUBJECT, txtShareHead);
				shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
				shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(Intent.createChooser(shareIntent, txtShare + " via"));
				return true;
			}
		} else {
			String txtRestart = render.getResources().getString(R.string.restart);
			paint.getTextBounds(txtRestart.toCharArray(), 0, txtRestart.length(), rect);
			rect.offsetTo(width / 2 - rect.width() / 2, (int) (height / 2 + textHeight * 1.5f - rect.height() / 2));
			if (rect.intersect(rectTouched)) {
				restart();
				return true;
			}

		}

		return false;
	}

	private boolean determineActionStart(float ex, float ey) {
		Paint paint = render.getPaintStroke();
		Rect rectTouched = new Rect((int) (ex - ERROR), (int) (ey - ERROR), (int) (ex + ERROR), (int) (ey + ERROR));
		Rect rect = new Rect();

		String playText = render.getResources().getString(R.string.btn_start);
		paint.getTextBounds(playText.toCharArray(), 0, playText.length(), rect);
		rect.offsetTo(width / 2 - rect.width() / 2, 3 * height / 5 - rect.height() / 2);
		if (rect.intersect(rectTouched)) {
			setBlocked(true);
			final long time = System.nanoTime() + ANIM_DURATION * 1000000L;
			render.playCurtainTransition(time);
			timeSpeak = System.nanoTime() + 2400000000L;
			playSound(sounds[SOUND_SALV_SIREN], true, 0.5f);
			new Thread(new Runnable() {

				@Override
				public void run() {
					while (isBlocked()) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
						}
					}
					restart();
				}
			}).start();
			return true;
		}

		// String helpText = render.getResources().getString(R.string.btn_help);
		// paint.getTextBounds(helpText.toCharArray(), 0, helpText.length(),
		// rect);
		// rect.offsetTo(width / 2 - rect.width() / 2, 4 * height / 5 -
		// rect.height() / 2);
		// if (rect.intersect(rectTouched)) {
		// status = GameStatus.GAME_HELP;
		// return true;
		// }
		return false;
	}

	private boolean determineActionPause(float ex, float ey) {
		Paint paint = render.getPaintStroke();
		Rect rectTouched = new Rect((int) (ex - ERROR), (int) (ey - ERROR), (int) (ex + ERROR), (int) (ey + ERROR));
		Rect rect = new Rect();

		String restart = "Restart";
		paint.getTextBounds(restart.toCharArray(), 0, restart.length(), rect);
		rect.offsetTo(width / 2 - rect.width() / 2, height / 2 - rect.height() / 2);
		if (rect.intersect(rectTouched)) {
			restart();
			return true;
		}

		String menu = "Menu";
		paint.getTextBounds(menu.toCharArray(), 0, menu.length(), rect);
		rect.offsetTo(width / 2 - rect.width() / 2, 3 * height / 4 - rect.height() / 2);
		if (rect.intersect(rectTouched)) {
			setBlocked(true);
			final long time = System.nanoTime() + ANIM_DURATION * 1000000L;
			render.playCurtainTransition(time);
			// timeSpeak = System.nanoTime() + 2400000000L;
			// playSound(sounds[SOUND_SALV_SIREN]);
			new Thread(new Runnable() {

				@Override
				public void run() {
					while (isBlocked()) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
						}
					}
					status = GameStatus.GAME_START_SCREEN;
					// giftizButton.setVisibility(View.VISIBLE);
				}
			}).start();
			return true;
		}

		status = GameStatus.GAME_RUNNING;
		return true;
	}

	public int getBoatsLeft() {
		return boatsLeft;
	}

	private Boat getBoat(int posX, int posY) {
		for (Boat b : boats) {
			for (int i = 0; i < b.coords.length; ++i)
				if (b.coords[i][0] == posX && b.coords[i][1] == posY)
					return b;
		}
		return null;
	}

	private void restart() {
		// if (giftizButton == null) {
		// Log.d("engine", "giftiz button null");
		// } else
		// giftizButton.setVisibility(View.INVISIBLE);
		boats.clear();
		Arrays.fill(map, VOID);
		generateBoats();
		bombsLeft = NB_BOMBS;
		status = GameStatus.GAME_RUNNING;
		nbGames++;
		if (nbGames != 0 && (nbGames % ADS_EVERY == 0)) {
			AdBuddiz.getInstance().showAd();
		}
	}

	public synchronized boolean isSpeaking() {
		long nano = System.nanoTime();
		return nano < timeSpeak && timeSpeak - nano > 100000000;
	}

	class Boat {
		private int[][]	coords;
		private boolean	vertical;

		/**
		 * First dimension represents the size Second dimension the x,y
		 * coordinates
		 * 
		 * @param size
		 */
		public Boat(int size) {
			coords = new int[size][2];
		}

		public boolean isDestoyed() {
			for (int i = 0; i < coords.length; ++i) {
				int idx = coords[i][0] + coords[i][1] * SIZE_MAP;
				if (map[idx] != BOAT_TOUCHED)
					return false;
			}
			return true;
		}

		public void assignToMap() {
			for (int i = 0; i < coords.length; ++i)
				map[coords[i][0] + coords[i][1] * SIZE_MAP] = BOAT;
		}

		public boolean overlap(int x, int y) {
			for (Boat b : boats) {
				if (b == null)
					continue;
				for (int i = 0; i < b.coords.length; ++i) {
					if (b.coords[i][0] == x && b.coords[i][1] == y)
						return true;
				}
			}
			return false;
		}

		public boolean positionBoat() {
			int size = coords.length;

			vertical = rand.nextBoolean();
			int x;
			int y;
			if (vertical) {
				x = rand.nextInt(SIZE_MAP - 1);
				y = rand.nextInt(SIZE_MAP - size - 1);
			} else {
				x = rand.nextInt(SIZE_MAP - size - 1);
				y = rand.nextInt(SIZE_MAP - 1);
			}
			// Log.v("renderer", "boat nb : " + size + "vert:" + vertical +
			// " / " + x + " , " + y);
			coords[0][0] = x;
			coords[0][1] = y;
			if (overlap(coords[0][0], coords[0][1]))
				return false;
			for (int i = 1; i < coords.length; ++i) {
				if (vertical) {
					coords[i][0] = coords[0][0]; // x doesn't change
					coords[i][1] = coords[i - 1][1] + 1;// y + 1
				} else {
					coords[i][0] = coords[i - 1][0] + 1;// x +11
					coords[i][1] = coords[0][1]; // y doesn't change
				}
				if (overlap(coords[i][0], coords[i][1]))
					return false;
			}
			return true;
		}

		/**
		 * First dimension represents the position. Size : size of the boat.<br/>
		 * Second dimension represents the x and y coordinates. Size : 2
		 * 
		 * @return
		 */
		public int[][] getCoords() {
			return coords;
		}
		
		public boolean isVertical() {
			return vertical;
		}
	}

	public void playSound(int soundID, boolean voice) {
		playSound(soundID, voice, 1f);
	}

	public void playSound(int soundID, boolean voice, float volumeFactor) {
		if (muted)
			return;
		// Log.d("renderer", "play sound destroyed");
		AudioManager mgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
		float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = streamVolumeCurrent / streamVolumeMax * volumeFactor;

		if (voice)
			lastSoundVoicePlayed = soundPoolVoice.play(soundID, volume * 0.5f, volume * 0.5f, 1, 0, 1f);
		else
			lastSoundGlobalPlayed = soundPoolGlobal.play(soundID, volume * 0.5f, volume * 0.5f, 1, 0, 1f);
	}

	public int getBombsLeft() {
		return bombsLeft;
	}

	public void setBombsLeft(int bombsLeft) {
		this.bombsLeft = bombsLeft;
	}

	public void destroy() {
		soundPoolVoice.release();
		soundPoolGlobal.release();
		salvatore.clear();
		muteButton.clear();
	}

	public GameStatus getStatus() {
		return status;
	}

	public int getHighscore() {
		return highscore;
	}

	public boolean isMuted() {
		return muted;
	}

	public Sprite getMuteButton() {
		return muteButton;
	}

	public Sprite getSalvatore() {
		return salvatore;
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inScaled = false;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap decodedBmp = BitmapFactory.decodeResource(res, resId, options);
		return Bitmap.createScaledBitmap(decodedBmp, reqWidth, reqHeight, true);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to
			// the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public synchronized void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

}
