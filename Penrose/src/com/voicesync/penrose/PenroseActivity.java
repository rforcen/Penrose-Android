package com.voicesync.penrose;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PenroseActivity extends Activity implements ColorPickerDialog.OnColorChangedListener, OnClickListener {

	private GLSurfaceView glSurface;
	private OGLRenderer   renderer;

	int colorDart=Color.BLUE, colorKite=Color.RED, rec=1;
	boolean fill, bar;

	int dt=0; // dart=0, kite=1 in color selection
	String[]mt={"dart color", "kite color", "config", "save", "reset"};

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_penrose);

		glSurface=(GLSurfaceView)findViewById(R.id.glSurface); // assign the renderer
		renderer=new OGLRenderer(this);
		glSurface.setRenderer(renderer);
		glSurface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // stop animation
	}
	@Override public boolean onTouchEvent(MotionEvent event) {
		float  w=renderer.getWidth(), h=renderer.getHeight(), w2=w/2, h2=h/2;
		float 	x = event.getX(), y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
			float mf=5;
			renderer.setOffset(mf*(x-w2)/w, -mf*(y-h2)/h);
			break;
		case MotionEvent.ACTION_DOWN:
			break;
		}
		glSurface.requestRender();

		return true;
	}
	@Override public boolean onCreateOptionsMenu(Menu menu) {
		for (int i=0; i<mt.length; i++) menu.add(Menu.NONE, i, i, mt[i]);
		return true;
	}
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		doMenu(item.getItemId());
		return false;
	}
	void doMenu(int item) {
		switch (item) {
		case 0:	dt=0; new ColorPickerDialog(this, this, colorDart).show();	break;
		case 1:	dt=1; new ColorPickerDialog(this, this, colorKite).show();	break;
		case 2: SettingsDialog();											break;
		case 3: renderer.requestScreenShot(); 								break;
		case 4: renderer.reset(); 											break;
		}
		glSurface.requestRender();
	}
	void SettingsDialog() {
		final Dialog settingsDlg = new Dialog(this, android.R.style.Theme_Panel);
		settingsDlg.setTitle("Settings");
		LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.zoom_dialog, (ViewGroup)findViewById(R.id.zoomLayout));
		settingsDlg.setContentView(layout);
		final 	SeekBar sbZoom 	= (SeekBar)layout.findViewById(R.id.sbZoom),
						sbRec  	= (SeekBar)layout.findViewById(R.id.sbRec),
						sbXoffset= (SeekBar)layout.findViewById(R.id.sbXoffset);
		final	Button btnOk	= (Button)layout.findViewById(R.id.btnOk);
		final	CheckBox 	cbFill	= 	(CheckBox)layout.findViewById(R.id.cbFill),
							cbBar  	=	(CheckBox)layout.findViewById(R.id.cbBar);
		sbZoom.setProgress(renderer.getZoom()); // set initial values
		sbRec .setProgress(renderer.getRec());
		sbXoffset.setProgress(renderer.getXOffset());
		cbFill.setChecked(renderer.getFill());
		cbBar.setChecked(renderer.getBar());

		OnSeekBarChangeListener zoomSeekBarListener = new OnSeekBarChangeListener() {
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (seekBar==sbZoom)  		renderer.setZoom(progress);
				if (seekBar==sbRec)  		renderer.setRec(progress+1);
				if (seekBar==sbXoffset)  	renderer.setXOffset(progress);
				glSurface.requestRender();	    	
			}
		};
		sbZoom		.setOnSeekBarChangeListener(zoomSeekBarListener);
		sbRec 		.setOnSeekBarChangeListener(zoomSeekBarListener);
		sbXoffset	.setOnSeekBarChangeListener(zoomSeekBarListener);
		cbFill.setOnClickListener(new OnClickListener () {
			@Override public void onClick(View v) { renderer.setFill(((CheckBox)v).isChecked()); glSurface.requestRender();}	 });
		cbBar.setOnClickListener(new OnClickListener () {
			@Override public void onClick(View v) { renderer.setBar(((CheckBox)v).isChecked()); glSurface.requestRender();}	 });
		btnOk .setOnClickListener(new OnClickListener () {
			@Override public void onClick(View v) { settingsDlg.dismiss(); }
		});
		settingsDlg.show();
	}
	@Override public void colorChanged(int color) { // ColorPickerDialog
		switch (dt) {
		case 0: colorDart=color; renderer.setclDart(colorDart); break;
		case 1: colorKite=color; renderer.setclKite(colorKite); break;
		}
		glSurface.requestRender();
	}

	private void menuDialog() { // select the sample rate from a list
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("menu");
		alert.setItems(mt, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				doMenu(item);
			}
		});
		alert.show();
	}
	@Override public void onClick(View v) { 	}
}

