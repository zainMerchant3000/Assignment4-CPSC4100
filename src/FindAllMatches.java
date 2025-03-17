//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.io.*;
import java.util.*;

public class FindAllMatches {
    // internal method only used in 'find'.
    // requires fr and rank to be zeroed.
    private static void compress(byte[] xs, int[] fr, byte[] rank, TreeMap<Byte, ArrayList<Integer>> bag,ArrayList<Integer>[] rankToPositions ) {
        byte x = 0;
        for (var n : xs) fr[n]++;
        for (int i = 1; i < 101; i++) rank[i] = fr[i] > 0 ? x++ : x;
        for (int i = 0; i < xs.length; i++) {
            byte a = xs[i] = rank[xs[i]];
            System.out.println("xs[" + i + "]: " + xs[i]);
            //System.out.println("a: " + a);
            bag.computeIfAbsent(a, _ -> new ArrayList<>());
            bag.get(a).add(i);
            //update rankToPositions array:
            rankToPositions[xs[i]].add(i);
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
            for (int i : indices) {
                // Calculate (rank[i] + 1)^r
                long rankTerm = (long)Math.pow(rank + 1, POWER);
                // Calculate (pos[i] + a)
                long posTerm = (i - baseindex + 1) + A;
                // Multiply and add to hash
                hash += rankTerm * posTerm;
            }
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
        //
        ArrayList<Integer>[] rankToPositions = new ArrayList[101];
        for (int i = 0; i < 101; i++) {
            rankToPositions[i] = new ArrayList<>();
        }
        System.out.println("compress needle");

        compress(n, fr, rank, bag, rankToPositions);
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
        System.out.println("compress haystack");
        compress(h, fr, rank, bag, rankToPositions);

        // During compression, store positions by rank
        // i = rank
        // rankToPositions[i] = y-coordinate of rank
        for (int i = 0; i < h.length - n.length; i++) {
            // calculate hash for current window
            long k = calculateHash(bag, i);
            if (k == hash) result.add(i); // Monte-Carlo (may be inaccurate).

            // Skip window sliding if we've reached the end
            if (i == h.length - n.length) break;

            // Get elements at edges of window
            byte removedElement = h[i];
            byte addedElement = h[i + n.length];

            //update for removing element from left side
            // First remove this position from its rank
            int removedPos = i;
            byte removedRank = - 1;

            // Find rank of removed element
            for (byte r = 0; r < rankToPositions.length; r++) {
                if (rankToPositions[r].contains(removedPos)) {
                    removedRank = r;
                    break;
                }
            }
            if (removedRank != -1) {
                // Remove the position
                rankToPositions[removedRank].remove(Integer.valueOf(removedPos));
                bag.get(removedRank).remove(Integer.valueOf(removedPos));

                // If rank becomes empty, remove it from bag
                if (rankToPositions[removedRank].isEmpty()) {
                    bag.remove(removedRank);

                    // Decrease rank of all higher elements
                    for (byte r = (byte)(removedRank + 1); r < 101; r++) {
                        if (!rankToPositions[r].isEmpty()) {
                            // Update both data structures
                            rankToPositions[r-1] = new ArrayList<>(rankToPositions[r]);
                            rankToPositions[r] = new ArrayList<>();

                            // Move in the TreeMap
                            bag.put((byte)(r-1), bag.get(r));
                            bag.remove(r);
                        }
                    }
                }
            }

            // Update for adding element on right side
            // Find the rank this element should have
            int addedPos = i + n.length;
            byte valueToAdd = h[addedPos];
            byte addedRank = 0;

            // Find the appropriate rank
            boolean rankExists = false;
            for (byte r = 0; r < 101; r++) {
                if (!rankToPositions[r].isEmpty() && h[rankToPositions[r].get(0)] == valueToAdd) {
                    addedRank = r;
                    rankExists = true;
                    break;
                }
                if (!rankToPositions[r].isEmpty()) {
                    addedRank = (byte)(r + 1); // Take the next rank
                }
            }

            // If new value doesn't match existing rank, increase ranks as needed
            if (!rankExists) {
                // Increase rank of all equal or higher elements
                for (byte r = (byte)(addedRank); r < 101; r++) {
                    if (!rankToPositions[r].isEmpty()) {
                        // Move to higher rank in both structures
                        rankToPositions[r+1] = new ArrayList<>(rankToPositions[r]);
                        rankToPositions[r] = new ArrayList<>();

                        // Move in the TreeMap
                        bag.put((byte)(r+1), bag.get(r));
                        bag.remove(r);
                    }
                }
            }

            // Add the new element
            rankToPositions[addedRank].add(addedPos);
            bag.computeIfAbsent(addedRank, _ -> new ArrayList<>());
            bag.get(addedRank).add(addedPos);


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
            // Create an array to map ranks to their positions

            /*
            // bag.sequencedSet().stream() -> maps each rank to list of positions where that rank appears
            // .dropwhile(e-> e.getValue().getFirst() < hi) -> skip entries whose position < h[i] -> value being removed from
            // left side of window
            var ranks = bag.sequencedEntrySet().stream()
                    .dropWhile(e -> e.getValue().getFirst() < hi)
                    .map(Map.Entry::getKey).toList();
            // checking if element being removed has unique rank
            //   ->
             // h[bag.get(ranks.getFirst()).getFirst()] != hi -> check if position at lowest effected rank is different
             // than value being removed

            // for (var r: ranks) bag.put((byte) (r - 1), bag.get(r)) -> move position by one rank for each affected rank
            if (!ranks.isEmpty() && h[bag.get(ranks.getFirst()).getFirst()] != hi)
                for (var r : ranks) bag.put((byte) (r - 1), bag.get(r));
            // we are getting the new value (value in new part of sliding window)
            final var hin = h[i + n.length];
            ranks = bag.sequencedEntrySet().stream()
                    .filter(e -> e.getValue().getFirst() >= hi)
                    .map(Map.Entry::getKey).toList().reversed();
            if (!ranks.isEmpty() && h[bag.get(ranks.getLast()).getFirst()] != hin)
                for (var r : ranks) bag.put((byte) (r + 1), bag.get(r));
                */
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

