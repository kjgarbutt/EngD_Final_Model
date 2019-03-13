package comparators;

import java.util.Comparator;

import objects.AidLoad;
import sim.util.geo.MasonGeometry;

/**
 * Orders in terms of increasing distance from HQ/Depot
 * 
 * @author KJGarbutt
 *
 */

public class AidLoadDistanceComparator implements Comparator<AidLoad> {

	MasonGeometry targetLocation = null;

	public AidLoadDistanceComparator(MasonGeometry mg) {
		super();
		targetLocation = mg;
	}

	@Override
	public int compare(AidLoad o1, AidLoad o2) {
		double o1_priority = o1.getTargetCommunity().geometry.distance(targetLocation.geometry);
		double o2_priority = o2.getTargetCommunity().geometry.distance(targetLocation.geometry);
		if (o1_priority == o2_priority)
			return 0;
		else if (o1_priority > o2_priority)
			return 1;
		else
			return -1;
	}

}