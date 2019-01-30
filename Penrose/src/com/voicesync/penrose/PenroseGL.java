package com.voicesync.penrose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.util.FloatMath;

public class PenroseGL {
	// Constants
	final float phi   =1.6180339887498948f;
	final float sin36 =0.5877852522924731f;
	final float cos36 =0.8090169943749474f;
	final float sin72 =0.9510565162951536f;
	final float cos72 =0.3090169943749474f;

	// Global variables to implement various options: wireframe vs. filled,
	// Amman bars, zoom, pan, and numeric labeling.
	boolean fill = false, bar=false;
	int rec = 1;
	int colorDart=Color.BLUE, colorKite=Color.GREEN;

	GL10 gl;

	// assign a set of (float) coords to a FloatBuffer
	private FloatBuffer putCoords(float []coords)
	{
		FloatBuffer pntBuff;
		ByteBuffer vbb = ByteBuffer.allocateDirect(coords.length*4);
		vbb.order(ByteOrder.nativeOrder());
		pntBuff = vbb.asFloatBuffer();
		pntBuff.put(coords);
		pntBuff.position(0);
		return pntBuff;
	}

	public void setRecursion(int rec)		{ this.rec=rec; 	}
	public void setFill(boolean fill)		{ this.fill=fill; 	}
	public void setBar(boolean bar)		{ this.bar=bar; 	}
	public void setcolorDart(int color)	{ colorDart=color;  }
	public void setcolorKite(int color)	{ colorKite=color;  }
	void halfDart()
	{
		float dartPoints[] = {0,0,-cos72,sin72,1,0};
		gl.glColor4f(1, 0, 0, 1); //Draw darts in red.
		gl.glVertexPointer(  2, GL10.GL_FLOAT, 0, putCoords(dartPoints) );
		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 3);
		if (fill) {
			gl.glColor4f(Color.red(colorDart)/255f, Color.green(colorDart)/255f, Color.blue(colorDart)/255f, 1);
			gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 3);
		}
		if (bar) {
			gl.glLineWidth(3);
			gl.glColor4f(1, 0, 1, 1);
			gl.glVertexPointer(  3, GL10.GL_FLOAT, 0, putCoords(new float[]{
					dartPoints[4]/2, 0, 0,
					(dartPoints[2]+dartPoints[4])/2, (dartPoints[3]+dartPoints[5])/2, 0}));
			gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glColor4f(1, 1, 0, 1);
			gl.glVertexPointer(  3, GL10.GL_FLOAT, 0, putCoords(new float[]{
					dartPoints[2]/2, dartPoints[3]/2, 0,
					dartPoints[4]/2, 0, 0
					}) );
			gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glLineWidth(1);
		}
	}
	void halfKite()
	{
		float kitePoints[] = {0,0,cos36*phi,sin36*phi,phi,0};
		gl.glColor4f(0, 0, 1, 1); //Draw kites in blue.
		gl.glVertexPointer(  2, GL10.GL_FLOAT, 0, putCoords(kitePoints) );
		gl.glDrawArrays(GL10.GL_LINE_LOOP, 0, 3);
		if (fill) { 
			gl.glColor4f(Color.red(colorKite)/255f, Color.green(colorKite)/255f, Color.blue(colorKite)/255f, 1);
			gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 3);
		}
		if (bar) {
			gl.glLineWidth(3);
			gl.glColor4f(1, 0, 1, 1);
			gl.glVertexPointer(  3, GL10.GL_FLOAT, 0, putCoords(new float[]{
					kitePoints[2]/2, kitePoints[3]/2, 0,
					kitePoints[4]/2, 0, 0}));
			gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glColor4f(1, 1, 0, 1);
			gl.glVertexPointer(  3, GL10.GL_FLOAT, 0, putCoords(new float[]{
					(kitePoints[2]+kitePoints[4])/2, (kitePoints[3]+kitePoints[5])/2 , 0,
					kitePoints[4]/2, 0, 0					
					}) );
			gl.glDrawArrays(GL10.GL_LINES, 0, 2);
			gl.glLineWidth(1);
		}
	}
	void kiteGen(int n) 
	{
		gl.glPushMatrix();
		gl.glScalef(1/phi, 1/phi, 0);
		gl.glTranslatef((float)(phi+0.5f), FloatMath.sqrt(phi*phi-0.25f), 0);
		gl.glRotatef(-108, 0, 0, 1);
		if (n==0) halfKite();
		else kiteGen(n-1);
		gl.glRotatef(180, 1, 0, 0);
		if (n==0) halfKite();
		else kiteGen(n-1);
		gl.glTranslatef(cos36*phi, sin36*phi, 0);
		gl.glRotatef(-144, 0, 0, 1);
		gl.glRotatef(180, 0, 1, 0);
		if (n==0) halfDart();
		else dartGen(n-1);
		gl.glPopMatrix();
	}
	void dartGen(int n) 
	{
		gl.glPushMatrix();
		gl.glScalef(1/phi, 1/phi, 1);
		gl.glTranslatef(phi, 0, 0);
		gl.glRotatef(180, 0, 1, 0);
		if (n==0) halfKite();
		else kiteGen(n-1);
		gl.glTranslatef(cos36*phi, sin36*phi, 0);
		gl.glRotatef(180, 0, 1, 0);
		gl.glRotatef(144, 0, 0, 1);
		if (n==0) halfDart();
		else dartGen(n-1);
		gl.glPopMatrix();
	}
	void display()
	{
		dartGen(rec);		gl.glRotatef(180, 1, 0, 0);		dartGen(rec);

		gl.glPushMatrix();
		gl.glTranslatef(-phi, 0, 0); 	gl.glRotatef(-36, 0, 0, 1);		kiteGen(rec);		gl.glRotatef(180, 1, 0, 0);		kiteGen(rec);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslatef(-phi, 0, 0);	gl.glRotatef(36, 0, 0, 1);		kiteGen(rec);		gl.glRotatef(180, 1, 0, 0);		kiteGen(rec);
		gl.glPopMatrix();
	}	
	public void draw(GL10 gl, int rec, boolean fill, boolean bar) { // draw figure (recursive level, fill)
		this.gl=gl; this.rec=rec; this.fill=fill; this.bar=bar;
		display();
	}
}