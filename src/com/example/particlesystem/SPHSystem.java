/** File:		SPHSystem.java
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

/* This file is the implementation of paper
 *  Particle-based fluid simulation for interactive applications, SCA 2003
 */

package com.example.particlesystem;

import java.util.ArrayList;
import java.util.Iterator;
import android.util.Log;

// definition of SPH System
public class SPHSystem 
{
	private float kernel;    // general kernel length of particle
	private float mass;      // general mass of particle
	
	private int numParticle; // total number particles
	
	private Vec2f worldSize; // simulation world width and height
	private Vec2i gridSize;  
	private float cellSize; // size of each cell
	private int totCell;    // total number of cells
	
	// params
	private Vec2f gravity;     // gravity x
	private float stiffness;   // gas constant
	private float restDensity; // rest density 
	private float timeStep; 
	private float wallDamping;
	private float viscosity;
	
	private ArrayList<Particle> list;  // list of particles for simulation
	private ArrayList<Cell> clist;     // list of cells for simulation
	
	private int external_event = 0;    // 1 means external event injected
	private float external_x;          // touched position
	private float external_y;          // touched position
	private float force_x; 			   // finger added force
	private float force_y;             // finger added force
	
	public SPHSystem(Vec2f w)
	{
		// init data structures
		worldSize = new Vec2f();
		gridSize  = new Vec2i();
		gravity   = new Vec2f();
		
		kernel = 0.04f;
		mass = 0.02f;
		
		numParticle = 0;
		
		worldSize.x = w.x;
		worldSize.y = w.y;
		cellSize = kernel;
		gridSize.x = (int)(w.x/cellSize);
		gridSize.y = (int)(w.y/cellSize);
		totCell = gridSize.x*gridSize.y;
		
		gravity.x = 0.0f;
		gravity.y = -1.8f;
		stiffness = 200.0f;
		restDensity = 5000.0f;
		timeStep = 0.01f;
		wallDamping = 0.0f;
		viscosity = 1.0f;
		
		list = new ArrayList<Particle>();
		clist = new ArrayList<Cell>();
		
		// init the grid system
		for(int i=0; i<totCell; i++)
		{
			Cell c = new Cell();
			clist.add(c);
		}
		
		//Log.i("DONGLI", "Grid X: "+gridSize.x);
		//Log.i("DONGLI", "Grid Y: "+gridSize.y);
		//Log.i("DONGLI", "TOT: "+totCell);
	}
	
	// init the particles
	public void InitFluid()
	{
		float initVelX = 0.0f;
		float initVelY = 0.0f;
		
		for(float i=worldSize.x*0.0f; i<=worldSize.x*0.8f; i+=kernel*0.8f)
		{
			for(float j=worldSize.y*0.0f; j<=worldSize.y*0.9f; j+=kernel*0.8f)
			{
				Particle p = new Particle();
				p.id    = numParticle;
				p.pos.x = i;
				p.pos.y = j;
				p.vel.x = initVelX;
				p.vel.y = initVelY;
				p.acc.x = 0.0f;
				p.acc.y = 0.0f;
				p.ev.x  = initVelX;
				p.ev.y  = initVelY;
				p.dens = restDensity;
				list.add(p);
				numParticle++;
			}
		}
	}
	
	public Vec2f GetWorldSize()
	{
		return this.worldSize;
	}
	
	public ArrayList<Particle> GetList()
	{
		return this.list;
	}
	
	public int GetNumParticle()
	{
		return this.numParticle;
	}
	
	// given the x and y of cell in grid system, return the index of cell in arraylist
	private int CalcCellHash(int x, int y)
	{
		if(x<0 || x>=gridSize.x || y<0 || y>=gridSize.y)
		{
			return -1;
		}
		
		int hash = y*gridSize.x + x;
		if(hash >= totCell)
		{
			Log.e("DONGLI", "Error at computing hash");
		}
		
		return hash;
	}
	
