package com.tprovoost.spluuush;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.tprovoost.spluuush.Engine.Boat;
import com.tprovoost.spluuush.Engine.GameStatus;

public class Renderer extends SurfaceView implements SurfaceHolder.Callback {

	// ---------
	// VARIABLES
	// ---------
	private int					width				= 0;
	private int					height				= 0;
	private static final int	TEXT_SIZE_FACTOR	= 32;
	private Paint				paint				= new Paint();
	private Paint				paintStroke			= new Paint();
	private Paint				paintStrokeRed		= new Paint();
	private SplushThread		thread;
	private Engine				engine;
	private long				curtainAnimTimeEnd;

	// GRAPHICS RESOURCES
	private Bitmap				bBackground;
	private Bitmap				bCurtain;
	private Bitmap				bBomb;
	private Bitmap				bBombDead;
	private Bitmap				bFail				= BitmapFactory.decodeResource(getResources(), R.drawable.fail);
	private Bitmap				bTouch				= BitmapFactory.decodeResource(getResources(), R.drawable.touch);
	private Bitmap				bSquid				= BitmapFactory.decodeResource(getResources(), R.drawable.squid);
	private Bitmap				bSquidKilled		= BitmapFactory.decodeResource(getResources(), R.drawable.squid_killed);
	private Bitmap				bSquidResultV		= BitmapFactory.decodeResource(getResources(), R.drawable.squid);
	private Bitmap				bSquidResultH		= BitmapFactory.decodeResource(getResources(), R.drawable.squid);

	// TEXT RESOURCES
	private String				txtMonsters;
	private String				txtMonsters2;
	private String				txtHighScore;
	private String				txtContinue;
	private String				txtRestart;
	private String				txtMenu;
	private String				txtWin;
	private String				txtMwahaha;
	private String				txtLose;
	private String				txtShare;
	private Matrix				matrix;
	private RectF				rectOrigin;
	private RectF				rectDest			= new RectF();

	// ---------
	// CONSTANTS
	// ----------
	// SIZES
	// Cut the GUI in 3 parts (in %)
	static final float			MARGIN				= 0.05f;

	static final float			LEFT_WIDTH			= 0.2f;
	static final float			CENTER_WIDTH		= 0.6f;
	static final float			RIGHT_PART_SIZE		= 0.2f;

	static final float			LEFT_PART_BOMBS		= 0.8f;
	static final float			CENTER_PART_GAME	= 0.8f;
	static final float			RIGHT_PART_ENNEMIES	= 0.8f;
	private static final long	REFRESH_RATE		= 10L;

	// ------------
	// CONSTRUCTORS
	// ------------
	public Renderer(Context context) {
		super(context);
		init();
	}

	public Renderer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public Renderer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	// ---------
	// METHODS
	// ---------
	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	private void init() {
		// Log.d("engine", "init");

		// SELECT FONT
		Typeface fontFace = Typeface.createFromAsset(getContext().getAssets(), "fonts/LITHOGRB.TTF");
		paint.setTypeface(Typeface.create(fontFace, Typeface.BOLD));
		paintStroke.setTypeface(Typeface.create(fontFace, Typeface.BOLD));

		paint.setAntiAlias(true);
		paintStroke.setAntiAlias(true);
		paint.setTextAlign(Align.CENTER);
		paintStroke.setTextAlign(Align.CENTER);

		paintStroke.setColor(Color.BLACK);
		paintStroke.setStyle(Style.STROKE);
		paintStroke.setStrokeWidth(8);
		paintStroke.setTextSize(paint.getTextSize());

		paintStrokeRed.setColor(Color.argb(255, 180, 20, 35));
		paintStrokeRed.setStyle(Style.STROKE);
		paintStrokeRed.setStrokeWidth(8);

		// load Strings
		Resources res = getResources();
		txtContinue = res.getString(R.string.continu);
		txtRestart = res.getString(R.string.restart);
		txtHighScore = res.getString(R.string.highscore);
		txtWin = res.getString(R.string.win);
		txtMenu = res.getString(R.string.menu);
		txtLose = res.getString(R.string.lose);
		txtMwahaha = res.getString(R.string.mwahaha);
		txtShare = res.getString(R.string.share);
		txtMonsters = res.getString(R.string.monsters_attack);
		txtMonsters2 = res.getString(R.string.monsters_attack2);

		// add the callback
		getHolder().addCallback(this);
		setFocusable(true);
	}

