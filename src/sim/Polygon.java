package sim;

import java.util.ArrayList;

import sim.util.geo.MasonGeometry;

/**
 * Polygon.java
 *
 * Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and George Mason
 * University.
 *
 * Licensed under the Academic Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: Polygon.java 842 2012-12-18 01:09:18Z mcoletti $
 */
public class Polygon extends MasonGeometry {
	String soc;

	ArrayList<Polygon> neighbors;

	public Polygon() {
		super();
		neighbors = new ArrayList<Polygon>();
	}

	public void init() {
		//soc = getStringAttribute("NFZ2COL");
		soc = getStringAttribute("NFZ3COL");
		//soc = getStringAttribute("LFZ2COL");
		//soc = getStringAttribute("LFZ3COL");
		// soc = getIntegerAttribute("OSVIRankCo");
	}

	String getSoc() {
		if (soc == null) {
			init();
		}
		return soc;
	}
}