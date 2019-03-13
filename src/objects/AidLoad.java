package objects;

import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

import com.vividsolutions.jts.geom.Coordinate;

import sim.EngD_Final;
import sim.util.geo.MasonGeometry;
import swise.agents.MobileAgent;

public class AidLoad extends MobileAgent {

	EngD_Final world;

	Burdenable carryingUnit = null;
	MasonGeometry targetCommunity = null;
	Coordinate deliveryLocation;
	double dim_x, dim_y, dim_z, weight;
	ArrayList<String> history = new ArrayList<String>();
	String parcelID = null;
	int status; // 0 = undelivered, 1 = failed delivery attempt, 2 = out for delivery, 
				// 3 = delivered

	public AidLoad(Burdenable hq, MasonGeometry target, EngD_Final state) {
		super(hq.getLocation());
		parcelID = RandomStringUtils.randomAlphanumeric(4).toUpperCase() + System.currentTimeMillis();
		carryingUnit = hq;
		history = new ArrayList<String>();
		hq.addLoad(this);
		isMovable = true;
		targetCommunity = target;
		world = state;
		// history.add("Parcel Initialised and Allocated to: \t" +
		// target.getStringAttribute("LSOA_CODE"));
	}

	public String giveName() {
		return parcelID;
	}

	public MasonGeometry getTargetCommunity() {
		return targetCommunity;
	}

	public void setDeliveryLocation(Coordinate c) {
		deliveryLocation = (Coordinate) c.clone();
	}

	public Coordinate getDeliveryLocation() {
		return deliveryLocation;
	}

	public Coordinate getLocation() {
		if (carryingUnit != null)
			return carryingUnit.getLocation();
		else
			return this.geometry.getCoordinate();
	}

	public boolean transfer(Burdenable to) {
		try {
			String fromName = "<from>", toName = "<to>";
			if (carryingUnit != null) {
				carryingUnit.removeLoad(this);
				fromName = carryingUnit.giveName();
			}
			if (to != null) {
				to.addLoad(this);
				toName = to.giveName();
			}

			carryingUnit = to;
			//history.add("\t" + to.getStringAttribute("Name") + "\t" + toName + "\t" + world.schedule.getTime());
			history.add("\t" + fromName + "\t" + toName + "\t" + world.schedule.getTime());
			// Load was transferred from + by Driver + at Time Step
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean deliver() {

		if (getLocation().distance(deliveryLocation) < EngD_Final.resolution) {

			carryingUnit.removeLoad(this);
			geometry = world.fa.createPoint(deliveryLocation);
			world.deliveryLocationLayer.addGeometry(this);
			history.add(targetCommunity.getStringAttribute("LSOA_NAME") + "\t" + world.schedule.getTime());
			// Target LSOA + Time Step

			status = 3; // delivered
			return true;
		}
		status = 1; // failed delivery attempt
		history.add("Failed Delivery to: \t" + deliveryLocation.toString());
		return false;
	}

	public ArrayList<String> getHistory() {
		return history;
	}

	public boolean equals(Object o) {
		if (!(o instanceof AidLoad))
			return false;
		else
			return ((AidLoad) o).parcelID.equals(parcelID);
	}

	public int hashCode() {
		return parcelID.hashCode();
	}
}