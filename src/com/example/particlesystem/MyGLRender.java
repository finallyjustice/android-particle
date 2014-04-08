/** File:		MyGLRender.java
 ** Author:		Dongli Zhang
 ** Contact:	dongli.zhang0129@gmail.com
 **
 ** Copyright (C) Dongli Zhang 2013
 **
 ** This program is free software;  you can redistribute it and/or modify
 ** it under the terms of the GNU General Public License as published by
 ** the Free Software Foundation; either version 2 of the License, or
 ** (at your option) any later version.
 **
 ** This program is distributed in the hope that it will be useful,
 ** but WITHOUT ANY WARRANTY;  without even the implied warranty of
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 ** the GNU General Public License for more details.
 **
 ** You should have received a copy of the GNU General Public License
 ** along with this program;  if not, write to the Free Software 
 ** Foundation, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package com.example.particlesystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

public class MyGLRender implements GLSurfaceView.Renderer
{	
	private float vert[];
	private FloatBuffer vertexBuffer;
	SPHSystem sph;
	int window_width;
	int window_height;
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) 
	{
		// init SPH System
		sph = new SPHSystem(new Vec2f(0.64f, 0.64f));
		// init all particles
		sph.InitFluid();
		// create the vertex buffer for particles
		vert = new float[sph.GetNumParticle()*2];
		
		int numParticle = sph.GetNumParticle();
		for(int i=0; i<numParticle; i++)
		{
			vert[i*2]   = sph.GetList().get(i).pos.x;
			vert[i*2+1] = sph.GetList().get(i).pos.y;
		}
		
		ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vert.length*4);
		vertexByteBuffer.order(ByteOrder.nativeOrder());
		vertexBuffer = vertexByteBuffer.asFloatBuffer();
		vertexBuffer.put(vert);
		vertexBuffer.position(0);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) 
	{
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, 0.0f, sph.GetWorldSize().x, 0.0f, sph.GetWorldSize().y);
		
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		window_width  = width;
		window_height = height;
	}

	@Override
	public void onDrawFrame(GL10 gl) 
	{
		// for each iteration, animate the system and generate one frame
		sph.animation();
		int numParticle = sph.GetNumParticle();
		
		// copy the particle position to vertex buffer
		for(int i=0; i<numParticle; i++)
		{
			vert[i*2]   = sph.GetList().get(i).pos.x;
			vert[i*2+1] = sph.GetList().get(i).pos.y;
		}
		
		vertexBuffer.put(vert);
		vertexBuffer.position(0);
		
		// smooth point (particle)
		gl.glEnable(GL10.GL_POINT_SMOOTH);
		// set background to light green
		gl.glClearColor(0.0f, 1.0f, 1.0f, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT|GL10.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
		gl.glPointSize(20.0f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
		// main function to draw points
		gl.glDrawArrays(GL10.GL_POINTS, 0, sph.GetNumParticle());
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
	
	public SPHSystem GetSPH()
	{
		return sph;
	}
	
	public int GetWindowWidth()
	{
		return window_width;
	}
	
	public int GetWindowHeight()
	{
		return window_height;
	}
}