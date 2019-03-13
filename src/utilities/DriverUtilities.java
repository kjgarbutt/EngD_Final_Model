package utilities;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.Point;

import objects.Headquarters;
import objects.Driver;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import sim.EngD_Final;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomVectorField;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import utilities.InputCleaning;
import swise.objects.PopSynth;

public class DriverUtilities {

	public static synchronized ArrayList<Driver> setupDriversAtRandom(GeomVectorField buildings, EngD_Final world,
			GeometryFactory fa, int numDrivers) {

		ArrayList<Driver> agents = new ArrayList<Driver>();
		Bag myBuildings = buildings.getGeometries();
		int myBuildingsSize = myBuildings.numObjs;

		for (int i = 0; i < numDrivers; i++) {

			Object o = myBuildings.get(world.random.nextInt(myBuildingsSize));
			MasonGeometry mg = (MasonGeometry) o;
			while (mg.geometry.getArea() > 1000) {
				o = myBuildings.get(world.random.nextInt(myBuildingsSize));
				mg = (MasonGeometry) o;
			}
			// Point myPoint = mg.geometry.getCentroid();
			// Coordinate myC = new Coordinate(myPoint.getX(), myPoint.getY());
			Coordinate myC = (Coordinate) mg.geometry.getCoordinate().clone();
			Driver a = new Driver(world, myC);
			agents.add(a);

			world.schedule.scheduleOnce(a);
		}

		return agents;
	}

	public static synchronized ArrayList<Driver> setupDriversAtDepots(EngD_Final world, GeometryFactory fa,
			int numDrivers) {

		ArrayList<Driver> agents = new ArrayList<Driver>();
		Bag myDepots = world.depotLayer.getGeometries();
		int myDepotsSize = myDepots.numObjs;

		for (int i = 0; i < numDrivers; i++) {

			Object o = myDepots.get(world.random.nextInt(myDepotsSize));
			Headquarters depot = (Headquarters) o;
			Coordinate myC = (Coordinate) depot.geometry.getCoordinate().clone();
			Driver driver = new Driver(world, myC);
			agents.add(driver);

			depot.enterDepot(driver);
			// driver.setNode(depot.getNode());
		}

		return agents;
	}
}