	public Paint getPaint() {
		return paint;
	}

	public Paint getPaintStroke() {
		return paintStroke;
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		if (width != w || height != h) {
			width = w;
			height = h;
			engine.init();
		}
		// depending on the status, draw the game
		GameStatus status = engine.getStatus();
		if (status != GameStatus.GAME_PAUSE) {
			if (status == GameStatus.GAME_START_SCREEN) {
				// Draw the background first
				drawBackground(canvas);
				drawStartScreen(canvas);
			} else if (status == GameStatus.GAME_RUNNING) {
				// Draw the background first
				drawBackground(canvas);
				drawLeftInterface(canvas);
				drawRightInterface(canvas);
				drawGame(canvas);
			} else if (status == GameStatus.GAME_SHOW_RESULTS) {
				drawBackground(canvas);
				drawLeftInterface(canvas);
				drawRightInterface(canvas);
				drawResults(canvas);
				drawGame(canvas);
			} else if (status == GameStatus.GAME_OVER) {
				drawBackground(canvas);
				drawLeftInterface(canvas);
				drawRightInterface(canvas);
				drawResults(canvas);
				drawGame(canvas);
				drawGameOver(canvas);
			} else if (status == GameStatus.GAME_IDLE) {
				drawBackground(canvas);
				drawLeftInterface(canvas);
				drawRightInterface(canvas);
				drawGame(canvas);
			}

			// draw the mute/unmute button
			for (Sprite s : engine.getSprites()) {
				s.drawSprite(canvas, paint);
				s.drawSprite(canvas, paint);
			}
		} else {
			// Draw the background first
			drawBackground(canvas);
			drawLeftInterface(canvas);
			drawRightInterface(canvas);
			drawGame(canvas);

			// draw the mute/unmute button
			for (Sprite s : engine.getSprites()) {
				s.drawSprite(canvas, paint);
				s.drawSprite(canvas, paint);
			}
			drawPause(canvas);
		}
	}

