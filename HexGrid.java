package standalone;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HexGrid {

    private final Map<Long, HexCell> cells = new HashMap<>();
    private final int radius;

    public HexGrid(int radius, int initialFood) {
        this.radius = radius;
        for (int q = -radius; q <= radius; q++) {
            int r1 = Math.max(-radius, -q - radius);
            int r2 = Math.min( radius, -q + radius);
            for (int r = r1; r <= r2; r++)
                cells.put(key(q, r), new HexCell(q, r, initialFood));
        }
    }

    public HexCell getCell(int q, int r) { return cells.get(key(q, r)); }
    public boolean inBounds(int q, int r) { return cells.containsKey(key(q, r)); }
    public Collection<HexCell> getAllCells() { return cells.values(); }
    public int getRadius() { return radius; }

    public HexCell[] getNeighbors(HexCell cell) {
        int[][] coords = cell.neighborCoords();
        HexCell[] buf  = new HexCell[6];
        int count = 0;
        for (int[] c : coords) {
            HexCell n = getCell(c[0], c[1]);
            if (n != null) buf[count++] = n;
        }
        HexCell[] result = new HexCell[count];
        System.arraycopy(buf, 0, result, 0, count);
        return result;
    }

    public void tickFoodGrowth() {
        for (HexCell c : cells.values()) c.computeNextFood(getNeighbors(c));
        for (HexCell c : cells.values()) c.applyNextFood();
    }

    private static long key(int q, int r) {
        return ((long) q << 32) | (r & 0xFFFFFFFFL);
    }
}
