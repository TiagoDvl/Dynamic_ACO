import java.util.Random;

/**
 * 
 * @author Dhinakaran P.
 */
// Create a class extends with TimerTask
public class ScheduledTask {

	private double[][] vertices;
	private Random generator;

	public ScheduledTask(double[][] vertices) {
		this.vertices = vertices;
		this.generator = new Random();
	}

	// Modify edges values inside.
	public double[][] runThis() {
		for (int i = 0; i < vertices.length; i++) {
			for (int j = 0; j < vertices.length; j++) {
				int seed = (int) vertices[i][j];
				if (seed > 0.0) {
					int randomNumber = generator.nextInt(seed) + 1;
					vertices[i][j] = randomNumber;
				}
			}
		}

		return vertices;

	}
}