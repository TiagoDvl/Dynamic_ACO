import java.util.Random;

public class Ant implements Runnable {
	
	private ACO instance;
	private boolean[] visited;
	private int[] way;
	private double distance;
	private int start;
	private int toVisit;
	
	public Ant(ACO in, int start) {
		this.instance = in;
		this.visited = new boolean[in.getProblem().length];
		this.way = new int[in.getProblem().length];
		this.visited[start] = true;
		this.distance = 0.0;
		this.start = start;
		this.toVisit = this.visited.length - 1;
	}
	
	private double invertDouble(double distance) {
		if (distance == 0.0)
			return 0.0;
		else
			return 1.0 / distance;
	}

	private int getNextNode(int lastNode) {
		
		double columnSum = 0.0;
		int danglingUnvisited = -1;
		final double[] weights = new double[this.visited.length];
		
		if (this.toVisit <= 0)
			return -1;
		
		for (int i = 0; i < this.visited.length; i++) {
			columnSum += Math.pow(this.instance.readPheromone(lastNode, i),
					ACO.ALPHA)
					* Math.pow(this.invertDouble(this.instance.getProblem()[lastNode][i]),
							ACO.BETA);
		}

		double sum = 0.0;
		
		for (int x = 0; x < this.visited.length; x++) {
			if (!this.visited[x]) {
				weights[x] = calculateProbability(x, lastNode, columnSum);
				sum += weights[x];
				danglingUnvisited = x;
			}
		}
		
		if (sum == 0.0d)
			return danglingUnvisited;
		
		// weighted indexing stuff
		double pSum = 0.0;
		for (int i = 0; i < this.visited.length; i++) {
			pSum += weights[i] / sum;
			weights[i] = pSum;
		}

		final double r = new Random(System.currentTimeMillis()).nextDouble();
		
		for (int i = 0; i < this.visited.length; i++) {
			if (!this.visited[i]) {
				if (r <= weights[i]) {
					return i;
				}
			}
		}
		
		return -1;
	}

	private double calculateProbability(int row, int column, double sum) {
		final double p = Math.pow(instance.readPheromone(column, row),
				ACO.ALPHA)
				* Math.pow(this.invertDouble(this.instance.getProblem()[column][row]),
						ACO.BETA);
		return p / sum;
	}

	@Override
	public void run() {
		int  i, next, lastNode;
		
		i = 0;
		next = lastNode = start;
		
		while((next = this.getNextNode(lastNode)) != -1) {
			this.way[i++] = lastNode;
			this.distance += this.instance.getProblem()[lastNode][next];
		    final double phero = (ACO.Q / (this.distance));
		    this.instance.adjustPheromone(lastNode, next, phero);
			this.visited[next] = true;
			lastNode = next;
			this.toVisit--;
		}
		
		this.distance += this.instance.getProblem()[lastNode][this.start];
	    this.way[i] = lastNode;
	    
	    try {
			this.instance.getResults().put(new AntResult(this.way, this.distance));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
