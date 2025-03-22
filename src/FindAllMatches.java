import java.io.*;
import java.util.*;

public class FindAllMatches {
    static class B {
        // rolling hash formula.
        //  primary: sum of "rank" to the power of POW * ("x-value" + OFF)
        //  for all points in the array / window in the array.
        //  rolling, three terms added to old hash (hash_):
        //      for the removed point:
        //          - "rank" to the power of POW * ("x-value" + OFF)
        //      * sequence point: ranks have changed now. *
        //      for the other existing points (loop):
        //          - sum of (("rank" to the power of POW) * (number of points in the rank))
        //      for the added point:
        //          + "would-be rank" to the power of POW * ("would-be x-value" + OFF)
        //      * sequence point: change ranks again. *
        //  ^ note: "x-value" should be relative to the window, not absolute.
        //  ^ note: "rank" should actually be 0-based rank plus 1 to prevent zero terms.
        //          we use a 0-based rank.
        static long POW = 1, OFF = 1;
        // sz: number of distinct y values (expect to grow or shrink)
        // (also, the number of ranks).
        int sz;
        // rolling window hash, initialized when initializing
        long hash_;

        // we have a pair of bijections here between ranks and y values.
        // when we update one, we must update the other, too, which is tricky.

        // rank to y
        byte[] ys;
        // y to rank or (-1) on nonexistence
        byte[] ranks;

        // unidirectional relationship.

        // rank to xs, ascending
        List<Set<Integer>> xss;

        // initialize bag given values and window length
        // window will be positioned at index 0 and a hash will be computed for it.
        public B(byte[] values, int length) {
            // ~counting sort
            var ex = new boolean[101];
            ranks = new byte[101];
            ys = new byte[101];
            Arrays.fill(ranks, (byte) -1);
            Arrays.fill(ys, (byte) -1);
            for (var i = 0; i < length; i++) ex[values[i]] = true;
            byte r = 0;
            for (byte y = 1; y < ex.length; y++)
                if (ex[y]) {
                    ranks[y] = r;
                    ys[r++] = y;
                }
            sz = r;
            xss = new ArrayList<>();
            for (int i = 0; i < 101; i++) xss.add(new TreeSet<>());
            for (int x = 0; x < length; x++)
                xss.get(ranks[values[x]]).add(x);
            hash_ = 0;
            for (r = 0; r < sz; r++) {
                long mul = hmul(r), sum = 0;
                for (var x : xss.get(r)) sum += x + OFF;
                if (sum == 0)
                    throw new RuntimeException("sum == 0");
                plushash(mul * sum);
            }
        }

        private static long hmul(long r) {
            return (long) Math.pow(r + 1, POW);
        }

        private void plushash(long h) {
            if (h == 0) throw new RuntimeException("plushash(0)");
            hash_ = hash_ + h;
        }

        // Remove the rank.
        private void evict(int r1) {
            // (as though simultaneously):
            // ranks[y] <- ranks[y] - 1
            // ys[ranks[y] - 1] <- ys[ranks[y]]
            // pull down...
            for (int r = r1 + 1; r < sz; r++) {
                byte s = (byte) (r - 1), y = ys[r];
                var xs = xss.get(r);
                ys[s] = y;
                xss.set(s, xs);
                ranks[y] = s;
            }
            // - because of the bijection requirement,
            // all the y values in ranks are unique, we
            // don't kneed to check for aliasing.
            // - sever the duplicated arrow and also clear the thing.
            ranks[--sz] = -1;
            xss.set(sz, new TreeSet<>());
        }

        // Insert the rank.
        private void uprank(int r2) {
            // now:     10 0 20 20 30 -- ranks {0<->0, 10<->1, 20<->2, 30<->3}
            // now:        0 20 20 30 -- ranks {0<->0, 20<->1, 30<->2}
            // new:        0 20 20 30 25 -- ranks {0<->0, 20<->1, 30<->2} *conflict {25,30}
            // now:        0 20 20 30 25 -- ranks {0<->0, 20<->1, __<->2, 30<->3} *rank of 30 -> 3
            for (var r = (byte) sz++; r > r2; r--) {
                byte s = (byte) (r - 1), y = ys[s];
                var xs = xss.get(s);
                ys[r] = y;
                xss.set(r, xs);
                ranks[y] = r;
            }
            xss.set(r2, new TreeSet<>());
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
            var r1 = ranks[y1]; // if r1 == -1, it's a bug; y1 should exist because it was in the window.
            var y1xs = xss.get(r1);
            // 1st term.
            plushash(-(hmul(r1) * (y1xs.size() + OFF)));
            // apply side effect before we compute the next term.
            if (y1xs.size() == 1) evict(r1);
            else y1xs.remove(x1);
            // 2nd term.
            for (int r = 0; r < sz; r++)
                plushash(-hmul(r) * (xss.get(r).size() + OFF));
            // it's legit possible that y2 either exists (in the window) or it doesn't.
            var r2 = ranks[y2];
            if (r2 == -1) {
                // TODO: Replace with binary search.
                // either way, it's constant time.
                // find the would-be rank of the y-value.
                for (var r = 0; r < sz; r++)
                    if (ys[r] > y2) {
                        r2 = (byte) r;
                        break;
                    }
                uprank(r2);
                ranks[y2] = r2;
            } else
                // new y value already in window.
                xss.get(ranks[y2]).add(x2);
            // 3rd term.
            r2 = ranks[y1]; // re-fetch r2 due to update in rank
            long newPointTerm = hmul(r2) * ((x2-x1-1)+ OFF);
            plushash(newPointTerm);
           // plushash(x2 - x1 - 1 + OFF);
            System.out.print("");
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
        bag = new B(haystack, M);
        for (var i = 0; i < N - M; i++) {
            var im = i + M;
            int currentHash = (int) bag.hash();
            System.out.println(currentHash);
            if (currentHash == needleHash)
//                if (Arrays.compare(haystack, i, im, needle, 0, M) == 0)
                    System.out.printf("!%d ", i);
            bag.roll(i, haystack[i], im, haystack[im]);
        }
        int currentHash = (int) bag.hash();
        if (currentHash == needleHash)
//            if (Arrays.compare(haystack, N - M, N, needle, 0, M) == 0)
                System.out.printf("!%d ", N - M);
    }
}

