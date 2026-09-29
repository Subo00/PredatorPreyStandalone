package standalone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Simulation {

    // ── tweak these to change behaviour ───────────────────────────────────────
    public static final int    GRID_RADIUS               = 7;
    public static final int    NUM_PREDATORS             = 4;
    public static final int    NUM_PREY                  = 15;
    public static final int    FOOD_INIT_MIN             = 0;
    public static final int    FOOD_INIT_MAX             = 60;
    public static final double PREDATOR_INIT_ENERGY      = 120.0;
    public static final double PREY_INIT_ENERGY          = 80.0;
    public static final double ENERGY_DECAY_PRED         = 0.5;
    public static final double ENERGY_DECAY_PREY         = 0.3;
    public static final double ENERGY_GAIN_CATCH         = 50.0;
    public static final double FOOD_ENERGY_GAIN          = 2.0;
    public static final int    CATCH_RADIUS              = 1;
    public static final int    PREDATOR_STARVATION_STEPS = 40;
    public static final int    PREY_STARVATION_STEPS     = 60;
    public static final double PRED_REPRO_THRESHOLD      = 80.0;
    public static final double PREY_REPRO_THRESHOLD      = 30.0;
    public static final double REPRO_ENERGY_RATE         = 0.08;
    public static final int    MAX_PREDATORS             = 20;
    public static final int    MAX_PREY                  = 60;

    // ── state ─────────────────────────────────────────────────────────────────
    private HexGrid        grid;
    private List<Agent>    predators = new ArrayList<>();
    private List<Agent>    prey      = new ArrayList<>();
    private final Random   rng;

    private int nextPredId = 0;
    private int nextPreyId = 0;
    private int step       = 0;
    private int catches    = 0;
    private int eats       = 0;
    private int deaths     = 0;
    private int births     = 0;

    private final List<String> lastEvents = new ArrayList<>();

    public Simulation(Random rng) {
        this.rng = rng;
        reset();
    }

    // ── public API ────────────────────────────────────────────────────────────

    public void reset() {
        step = catches = eats = deaths = births = 0;
        nextPredId = nextPreyId = 0;
        lastEvents.clear();

        grid = new HexGrid(GRID_RADIUS, 0);
        for (HexCell c : grid.getAllCells()) {
            int range = FOOD_INIT_MAX - FOOD_INIT_MIN;
            c.setFood(FOOD_INIT_MIN + (range > 0 ? rng.nextInt(range + 1) : 0));
        }

        List<HexCell> cells = new ArrayList<>(grid.getAllCells());
        Collections.shuffle(cells, rng);
        int idx = 0;

        predators.clear();
        for (int i = 0; i < NUM_PREDATORS; i++) {
            HexCell c = cells.get(idx++);
            predators.add(new Agent(Agent.Role.PREDATOR, nextPredId++,
                    c.getQ(), c.getR(), PREDATOR_INIT_ENERGY));
            c.setState(HexCell.CellState.PREDATOR);
        }

        prey.clear();
        for (int i = 0; i < NUM_PREY; i++) {
            HexCell c = cells.get(idx++);
            prey.add(new Agent(Agent.Role.PREY, nextPreyId++,
                    c.getQ(), c.getR(), PREY_INIT_ENERGY));
            c.setState(HexCell.CellState.PREY);
        }
    }

    public void step() {
        if (!isAlive()) return;
        lastEvents.clear();

        decideAll();       // 1. every agent picks an action randomly
        resolveEating();   // 2. prey that chose eat consume grass
        applyMovement();   // 3. agents that chose a direction move
        applyDecay();      // 4. everyone loses a little energy
        applyHunger();     // 5. hunger clock ticks; starved agents die
        resolveCatches();  // 6. predators adjacent to prey kill them
        resolveReproduction(); // 7. healthy agents may spawn offspring
        grid.tickFoodGrowth(); // 8. grass spreads

        step++;
    }

    public boolean isAlive() {
        return predators.stream().anyMatch(Agent::isAlive)
                && prey.stream().anyMatch(Agent::isAlive);
    }

    // ── getters ───────────────────────────────────────────────────────────────
    public HexGrid      getGrid()       { return grid;       }
    public List<Agent>  getPredators()  { return predators;  }
    public List<Agent>  getPrey()       { return prey;       }
    public int          getStep()       { return step;       }
    public int          getCatches()    { return catches;    }
    public int          getEats()       { return eats;       }
    public int          getDeaths()     { return deaths;     }
    public int          getBirths()     { return births;     }
    public List<String> getLastEvents() { return lastEvents; }
    public long alivePredators() { return predators.stream().filter(Agent::isAlive).count(); }
    public long alivePrey()      { return prey.stream()     .filter(Agent::isAlive).count(); }

    // ── 1. random decisions ───────────────────────────────────────────────────

    private void decideAll() {
        for (Agent a : predators) {
            if (!a.isAlive()) continue;
            // Predators always move randomly
            a.setIntendedMove(rng.nextInt(6));
        }
        for (Agent a : prey) {
            if (!a.isAlive()) continue;
            // Prey: 30% chance to eat if there is food here, otherwise move
            HexCell cell = grid.getCell(a.getQ(), a.getR());
            if (rng.nextFloat() < 0.30f && cell != null && cell.getFood() > 0) {
                a.setIntendedMove(Agent.STAY_AND_EAT);
            } else {
                a.setIntendedMove(rng.nextInt(6));
            }
        }
    }

    // ── 2. eating ─────────────────────────────────────────────────────────────

    private void resolveEating() {
        for (Agent p : prey) {
            if (!p.isAlive() || p.getIntendedMove() != Agent.STAY_AND_EAT) continue;
            HexCell cell = grid.getCell(p.getQ(), p.getR());
            if (cell == null) { p.setIntendedMove(Agent.STAY); continue; }
            int consumed = cell.consumeFood(HexCell.FOOD_EAT_AMOUNT);
            if (consumed > 0) {
                double gained = consumed * FOOD_ENERGY_GAIN;
                p.addEnergy(gained);
                eats++;
                lastEvents.add(String.format("Prey#%d ate %d food (+%.1f energy)",
                        p.getId(), consumed, gained));
            }
            p.setIntendedMove(Agent.STAY);
        }
    }

    // ── 3. movement ───────────────────────────────────────────────────────────

    private void applyMovement() {
        for (Agent a : predators) move(a);
        for (Agent a : prey)      move(a);
    }

    private void move(Agent a) {
        if (!a.isAlive()) return;
        int dir = a.getIntendedMove();
        if (dir < 0 || dir > 5) return;
        int[] d  = HexCell.DIRECTIONS[dir];
        int   nq = a.getQ() + d[0];
        int   nr = a.getR() + d[1];
        if (grid.inBounds(nq, nr)) {
            grid.getCell(a.getQ(), a.getR()).setState(HexCell.CellState.EMPTY);
            a.moveTo(nq, nr);
            grid.getCell(nq, nr).setState(
                    a.getRole() == Agent.Role.PREDATOR
                            ? HexCell.CellState.PREDATOR
                            : HexCell.CellState.PREY);
        }
    }

    // ── 4. energy decay ───────────────────────────────────────────────────────

    private void applyDecay() {
        for (Agent a : predators) if (a.isAlive()) a.addEnergy(-ENERGY_DECAY_PRED);
        for (Agent a : prey)      if (a.isAlive()) a.addEnergy(-ENERGY_DECAY_PREY);
    }

    // ── 5. starvation ─────────────────────────────────────────────────────────

    private void applyHunger() {
        for (Agent a : predators) {
            if (!a.isAlive()) continue;
            a.incrementHunger();
            if (a.getStepsSinceLastMeal() >= PREDATOR_STARVATION_STEPS)
                kill(a, "starved");
        }
        for (Agent a : prey) {
            if (!a.isAlive()) continue;
            a.incrementHunger();
            if (a.getStepsSinceLastMeal() >= PREY_STARVATION_STEPS)
                kill(a, "starved");
        }
    }

    private void kill(Agent a, String reason) {
        a.die();
        HexCell c = grid.getCell(a.getQ(), a.getR());
        if (c != null) c.setState(HexCell.CellState.EMPTY);
        deaths++;
        lastEvents.add(String.format("%s#%d %s at (%d,%d)",
                a.getRole() == Agent.Role.PREDATOR ? "Predator" : "Prey",
                a.getId(), reason, a.getQ(), a.getR()));
    }

    // ── 6. catches ────────────────────────────────────────────────────────────

    private void resolveCatches() {
        for (Agent pred : predators) {
            if (!pred.isAlive()) continue;
            for (Agent p : prey) {
                if (!p.isAlive()) continue;
                if (axialDist(pred, p) <= CATCH_RADIUS) {
                    kill(p, "caught by Predator#" + pred.getId());
                    pred.addEnergy(ENERGY_GAIN_CATCH);
                    catches++;
                    lastEvents.add(String.format(
                            "Predator#%d caught Prey#%d [+%.0f energy]",
                            pred.getId(), p.getId(), ENERGY_GAIN_CATCH));
                    break;
                }
            }
        }
    }

    // ── 7. reproduction ───────────────────────────────────────────────────────

    private void resolveReproduction() {
        List<Agent> newPred = new ArrayList<>();
        List<Agent> newPrey = new ArrayList<>();

        for (Agent a : predators) {
            if (!a.isAlive()) continue;
            if (predators.size() + newPred.size() >= MAX_PREDATORS) break;
            if (accumulate(a, PRED_REPRO_THRESHOLD)) {
                Agent child = spawn(a, Agent.Role.PREDATOR, nextPredId++);
                if (child != null) {
                    newPred.add(child);
                    lastEvents.add(String.format("Predator#%d reproduced → Predator#%d",
                            a.getId(), child.getId()));
                }
            }
        }

        for (Agent a : prey) {
            if (!a.isAlive()) continue;
            if (prey.size() + newPrey.size() >= MAX_PREY) break;
            if (accumulate(a, PREY_REPRO_THRESHOLD)) {
                Agent child = spawn(a, Agent.Role.PREY, nextPreyId++);
                if (child != null) {
                    newPrey.add(child);
                    lastEvents.add(String.format("Prey#%d reproduced → Prey#%d",
                            a.getId(), child.getId()));
                }
            }
        }

        births += newPred.size() + newPrey.size();
        predators.addAll(newPred);
        prey.addAll(newPrey);
    }

    private boolean accumulate(Agent a, double threshold) {
        double maxE    = a.getRole() == Agent.Role.PREDATOR
                ? PREDATOR_INIT_ENERGY : PREY_INIT_ENERGY;
        double accrual = (a.getEnergy() / maxE) * REPRO_ENERGY_RATE * threshold;
        a.addReproductionEnergy(accrual);
        if (a.getReproductionAccumulator() >= threshold) {
            a.resetReproductionAccumulator();
            return true;
        }
        return false;
    }

    private Agent spawn(Agent parent, Agent.Role role, int id) {
        HexCell parentCell = grid.getCell(parent.getQ(), parent.getR());
        if (parentCell == null) return null;
        List<HexCell> empty = new ArrayList<>();
        for (HexCell n : grid.getNeighbors(parentCell))
            if (n.getState() == HexCell.CellState.EMPTY) empty.add(n);
        if (empty.isEmpty()) return null;
        Collections.shuffle(empty, rng);
        HexCell bc = empty.get(0);
        Agent child = new Agent(role, id, bc.getQ(), bc.getR(), parent.getEnergy() * 0.5);
        bc.setState(role == Agent.Role.PREDATOR
                ? HexCell.CellState.PREDATOR : HexCell.CellState.PREY);
        return child;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private int axialDist(Agent a, Agent b) {
        return (Math.abs(a.getQ() - b.getQ())
              + Math.abs(a.getQ() + a.getR() - b.getQ() - b.getR())
              + Math.abs(a.getR() - b.getR())) / 2;
    }
}
