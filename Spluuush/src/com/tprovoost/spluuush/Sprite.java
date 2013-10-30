package com.tprovoost.spluuush;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public abstract class Sprite {

	protected ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
	protected int idxAnim = 0;
	float x = -1;
	float y = -1;

	public boolean contains(float ex, float ey) {
		if (bitmaps == null || idxAnim < 0 || idxAnim >= bitmaps.size())
			return false;
		return ex >= x && ey >= y && ex <= x + bitmaps.get(idxAnim).getWidth() && ey <= y + bitmaps.get(idxAnim).getHeight();
	}

	public abstract void touched();

	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public void addBitmap(Bitmap bitmap) {
		bitmaps.add(bitmap);
	}

	public Bitmap getBitmap() {
		return bitmaps.get(idxAnim);
	}

	public void drawSprite(Canvas canvas, Paint paint) {
		if (idxAnim < bitmaps.size() && idxAnim >= 0)
			canvas.drawBitmap(getBitmap(), x, y, paint);
	}

	public void setIdxAnim(int idxAnim) {
		this.idxAnim = idxAnim;
	}

	public int getIdxAnim() {
		return idxAnim;
	}

	public void nextAnim() {
		idxAnim = ++idxAnim % bitmaps.size();
	}

	public void clear() {
		bitmaps.clear();
	}

}