	// compute the cell index of each particle and assign particles to each cell
	private void BuildGrid()
	{
		// clear each cell
		for(int i=0; i<totCell; i++)
		{
			Cell c = (Cell)clist.get(i);
			c.list.clear();
		}
		
		// assign each particle to its corresponding cell
		for(int i=0; i<numParticle; i++)
		{
			Particle p = list.get(i);
			int x = (int)(p.pos.x/cellSize);
			int y = (int)(p.pos.y/cellSize);
			int hash = CalcCellHash(x, y);
			Cell c = (Cell)clist.get(hash);
			c.list.add(p);
		}
	}
	
	// compute density and pressure for each particle
	private void CompDensPressure()
	{
		Vec2i cellPos = new Vec2i();;
		Vec2i nearPos = new Vec2i();
		int hash;
		
		for(int k=0; k<numParticle; k++)
		{
			// for each particle in the simulation
			Particle p = list.get(k);
			p.dens = 0.0f;
			p.pres = 0.0f;
			
			// compute the position of particle in grid
			cellPos.x = (int)(p.pos.x/cellSize);
			cellPos.y = (int)(p.pos.y/cellSize);
			
			for(int i=-1; i<=1; i++)
			{
				for(int j=-1; j<=1; j++)
				{
					nearPos.x = cellPos.x + i;
					nearPos.y = cellPos.y + j;
					hash = CalcCellHash(nearPos.x, nearPos.y);
					if(hash == -1) continue;
					
					Cell c = clist.get(hash);
					
					for(Iterator<Particle> iter=c.list.iterator(); iter.hasNext(); )
					{
						Particle np = (Particle)iter.next();
						
						float distVecX = np.pos.x - p.pos.x;
						float distVecY = np.pos.y - p.pos.y;
						float dist2 = distVecX*distVecX+distVecY*distVecY;

						if(dist2<0.000001f || dist2>=kernel*kernel)
						{
							continue;
						}

						// for all neighboring particles within the kernel length of this particle
						p.dens = p.dens + mass * poly6(dist2);
					}
				}
			}
			
			// the final smoothed density of this particle
			p.dens = p.dens + mass*poly6(0.0f);
			p.pres = (float) ((Math.pow(p.dens / restDensity, 7) - 1) * stiffness);
		}
	}
	
	// compute final smoothed force for each particle
	private void CompForce()
	{
		Vec2i cellPos = new Vec2i();;
		Vec2i nearPos = new Vec2i();
		int hash;
		
		for(int k=0; k<numParticle; k++)
		{
			// for each particle in the simulation
			Particle p = list.get(k);
			p.acc.x = 0.0f;
			p.acc.y = 0.0f;
			
			// compute the position of particle in grid
			cellPos.x = (int)(p.pos.x/cellSize);
			cellPos.y = (int)(p.pos.y/cellSize);
			
			for(int i=-1; i<=1; i++)
			{
				for(int j=-1; j<=1; j++)
				{
					nearPos.x = cellPos.x + i;
					nearPos.y = cellPos.y + j;
					hash = CalcCellHash(nearPos.x, nearPos.y);
					if(hash == -1) continue;
					
					Cell c = clist.get(hash);
					
					for(Iterator<Particle> iter=c.list.iterator(); iter.hasNext(); )
					{
						Particle np = (Particle)iter.next();
						//Log.i("DONGLI", "Pos: "+np.pos.x+", "+np.pos.y);
						
						float distVecX = np.pos.x - p.pos.x;
						float distVecY = np.pos.y - p.pos.y;
						float dist2 = distVecX*distVecX+distVecY*distVecY;
						if(dist2<0.000001f || dist2>=kernel*kernel)
						{
							continue;
						}

						float dist = (float) Math.sqrt(dist2);
						float V = mass/p.dens;
						
						float tempForce = V * (p.pres+np.pres) * spiky(dist);
						p.acc.x = p.acc.x - distVecX*tempForce/dist;
						p.acc.y = p.acc.y - distVecY*tempForce/dist;
						
						float relVelX = np.ev.x-p.ev.x;
						float relVelY = np.ev.y-p.ev.y;
						tempForce = V * viscosity * visco(dist);
						p.acc.x = p.acc.x + relVelX*tempForce;
						p.acc.y = p.acc.y + relVelY*tempForce;
					}
				}
			}
			
			// final acceleration of this particle
			p.acc.x = p.acc.x/p.dens + gravity.x;
			p.acc.y = p.acc.y/p.dens + gravity.y;
		}
	}
	
