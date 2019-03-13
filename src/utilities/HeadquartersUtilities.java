package utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.logging.Level;

//import com.srbenoit.math.delaunay.Delaunay;
//import com.srbenoit.math.delaunay.Vertex;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import objects.AidLoad;
import sim.field.geo.GeomVectorField;

public class HeadquartersUtilities {

	public static ArrayList<ArrayList<AidLoad>> gridDistribution(ArrayList<AidLoad> parcels, GeomVectorField space,
			int targetSize) {

		// establish the parameters on the space
		Envelope e = space.MBR;
		double num = parcels.size(), width = e.getWidth(), height = e.getHeight(), minx = e.getMinX(),
				miny = e.getMinY();

		// establish the number of rounds necessary
		int approxNumRounds = (int) (Math.ceil(num / targetSize)); // the number of parcels relative to the size of a
																	// round
		double roundsPerSide = Math.ceil(Math.sqrt(approxNumRounds)); // the approximate area covered per round

		// set up a round for each category
		ArrayList<ArrayList<AidLoad>> rounds = new ArrayList<ArrayList<AidLoad>>(); // for safety??
		// Collections.nCopies((int)Math.ceil(roundsPerSide * roundsPerSide)+1, new
		// ArrayList <Parcel> ())

		for (int i = 0; i < (int) Math.ceil(roundsPerSide * roundsPerSide) + 1; i++) {
			rounds.add(new ArrayList<AidLoad>());
		}

		// define the limits on the distances
		double dx = width / roundsPerSide; // the width cut into those chunks
		double dy = height / roundsPerSide; // the height cut into those relative chunks
		int xperRow = (int) Math.floor(roundsPerSide);
		for (AidLoad p : parcels) {
			Coordinate c = p.getDeliveryLocation();
			int myX = (int) Math.floor((c.x - minx) / dx), myY = (int) Math.floor((c.y - miny) / dy);
			rounds.get(myX + myY * xperRow).add(p);
			p.addIntegerAttribute("round", myX + myY * xperRow);
		}

		Collections.sort(rounds, (o1, o2) -> ((Integer) o1.size()).compareTo(o2.size()));
		for (int i = 0; i < rounds.size(); i++) {
			if (rounds.get(i).size() > 0)
				return new ArrayList<ArrayList<AidLoad>>(rounds.subList(i, rounds.size()));
		}
		return new ArrayList<ArrayList<AidLoad>>();
	}

}