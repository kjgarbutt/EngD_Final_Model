package objects;

import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

import sim.EngD_Final;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.network.Edge;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import swise.agents.TrafficAgent;
import swise.objects.network.GeoNode;
import swise.objects.network.ListEdge;

public class Driver extends TrafficAgent implements Steppable, Burdenable {

	EngD_Final world;
	Coordinate homeBase = null;
	Coordinate targetDestination = null;
	double roundStartTime = -1;
	double roundDriveDistance = 0, roundWalkDistance = 0;

	ArrayList<AidLoad> loads = new ArrayList<AidLoad>();
	ArrayList<AidLoad> myLoad = new ArrayList<AidLoad>();
	ArrayList<String> history = new ArrayList<String>();

	int index = 0;
	String driverID = null;
	public Stoppable stopper = null;
	double speed = 3.;

	double enteredRoadSegment = -1;

	AidLoad currentDelivery = null;

	public Driver(EngD_Final world, Coordinate c) {
		super(c);
		driverID = "Driver " + RandomStringUtils.randomAlphanumeric(4).toUpperCase();
		homeBase = (Coordinate) c.clone();
		this.world = world;
		loads = new ArrayList<AidLoad>();

		speed = world.speed_vehicle;

		edge = EngD_Final.getClosestEdge(c, world.resolution, world.networkEdgeLayer, world.fa);

		if (edge == null) {
			System.out.println("\tINIT_ERROR: no nearby edge");
			return;
		}

		GeoNode n1 = (GeoNode) edge.getFrom();
		GeoNode n2 = (GeoNode) edge.getTo();

		if (n1.geometry.getCoordinate().distance(c) <= n2.geometry.getCoordinate().distance(c))
			node = n1;
		else
			node = n2;

		segment = new LengthIndexedLine((LineString) ((MasonGeometry) edge.info).geometry);
		startIndex = segment.getStartIndex();
		endIndex = segment.getEndIndex();
		currentIndex = segment.indexOf(c);

		this.isMovable = true;
	}

	public void startRoundClock() {
		roundStartTime = world.schedule.getTime();
		roundDriveDistance = 0;
		roundWalkDistance = 0;
		index = 0;
		updateRound();
	}

	@Override
	public void step(SimState state) {

		double time = world.schedule.getTime(); // find the current time
		
		if (world.random.nextDouble() < world.probBreakdown) {
			System.out.println(
					this.driverID + " has BROKEN DOWN! ");
			world.schedule.scheduleOnce(time + world.breakdownRecoveryTime, this);
			headFor(homeBase);
			world.schedule.scheduleOnce(this);
			return;
		}
		
		// if you're in the process of delivering it, proceed
		if (currentDelivery != null && this.geometry.distance(currentDelivery.targetCommunity.geometry) <= world.resolution) {

			// if you've arrived at the delivery point, try to deliver the parcel!
			// attempt delivery
			if (world.random.nextDouble() < world.probFailedDelivery) { // failed delivery ):
				index++;
				System.out.println(
						// this.toString() + " has NOT been able to deliver parcel to:" +
						// currentDelivery.toString());
						this.driverID + " has NOT been able to deliver parcel to: " + currentDelivery.toString());
				// this.driverID + " has NOT been able to deliver parcel to:" +
				// currentDelivery.getAttribute("id"));
			} else { // successful delivery! :)
				currentDelivery.deliver();
				loads.remove(currentDelivery);
				//System.out.println(
				//this.toString() + " has delivered the parcel to:" +
				//currentDelivery.toString());
				//this.driverID + " has delivered the parcel to: " +
				//currentDelivery.toString());
				//currentDelivery.geometry =
				//world.fa.createPoint(currentDelivery.deliveryLocation);
				//world.deliveryLocationLayer.addGeometry(currentDelivery);

			}

			world.schedule.scheduleOnce(time + world.deliveryTime, this);
			currentDelivery = null;
			path = null;

			return;
		}

		// if you're still moving, keep moving
		else if (path != null) {
			navigate(world.resolution);
			world.schedule.scheduleOnce(this);
			return;
		}

		// otherwise, if you've still got parcels to deliver, deliver that parcel!
		else if (loads.size() > index) {

			AidLoad nextLoad = loads.get(index);
			currentDelivery = nextLoad;
			if (geometry.getCoordinate().distance(nextLoad.deliveryLocation) > world.resolution) {
				headFor(nextLoad.deliveryLocation);
				roundDriveDistance += calculateDistance(path);
			}
			world.schedule.scheduleOnce(this);
			return;
		}

		// otherwise, if you've finished delivering all your parcels and you're not at
		// the depot, go back to the depot
		else if (homeBase.distance(geometry.getCoordinate()) > world.resolution) {
			headFor(homeBase);
			world.schedule.scheduleOnce(this);
			return;
		}

		// otherwise, you're at the Depot - enter it
		else {

			double roundTime = world.schedule.getTime() - roundStartTime;
			history.add(driverID + "\t" + roundTime + "\t" + roundDriveDistance + "\t" + state.schedule.getTime());
			// System.out.println(this.toString() + " is done with the round! It took "
			// System.out.println(this.driverID + " is done with the round! It took "
			// + (world.schedule.getTime() - roundStartTime));
			Bag b = world.depotLayer.getObjectsWithinDistance(geometry, world.resolution);
			if (b.size() > 0) {
				Headquarters d = (Headquarters) b.get(0);
				d.enterDepot(this);
				if (loads.size() > 0) {
					System.out.println( "Driver: " + this.toString() + " has finished its round.");
					makeTransferTo(loads, d);
				}
			}
		}
	}

