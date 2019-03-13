package sim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

import comparators.AidLoadPriorityComparator;
import ec.util.MersenneTwisterFast;
import objects.AidLoad;
import objects.Driver;
import objects.Headquarters;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.geo.AttributeValue;
import sim.util.geo.MasonGeometry;
import swise.agents.communicator.Information;
import swise.objects.NetworkUtilities;
import swise.objects.network.GeoNode;
import swise.objects.network.ListEdge;
import utilities.DriverUtilities;
import utilities.InputCleaning;
import utilities.RoadNetworkUtilities;

/**
 * "EngD_Final" is the final iteration of my EngD project model.
 * 
 * The model is adapted from the MASON demo, "Gridlock", made by Sarah Wise,
 * Mark Coletti, and Andrew Crooks and "SimpleDrivers," made by Sarah Wise.
 * 
 * The model is an example of a simple ABM framework to explore delivering goods
 * during a flood. The model reads a number of GIS shapefiles and displays a
 * road network, two Environment Agency flood maps and a bespoke Open Source
 * Vulnerability Index (OSVI). The model reads in a .CSV and generates a
 * predetermined number of agents with set characteristics. The agents are
 * placed on the road network and are located at a Red Cross office. The model
 * reads a separate .CSV and assigns goal locations to each agent at random from
 * a predetermined list. The agents are assigned speeds at random. Once the
 * model is started, the agents move from A to B, then they change direction and
 * head back to their start position. The process repeats until the user quits.
 *
 * The temporal granularity of the simulation: one tick is 5 minutes
 *
 * @author KJGarbutt
 *
 */
public class EngD_Final extends SimState {

	////////////////////////////////////////////////
	/////////////// MODEL PARAMETERS ///////////////
	////////////////////////////////////////////////

	private static final long serialVersionUID = 1L;
	public static int grid_width = 970;
	public static int grid_height = 620;
	public static double resolution = 5;
	// the granularity of the simulation (fiddle around with this to merge nodes
	// into one another)

	public static double speed_vehicle = 1000; // approximately 30MPH

	public static int loadingTime = 4; // 1 = 5 minutes
	public static int deliveryTime = 6; // 1 = 5 minutes
	
	///////////// COMMODITY PARAMETERS ////////////////
	// public static int approxManifestSize = 4; // Sandbags. 6 per household. 24 per load/car
	// public static int approxManifestSize = 10; // Water+Blanket Combo. 1
	// 24-pack+3 blankets per household. 10 per load/car
	// public static int approxManifestSize = 15; // Water+Cleaning Kit Combo. 1
	// 24-pack+1 Cleaning Kit per household. 15 per load/car
	public static int approxManifestSize = 20; // Water. 1 24-pack per household. 20 per load/car
	// public static int approxManifestSize = 40; // Blankets. 3 per household. 120
	// per load/car
	// public static int approxManifestSize = 50; // Cleaning kits. 1 per household.
	// 50 per load/car

	public static int numMaxAgents = 10;
	public static int numMaxLoads = 10000;
	public static int numBays = 10;
	public static double probFailedDelivery = .0;
	public double probBreakdown = .0;
	public int breakdownRecoveryTime = 25;
	

	/////////////// DATA SOURCES ///////////////
	String dirName = "data/";

	/////////////// CONTAINERS ///////////////
	public GeomVectorField world = new GeomVectorField();
	public GeomVectorField baseLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField osviLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField boundaryLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField fz2Layer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField fz3Layer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField roadLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField depotLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField centroidsLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField deliveryLocationLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField agentLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField networkLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField networkEdgeLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField majorRoadNodesLayer = new GeomVectorField(grid_width, grid_height);

	public Bag roadNodes = new Bag();
	public Network roads = new Network(false);

	/////////////// OBJECTS ///////////////
	
