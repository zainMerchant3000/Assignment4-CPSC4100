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
        static long POW = 2, OFF = 2;

        long p = 31;
        long q = 1000000007;  // Modulus (large prime number to avoid overflow)

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
            // will optimize this:

            hash_ = 0;
            for (r = 0; r < sz; r++) {
                long mul = powp1(r), sum = 0;
                for (var x : xss.get(r)) sum += x + OFF;
                // Accumulate hash using Modular arithmetic:
                // Accumulate the hash with modular arithmetic
                hash_ = (hash_ + (mul * sum) % q) % q;  // Accumulate while applying modulus to avoid overflow
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
            System.out.println("Rolling window with:");
            System.out.println("x1 (evict index): " + x1 + ", y1 (evict value): " + y1);
            System.out.println("x2 (new index): " + x2 + ", y2 (new value): " + y2);
            // evict y1
            var r1 = ranks[y1];
            if (r1 < 0 || r1 >= xss.size()) {
                // Instead of skipping recalculate the rank
                // This is a recovery mechanism
                for (int r = 0; r < sz; r++) {
                    if (ys[r] == y1) {
                        r1 = (byte)r;
                        break;
                    }
                }
            }
            var y1xs = xss.get(r1);
            System.out.println("Current xss for rank " + r1 + " (y1 = " + y1 + "): " + y1xs);
            // check if rank has only 1 element (determine whether we need to completly remove
            // rank (y-value)
            var evict = y1xs.size() == 1;
            System.out.println("Should evict: " + evict);
            // update hash value for removal of rank
            long previousHash = hash_;
            hash_ -= powp1(r1) * OFF;
            System.out.println("Updated hash after subtraction (before addition of y2): " + previousHash + " -> " + hash_);
            if (evict) {
                System.out.println("Evicting entire rank " + r1 + " for y1 = " + y1);
                // evict rank
                y1xs.clear();
                System.out.println("Cleared xss for rank " + r1 + ": " + y1xs);
                for (int r = r1; r < sz - 1; r++) {
                    // TODO: CHECK
                    if (r >= ys.length || r + 1 >= ys.length) {
                        System.out.println("Invalid index during rank shifting. Skipping shift.");
                        break;
                    }
                    ranks[ys[r] = ys[r + 1]]--;
                    System.out.println("Shifting y-values and ranks:");
                    System.out.println("ys[" + r + "] " + ys[r] + ", ranks[" + ys[r] + "] = " + ranks[ys[r]]);
                    xss.set(r, xss.get(r + 1));
                    System.out.println("xss[" + r + "] = " + xss.get(r));

                }
                // TODO: CHECK
                // remove last rank
                if (sz > 0) {
                    ranks[ys[--sz]] = -1; // remove last rank
                    xss.set(sz, new TreeSet<>());
                    System.out.println("Rank and xss after eviction: " + Arrays.toString(ranks) + ", " + xss);
                }
            } else {
                System.out.println("Removing point from existing rank");
                y1xs.remove(x1); // otherwise can just remove point
                System.out.println("Updated xss for rank " + r1 + " after removal: " + y1xs);
            }
            // update hash for the bulk that remains:
            //  after we remove point 1, but
            //  before we add point 2.
            for (int r = 0; r < sz; r++) {
                if (r >= xss.size()) {
                    System.out.println("Invalid rank access: " + r + ". Skipping.");
                    continue; // Skip invalid rank access
                }

                // evict -> determine whether rank should be adjusted
                // r >= r1 -> current rank (r) greater or equal to rank evicted (r1)
                // evict && r >= r1 -> eviction and current rank larger than rank evicted
                // if(evict && r>= r1 == true) r+1;
                // if(evict && r >= r1 == false) r;
                // xss.get(r).size(); -> getting number of
                long pHash = hash_;
                System.out.println("Evict check for r = " + r + " (ranks comparison) -> evict = " + evict);
              //  System.out.println("Rank adjustment logic for r = " + r + ": " + (evict && r >= r1 ? r + 1 : r));
               // hash_ -= powp1(evict && r >= r1 ? r + 1 : r) * xss.get(r).size();
                hash_ -= powp1(r) * xss.get(r).size();
                System.out.println("Updated hash after subtraction (before addition of y2): " + previousHash + " -> " + hash_);
            }
            // append y2
            var r2 = ranks[y2];
            System.out.println("Appending y2 = " + y2 + " with initial rank r2: " + r2);
            //
            if (r2 != -1) {
                System.out.println("Rank already exists for y2 = " + y2 + ", adding x2 = " + x2 + " to xss at rank r2");
                xss.get(ranks[y2]).add(x2);
                System.out.println("Updated xss for rank r2: " + xss.get(ranks[y2]));
            } else {
                // Rank does not exist yet for y2; find the appropriate rank for y2
                System.out.println("No existing rank for y2 = " + y2 + ", finding appropriate rank.");
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
                System.out.println("Calculated rank for y2 = " + y2 + ": " + r2);
                // TODO: CHECK
                // shift ranks and adjust xss to insert y2 at the correct position
                System.out.println("Shifting ranks and xss to insert y2 at rank r2: " + r2);
                for (int r = sz++; r > r2; r--) {
                    if (r < 0 || r - 1 < 0) {
                        System.out.println("Invalid index during shifting ranks. Skipping.");
                        break;
                    }
                    ranks[ys[r] = ys[r - 1]]++; // shift y-values and adjust the ranks
                    System.out.println("Shifted ys[" + r + "] = " + ys[r] + ", updated ranks[" + ys[r] + "] = " + ranks[ys[r]]);
                    xss.set(r2, xss.get(r - 1)); // shift xss sets
                    System.out.println("Shifted xss[" + r2 + "] = " + xss.get(r2));
                }
                // TODO: CHECK
                // final adjustments after shift
                ranks[ys[r2]] = -1; // mark rank for inserted y-val
                xss.set(r2, new TreeSet<>()); // initialize set for new rank
                System.out.println("Final rank and xss after insertion of y2: " + Arrays.toString(ranks) + ", " + xss);
            }
            // this time we add instead of subtract since we're adding this point.
            // TODO: CHECK
            System.out.println("Adding point (x2 = " + x2 + ", y2 = " + y2 + ") to hash_.");
            hash_ += powp1(ranks[y2]) * (x2 - x1 - 1 + OFF);
            System.out.println("Updated hash after adding point: " + hash_);
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
            System.out.println("needleHash: " + needleHash);
            int currentHash = (int) bag.hash();
            System.out.println("bag.hash(): " + currentHash);
            // Compare the arrays and print the result of Arrays.compare
            int compareResult = Arrays.compare(haystack, i, im, needle, 0, M);
            System.out.println("Arrays.compare result: " + compareResult);  // Debug print statement

            if (currentHash == needleHash) {
                System.out.printf("%d ", i);
            }
             /*
            // The if condition checking hash and array comparison
            if (currentHash == needleHash && compareResult == 0) {
                System.out.printf("%d ", i);
            }

            if (bag.hash() == needleHash && Arrays.compare(haystack, i, im, needle, 0, M) == 0) {
                System.out.printf("%d ", i);
            }

             */
            System.out.println("calling bag.roll");
            bag.roll(i, haystack[i], im, haystack[im]);
        }
    }
}

