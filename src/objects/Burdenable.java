package objects;

import java.util.ArrayList;
import java.util.Collection;

import com.vividsolutions.jts.geom.Coordinate;

public interface Burdenable {

	public void addLoad(AidLoad p);

	public boolean removeLoad(AidLoad p);

	public void addLoads(ArrayList<AidLoad> ps);

	public boolean removeLoads(ArrayList<AidLoad> ps);

	public Coordinate getLocation();

	public boolean makeTransferTo(Object o, Burdenable b);

	public String giveName();
}
