import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class ACO {

	private static final int MAX_ANTS = 2048 * 20;
	private final String TSP_FILE;
	private double[][] problem;
	private double[][] pheromones;
	private Random random;
	private ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private LinkedBlockingQueue<AntResult> results;

	public static final double PHEROMONE_PERSISTENCE = 0.3d; // between 0 and 1

	// greedy
	public static final double ALPHA = -0.2d;
	// rapid selection
	public static final double BETA = 9.6d;

	public static final double Q = 0.0001d; // somewhere between 0 and 1

	public ACO(String file) {
		this.TSP_FILE = file;
		this.random = new Random(System.currentTimeMillis());
		this.results = new LinkedBlockingQueue<AntResult>();
	}

	private final double[][] readMatrixFromFile() throws IOException {

		final BufferedReader br = new BufferedReader(new FileReader(new File(TSP_FILE)));

		final ArrayList<Record> records = new ArrayList<Record>();

		boolean readAhead = false;
		String line;

		// Read the TSP file containind coordinates and populate a List of Record object.
		while ((line = br.readLine()) != null) {

			if (line.equals("EOF")) {
				break;
			}

			if (readAhead) {
				String[] split = sweepNumbers(line.trim());
				records.add(new Record(Double.parseDouble(split[1].trim()), Double.parseDouble(split[2].trim())));
			}

			if (line.equals("NODE_COORD_SECTION")) {
				readAhead = true;
			}
		}

		br.close();

		// Create a matrix N x N; N is the size of the list that was populated.
		final double[][] localMatrix = new double[records.size()][records.size()];
		System.out.println(records.size());

		int rIndex = 0;
		// For each record populated in the matrix,
		for (Record r : records) {
			int hIndex = 0;
			for (Record h : records) {
				// Calculate the euclidian distance between them.
				localMatrix[rIndex][hIndex] = calculateEuclidianDistance(r.x, r.y, h.x, h.y);
				hIndex++;
			}
			rIndex++;
		}
		// The matrix with all the distances within it's vertices.
		return localMatrix;
	}

	private final String[] sweepNumbers(String trim) {
		String[] arr = new String[3];
		int currentIndex = 0;
		for (int i = 0; i < trim.length(); i++) {
			final char c = trim.charAt(i);
			if ((c) != 32) {
				for (int f = i + 1; f < trim.length(); f++) {
					final char x = trim.charAt(f);
					if ((x) == 32) {
						arr[currentIndex] = trim.substring(i, f);
						currentIndex++;
						break;
					} else if (f == trim.length() - 1) {
						arr[currentIndex] = trim.substring(i, trim.length());
						break;
					}
				}
				i = i + arr[currentIndex - 1].length();
			}
		}
		return arr;
	}

	private final double calculateEuclidianDistance(double x1, double y1, double x2, double y2) {
		// In mathematics, the Euclidean distance or Euclidean metric is the "ordinary" distance between two points in Euclidean space. With this distance, Euclidean space becomes a metric space. The
		// associated norm is called the Euclidean norm. Older literature refers to the metric as Pythagorean metric.
		// http://en.wikipedia.org/wiki/Euclidean_distance
		final double xDiff = x2 - x1;
		final double yDiff = y2 - y1;
		return Math.abs((Math.sqrt((xDiff * xDiff) + (yDiff * yDiff))));
	}

	private final double[][] initializePheromones() {
		double randValue = this.random.nextDouble();
		final double[][] localMatrix = new double[problem.length][problem.length];

		for (int i = 0; i < localMatrix.length; i++) {
			for (int j = 0; j < localMatrix[i].length; j++) {
				localMatrix[i][j] = randValue;
			}
		}

		return localMatrix;
	}

	private AntResult bestResult(ArrayList<AntResult> list) {
		AntResult ar, bestResult;
		double bestDistance = Double.MAX_VALUE;
		bestResult = null;

		for (int i = 0; i < list.size(); i++) {
			ar = list.get(i);
			if (ar.getDistance() < bestDistance) {
				bestResult = ar;
				bestDistance = ar.getDistance();
			}
		}

		return bestResult;
	}

	private void printBest(ArrayList<AntResult> list) {
		AntResult best;

		best = this.bestResult(list);

		if (best != null)
			System.out.printf("%s\n", best);
	}

	private void findBestWay() {

		ArrayList<AntResult> partialResult = new ArrayList<AntResult>(ACO.MAX_ANTS);
		for (int i = 0; i < ACO.MAX_ANTS; i++)
			this.service.execute(new Ant(this, this.random.nextInt(this.problem.length)));

		for (int i = 0, j = 0; i < ACO.MAX_ANTS; i++, j++) {
			try {
				AntResult ar = this.results.take();
				partialResult.add(ar);

				// Pretty cool
				if (j % 10000 == 0){
					this.printBest(partialResult);
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.printf("----Final Result----\n");
		this.printBest(partialResult);
		this.service.shutdown();
	}

	private final double calculatePheromones(double current, double newPheromone) {
		final double result = (1 - ACO.PHEROMONE_PERSISTENCE) * current + newPheromone;
		return result;
	}

	public synchronized void adjustPheromone(int x, int y, double newPheromone) {
		final double result = calculatePheromones(this.pheromones[x][y], newPheromone);
		if (result >= 0.0) {
			this.pheromones[x][y] = result;
		} else {
			this.pheromones[x][y] = 0;
		}
	}

	public synchronized double readPheromone(int x, int y) {
		return pheromones[x][y];
	}

	public void start() throws IOException {
		this.problem = this.readMatrixFromFile();
		this.pheromones = this.initializePheromones();
		this.findBestWay();

		// Print the matrix with all the distance values.
		for (int i = 0; i < problem.length; i++) {
			for (int j = 0; j < problem[i].length; j++) {
				System.out.printf("%g\t", problem[i][j]);
			}
			System.out.printf("\n");
		}

	}

	public double[][] getProblem() {
		return problem;
	}

	public LinkedBlockingQueue<AntResult> getResults() {
		return results;
	}

}

class AntResult {

	private int[] way;
	private double distance;

	public AntResult(int[] w, double d) {
		this.way = w;
		this.distance = d;
	}

	public int[] getWay() {
		return way;
	}

	public void setWay(int[] way) {
		this.way = way;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public String toString() {
		StringBuffer r = new StringBuffer("");

		r.append("Distance:" + String.valueOf(distance) + "\nNodes:");

		for (int i = 0; i < way.length; i++)
			r.append(String.valueOf(way[i]) + "\t");

		return r.toString();

	}

}
