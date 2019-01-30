package com.voicesync.penrose;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.FloatMath;

public class Penrose {
	final float PI = 3.141592f;
	/* following two parameters are PostScript linewidths (in cm) */
	int MAX = 100; // Increase this to make more tiles 

	/* --- variables used only by 'plot' subroutine --- */
	float window;
	float oldx, oldy;
	float color;
	int fillon;
	float gray;

	// --- variables used by whole program ---
	int[] midon = new int[2];
	float scale;
	char[] prgname;
	float offsetx, offsety;
	int oldflag, rotate;
	float xcen, ycen; // center of picture
	int symmetry;
	int zfill;
	int maxmax;

	// sets
	public void setWindow(float window) {
		this.window = window;
	}
	public void setWindowScale(float window, float scale) {
		this.window = window;
		this.scale  = scale;
	}
	public void setFill(boolean fillon) {
		this.zfill = this.fillon = fillon ? 1 : 0;
	}
	public void setGrayScale() {
		colorFrom=Color.WHITE; colorTo=Color.BLACK;
	}
	public void setColorRange(int colorFrom, int colorTo) {
		this.colorFrom=colorFrom;
		this.colorTo=colorTo;
	}

	// drawing stuff
	Paint paint;
	Path path;
	Canvas canvas;
	float scx, scy;
	int colorFrom=Color.WHITE, colorTo=Color.BLACK;

	Penrose() {
		defaultValues();
	}

	public void draw(Canvas canvas) {
		this.canvas = canvas;
		paint 	= new Paint();
		path 	= new Path();
		scx 	= canvas.getWidth()	 / window;
		scy 	= canvas.getHeight() / window; // scale to canvas
		startLap();
			quasi();
		drawLap();
	}

	long lap;
	void startLap() { lap=System.currentTimeMillis();}
	void drawLap() {
		paint.setTextSize(23);
		paint.setColor(Color.BLACK);
		canvas.drawText(String.format("lap=%.2f", (float)(System.currentTimeMillis()-lap)/1000f), 0, 20, paint);
	}
	
	void defaultValues() {
		float magnifya;

		// --- set up defaults ---
		window = 20;
		scale = 20; // 15 cm wide boxes
		magnifya = 1; // no magnification
		rotate = 0; // don't rotate picture
		fillon = 0; // don't do polygon fill
		midon[0] = 0; // don't connect midpoints
		midon[1] = 0; // don't connect midpoints
		symmetry = 5; // five-fold symmetry
		zfill = 0; // don't use zfill option
		maxmax = 30;

		xcen = 0;
		ycen = 0;
		window = window / magnifya;
		// --- done with initialization stuff

		offsetx = 0; // lower left corner of picture
		offsety = 0;
	}