	// Model ArrayLists for agents and OSVI Polygons
	public ArrayList<Driver> agents = new ArrayList<Driver>(10);
	ArrayList<Integer> assignedWards = new ArrayList<Integer>();
	public HashMap<MasonGeometry, Integer> visitedWardRecord = new HashMap<MasonGeometry, Integer>();
	ArrayList<AidLoad> loadsRecord = new ArrayList<AidLoad>();

	ArrayList<Polygon> polys = new ArrayList<Polygon>();
	ArrayList<String> csvData = new ArrayList<String>();
	ArrayList<ArrayList<AidLoad>> rounds;
	ArrayList<ArrayList<AidLoad>> loads;
	public ArrayList<AidLoad> history;

	public GeometryFactory fa = new GeometryFactory();

	long mySeed = 0;

	Envelope MBR = null;

	boolean verbose = false;

	///////////////////////////////////////////////
	/////////////// BEGIN functions ///////////////
	///////////////////////////////////////////////

	/**
	 * Default Constructor
	 * 
	 * @param randomSeed
	 */
	public EngD_Final(long randomSeed) {
		super(randomSeed);
		random = new MersenneTwisterFast(12345);
	}

	/**
	 * OSVI Polygon Setup
	 */
	void setup() {
		// copy over the geometries into a list of Polygons
		Bag ps = world.getGeometries();
		polys.addAll(ps);
	}

