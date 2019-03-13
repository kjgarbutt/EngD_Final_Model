package sim;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.gui.ColorMap;
import sim.util.media.chart.TimeSeriesChartGenerator;
import swise.visualization.AttributePolyPortrayal;
import swise.visualization.SegmentedColorMap;

public class EngD_FinalWithUI extends GUIState {

	/////////////////////////////////////////////////
	/////////////// DISPLAY FUNCTIONS ///////////////
	/////////////////////////////////////////////////

	// SparseGridPortrayal2D driversPortrayal = new SparseGridPortrayal2D ();
	public Display2D display;
	public JFrame displayFrame;

	GeomVectorFieldPortrayal polyPortrayal = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal roads = new GeomVectorFieldPortrayal();
	// private GeomVectorFieldPortrayal lsoa = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal osvi = new GeomVectorFieldPortrayal(true);
	private GeomVectorFieldPortrayal boundary = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal floods2 = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal floods3 = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal centroids = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal headquarters = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal drivers = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal deliveryLocations = new GeomVectorFieldPortrayal();

	TimeSeriesChartGenerator trafficChart;
	XYSeries maxSpeed;
	XYSeries avgSpeed;
	XYSeries minSpeed;

	ColorMap cm = null;

	///////////////////////////////////////////////
	/////////////// BEGIN FUNCTIONS ///////////////
	///////////////////////////////////////////////

	/** constructor function */
	public EngD_FinalWithUI(SimState state) {
		super(state);
	}

	/** constructor function */
	public EngD_FinalWithUI() {
		super(new EngD_Final(System.currentTimeMillis()));
	}

	/**
	 * Model Modification
	 * 
	 * This must be included to have model tab, which allows mid-simulation
	 * modification of the coefficients
	 */
	public Object getSimulationInspectedObject() {
		return state;
	} // non-volatile

	/** Begins the simulation */
	public void start() {
		super.start();

		System.out.println();
		System.out.println("Starting simulation...");

		setupPortrayals();
	}

	/** Loads the simulation from a point */
	public void load(SimState state) {
		super.load(state);

		System.out.println("Loading simulation...");

		// we now have new grids. Set up the portrayals to reflect that
		setupPortrayals();
		// setupCharts();
	}

	public void setupPortrayals() {
		EngD_Final world = (EngD_Final) state;

		System.out.println("Setting up Model GUI...");

		boundary.setField(world.baseLayer);
		boundary.setPortrayalForAll(new GeomPortrayal(new Color(255, 150, 150, 50), 2, false));
		boundary.setImmutableField(true);

		// osvi.setField(world.osviLayer);
		// osvi.setPortrayalForAll(new OSVIPolyPortrayal());

		roads.setField(world.roadLayer);
		// roads.setPortrayalForAll(new GeomPortrayal(new Color(100,100,100, 50), 2,
		// false));
		roads.setPortrayalForAll(new GeomPortrayal(Color.LIGHT_GRAY, 0.0005, false));
		roads.setImmutableField(true);

		centroids.setField(world.centroidsLayer);
		// buildings.setPortrayalForAll(new GeomPortrayal(new Color(150,150,150, 100),
		// true));
		centroids.setPortrayalForAll(new GeomPortrayal(Color.YELLOW, true));
		centroids.setImmutableField(true);

		floods2.setField(world.fz2Layer);
		floods2.setPortrayalForAll(new GeomPortrayal(Color.BLUE, true));
		floods3.setImmutableField(true);

		floods3.setField(world.fz3Layer);
		floods3.setPortrayalForAll(new GeomPortrayal(Color.CYAN, true));
		floods3.setImmutableField(true);

		headquarters.setField(world.depotLayer);
		headquarters.setPortrayalForAll(new GeomPortrayal(Color.BLACK, 200, true));
		
		polyPortrayal.setField(world.world);
		polyPortrayal.setPortrayalForAll(new PolyPortrayal());
		polyPortrayal.setImmutableField(true);

		deliveryLocations.setField(world.deliveryLocationLayer);
		double[] levels = new double[100];
		Color[] colors = new Color[100];
		for (int i = 0; i < 100; i++) {
			levels[i] = i;
			colors[i] = new Color(world.random.nextInt(255), world.random.nextInt(255), world.random.nextInt(255));
		}
		SegmentedColorMap scm = new SegmentedColorMap(levels, colors);
		deliveryLocations.setPortrayalForAll(new AttributePolyPortrayal(scm, // new SimpleColorMap(0,100, Color.red,
																				// Color.green),
				"round", new Color(0, 0, 0, 0), true, 150));
		// agents.setImmutableField(true);

		drivers.setField(world.agentLayer);
		// drivers.setPortrayalForAll(new GeomPortrayal(new Color(255,150,150), 50));
		// agentPortrayal.setField(world.agentsLayer);
		drivers.setPortrayalForAll(new GeomPortrayal(Color.MAGENTA, 150, true));

		display.reset();
		// display.setBackdrop(new Color(10,10,10)); // black
		display.setBackdrop(Color.WHITE);
		// redraw the display
		display.repaint();
	}

