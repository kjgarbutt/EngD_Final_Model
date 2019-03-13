package comparators;

import java.util.Comparator;

import objects.AidLoad;

/**
 * Orders in terms of decreasing priority
 * 
 * @author KJGarbutt
 *
 */
public class AidLoadPriorityComparator implements Comparator<AidLoad> {

	@Override
	public int compare(AidLoad o1, AidLoad o2) {
		int o1_priority = o1.getTargetCommunity().getIntegerAttribute("NPRIO");
		int o2_priority = o2.getTargetCommunity().getIntegerAttribute("NPRIO");
		if (o1_priority == o2_priority)
			return 0;
		else if (o1_priority < o2_priority)
			return 1;
		else
			return -1;
	}

}