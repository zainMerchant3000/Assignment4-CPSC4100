//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.io.*;
import java.util.*;

public class FindAllMatches {
    // internal method only used in 'find'.
    // requires fr and rank to be zeroed.
    private static void compress(byte[] xs, int[] fr, byte[] rank, Map<Byte, List<Integer>> bag) {
        byte x = 0;
        // count frequency of each element
        for (var n : xs) fr[n]++;
        for (int i = 1; i < 101; i++) {
            rank[i] = fr[i] > 0 ? x++ : x;
          //  System.out.println("rank[" + i + "]" + "= " + rank[i]);
        }
        // compress based on rank
        for (int i = 0; i < xs.length; i++)  {
            xs[i] = rank[xs[i]];
            System.out.println("xs[" + i + "]" + "= " + xs[i]);
            // check if contains no
            // !bag.containsKey(xs[0])
            // !bag.containsKey(0)
            if (!bag.containsKey(xs[i])) {
                // insert key with empty list
                bag.put(xs[i], new ArrayList<>());
            }
            // otherwise insert given indices
            bag.get(xs[i]).add(i);
        }
        System.out.println("bag: " + bag);

    }

    // Method to calculate hash value using given summation formula:
    // in replacement with Horner's method
    private static long calculateHash(Map<Byte, List<Integer>> map, int a, int r) {
        long hash = 0;
        for (Map.Entry<Byte, List<Integer>> entry : map.entrySet()) {
            //
            byte rank = entry.getKey();
            List<Integer> indices = entry.getValue();
            // retrieve indices at given rank
            for (int i : indices) {
                int pos = i * a;
                hash += (long) (rank + 1) * r * (pos + a);
            }
        }
        return hash;
    }

    // Return a list of all *potential* matches (need to be checked).
    static ArrayList<Integer> find(byte[] n, byte[] h0) {
        final int MOD = 101;
        final int RAD = 10193; // least prime greater than 101^2
        long hash = 0, bigrad = 1, temp = RAD % MOD;
        byte[] h = new byte[n.length];
        var result = new ArrayList<Integer>();
        // initialize
        //  hash - hash of the needle (n)
        //  bigrad - RAD ^ (M - 1), where M is |n|
        var fr = new int[101];
        var rank = new byte[101];
        // creating our bag
        // key -> is our rank #
        // value -> is our associated indices
        var bag = new HashMap<Byte, List<Integer>>();
        compress(n, fr, rank, bag);
        // calculate hash for needle (in replacement of horners')
        // a = 1, r = 1
        hash = calculateHash(bag, 1,1);
        // using rolling hash function
        for (var b : n) hash = (((hash * (RAD % MOD)) % MOD) + b) % MOD;
        // calculate exponent
        for (int p = n.length - 1; p > 0; p >>= 1) {
            if ((p & 1) == 1) bigrad = (bigrad * temp) % MOD;
            temp = (temp * temp) % MOD;
        }

        // Rabin-Karp
        //  k - hash of a part of haystack
        //      to be compared with 'hash' (for the needle (n))
        for (int i = 0; i < h0.length - n.length; i++) {
            Arrays.fill(rank, (byte) 0);
            Arrays.fill(fr, (byte) 0);
            System.arraycopy(h0, i, h, 0, n.length);
            bag.clear();
            compress(h, fr, rank, bag);
           // long k_ = calculateHash(rank, 0, n.length);
            long k = 0;
            for (var b : h) k = (k * (RAD % MOD) + b) % MOD;
            if (k == hash) result.add(i);
            // x_(i+1) = (x_i - t_i * R^(M-1)) * R + t_(i+M)
            // k = ((k + MOD - h[i] * ((RAD % MOD) * RAD) % MOD) * RAD + h[i + n.length]) % MOD;
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
            // inaccuracy
            System.out.printf("%d ", match);
        System.out.println();
    }
}

