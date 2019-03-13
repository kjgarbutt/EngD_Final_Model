package sim;

/**
 * Allows the user to run the model a set number of times and just walk away
 * 
 * @author KJGarbutt
 *
 */

public class ScenarioRunner {

	public static void main(String[] args) {

		for (int x = 0; x < 10; x++) {

			EngD_Final dummySim = new EngD_Final(System.currentTimeMillis());

			System.out.println("///////////////////////\nNEW RUN..." + x + "\n///////////////////////");
			// System.out.println("NEW RUN..." + x);
			System.out.println("///////////////////////\nLOADING...\n///////////////////////");
			// System.out.println("Loading...");

			dummySim.start();

			System.out.println("///////////////////////\nRUNNING...\n///////////////////////");
			// System.out.println("Running...");

			while (!dummySim.schedule.scheduleComplete() && dummySim.schedule.getTime() < 288 * 3) {
				dummySim.schedule.step(dummySim);
			}

			dummySim.finish();

			System.out.println("///////////////////////\n...RUN FINISHED\n///////////////////////");
			// System.out.println("...run finished");
		}

	}
}