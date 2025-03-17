//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.io.*;
import java.util.*;

public class FindAllMatches {
    // internal method only used in 'find'.
    // requires fr and rank to be zeroed.
    private static void compress(byte[] xs, int[] fr, byte[] rank, TreeMap<Byte, ArrayList<Integer>> bag) {
        byte x = 0;
        for (var n : xs) fr[n]++;
        for (int i = 1; i < 101; i++) rank[i] = fr[i] > 0 ? x++ : x;
        for (int i = 0; i < xs.length; i++) {
            byte a = xs[i] = rank[xs[i]];
            bag.computeIfAbsent(a, _ -> new ArrayList<>());
            bag.get(a).add(i);
        }
    }

    private static long calculateHash(TreeMap<Byte, ArrayList<Integer>> map, int baseindex) {
        final long POWER = 3L;
        final long A = 5L;
        long hash = 0;
        for (var entry : map.entrySet()) {
            var rank = entry.getKey();
            var indices = entry.getValue();
            // FIXME: revise the computation to match the description in the PDF.
            for (int i : indices) hash += (long) (rank + 1) * 3 * (i - baseindex + 1);
        }
        return hash;
    }

    // Return a list of all *potential* matches (need to be checked).
    static ArrayList<Integer> find(byte[] n, byte[] h) {
        final int MOD = 101;
        final int RAD = 10193; // least prime greater than 101^2
        long bigrad = 1, temp = RAD % MOD;
        var result = new ArrayList<Integer>();
        // initialize
        //  hash - hash of the needle (n)
        //  bigrad - RAD ^ (M - 1), where M is |n|
        var fr = new int[101];
        var rank = new byte[101];
        // creating our bag
        // key -> is our rank #
        // value -> is our associated indices
        var bag = new TreeMap<Byte, ArrayList<Integer>>();
        compress(n, fr, rank, bag);
        // needle hash
        final var hash = calculateHash(bag, 0);
        // calculate exponent
        for (int p = n.length - 1; p > 0; p >>= 1) {
            if ((p & 1) == 1) bigrad = (bigrad * temp) % MOD;
            temp = (temp * temp) % MOD;
        }
        // modified Rabin-Karp with our bag model.
        // this bag will now be repurposed for computing the hashes for
        // the subarrays of the haystack (h).
        bag = new TreeMap<>();
        compress(h, fr, rank, bag);
        for (int i = 0; i < h.length - n.length; i++) {
            long k = calculateHash(bag, i);
            if (k == hash) result.add(i); // Monte-Carlo (may be inaccurate).
            final var hi = h[i];
            // update bag
            //  1. if old item (leftmost)
            //      a. shares a rank with another item: do nothing.
            //      b. if not: downrank all items greater than its rank.
            //  2. if new item (rightmost)
            //      a. shares: do nothing.
            //      b. if not: uprank all items greater than its rank.
            //  3. recompute the hash with new relative index (i) -> this will be done
            //      automatically in the next iteration.
            // FIXME: possible inefficiency.
            // FIXME: do away with this complication by storing the y-coordinate
            // FIXME: of each rank in a separate array: rank -> y
            var ranks = bag.sequencedEntrySet().stream()
                    .dropWhile(e -> e.getValue().getFirst() < hi)
                    .map(Map.Entry::getKey).toList();
            if (!ranks.isEmpty() && h[bag.get(ranks.getFirst()).getFirst()] != hi)
                for (var r : ranks) bag.put((byte) (r - 1), bag.get(r));
            final var hin = h[i + n.length];
            ranks = bag.sequencedEntrySet().stream()
                    .filter(e -> e.getValue().getFirst() >= hi)
                    .map(Map.Entry::getKey).toList().reversed();
            if (!ranks.isEmpty() && h[bag.get(ranks.getLast()).getFirst()] != hin)
                for (var r : ranks) bag.put((byte) (r + 1), bag.get(r));
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        var in = new BufferedReader(new FileReader(args[0]));
        var head = in.readLine().split(" ");
        int N = Integer.parseInt(head[0]), M = Integer.parseInt(head[1]);
        byte[] haystack = new byte[N], needle = new byte[M];
        for (int i = 0; i < N; i++) haystack[i] = (byte) Integer.parseInt(in.readLine());
        for (int i = 0; i < M; i++) needle[i] = (byte) Integer.parseInt(in.readLine());
        in.close();
        var matches = find(needle, haystack);
        for (var match : matches)
            // FIXME: Check the matches to see if they're actual matches.
            System.out.printf("%d ", match);
        System.out.println();
    }
}