	@Override
	public void addLoad(AidLoad p) {
		loads.add(p);
	}

	@Override
	public boolean removeLoad(AidLoad p) {
		return loads.remove(p);
	}

	public boolean removeLoads(ArrayList<AidLoad> ps) {
		return loads.removeAll(ps);
	}

	@Override
	public void addLoads(ArrayList<AidLoad> ps) {
		loads.addAll(ps);
	}

	// really basic right now: start with the first one and greedily pick the next
	// closest, until you have them all
	public void updateRound() {
		//System.out.println(" Updating the round...");
		if (loads.size() < 1)
			return;

		ArrayList<AidLoad> tempLoads = new ArrayList<AidLoad>(loads);

		for (int i = 1; i < tempLoads.size(); i++) {
			AidLoad p = tempLoads.get(i - 1);
			double dist = Double.MAX_VALUE;
			int best = -1;

			for (int j = i; j < tempLoads.size(); j++) {
				AidLoad pj = tempLoads.get(j);
				double pjdist = pj.deliveryLocation.distance(p.deliveryLocation);
				if (pjdist < dist) {
					dist = pjdist;
					best = j;
				}
			}
			AidLoad closestLoad = tempLoads.remove(best);
			tempLoads.add(i, closestLoad);
		}
		myLoad = tempLoads;
		//System.out.println("\t driver.updateRound() myLoad: " + myLoad); //all the same HQ POINT (381183.4246222474 212709.48584634456)]
		
	}

