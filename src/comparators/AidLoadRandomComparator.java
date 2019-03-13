package comparators;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import objects.AidLoad;
import sim.util.geo.MasonGeometry;

/**
 * Orders in terms of increasing distance from HQ/Depot
 * 
 * @author KJGarbutt
 *
 */

public class AidLoadRandomComparator implements Comparator<AidLoad> {

	// Get random ward from the Centroid geomoetry
	//
	// Create your list of parcels as well as a new, 
	// empty list of parcels. For n = number of parcels 
	// in the first list, randomly pick a number x between 
	// 0 and n, exclusive. Take the xth parcel from the 
	// first list and add it to the end of the second. 
	// Repeat this until the first list is empty. The 
	// second list should be a (fairly naively) randomised 
	// ordering of the parcels from the first.
	
	
	@Override
	public int compare(AidLoad o1, AidLoad o2) {
		int o1_priority = o1.getTargetCommunity().getIntegerAttribute("NPRIO");
		int o2_priority = o2.getTargetCommunity().getIntegerAttribute("NPRIO");
		if(o1_priority == o2_priority) return 0;
		else if(o1_priority < o2_priority) return 1;
		else return -1;
	}
}