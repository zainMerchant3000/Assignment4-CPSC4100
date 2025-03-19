//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.io.*;
import java.util.*;

public class FindAllMatches {
    // bag (rolling window)
    static class B {
        // rolling hash formula.
        //  primary: sum of ("rank" + 1) to the power of POW * ("x-value" + 1)
        //  for all points in the array / window in the array.
        //  rolling, three terms added to old hash (hash_):
        //      for the removed point:
        //          - ("rank" + 1) to the power of POW * ("x-value" + 1)
        //      for the other existing points:
        //          - (sum of ("rank" + 1) th the power of POW * (number of points in the rank)
        //      for the added point:
        //          + ("would-be rank" + 1) to the power of POW * ("would-be x-value" + 1)

        // FIXME: choose better values for collision avoidance.
        static long POW = 1, OFF = 1;

        // sz: number of distinct y values (expect to grow or shrink)
        int sz;
        long hash_;
        // rank to y
        byte[] ys;
        // y to rank or (-1) on nonexistence
        byte[] ranks;
        // rank to xs, ascending
        List<Set<Integer>> xss;

        // initialize bag given values and window length
        // window will be positioned at index 0 and a hash will be computed for it.
        public B(byte[] values, int length) {
            // ~counting sort
            // array to track which y-values exist
            var ex = new boolean[101];
            ranks = new byte[101];
            Arrays.fill(ranks, (byte) -1);
            // marking y-values
            for (var v : values) ex[v] = true;

            byte r = 0;
            for (int y = 1; y < ex.length; y++) {
                if (ex[y]) {
                    ranks[y] = r++;
                   // System.out.println("ranks[" + y + "] = " + ranks[y]);
                }
            }
            sz = r;
            ys = new byte[sz];
            // creating bag
            xss = new ArrayList<>();
            for (int i = 0; i < sz; i++) xss.add(new TreeSet<>());
            // mapping ranks to values:
            // ex)
            r = 0;
            for (int y = 1; y < ex.length; y++) {
                if (ex[y]) {
                    ys[r++] = (byte) y;
                   // System.out.println("ys[" + (r-1) + "] = " + ys[r-1]);
                }
            }
            for (int x = 0; x < length; x++) {
                // values[x] = values[0] = 1
                xss.get(ranks[values[x]]).add(x);
               // System.out.println("xss.get(ranks[" + x + "])");
                //System.out.println(xss.get(ranks[values[x]]));
               // System.out.println(xss);
            }
            // compute initial hash
            hash_ = 0;
            for (r = 0; r < sz; r++) {
                long mul = powp1(r), sum = 0;
                for (var x : xss.get(r)) sum += x + OFF;
                hash_ = mul * sum;
            }
        }

        private static long powp1(long r) {
            return (long) Math.pow(r + 1, POW);
        }

        // get last stored hash.
        // hash first computed at construction, then updated
        // every time at roll. each roll call also gives hash.
        public long hash() {
            return hash_;
        }

        // evict (x1, y1); append (x2, y2).
        // x1 will always be the leftmost item in the window
        // x2 will always be the new item, located just to the right of the window.
        public void roll(int x1, int y1, int x2, int y2) {
            System.out.println("x1: " + x1 + ", y1: " + y1 + ", x2: " + x2 + ", y2: " + y2);
            // evict y1
            var r1 = ranks[y1];
            System.out.println("ranks[13:] = " + ranks[13]);
            var y1xs = xss.get(r1);
            System.out.println("y1xs: " + y1xs);
            // check if rank has only 1 element (determine whether we need to completly remove
            // rank (y-value)
            var evict = y1xs.size() == 1;
            // update hash value for removal of rank
            hash_ -= powp1(r1) * OFF;
            if (evict) {
                // evict it...
                System.out.println("evicting");
                y1xs.clear();
                for (int r = r1; r < sz - 1; r++) {
                    // TODO: CHECK
                    ranks[ys[r] = ys[r + 1]]--;
                    System.out.println("ys[" + r + "] " + ys[r]);
                    xss.set(r, xss.get(r + 1));
                }
                // TODO: CHECK
                ranks[ys[--sz]] = -1;
                xss.set(sz, new TreeSet<>());
            } else {
                System.out.println("simply just remove point");
                y1xs.remove(x1); // otherwise can just remove point
                System.out.println("y1xs: " + y1xs);
        }
            // update hash for the bulk that remains:
            //  after we remove point 1, but
            //  before we add point 2.
            for (int r = 0; r < sz; r++)
                // evict -> determine whether rank should be adjusted
                // r >= r1 -> current rank (r) greater or equal to rank evicted (r1)
                // evict && r >= r1 -> eviction and current rank larger than rank evicted
                hash_ -= powp1(evict && r >= r1 ? r + 1 : r) * xss.get(r).size();
            // append y2
            var r2 = ranks[y2];
            if (r2 != -1) {
                xss.get(ranks[y2]).add(x2);
            } else {
                // find the rank of the greatest y value less than y2
                // could use a binary search ngl...
                // anyway, its rank + 1 will be the rank it will have.
                //
                // or if y2 is the smallest one, then it'll have rank 0.
                // so this way we find the would-be rank of y2.
                for (int y = y2 - 1; y >= 0; y--) {
                    if (ranks[y] != -1) {
                        r2 = (byte) (ranks[y] + 1);
                        break;
                    }
                }
                if (r2 == -1) r2 = 0;
                // TODO: CHECK
                for (int r = sz++; r > r2; r--) {
                    ranks[ys[r] = ys[r - 1]]++;
                    xss.set(r2, xss.get(r - 1));
                }
                // TODO: CHECK
                ranks[ys[r2]] = -1;
                xss.set(r2, new TreeSet<>());
            }
            // this time we add instead of subtract since we're adding this point.
            // TODO: CHECK
            hash_ += powp1(ranks[y2]) * (x2 - x1 - 1 + OFF);
        }
    }

    public static void main(String[] args) throws IOException {
        var in = new BufferedReader(new FileReader(args[0]));
        var head = in.readLine().split(" ");
        int N = Integer.parseInt(head[0]), M = Integer.parseInt(head[1]);
        byte[] haystack = new byte[N], needle = new byte[M];
        for (int i = 0; i < N; i++) haystack[i] = (byte) Integer.parseInt(in.readLine());
        for (int i = 0; i < M; i++) needle[i] = (byte) Integer.parseInt(in.readLine());
        in.close();
        var bag = new B(needle, M);
        var needleHash = bag.hash();
        bag = new B(haystack, M); // still needle's length, so M, not N. (processing based on window approach)
        for (var i = 0; i < N - M; i++) {
            var im = i + M;
            // Arrays.compare -> different arrays can produce the same hash value (collisions)
            //
            if (bag.hash() == needleHash && Arrays.compare(haystack, i, im, needle, 0, M) == 0)
                System.out.printf("%d ", i);
            bag.roll(i, haystack[i], im, haystack[im]);
        }
    }
}

