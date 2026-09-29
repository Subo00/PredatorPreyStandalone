package standalone;

public class Agent {

    public enum Role { PREDATOR, PREY }

    public static final int STAY         = -1;
    public static final int STAY_AND_EAT = -2;

    private final Role   role;
    private final int    id;
    private int          q, r;
    private double       energy;
    private boolean      alive                  = true;
    private int          stepsSinceLastMeal     = 0;
    private double       reproductionAccumulator = 0.0;
    private int          intendedMove           = STAY;

    public Agent(Role role, int id, int q, int r, double energy) {
        this.role   = role;
        this.id     = id;
        this.q      = q;
        this.r      = r;
        this.energy = energy;
    }

    // ── position ──────────────────────────────────────────────────────────────
    public int  getQ()              { return q; }
    public int  getR()              { return r; }
    public void moveTo(int q, int r){ this.q = q; this.r = r; }

    // ── energy / alive ────────────────────────────────────────────────────────
    public double  getEnergy()        { return energy; }
    public boolean isAlive()          { return alive;  }
    public void    die()              { alive = false; }

    public void addEnergy(double d) {
        energy = Math.max(0, energy + d);
        if (d > 0) stepsSinceLastMeal = 0;  // any positive gain resets hunger
    }

    // ── hunger ────────────────────────────────────────────────────────────────
    public int  getStepsSinceLastMeal() { return stepsSinceLastMeal; }
    public void incrementHunger()       { stepsSinceLastMeal++; }

    // ── reproduction ──────────────────────────────────────────────────────────
    public double getReproductionAccumulator()    { return reproductionAccumulator; }
    public void   addReproductionEnergy(double d) { reproductionAccumulator += d; }
    public void   resetReproductionAccumulator()  { reproductionAccumulator = 0; }

    // ── action ────────────────────────────────────────────────────────────────
    public int  getIntendedMove()        { return intendedMove; }
    public void setIntendedMove(int dir) { intendedMove = dir;  }

    // ── identity ──────────────────────────────────────────────────────────────
    public Role getRole() { return role; }
    public int  getId()   { return id;   }
}