	public void drawPause(Canvas canvas) {
		canvas.drawColor(Color.argb(160, 60, 60, 60));

		long nano = System.nanoTime();
		if (nano > curtainAnimTimeEnd) {
			if (engine.isBlocked()) {
				engine.setBlocked(false);

				double res = Engine.ANIM_DURATION * 1000000d / (curtainAnimTimeEnd - nano);

				float curtainHeight = (float) (-height / res);
				canvas.drawBitmap(bCurtain, 0, curtainHeight, paint);
			} else {
				paint.setColor(Color.WHITE);
				paint.setTextSize(width / TEXT_SIZE_FACTOR);
				paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);

				canvas.drawText(txtContinue, width / 2, height / 4, paintStroke);
				canvas.drawText(txtContinue, width / 2, height / 4, paint);

				canvas.drawText(txtRestart, width / 2, height / 2, paintStroke);
				canvas.drawText(txtRestart, width / 2, height / 2, paint);

				canvas.drawText(txtMenu, width / 2, 3 * height / 4, paintStroke);
				canvas.drawText(txtMenu, width / 2, 3 * height / 4, paint);
			}
		} else {
			// curtain going down
			double res = Engine.ANIM_DURATION * 1000000d / (curtainAnimTimeEnd - nano);

			float curtainHeight = (float) (-height / res);
			canvas.drawBitmap(bCurtain, 0, curtainHeight, paint);
		}
	}

	private void drawStartScreen(Canvas canvas) {
		drawBackground(canvas);

		long nano = System.nanoTime();
		if (nano > curtainAnimTimeEnd) {
			if (engine.isBlocked()) {
				engine.setBlocked(false);

				// curtain going up
				double curtainHeight = -height + height * 1f / ((Engine.ANIM_DURATION * 1000000d / (curtainAnimTimeEnd - nano)));
				canvas.drawBitmap(bCurtain, 0, (float) curtainHeight, paint);

				paint.setColor(Color.WHITE);
				paint.setTextSize(width / TEXT_SIZE_FACTOR);
				paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);

				canvas.drawText(txtMonsters, width / 2, height / 3, paintStroke);
				canvas.drawText(txtMonsters, width / 2, height / 3, paint);

				canvas.drawText(txtMonsters2, width / 2, 2 * height / 3, paintStroke);
				canvas.drawText(txtMonsters2, width / 2, 2 * height / 3, paint);
			} else {

				canvas.drawBitmap(bCurtain, 0, 0, paint);

				paint.setColor(Color.WHITE);
				paint.setTextSize((int) (width / TEXT_SIZE_FACTOR * 3));
				paintStroke.setTextSize(paint.getTextSize());

				canvas.drawText("Spluuush!", width / 2, height / 3, paintStroke);
				canvas.drawText("Spluuush!", width / 2, height / 3, paint);

				paint.setTextSize(width / TEXT_SIZE_FACTOR);
				paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);

				canvas.drawText("Start", width / 2, 3 * height / 5, paintStroke);
				canvas.drawText("Start", width / 2, 3 * height / 5, paint);

				// canvas.drawText("Help", width / 2, 4 * height / 5,
				// paintStroke);
				// canvas.drawText("Help", width / 2, 4 * height / 5, paint);
			}
		} else {
			// curtain going up
			double curtainHeight = -height + height * 1f / ((Engine.ANIM_DURATION * 1000000d / (curtainAnimTimeEnd - nano)));
			canvas.drawBitmap(bCurtain, 0, (float) curtainHeight, paint);

			paint.setColor(Color.WHITE);
			paint.setTextSize(width / TEXT_SIZE_FACTOR);
			paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);

			canvas.drawText(txtMonsters, width / 2, height / 3, paintStroke);
			canvas.drawText(txtMonsters, width / 2, height / 3, paint);

			canvas.drawText(txtMonsters2, width / 2, 2 * height / 3, paintStroke);
			canvas.drawText(txtMonsters2, width / 2, 2 * height / 3, paint);
		}
	}

	private void drawBackground(Canvas canvas) {
		if (bBackground == null) {
			bBackground = Engine.decodeSampledBitmapFromResource(getResources(), R.drawable.full_bg, width, height);
			bCurtain = Engine.decodeSampledBitmapFromResource(getResources(), R.drawable.curtain_stage, width, height);
		}
		paint.setStyle(Style.FILL_AND_STROKE);
		canvas.drawBitmap(bBackground, 0, 0, paint);
	}

	private void drawGame(Canvas canvas) {
		byte[] map = engine.getMap();
		int size = engine.getSize();

		// initialize if not done already
		if (size == -1) {
			if (width > height)
				size = height;
			else if (height < width)
				size = width;
			size = (int) (size * CENTER_PART_GAME);

			engine.setX(width * LEFT_WIDTH + (width * CENTER_WIDTH - size) * 0.5f);
			engine.setY(size * (1 - CENTER_PART_GAME) * 0.9f);
			engine.setSize(size);

		}
		float x = engine.getX();
		float y = engine.getY();

		paint.setColor(Color.argb(150, 50, 50, 50));
		canvas.drawRect(x, y, x + size, y + size, paint);

		paint.setStyle(Style.STROKE);
		paint.setTextSize(width / TEXT_SIZE_FACTOR);

		// dimensions
		paint.setColor(Color.WHITE);
		canvas.drawRect(width * LEFT_WIDTH, 0, width * LEFT_WIDTH + width * CENTER_WIDTH, height - 1, paint);

		paint.setColor(Color.GRAY);
		canvas.drawRect(x, y, x + size, y + size, paint);

		// draw inner grid
		float mov = 1f * size / Engine.SIZE_MAP;
		for (float i = x; i < x + size; i += mov) {
			canvas.drawLine(i, y, i, y + size, paint);
		}
		for (float j = y; j < y + size; j += mov) {
			canvas.drawLine(x, j, x + size, j, paint);
		}

		// rescale bitmaps if necessary
		if (bTouch.getWidth() != (int) (mov - 1) || bTouch.getHeight() != (int) (mov - 1)) {
			bTouch = Bitmap.createScaledBitmap(bTouch, (int) mov - 1, (int) mov - 1, true);
			bFail = Bitmap.createScaledBitmap(bFail, (int) mov - 1, (int) mov - 1, true);
		}

		paint.setStyle(Style.FILL);
		// draw the map
		for (int j = 0; j < Engine.SIZE_MAP; ++j) {
			for (int i = 0; i < Engine.SIZE_MAP; ++i) {
				int idx = i + j * Engine.SIZE_MAP;
				if (map[idx] == Engine.BOAT_TOUCHED) {
					canvas.drawBitmap(bTouch, x + mov * i, y + mov * j, paint);
				} else if (map[idx] == Engine.VOID_MISS) {
					canvas.drawBitmap(bFail, x + mov * i, y + mov * j, paint);
				}
			}
		}

		int highscore = engine.getHighscore();
		if (highscore != -1) {
			paint.setTextSize(width / TEXT_SIZE_FACTOR);
			paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);
			paint.setColor(Color.WHITE);
			canvas.drawText(txtHighScore + highscore, x + size / 2, y + paint.ascent(), paintStroke);
			canvas.drawText(txtHighScore + highscore, x + size / 2, y + paint.ascent(), paint);
		}
	}

	private void drawLeftInterface(Canvas canvas) {
		float w = width * LEFT_WIDTH;
		float h = height * LEFT_PART_BOMBS;

		int idealSizeW = (int) (w / 4 - 1);
		int idealSizeH = (int) (h / 10 - 1);
		int idealSize;
		if (idealSizeW < idealSizeH)
			idealSize = idealSizeW;
		else
			idealSize = idealSizeH;
		if (bBomb == null || bBomb.getWidth() != idealSize) {
			bBomb = Engine.decodeSampledBitmapFromResource(getResources(), R.drawable.bomb_ok, idealSizeW, idealSizeH);
			bBombDead = Engine.decodeSampledBitmapFromResource(getResources(), R.drawable.bomb_over, idealSizeW, idealSizeH);
		}

		// DRAW THE TEXT
		float x = (w - 3 * (idealSizeW + 1)) / 2;
		float y = (height - h) / 2;
		paint.setColor(Color.WHITE);
		paint.setTextSize(width / TEXT_SIZE_FACTOR);
		paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);
		paint.setStyle(Style.FILL);
		int score = engine.getBombsLeft();
		canvas.drawText("" + score, w - idealSizeW - width * MARGIN, y, paintStroke);
		canvas.drawText("" + score, w - idealSizeW - width * MARGIN, y, paint);

		// DRAW THE BOMBS
		y += h * MARGIN;
		for (int i = 0; i < Engine.NB_BOMBS; ++i) {
			if (i != 0 && i % 3 == 0)
				y += idealSizeH + 1;
			if (i < Engine.NB_BOMBS - engine.getBombsLeft())
				canvas.drawBitmap(bBombDead, x + i % 3 * (idealSizeW + 1), y, paint);
			else
				canvas.drawBitmap(bBomb, x + i % 3 * (idealSizeW + 1), y, paint);
		}
	}

	private void drawRightInterface(Canvas canvas) {
		int idealSize = (int) (width * RIGHT_PART_SIZE / 3 - 1);
		if (idealSize != 0 && bSquid.getWidth() != idealSize) {
			bSquid = Bitmap.createScaledBitmap(bSquid, idealSize, idealSize, true);
			bSquidKilled = Bitmap.createScaledBitmap(bSquidKilled, idealSize, idealSize, true);
		}
		int idealSize2 = engine.getSize() / Engine.SIZE_MAP;
		if (idealSize2 != 0 && bSquidResultH.getWidth() != idealSize2) {
			bSquidResultH = Bitmap.createScaledBitmap(bSquid, idealSize2, idealSize2, true);
			bSquidResultV = Bitmap.createScaledBitmap(bSquid, idealSize2, idealSize2, true);
		}
		float x = width * (1 - RIGHT_PART_SIZE);
		float y = height * (1 - RIGHT_PART_ENNEMIES) / 2;

		int boatsLeft = engine.getBoatsLeft();
		int totalBoats = engine.getBoats().size();
		for (int i = 0; i < totalBoats; ++i) {
			if (i < totalBoats - boatsLeft)
				canvas.drawBitmap(bSquidKilled, x + width * RIGHT_PART_SIZE / 2 - idealSize / 2, y, paint);
			else
				canvas.drawBitmap(bSquid, x + width * RIGHT_PART_SIZE / 2 - idealSize / 2, y, paint);
			y += idealSize + 1;
		}
	}

	private void drawGameOver(Canvas canvas) {
		paint.setColor(Color.WHITE);
		paint.setTextSize((int) (width / TEXT_SIZE_FACTOR * 2));
		paintStroke.setTextSize(paint.getTextSize());
		float textHeight = paintStroke.getFontMetrics(null);

		if (engine.getBoatsLeft() == 0) {
			canvas.drawText(txtWin, width / 2, height / 2 - textHeight * 1.25f, paintStroke);
			canvas.drawText(txtWin, width / 2, height / 2 - textHeight * 1.25f, paint);
			paint.setTextSize(width / TEXT_SIZE_FACTOR);
			paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);
			canvas.drawText("Score: " + (Engine.NB_BOMBS - engine.getBombsLeft()), width / 2, height / 2, paintStroke);
			canvas.drawText("Score: " + (Engine.NB_BOMBS - engine.getBombsLeft()), width / 2, height / 2, paint);

			paint.setTextSize(width / TEXT_SIZE_FACTOR);
			paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);
			canvas.drawText(txtRestart, width / 3, height / 2 + textHeight * 1.25f, paintStroke);
			canvas.drawText(txtRestart, width / 3, height / 2 + textHeight * 1.25f, paint);

			canvas.drawText(txtShare, 2 * width / 3, height / 2 + textHeight * 1.25f, paintStroke);
			canvas.drawText(txtShare, 2 * width / 3, height / 2 + textHeight * 1.25f, paint);
		} else {
			canvas.drawText(txtMwahaha, width / 2, height / 2 - textHeight * 1.25f, paintStroke);
			canvas.drawText(txtMwahaha, width / 2, height / 2 - textHeight * 1.25f, paint);
			canvas.drawText(txtLose, width / 2, height / 2, paintStroke);
			canvas.drawText(txtLose, width / 2, height / 2, paint);

			paint.setTextSize(width / TEXT_SIZE_FACTOR);
			paintStroke.setTextSize(width / TEXT_SIZE_FACTOR);
			canvas.drawText(txtRestart, width / 2, height / 2 + textHeight * 1.25f, paintStroke);
			canvas.drawText(txtRestart, width / 2, height / 2 + textHeight * 1.25f, paint);
		}
	}

	private void drawResults(Canvas canvas) {
		float incr = (float) engine.getSize() / Engine.SIZE_MAP;
		float x0 = engine.getX();
		float y0 = engine.getY();
		ArrayList<Boat> boats = engine.getBoats();
		if (boats == null)
			return;
		for (Boat b : boats) {
			if (engine.getStatus() != Engine.GameStatus.GAME_SHOW_RESULTS && engine.getStatus() != Engine.GameStatus.GAME_OVER)
				return;
			int[][] coords = b.getCoords();
			if (matrix == null) {
				matrix = new Matrix();
				rectOrigin = new RectF(0, 0, incr, incr);
			}
			rectDest.set(x0 + coords[0][0] * incr, y0 + coords[0][1] * incr, x0 + coords[coords.length - 1][0] * incr + incr, y0 + coords[coords.length - 1][1]
					* incr + incr);
			if (!b.isVertical()) {
				matrix.reset();
				matrix.setRectToRect(rectOrigin, rectDest, Matrix.ScaleToFit.FILL);
				matrix.preRotate(-90, bSquidResultH.getWidth() / 2, bSquidResultH.getWidth() / 2);
				canvas.drawBitmap(bSquidResultH, matrix, null);
			} else {
				matrix.reset();
				matrix.setRectToRect(rectOrigin, rectDest, Matrix.ScaleToFit.FILL);
				canvas.drawBitmap(bSquidResultV, matrix, null);
			}
			// canvas.drawRect(x0 + coords[0][0] * incr, y0 + coords[0][1] *
			// incr, x0 + coords[coords.length - 1][0] * incr + incr, y0
			// + coords[coords.length - 1][1] * incr + incr, paintStrokeRed);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float ex = event.getX();
		float ey = event.getY();

		if (event.getAction() == MotionEvent.ACTION_UP) {
			engine.touched(ex, ey);
			if (thread != null)
				thread.repaint();
		}
		return true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Log.d("renderer", "surface changed");
		thread.repaint();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		// Log.d("renderer", "surface created");
		thread = new SplushThread();
		thread.setPause(false);
		thread.setRunning(true);
		thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Log.d("renderer", "surface destroyed");
		// thread.setPause(true);
		destroy();
	}

	private class SplushThread extends Thread {

		private boolean	running;
		private boolean	pause;

		public SplushThread() {
		}

		@Override
		public void run() {
			while (running) {
				while (pause) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
				try {
					Thread.sleep(REFRESH_RATE);
				} catch (InterruptedException e) {
				}
				if (running) {
					repaint();
				}
			}
			// Log.d("renderer", "thread stopped running");
		}

		private synchronized void setRunning(boolean running) {
			this.running = running;
		}

		private synchronized void setPause(boolean pause) {
			this.pause = pause;
		}

		@SuppressWarnings("unused")
		private boolean getRunning() {
			return running;
		}

		private void repaint() {
			Canvas canvas = null;
			try {
				canvas = getHolder().lockCanvas();
				synchronized (getHolder()) {
					draw(canvas);
				}
			} finally {
				if (canvas != null) {
					getHolder().unlockCanvasAndPost(canvas);
				}
			}
		}
	}

	public void setPause(boolean b) {
		if (thread != null)
			thread.setPause(b);
	}

	public void destroy() {
		// Log.d("FullScreen", "render destroyed");
		boolean retry = true;
		thread.setPause(false);
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	public void playCurtainTransition(long animStopTime) {
		curtainAnimTimeEnd = animStopTime;
	}

	public static float[] slideAnim(float orig, float arr, long timeMs, long refreshRateMs) {
		if (2 * timeMs < refreshRateMs) {
			return new float[] { arr };
		}
		float[] toReturn = new float[(int) (timeMs / refreshRateMs)];

		for (int i = 0; i < toReturn.length; ++i) {
			toReturn[i] = (arr - orig) / toReturn.length * i;
		}
		return toReturn;
	}
}
