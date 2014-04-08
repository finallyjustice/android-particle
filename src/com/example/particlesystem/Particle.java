/** File:		Particle.java
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

/* The definition of Particle */
public class Particle 
{
	public int id;
	
	// position of particle
	public Vec2f pos;
	
	// velocity of particle
	public Vec2f vel;
	
	// acceleration of particle 
	public Vec2f acc;
	
	// accumulative velocity of particle
	public Vec2f ev;
	
	// density of particle
	public float dens;
	// pressure of particle
	public float pres;
	
	public Particle()
	{
		pos = new Vec2f();
		vel = new Vec2f();
		acc = new Vec2f();
		ev  = new Vec2f();
	}
}