	/**
	 * Read in data and set up the simulation
	 */
	public void start() {
		super.start();

		System.out.println();
		System.out.println("////////////////\nINPUTTING STUFFS\n////////////////");
		System.out.println();

		try {

			//////////////////////////////////////////////
			///////////// READING IN DATA ////////////////
			//////////////////////////////////////////////

			///////////// OSVI / LSOA ////////////////
			File wardsFile = new File("data/GL_OSVI_2019.shp");
			ShapeFileImporter.read(wardsFile.toURI().toURL(), world, Polygon.class);
			System.out.println("Reading in OSVI shapefile from " + wardsFile + "...done");
			// GeomVectorFieldPortrayal polyPortrayal = new GeomVectorFieldPortrayal(true);
			// for OSVI viz.
			
			/////////////////////////////////////////////////////////
			///////////// CENTROIDS / DELIVERY GOALS ////////////////
			/////////////////////////////////////////////////////////
			// ALL DELIVERY LOCATIONS FOR GL_ITN_MultipartToSinglepart.shp
			GeomVectorField dummyDepotLayer = new GeomVectorField(grid_width, grid_height);
			InputCleaning.readInVectorLayer(centroidsLayer,
			dirName + "GL_centroids_2019_FZOnly.shp", "Centroids", new Bag());

			// ALL DELIVERY LOCATIONS FOR GL_ITN_MultipartToSinglepart1s2s3s.shp
			//InputCleaning.readInVectorLayer(centroidsLayer, dirName +
			//"GL_Centroids_MovedForModel.shp", "All Centroids", new Bag());

			// ONLY DELIVERY LOCATIONS WITH A RED OSVI RATING +
			// GL_ITN_MultipartToSinglepart1s2s3s.shp
			//InputCleaning.readInVectorLayer(centroidsLayer, dirName + "GL_Centroids_MovedForFlooding_2019_NOSVIFZ3_RED.shp",
			//		"OSVI RED Centroids ONLY", new Bag());

			//////////////////////////////////////////
			///////////// HQ / DEPOTS ////////////////
			//////////////////////////////////////////
			//InputCleaning.readInVectorLayer(dummyDepotLayer, dirName + "BRC_HQ_GL.shp", "1x Depot", new Bag());
			//InputCleaning.readInVectorLayer(headquartersLayer, dirName + "BRC_HQ_GL_Reduced.shp", "1x Depot", new Bag()); // Shows HQ

			// TWO BRC DEPOTS IN GL
			 InputCleaning.readInVectorLayer(dummyDepotLayer, dirName + "BRC_HQ_GL_2.shp", "2x Depots", new Bag());
			 InputCleaning.readInVectorLayer(depotLayer, dirName + "BRC_HQ_GL_2.shp", "2x Depots", new Bag());
			
			//////////////////////////////////////////////
			///////////// ROADS / NETWORK ////////////////
			//////////////////////////////////////////////
			// FULL, NON-FLOODED ROAD NETWORK
			InputCleaning.readInVectorLayer(roadLayer, dirName +
			"GL_Roads.shp", "Full, Non-Flooded Road Network", new Bag()); 
			//InputCleaning.readInVectorLayer(roadLayer, dirName + "GL_Roads_GYO_2019.shp",
			//		"Flooded Road Network - Levels 1-3", new Bag()); // NO MAJOR FLOODED ROADS

			/////////////////////////////////////////////////
			///////////// BASELAYER / EXTRAS ////////////////
			/////////////////////////////////////////////////
			InputCleaning.readInVectorLayer(osviLayer, dirName + "GL_OSVI_2019.shp", "OSVI", new Bag());
			InputCleaning.readInVectorLayer(boundaryLayer, dirName + "Gloucestershire_Boundary_Line.shp",
					"County Boundary", new Bag());
			InputCleaning.readInVectorLayer(fz2Layer, dirName + "Gloucestershire_FZ_2.shp", "Flood Zone 2", new Bag());
			InputCleaning.readInVectorLayer(fz3Layer, dirName + "Gloucestershire_FZ_3.shp", "Flood Zone 3", new Bag());

			///////////////////////////////////////////////////
			////////////////// DATA CLEANUP ///////////////////
			//////////////////////////////////////////////////

			// standardize the MBRs so that the visualization lines up

			MBR = osviLayer.getMBR();
			MBR.init(340995, 438179, 185088, 247204);

			// System.out.println("Setting up OSVI Portrayals...");
			// System.out.println();

			setup();

			// clean up the road network
			System.out.println("\nCleaning the road network...");

			roads = NetworkUtilities.multipartNetworkCleanup(roadLayer, roadNodes, resolution, fa, random, 0);
			roadNodes = roads.getAllNodes();
			RoadNetworkUtilities.testNetworkForIssues(roads);

			// set up roads as being "open" and assemble the list of potential termini
			roadLayer = new GeomVectorField(grid_width, grid_height);
			for (Object o : roadNodes) {
				GeoNode n = (GeoNode) o;
				networkLayer.addGeometry(n);

				// check all roads out of the nodes
				for (Object ed : roads.getEdgesOut(n)) {

					// set it as being (initially, at least) "open"
					ListEdge edge = (ListEdge) ed;
					((MasonGeometry) edge.info).addStringAttribute("open", "OPEN");
					networkEdgeLayer.addGeometry((MasonGeometry) edge.info);
					roadLayer.addGeometry((MasonGeometry) edge.info);
					((MasonGeometry) edge.info).addAttribute("ListEdge", edge);
				}
			}

			Network majorRoads = RoadNetworkUtilities.extractMajorRoads(roads);
			RoadNetworkUtilities.testNetworkForIssues(majorRoads);

			// assemble list of secondary versus local roads
			ArrayList<Edge> myEdges = new ArrayList<Edge>();
			GeomVectorField secondaryRoadsLayer = new GeomVectorField(grid_width, grid_height);
			GeomVectorField localRoadsLayer = new GeomVectorField(grid_width, grid_height);
			for (Object o : majorRoads.allNodes) {

				majorRoadNodesLayer.addGeometry((GeoNode) o);

				for (Object e : roads.getEdges(o, null)) {
					Edge ed = (Edge) e;

					myEdges.add(ed);

					String type = ((MasonGeometry) ed.getInfo()).getStringAttribute("class");
					if (type.equals("Not Classified"))
						secondaryRoadsLayer.addGeometry((MasonGeometry) ed.getInfo());
					else if (type.equals("Unclassified"))
						localRoadsLayer.addGeometry((MasonGeometry) ed.getInfo());
				}
			}

			System.gc();

			// set up depots
			setupDepots(dummyDepotLayer);

			// reset MBRs in case they got messed up during all the manipulation
			world.setMBR(MBR);
			centroidsLayer.setMBR(MBR);
			roadLayer.setMBR(MBR);
			networkLayer.setMBR(MBR);
			networkEdgeLayer.setMBR(MBR);
			majorRoadNodesLayer.setMBR(MBR);
			deliveryLocationLayer.setMBR(MBR);
			agentLayer.setMBR(MBR);
			fz2Layer.setMBR(MBR);
			fz3Layer.setMBR(MBR);
			osviLayer.setMBR(MBR);
			baseLayer.setMBR(MBR);
			boundaryLayer.setMBR(MBR);
			depotLayer.setMBR(MBR);

			// System.out.print("done");

			//////////////////////////////////////////////
			////////////////// AGENTS ////////////////////
			//////////////////////////////////////////////
			/*
			for (Object o : depotLayer.getGeometries()) {
				Headquarters d = (Headquarters) o;
				//getMostVulnerableUnassignedWard();
				generateLoads(d);
				d.generateRounds();
			}
			*/
			
			//Way to generate loads first and then assign them to depots, instead of generating headquarters
			//and then the loads
			generateLoads();
		
			agents.addAll(DriverUtilities.setupDriversAtDepots(this, fa, numMaxAgents));
			System.out.println("Prioritising unassigned LSOA...");
			for (Driver p : agents) {
				agentLayer.addGeometry(p);
				getMostVulnerableUnassignedWard();
			}
			
			// seed the simulation randomly
			seedRandom(System.currentTimeMillis());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setupDepots(GeomVectorField dummyDepots) {
		Bag depots = dummyDepots.getGeometries();
		System.out.println("Setting up HQ...");

		for (Object o : depots) {
			MasonGeometry mg = (MasonGeometry) o;
			// int numbays = mg.getIntegerAttribute("loadbays");
			GeoNode gn = snapPointToNode(mg.geometry.getCoordinate());

			Headquarters d = new Headquarters(gn.geometry.getCoordinate(), numBays, this);
			System.out.println("\tHQ located: " + gn + " and has " + numBays + " loading bays.");
			d.setNode(gn);

			d.generateRounds();

			depotLayer.addGeometry(d);
			schedule.scheduleOnce(d);
		}
	}

	public Coordinate snapPointToRoadNetwork(Coordinate c) {
		ListEdge myEdge = null;
		double resolution = this.resolution;

		if (networkEdgeLayer.getGeometries().size() == 0)
			return null;

		while (myEdge == null && resolution < Double.MAX_VALUE) {
			myEdge = RoadNetworkUtilities.getClosestEdge(c, resolution, networkEdgeLayer, fa);
			resolution *= 10;
		}
		if (resolution == Double.MAX_VALUE)
			return null;

		LengthIndexedLine closestLine = new LengthIndexedLine(
				(LineString) (((MasonGeometry) myEdge.info).getGeometry()));
		double myIndex = closestLine.indexOf(c);
		return closestLine.extractPoint(myIndex);
	}

	public GeoNode snapPointToNode(Coordinate c) {
		ListEdge myEdge = null;
		double resolution = this.resolution;

		if (networkEdgeLayer.getGeometries().size() == 0)
			return null;

		while (myEdge == null && resolution < Double.MAX_VALUE) {
			myEdge = RoadNetworkUtilities.getClosestEdge(c, resolution, networkEdgeLayer, fa);
			resolution *= 10;
		}
		if (resolution == Double.MAX_VALUE)
			return null;

		double distFrom = c.distance(((GeoNode) myEdge.from()).geometry.getCoordinate()),
				distTo = c.distance(((GeoNode) myEdge.to()).geometry.getCoordinate());
		if (distFrom <= distTo)
			return (GeoNode) myEdge.from();
		else
			return (GeoNode) myEdge.to();
	}

	public static ListEdge getClosestEdge(Coordinate c, double resolution, GeomVectorField networkEdgeLayer,
			GeometryFactory fa) {

		// find the set of all edges within *resolution* of the given point
		Bag objects = networkEdgeLayer.getObjectsWithinDistance(fa.createPoint(c), resolution);
		if (objects == null || networkEdgeLayer.getGeometries().size() <= 0)
			return null; // problem with the network edge layer

		Point point = fa.createPoint(c);

		// find the closest edge among the set of edges
		double bestDist = resolution;
		ListEdge bestEdge = null;
		for (Object o : objects) {
			double dist = ((MasonGeometry) o).getGeometry().distance(point);
			if (dist < bestDist) {
				bestDist = dist;
				bestEdge = (ListEdge) ((AttributeValue) ((MasonGeometry) o).getAttribute("ListEdge")).getValue();
			}
		}

		// if it exists, return it
		if (bestEdge != null)
			return bestEdge;

		// otherwise return failure
		else
			return null;
	}

	public void generateLoads(Headquarters d) {
		System.out.println("Generating parcels...");
		// System.out.print("done");

		ArrayList<AidLoad> myLoads = new ArrayList<AidLoad>();
		Bag centroidGeoms = centroidsLayer.getGeometries();

		System.out.println("Assigning parcels to drivers...");

		for (Object o : centroidGeoms) {

			MasonGeometry myCentroid = (MasonGeometry) o;
			int households = myCentroid.getIntegerAttribute("FZHouses");

			// create a number of loads based on the number of households + 1 to cover any
			// stragglers
			int numLoads = households / approxManifestSize + 1;
			for (int i = 0; i < numLoads; i++) {

				Point deliveryLoc = myCentroid.geometry.getCentroid();
				Coordinate myCoordinate = deliveryLoc.getCoordinate();

				if (!MBR.contains(myCoordinate)) {
					System.out.println("myCoordinate is NOT in MBR!");
					i--;
					continue;
				}

				AidLoad p = new AidLoad(d, myCentroid, this);
				p.setDeliveryLocation(myCoordinate);
				myLoads.add(p);

				loadsRecord.add(p);
			}
		}
	}
	
	public void generateLoads() {
		System.out.println("Generating parcels...");
		// System.out.print("done");
		
		Bag centroidGeoms = centroidsLayer.getGeometries();

		//System.out.println("Assigning parcels to drivers...");

		for (Object o : centroidGeoms) {

			MasonGeometry myCentroid = (MasonGeometry) o;
			int households = myCentroid.getIntegerAttribute("FZHouses");
			Headquarters d = getClosestDepot(myCentroid);
			ArrayList<AidLoad> myLoads = new ArrayList<AidLoad>();

			// create a number of loads based on the number of households + 1 to cover any
			// stragglers
			int numLoads = households / approxManifestSize + 1;
			for (int i = 0; i < numLoads; i++) {

				Point deliveryLoc = myCentroid.geometry.getCentroid();
				Coordinate myCoordinate = deliveryLoc.getCoordinate();

				if (!MBR.contains(myCoordinate)) {
					System.out.println("myCoordinate is NOT in MBR!");
					i--;
					continue;
				}

				AidLoad p = new AidLoad(d, myCentroid, this);
				p.setDeliveryLocation(myCoordinate);
				myLoads.add(p);
				
				loadsRecord.add(p);
			}
			//d.addLoads(myLoads);
			//System.out.println("\t generateLoads() myLoads: " + myLoads); 
			//all the same HQ POINT (381183.4246222474 212709.48584634456)
		}
	}

	public Headquarters getClosestDepot(MasonGeometry target) {
		double minDist = Double.MAX_VALUE;
		Headquarters closestHQ = null;
		for (Object o : this.depotLayer.getGeometries()) {
			Headquarters h = (Headquarters) o;
			h.geometry.distance(target.geometry);
			double dist = h.geometry.distance(target.geometry);
			if (dist < minDist) {
				minDist = dist;
				closestHQ = h;
				
			}
		}
			
		return closestHQ;
	}
	

	int getMostVulnerableUnassignedWard() {
		//System.out.println("\nGetting unassigned LSOA with highest PRIO ratings...");
		Bag centroidGeoms = centroidsLayer.getGeometries();

		int highestOSVI = -1;
		MasonGeometry myCopy = null;

		for (Iterator it = centroidGeoms.iterator(); it.hasNext();) {
			MasonGeometry masonGeometry = (MasonGeometry) it.next();
			boolean isLast = !it.hasNext(); // does this fix myCopy ending up at null?
			// for (Object o : lsoaGeoms) {
			// MasonGeometry masonGeometry = (MasonGeometry) o;
			int id = masonGeometry.getIntegerAttribute("CentroidID");
			// int osviRating = masonGeometry.getIntegerAttribute("L_GL_OSVI_");
			String lsoaID = masonGeometry.getStringAttribute("LSOA_NAME");
			int tempOSVI = masonGeometry.getIntegerAttribute("NOSVIFZ3");
			//int tempOSVI = masonGeometry.getIntegerAttribute("LPRIO");
			
			int households = masonGeometry.getIntegerAttribute("FZHouses"); // num of households per LSOA
			Point highestWard = masonGeometry.geometry.getCentroid();
			//System.out.println(lsoaID + " - OSVI rating: " + tempOSVI + ", ID: " + id);
			if (assignedWards.contains(id))
				continue;

			// temp = the attribute in the "L_GL_OSVI_" column (int for each LSOA OSVI)
			if (tempOSVI > highestOSVI) { // if temp is higher than highest
				highestOSVI = tempOSVI; // update highest to temp
				myCopy = masonGeometry; // update myCopy, which is a POLYGON
			}
		}

		if (myCopy == null) {
			System.out.println("ALERT: LSOA layer is null! Panic and scream!");
			return -1; // no ID to find if myCopy is null, so just return a fake value
		}

		int id = myCopy.getIntegerAttribute("CentroidID"); // id changes to the highestOSVI
		assignedWards.add(id); // add ID to the "assignedWards" ArrayList
		System.out.println("\tHighest OSVI Raiting is: " + myCopy.getIntegerAttribute("NOSVIFZ3") + " for: "
				+ myCopy.getStringAttribute("LSOA_NAME") + " (ward ID: " + myCopy.getIntegerAttribute("CentroidID") + ")"
				+ " and it has " + myCopy.getIntegerAttribute("FZHouses") + " households that may need assistance.");
		
		//System.out.println("\tHighest PRIO Raiting is: " + myCopy.getIntegerAttribute("LPRIO") + " for: "
		//		+ myCopy.getStringAttribute("LSOA_NAME") + " (ward ID: " + myCopy.getIntegerAttribute("CentroidID") + ")"
		//		+ " and it has " + myCopy.getIntegerAttribute("FZHouses") + " households that may need assistance.");
		System.out.println("\t\tCurrent list of most vulnerable unassigned wards: " + assignedWards);
		//System.out.println("\t\tCurrent list of HIGH PRIORITY unassigned wards: " + assignedWards);
		// Prints out: the ID for the highestOSVI
		return myCopy.getIntegerAttribute("CentroidID"); // TODO: ID instead?
	}

	/**
	 * Finish the simulation and clean up
	 */
	public void finish() {
		super.finish();

		System.out.println();
		System.out.println("Simulation ended by user.");

		System.out.println();
		System.out.println("///////////////////////\nOUTPUTTING STUFFS\n///////////////////////");
		System.out.println();

		try {
			// save the history
			BufferedWriter output = new BufferedWriter(
					new FileWriter(dirName + "RoundRecord_" + formatted + "_" + mySeed + ".txt"));

			output.write("ROUND RECORD: " + "# Drivers: " + numMaxAgents + "; " + "# Bays: " + numBays + "; "
					+ "Loading Time: " + loadingTime + "; " + "Delivery Time: " + deliveryTime + "; "
					+ "Manifest Size: " + approxManifestSize + "\nDriver,Duration,Distance,Finish time\n");
			for (Driver a : agents) {
				for (String s : a.getHistory())
					output.write(s + "\n");
			}
			output.close();

			BufferedWriter output1 = new BufferedWriter(
					new FileWriter(dirName + "ParcelRecord_" + formatted + "_" + mySeed + ".txt"));
			output1.write("PARCEL RECORD: " + "# Drivers: " + numMaxAgents + "; " + "# Bays: " + numBays + "; "
					+ "Loading Time: " + loadingTime + "; " + "Delivery Time: " + deliveryTime + "; "
					+ "Manifest Size: " + approxManifestSize
					+ "\nLoad ID,Delivered to,Delivery time step,Load transferred from,Driver,Departure time step\n");
			// output1.write(
			// "Load ID,Delivered to,Delivery time step,Load transferred
			// from,Driver,Departure time step\\n");

			for (AidLoad al : loadsRecord) {
				output1.write(al.giveName() + "\t");
				String pOutput = "";
				for (int s = al.getHistory().size() - 1; s >= 0; s--)
					pOutput += al.getHistory().get(s);
				output1.write(pOutput + "\n");
			}
			output1.close();

			BufferedWriter output2 = new BufferedWriter(
					new FileWriter(dirName + "WardsVisited_" + formatted + "_" + mySeed + ".txt"));
			output2.write("WARDS VISITED: " + "# Drivers: " + numMaxAgents + "; " + "# Bays: " + numBays + "; "
					+ "Loading Time: " + loadingTime + "; " + "Delivery Time: " + deliveryTime + "; "
					+ "Manifest Size: " + approxManifestSize + "\nLSOA,Num. Visits\n");
			// output2.write("LSOA,Num. Visits\\n");

			for (MasonGeometry ward : visitedWardRecord.keySet()) {
				output2.write(ward.getStringAttribute("LSOA_NAME") + "\t" + visitedWardRecord.get(ward) + "\n");
			}

			/*
			 * for (AidParcel d : history) { for (String s : d.getHistory()) output.write(s
			 * + "\n"); }
			 */

			output2.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * RoadClosure structure holds information about a road closure
	 */
	public class RoadClosure extends Information {
		public RoadClosure(Object o, long time, Object source) {
			super(o, time, source, 5);
		}
	}

	/** set the seed of the random number generator */
	void seedRandom(long number) {
		random = new MersenneTwisterFast(number);
		mySeed = number;
	}

	SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd");
	String formatted = df.format(new Date());

	/**
	 * Main Function
	 * 
	 * Main function allows simulation to be run in stand-alone, non-GUI mode
	 */
	public static void main(String[] args) {

		if (args.length < 0) {
			System.out.println("///////////////////////\nUSAGE ERROR!\n///////////////////////");
			System.exit(0);
		}

		EngD_Final engd_Final = new EngD_Final(System.currentTimeMillis());

		System.out.println("Loading simulation...");

		engd_Final.start();

		System.out.println("Running simulation...");

		for (int i = 0; i < 288 * 5; i++) { // 288*5 = 1440minutes / 60minutes = 24hours
		//for (int i = 0; i < 576 * 5; i++) { // 576*5 = 2880minutes / 60minutes = 48hours
		//for (int i = 0; i < 864 * 5; i++) { // 864*5 = 4320minutes / 60minutes = 72hours
		//for (int i = 0; i < 2016 * 5; i++) { // 2016*5 = 10080minutes / 60minutes = 168 hours(7days)
			engd_Final.schedule.step(engd_Final);
		}

		engd_Final.finish();

		System.out.println("...simulation run finished.");

		System.exit(0);
	}
}