	public void quasi() {
		int[] index = new int[50];

		int t, r, i, m, n, flag;
		float[] vx = new float[50], vy = new float[50], mm = new float[50];
		float[][] b = new float[50][MAX];
		float phi, x0, y0, x1, y1, dx;
		/* v's are vectors, m's are slopes, b's are intercepts */
		float midx1, midx2, midx3, midx4, midy1, midy2, midy3, midy4;
		float dx1, dx2, dy1, dy2, dist1, dist2;
		int themin, themax;
		float minmin, rad1, rad2, rad;
		int halfmax;
		int midsix = 0;
		int type, segtype;

		halfmax = maxmax / 2;

		for (t = 0; t < symmetry; t++) {
			phi = (t * 2f) / (1f*symmetry) * PI;
			vx[t] = FloatMath.cos(phi);
			vy[t] = FloatMath.sin(phi);
			mm[t] = vy[t] / vx[t];
			for (r = 0; r < maxmax; r++) {
				y1 = vy[t] * (t * 0.1132f) - vx[t] * (r - halfmax); /* offset */
				x1 = vx[t] * (t * 0.2137f) + vy[t] * (r - halfmax);
				b[t][r] = y1 - mm[t] * x1; /* intercept */
			}
		}

		/*
		 * t is 1st direction, r is 2nd. look for intersection between pairs of
		 * lines in these two directions. (will be x0,y0)
		 */

		color = 0.2f;
		themax = (maxmax - 1);
		themin = themax / 2;
		for (minmin = 0; minmin <= (float) (themax); minmin += 0.4) {
			rad1 = minmin * minmin;
			rad2 = (minmin + 0.4f) * (minmin + 0.4f);
			for (n = 1; n < themax; n++) {
				for (m = 1; m < themax; m++) {
					rad = (float) ((n - themin) * (n - themin) + (m - themin)
							* (m - themin));
					/* rad = (float)(n*n+m*m); */
					if ((rad >= rad1) && (rad < rad2)) {

						for (t = 0; t < (symmetry - 1); t++) {
							for (r = t + 1; r < symmetry; r++) {
								x0 = (b[t][n] - b[r][m]) / (mm[r] - mm[t]);
								y0 = mm[t] * x0 + b[t][n];
								flag = 0;
								for (i = 0; i < symmetry; i++) {
									if ((i != t) && (i != r)) {
										dx = -x0 * vy[i] + (y0 - b[i][0])
												* vx[i];
										index[i] = (int) -dx;
										if ((index[i] > (maxmax - 3))
												|| (index[i] < 1))
											flag = 1;
									}
								}
								if (flag == 0) {
									index[t] = n - 1;
									index[r] = m - 1;
									x0 = 0;
									y0 = 0;
									for (i = 0; i < symmetry; i++) {
										x0 += vx[i] * index[i];
										y0 += vy[i] * index[i];
									}
									if (midon[0] > 0)
										gray = 0.8f; /* faint lines */
									/* color of tile unless zfill==1 */
									color += 0.05f;
									if (color > 1)
										color = 0.2f;
									if (zfill == 1) {
										color = 0;
										for (i = 0; i < symmetry; i++) {
											color += index[i];
										}
										while (color > ((symmetry - 1) / 2))
											color -= ((symmetry - 1) / 2);
										color = color / ((symmetry - 1) / 2)
												* .8f + .1f;
										color += Math.abs(vx[t] * vx[r] + vy[t]
												* vy[r]); /* dot product */
										if (color > 1)
											color -= 1;
									}
									plot(x0, y0, 0);
									x0 += vx[t];
									y0 += vy[t];
									plot(x0, y0, 1);
									x0 += vx[r];
									y0 += vy[r];
									plot(x0, y0, 1);
									x0 -= vx[t];
									y0 -= vy[t];
									plot(x0, y0, 1);
									x0 -= vx[r];
									y0 -= vy[r];
									plot(x0, y0, 2);
									if (midon[0] > 0) {
										midx1 = x0 + vx[t] * 0.5f;
										midy1 = y0 + vy[t] * 0.5f;
										midx2 = x0 + vx[t] + vx[r] * 0.5f;
										midy2 = y0 + vy[t] + vy[r] * 0.5f;
										midx3 = x0 + vx[r] + vx[t] * 0.5f;
										midy3 = y0 + vy[r] + vy[t] * 0.5f;
										midx4 = x0 + vx[r] * 0.5f;
										midy4 = y0 + vy[r] * 0.5f;
										dx1 = midx1 - midx2;
										dy1 = midy1 - midy2;
										dist1 = dx1 * dx1 + dy1 * dy1;
										dx2 = midx2 - midx3;
										dy2 = midy2 - midy3;
										dist2 = dx2 * dx2 + dy2 * dy2;
										gray = 0; /* dark lines */
										if (dist1 * dist2 < .1)
											type = 0;
										else
											type = 1;
										segtype = midon[type];
										if ((segtype == 1) || (segtype == 2)) {
											if (dist1 > dist2)
												segtype = 3 - segtype;
										} else if (segtype == 5) {
											midsix = 1 - midsix;
											segtype = midsix + 1;
										} else if (segtype == 6) {
											midsix++;
											if (midsix > 2)
												midsix = 0;
											segtype = midsix + 1;
										}

										if (segtype == 3) {
											/* X's */
											segment(midx1, midy1, midx3, midy3);
											segment(midx2, midy2, midx4, midy4);
										} else if (segtype == 1) {
											segment(midx1, midy1, midx2, midy2);
											segment(midx3, midy3, midx4, midy4);
										} else if (segtype == 2) {
											segment(midx1, midy1, midx4, midy4);
											segment(midx2, midy2, midx3, midy3);
										} else if (segtype == 4) {
											/* boxes */
											segment(midx1, midy1, midx2, midy2);
											segment(midx3, midy3, midx4, midy4);
											segment(midx1, midy1, midx4, midy4);
											segment(midx2, midy2, midx3, midy3);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	void segment(float x1, float y1, float x2, float y2) {
		plot(x1, y1, 0);
		plot(x2, y2, 2);
	}

	void plot(float x, float y, int plotflag) {
		float dx, dy, swap;
		float cmx, cmy; // x,y in centimeters */
		// flag variable: 0 = start line; 1 = lineto; 2 = endpoint

		dx = getdx(x, xcen);
		dy = getdx(y, ycen);
		if (rotate == 1) {
			swap = dx;
			dx = dy;
			dy = swap;
		}

		if ((dx < 1.3) && (dy < 1) && (dx > 0) && (dy > 0)) { // in window
			cmx = dx * scale + offsetx;
			cmx *= scx;
			cmy = dy * scale + offsety;
			cmy *= scy;
			if (plotflag < 1) {
				path.moveTo(cmx, cmy);
			} else {
				if (oldflag == 1)
					path.moveTo(cmx, cmy);
				path.lineTo(cmx, cmy);
				if (plotflag == 2) {
					if (fillon == 1) {
						path.close();

						paint.setColor(Color.BLACK); // draw stroke black
						paint.setStyle(Paint.Style.STROKE);
						canvas.drawPath(path, paint);

						paint.setColor(interpolateColor(colorFrom, colorTo, color)); // fill color
						paint.setStyle(Paint.Style.FILL);
						canvas.drawPath(path, paint);

						path.reset();
					} else {
						path.close();

						paint.setColor(Color.BLACK); // draw stroke black
						paint.setStyle(Paint.Style.STROKE);
						canvas.drawPath(path, paint);
						path.reset();
					}
					if (midon[0] > 0) {
					} // printf("%.1f sg\n",gray);
				}
			}
			oldflag = 0;
		} else {
			oldflag = 1;
		}

	}

	private static float interpolate(float a, float b, float proportion) {
		return (a + ((b - a) * proportion));
	}

	// Returns an interpoloated color, between a and b
	public static int interpolateColor(int a, int b, float proportion) {
		float[] hsva = new float[3];
		float[] hsvb = new float[3];
		Color.colorToHSV(a, hsva);
		Color.colorToHSV(b, hsvb);
		for (int i = 0; i < 3; i++) {
			hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
		}
		return Color.HSVToColor(hsvb);
	}

	float getdx(float x, float center) {
		float dx;

		dx = (x - center) / window;
		dx = 0.5f * (dx + 1);
		/* dx : 0 = left/bottom, +1 = right/top */
		return dx;
	}
}

