package standalone;

public class HexCell {

    public static final int    FOOD_MAX              = 100;
    public static final int    FOOD_SPREAD_THRESHOLD = 30;
    public static final double FOOD_SELF_GROWTH_RATE = 0.30;
    public static final int    FOOD_EAT_AMOUNT       = 10;

    public static final int[][] DIRECTIONS = {
        { 1,  0},
        { 1, -1},
        { 0, -1},
        {-1,  0},
        {-1,  1},
        { 0,  1}
    };

    public enum CellState { EMPTY, PREY, PREDATOR }

    private final int q;
    private final int r;
    private CellState state = CellState.EMPTY;
    private int food;
    int nextFood;

    public HexCell(int q, int r, int initialFood) {
        this.q    = q;
        this.r    = r;
        this.food = Math.min(Math.max(initialFood, 0), FOOD_MAX);
    }

    public int       getQ()     { return q; }
    public int       getR()     { return r; }
    public CellState getState() { return state; }
    public void      setState(CellState s) { this.state = s; }
    public int       getFood()  { return food; }
    public void      setFood(int f) { food = Math.min(Math.max(f, 0), FOOD_MAX); }

    public int consumeFood(int amount) {
        int consumed = Math.min(food, amount);
        food -= consumed;
        return consumed;
    }

    void computeNextFood(HexCell[] neighbours) {
        double gain = food * FOOD_SELF_GROWTH_RATE;
        for (HexCell n : neighbours)
            if (n.food >= FOOD_SPREAD_THRESHOLD)
                gain += (double) n.food / FOOD_SPREAD_THRESHOLD;
        nextFood = Math.min(Math.max(food + (int) gain, 0), FOOD_MAX);
    }

    void applyNextFood() { food = nextFood; }

    public int[][] neighborCoords() {
        int[][] nb = new int[6][2];
        for (int i = 0; i < 6; i++) {
            nb[i][0] = q + DIRECTIONS[i][0];
            nb[i][1] = r + DIRECTIONS[i][1];
        }
        return nb;
    }
}
