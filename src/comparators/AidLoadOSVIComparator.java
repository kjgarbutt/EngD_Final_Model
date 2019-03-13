package comparators;

import java.util.Comparator;

import objects.AidLoad;

/**
 * Orders in terms of decreasing priority
 * 
 * @author KJGarbutt
 *
 */
public class AidLoadOSVIComparator implements Comparator<AidLoad> {

	@Override
	public int compare(AidLoad o1, AidLoad o2) {
		int o1_priority = o1.getTargetCommunity().getIntegerAttribute("NOSVIFZ3");
		int o2_priority = o2.getTargetCommunity().getIntegerAttribute("NOSVIFZ3");
		if (o1_priority == o2_priority)
			return 0;
		else if (o1_priority < o2_priority)
			return 1;
		// else if(o1_priority > o2_priority) return 1; // REVERESED FOR TESTING
		else
			return -1;
	}

}