/////////////
//renderer, important: methods requesting long time should be 'synchronized'
/////////////
class OGLRenderer implements GLSurfaceView.Renderer  
{
	PenroseGL prGL=new PenroseGL();
	float zoom=-5.3f, incz=0.3f;
	float xoff=0, yoff=0;
	int rec=1;
	boolean fill=false, bar=false;
	int w,h;
	boolean screenshot=false;
	String fnScreenShot=Environment.getExternalStorageDirectory().getPath()+"/"+"Penrose.png";

	OGLRenderer() {	}
	OGLRenderer(Context context) {}

	int getWidth() {return w;}
	int getHeight() { return h;}
	public void setAll(int cd, int ck, boolean f, boolean b, int re, float z) {
		prGL.setcolorDart(cd);
		prGL.setcolorDart(ck);
		fill=f; bar=b;
		rec=re;
		zoom=z;
	}
	public void setclDart(int color) 	{ prGL.setcolorDart(color); }
	public void setclKite(int color) 	{ prGL.setcolorKite(color); }
	public void setZoom(int zoom)  	{ this.zoom=zoom/10-10; }
	public int  getZoom()  			{ return (int)((zoom+10)*10); } 
	public int	 getRec()  				{ return rec; }
	public void setRec(int rec)  		{ this.rec=rec; }
	public void incZoom() 	{ zoom+=incz; }
	public void decZoom() 	{ zoom-=incz; }
	public void swFill()	{ fill=!fill; }
	public void swBar()	{ bar=!bar; }
	public boolean getFill() { return fill; }
	public void setFill(boolean fill) { this.fill=fill; }
	public boolean getBar() { return bar; }
	public void setBar(boolean bar) { this.bar=bar; }

	public void setXOffset(float x)  { this.xoff=x/50; }
	public int  getXOffset()  { return (int)(this.xoff*50); }	
	public void setOffset(float x, float y) { this.xoff=x; this.yoff=y; } 
	public void reset() {zoom=-5.3f; rec=1; fill=bar=false; xoff=yoff=0; }

	@Override	public synchronized void onDrawFrame(GL10 gl) {
		// Clear the screen to black
		gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		// Position model so we can see it
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glTranslatef(xoff, yoff, zoom);

		// draw the model
		prGL.draw(gl, rec, fill, bar);

		getScreenShot(gl); // click!!
	}
	@Override	public void onSurfaceChanged(GL10 gl, int w, int h) {
		gl.glViewport(0, 0, w, h);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		float ratio = (float) w / h;
		GLU.gluPerspective(gl, 45.0f, ratio, 1, 100f); 
		this.w=w; this.h=h;
	}
	@Override	public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
		gl.glEnable(GL10.GL_DEPTH_TEST); 
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisable(GL10.GL_DITHER);
	}
	void requestScreenShot() {	screenshot=true; }
	void getScreenShot(GL10 gl) {
		if(screenshot){                     
			int screenshotSize = w * h;
			ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
			bb.order(ByteOrder.nativeOrder());
			gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
			int pixelsBuffer[] = new int[screenshotSize];
			bb.asIntBuffer().get(pixelsBuffer);
			bb = null;
			Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
			bitmap.setPixels(pixelsBuffer, screenshotSize-w, -w, 0, 0, w, h);
			pixelsBuffer = null;

			short sBuffer[] = new short[screenshotSize];
			ShortBuffer sb = ShortBuffer.wrap(sBuffer);
			bitmap.copyPixelsToBuffer(sb);

			//Making created bitmap (from OpenGL points) compatible with Android bitmap
			for (int i = 0; i < screenshotSize; ++i) {                  
				short v = sBuffer[i];
				sBuffer[i] = (short) (((v&0x1f) << 11) | (v&0x7e0) | ((v&0xf800) >> 11));
			}
			sb.rewind();
			bitmap.copyPixelsFromBuffer(sb);

			// save bitmap to screenshot file
			try {
				Bitmap.createBitmap(bitmap, 0, 0, w, h).compress(Bitmap.CompressFormat.PNG, 90, new FileOutputStream(fnScreenShot));
			} catch (Exception e) {	}
			screenshot = false;
		}
	}
}