	@Override
	public boolean makeTransferTo(Object o, Burdenable b) {
		try {
			if (o instanceof ArrayList) {
				for (AidLoad x : (ArrayList<AidLoad>) o)
					x.transfer(b);
			} else {
				((AidLoad) o).transfer(b);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public Coordinate getLocation() {
		return geometry.getCoordinate();
	}

	public void setNode(GeoNode n) {
		node = n;
	}

	/**
	 * 
	 * @param resolution
	 * @return 1 for success, -1 for failure
	 */
	public int navigate(double resolution) {
		myLastSpeed = -1;

		if (path != null) {
			double time = 1;// speed;
			while (path != null && time > 0) {
				time = move(time, speed, resolution);
			}

			if (segment != null)
				updateLoc(segment.extractPoint(currentIndex));

			if (time < 0) {
				return -1;
			} else
				return 1;
		}
		return -1;
	}

	/**
	 * 
	 * @param time
	 *            - a positive amount of time, representing the period of time
	 *            agents are allocated for movement
	 * @param obstacles
	 *            - set of spaces which are obstacles to the agent
	 * @return the amount of time left after moving, negated if the movement failed
	 */
	protected double move(double time, double mySpeed, double resolution) {

		// if we're at the end of the edge and we have more edges, move onto the next
		// edge
		if (arrived()) {

			// clean up any edge we leave
			if (edge != null && edge.getClass().equals(ListEdge.class)) {
				((ListEdge) edge).removeElement(this);

				// update the edge with how long you've spent on it
				// double durationOnSegment =
				// ((MasonGeometry)edge.info).getDoubleAttribute("MikeSim_timeOnRoad");

				// if(enteredRoadSegment > 0) // if you began on the edge and never really
				// entered it, don't consider this
				// ((MasonGeometry)edge.info).addDoubleAttribute("MikeSim_timeOnRoad",
				// durationOnSegment + world.schedule.getTime() - enteredRoadSegment);
			}

			// if we have arrived and there is no other edge in the path, we have finished
			// our journey: reset the path and return the remaining time
			if (goalPoint == null && path.size() == 0 && (currentIndex <= startIndex || currentIndex >= endIndex)) {
				path = null;
				return time;
			}

			// make sure that there is another edge in the path
			if (path.size() > 0) {

				// take the next edge
				Edge newEdge = path.remove(path.size() - 1);
				edge = newEdge;

				// make sure it's open
				// if it's not, return an error!
				/*
				 * if(((MasonGeometry)newEdge.info).getStringAttribute("open").equals("CLOSED"))
				 * { updateLoc(node.geometry.getCoordinate()); edge = newEdge; path = null;
				 * return -1; }
				 */
				// change our positional node to be the Node toward which we're moving
				node = (GeoNode) edge.getOtherNode(node);

				// format the edge's geometry so that we can move along it conveniently
				LineString ls = (LineString) ((MasonGeometry) edge.info).geometry;

				// set up the segment and coordinates
				segment = new LengthIndexedLine(ls);
				startIndex = segment.getStartIndex();
				endIndex = segment.getEndIndex();
				currentIndex = segment.project(this.geometry.getCoordinate());

				// if that was the last edge and we have a goal point, resize the expanse
				if (path.size() == 0 && goalPoint != null) {
					double goalIndex = segment.project(goalPoint);
					if (currentIndex < goalIndex)
						endIndex = goalIndex;
					else
						startIndex = goalIndex;
				}

				// make sure we're moving in the correct direction along the Edge
				if (node.equals(edge.to())) {
					direction = 1;
					currentIndex = Math.max(currentIndex, startIndex);
				} else {
					direction = -1;
					currentIndex = Math.min(currentIndex, endIndex);
				}

				if (edge.getClass().equals(ListEdge.class)) {
					((ListEdge) edge).addElement(this);
					// int numUsages =
					// ((MasonGeometry)edge.info).getIntegerAttribute("MikeSim_useages");
					// ((MasonGeometry)edge.info).addIntegerAttribute("MikeSim_useages", numUsages +
					// 1);

					enteredRoadSegment = world.schedule.getTime();
				}

			}

		}

		// otherwise, we're on an Edge and moving forward!
		// set our speed
		double speed;
		if (edge != null && edge.getClass().equals(ListEdge.class)) {

			// Each car has a certain amount of space: wants to preserve a following
			// distance. If the amount of following distance is less than 20 meters 
			// (~ 6 car lengths) it'll slow proportionately
			double val = ((ListEdge) edge).lengthPerElement() / 5;
			if (val < 10 && this.speed == EngD_Final.speed_vehicle) {
				speed = mySpeed / val;// minSpeed);
				if (speed < 1) { // if my speed is super low, set it to some baseline to keep traffic moving at
									// all
					int myIndexInEdge = ((ListEdge) edge).returnMyIndex(this);
					if (myIndexInEdge == 0 || myIndexInEdge == ((ListEdge) edge).numElementsOnListEdge() - 1)
						speed = this.speed; // if I'm at the head or end of the line, move ahead at a fairly normal
											// speed
				}
			} else
				speed = this.speed;
		} else
			speed = mySpeed;

		myLastSpeed = speed;

		// construct a new current index which reflects the speed and direction of
		// travel
		double proposedCurrentIndex = currentIndex + time * speed * direction;

		// great! It works! Move along!
		currentIndex = proposedCurrentIndex;

		if (direction < 0) {
			if (currentIndex < startIndex) {
				time = (startIndex - currentIndex) / speed; // convert back to time
				currentIndex = startIndex;
			} else
				time = 0;
		} else if (currentIndex > endIndex) {
			time = (currentIndex - endIndex) / speed; // convert back to time
			currentIndex = endIndex;
		} else
			time = 0;

		// don't overshoot if we're on the last bit!
		if (goalPoint != null && path.size() == 0) {
			double idealIndex = segment.indexOf(goalPoint);
			if ((direction == 1 && idealIndex <= currentIndex) || (direction == -1 && idealIndex >= currentIndex)) {
				currentIndex = idealIndex;
				time = 0;
				startIndex = endIndex = currentIndex;
			}
		}

		updateLoc(segment.extractPoint(currentIndex));

		if (path.size() == 0 && arrived()) {
			path = null;
			if (edge != null)
				((ListEdge) edge).removeElement(this);
		}
		return time;
	}

	/**
	 * Set up a course to take the Agent to the given coordinates
	 * 
	 * @param place
	 *            - the target destination
	 * @return 1 for success, -1 for a failure to find a path, -2 for failure based
	 *         on the provided destination or current position
	 */
	public int headFor(Coordinate place) {

		if (place == null) {
			System.out.println("ERROR: can't move toward nonexistant location");
			return -1;
		}

		// first, record from where the agent is starting
		startPoint = this.geometry.getCoordinate();
		goalPoint = null;

		if (!(edge.getTo().equals(node) || edge.getFrom().equals(node))) {
			System.out.println((int) world.schedule.getTime() + "\tMOVE_ERROR_mismatch_between_current_edge_and_node");
			return -2;
		}

		///////////////////// FINDING THE GOAL ////////////////////
		// set up goal information
		targetDestination = world.snapPointToRoadNetwork(place);

		GeoNode destinationNode = world.snapPointToNode(targetDestination);
		if (destinationNode == null) {
			System.out.println((int) world.schedule.getTime() + "\tMOVE_ERROR_invalid_destination_node");
			return -2;
		}

		// be sure that if the target location is not a node but rather a point along an
		// edge, that point is recorded
		if (destinationNode.geometry.getCoordinate().distance(targetDestination) > world.resolution)
			goalPoint = targetDestination;
		else
			goalPoint = null;

		///////////////// FINDING A PATH /////////////////////

		path = pathfinder.astarPath(node, destinationNode, world.roads);

		// if it fails, give up
		if (path == null) {
			return -1;
		}

		//////////////////// CHECK FOR BEGINNING OF PATH ///////////////////
		// we want to be sure that we're situated on the path *right now*, and that if
		// the path doesn't include the link we're on at this moment that we're both
		// a) on a link that connects to the startNode
		// b) pointed toward that startNode
		// Then, we want to clean up by getting rid of the edge on which we're already
		// located

		// Make sure we're in the right place, and face the right direction
		if (edge.getTo().equals(node))
			direction = 1;
		else if (edge.getFrom().equals(node))
			direction = -1;
		else {
			System.out.println((int) world.schedule.getTime() + "MOVE_ERROR_mismatch_between_current_edge_and_node_2");
			return -2;
		}

		// reset stuff
		if (path.size() == 0 && targetDestination.distance(geometry.getCoordinate()) > world.resolution) {
			path.add(edge);
			node = (GeoNode) edge.getOtherNode(node);
		}

		//////////////////// CHECK FOR END OF PATH //////////////

		if (goalPoint != null) {

			ListEdge myLastEdge = world.getClosestEdge(goalPoint, world.resolution, world.networkEdgeLayer, world.fa);

			if (myLastEdge == null) {
				System.out.println((int) world.schedule.getTime() + "\tMOVE_ERROR_goal_point_is_too_far_from_any_edge");
				return -2;
			}

			// make sure the point is on the last edge
			Edge lastEdge;
			if (path.size() > 0)
				lastEdge = path.get(0);
			else
				lastEdge = edge;

			Point goalPointGeometry = world.fa.createPoint(goalPoint);
			if (!lastEdge.equals(myLastEdge)
					&& ((MasonGeometry) lastEdge.info).geometry.distance(goalPointGeometry) > world.resolution) {
				if (lastEdge.getFrom().equals(myLastEdge.getFrom()) || lastEdge.getFrom().equals(myLastEdge.getTo())
						|| lastEdge.getTo().equals(myLastEdge.getFrom()) || lastEdge.getTo().equals(myLastEdge.getTo()))
					path.add(0, myLastEdge);
				else {
					System.out.println((int) world.schedule.getTime()
							+ "\tMOVE_ERROR_goal_point_edge_is_not_included_in_the_path");
					return -2;
				}
			}

		}

		// set up the coordinates
		this.startIndex = segment.getStartIndex();
		this.endIndex = segment.getEndIndex();

		return 1;
	}

	public double calculateDistance(ArrayList<Edge> edges) {
		double result = 0;
		for (Edge e : edges) {
			result += ((MasonGeometry) e.info).geometry.getLength();
		}
		return result;
	}

	public ArrayList<String> getHistory() {
		return history;
	}

	public String giveName() {
		return this.driverID;
	}
}