	/**
	 * Poly Portrayal Colours 
	 * The portrayal used to display Polygons with the appropriate colour
	 */
	class PolyPortrayal extends GeomPortrayal {

		private static final long serialVersionUID = 1L;

		@Override
		public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
			Polygon poly = (Polygon) object;

			if (poly.getSoc().equals("Red")) {
				paint = Color.red;
			}

			else if (poly.getSoc().equals("Orange")) {
				paint = Color.orange;
			}

			else if (poly.getSoc().equals("Yellow")) {
				paint = Color.yellow;
			}

			else if (poly.getSoc().equals("Green")) {
				paint = Color.green;
			} else {
				paint = Color.gray;
			}

			super.draw(object, graphics, info);
		}

	}
	/**
	 * Sets up the portrayals within the map visualization.
	 */
	/*
	 * public void setupCharts(){ maxSpeed = new XYSeries("Max Speed"); avgSpeed =
	 * new XYSeries("Average Speed"); minSpeed = new XYSeries("Min Speed");
	 * trafficChart.removeAllSeries(); trafficChart.addSeries(maxSpeed, null);
	 * trafficChart.addSeries(avgSpeed, null); trafficChart.addSeries(minSpeed,
	 * null);
	 * 
	 * state.schedule.scheduleRepeating(new Steppable() { private static final long
	 * serialVersionUID = -3749005402522867098L;
	 * 
	 * public void step(SimState state) { sim.MK_10 world = (sim.MK_10) state;
	 * double maxS = 0, minS = 10000, avgS = 0, count = 0;
	 * //////////////////////////// Main Agent ////////////////////// for (Agent a :
	 * world.agentList) { if (a.distributing) { continue; } count++; double speed =
	 * Math.abs(a.speed); avgS += speed; if (speed > maxS) { maxS = speed; } if
	 * (speed < minS) { minS = speed; } }
	 * 
	 * double time = state.schedule.time(); avgS /= count; maxSpeed.add(time, maxS,
	 * true); minSpeed.add(time, minS, true); avgSpeed.add(time, avgS, true); } }
	 * 
	 * }
	 */

	/**
	 * Visualisation Format
	 * 
	 * Initializes the simulation visualization. Sets up the display window, the
	 * JFrames, and the chart structure.
	 */
	public void init(Controller controller) {
		super.init(controller);

		/////////////// MAIN DISPLAY ///////////////
		// makes the displayer and visualises the maps
		display = new Display2D((int) (EngD_Final.grid_width), (int) (EngD_Final.grid_height), this);
		// turn off clipping
		// display.setClipping(false);

		// Put portrayals in order from bottom layer to top
		// display.attach(osvi, "OSVI");
		// display.attach(boundary, "County Outline");
		// display.attach(floods2, "FZ2 Zone");
		display.attach(polyPortrayal, "OSVI");
		display.attach(floods3, "FZ3 Zone");
		display.attach(roads, "Roads");
		display.attach(centroids, "Centroids");
		display.attach(deliveryLocations, "Delivery Locations"); // , false);
		display.attach(headquarters, "HQ");
		display.attach(drivers, "Drivers");

		displayFrame = display.createFrame();
		displayFrame.setTitle("Final EngD ABM Model");
		controller.registerFrame(displayFrame); // register the frame so it appears in the "Display" list
		displayFrame.setVisible(true);

		/////////////// CHART ///////////////
		// trafficChart = new TimeSeriesChartGenerator();
		// trafficChart.setTitle("Traffic Stats");
		// trafficChart.setYAxisLabel("Speed");
		// trafficChart.setXAxisLabel("Time");
		// JFrame chartFrame = trafficChart.createFrame(this);
		// chartFrame.pack();
		// c.registerFrame(chartFrame);
	}

	/**
	 * Model Finish
	 * Quits the simulation and cleans up.
	 */
	public void quit() {
		System.out.println("Model closed.");
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null; // let gc
		display = null; // let gc
	}

	/**
	 * Simulation Name
	 * 
	 * @return name of the simulation
	 */
	public static String getName() {
		return "Final EngD ABM Model";
	}

	public static void main(String[] args) {
		(new EngD_FinalWithUI()).createController();
		// EngD_FinalWithUI worldGUI = new EngD_FinalWithUI();
		// Console console = new Console(worldGUI);
		// console.setVisible(true);
	}
}