	// advect and move each particle to new position
	private void Advection()
	{
		for(int i=0; i<numParticle; i++)
		{
			Particle p = list.get(i);
			
			p.vel.x = p.vel.x+p.acc.x*timeStep;
			p.vel.y = p.vel.y+p.acc.y*timeStep;
			
			p.pos.x = p.pos.x+p.vel.x*timeStep;
			p.pos.y = p.pos.y+p.vel.y*timeStep;
			
			if(p.pos.x < 0.0f)
			{
				p.vel.x = p.vel.x * wallDamping;
				p.pos.x = 0.0f;
			}
			if(p.pos.x >= worldSize.x)
			{
				p.vel.x = p.vel.x * wallDamping;
				p.pos.x = worldSize.x - 0.0001f;
			}
			if(p.pos.y < 0.0f)
			{
				p.vel.y = p.vel.y * wallDamping;
				p.pos.y = 0.0f;
			}
			if(p.pos.y >= worldSize.y)
			{
				p.vel.y = p.vel.y * wallDamping;
				p.pos.y = worldSize.y - 0.0001f;
			}
			
			p.ev.x = (p.ev.x+p.vel.x)/2;
			p.ev.y = (p.ev.y+p.vel.y)/2;
		}
	}
	
	public void animation()
	{
		this.BuildGrid();           // compute the cell index of each particle and assign particles to each cell
		this.CompDensPressure();    // compute density and pressure for each particle
		this.CompForce();           // compute final smoothed force for each particle
		this.Add_external_force();  // finger added external force
		this.Advection();           // advect and move each particle to new position
	}
	
	// polynomial kernel function
	private float poly6(float r2)
	{ 
		return (float) (315.0f/(64.0f * Math.PI * Math.pow(kernel, 9)) * Math.pow(kernel*kernel-r2, 3)); 
	}
	
	// spiky kernel function
	private float spiky(float r)
	{ 
		return (float) (-45.0f/(Math.PI * Math.pow(kernel, 6)) * (kernel-r) * (kernel-r)); 
	}
	
	// visco kernel function
	private float visco(float r)
	{ 
		return (float) (45.0f/(Math.PI * Math.pow(kernel, 6)) * (kernel-r)); 
	}
	
	public void Add_external_event(float x, float y, float fx, float fy)
	{
		external_x = x;
		external_y = y;
		force_x = fx;
		force_y = fy;
		external_event = 1;
	}
	
	// finger added external force
	private void Add_external_force()
	{
		if(external_event == 1)
		{
			//Log.i("DONGLI", "Per "+external_x+","+external_y);
			if(external_x < 0.0f)
				external_x = 0.0f;
			if(external_y < 0.0f)
				external_y = 0.0f;
			if(external_x >= 1.0f)
				external_x = 0.9f;
			if(external_y >= 1.0f)
				external_y = 0.9f;
			
			int cellPosX = (int)(external_x * gridSize.x);
			int cellPosY = (int)(external_y * gridSize.y);
			
			float px = external_x * worldSize.x;
			float py = external_y * worldSize.y;
			
			for(int i=-1; i<=1; i++)
			{
				for(int j=-1; j<=1; j++)
				{
					int nearPosX = cellPosX + i;
					int nearPosY = cellPosY + j;
					int hash = CalcCellHash(nearPosX, nearPosY);
					if(hash == -1) continue;
					
					Cell c = clist.get(hash);
					
					for(Iterator<Particle> iter=c.list.iterator(); iter.hasNext(); )
					{
						Particle p = (Particle)iter.next();
						if(p.pos.x-px<=kernel*2 && p.pos.y-py<=kernel*2)
						{
							p.acc.x = p.acc.x + 300.0f*force_x;
							p.acc.y = p.acc.y + 300.0f*force_y;
						}
					}
				}	
			}
	
			external_event = 0;
		}
	}
}
