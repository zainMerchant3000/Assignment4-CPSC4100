//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class FindAllMatches {
    static class S {
        final int N, M, hash;
        final byte[] needle, haystack;
        final static int MOD = 1000000007;
        final static int RAD = 101;

        public S(int N, int M, byte[] needle, byte[] haystack) {
            this.N = N;
            this.M = M;
            this.needle = needle;
            this.haystack = haystack;
            // create array to store frequencies of values
            // 101 -> maximum value is 100
            var fr = new int[101];
            // create array to store
            var rank = new byte[101];
            // method to y-compress
            System.out.println("compressed needle");
            compress(needle, fr, rank);
            Arrays.fill(fr, 0);
            Arrays.fill(rank, (byte) 0);
            System.out.println("compressed haystack");
            compress(haystack, fr, rank);
            int h = 0;
            for (var b : needle) h = (h * RAD + b) % MOD;
            hash = h;
        }

        private void compress(byte[] xs, int[] fr, byte[] rank) {
            byte x = 0;
            // count the frequency of each value

            // needle: [5,6,5,13,13,9]
            // fr[5] = 2
            // fr[6] = 1
            //

            // will count frequency of values in the array
            // fr[0] = 2, fr[1] = 3
            // fr[0]++ ->
            for (var n : xs) {
                fr[n]++;
                //System.out.println("fr[n]: " + fr[n]);
            }
            for (var n : xs) {
                System.out.println("fr["+ n +"] = " + fr[n]);
            }

            for (int i = 1; i < 101; i++) {
                // rank[1] = 0
                rank[i] = x;
                System.out.println("rank["+ i +"] = " + rank[i]);
                // increment x if current rank (x) less than frequency of i in fr array
                if (x < fr[i]) x++;
            }
            for (int i = 0; i < xs.length; i++) xs[i] = rank[xs[i]];
        }

        public ArrayList<Integer> solve() {
            // use Rabin-Karp to enumerate all matches
            var ms = new ArrayList<Integer>();
            throw new RuntimeException("need to solve");
        }
    }

    public static void main(String[] args) throws IOException {
        /*
        if (args.length != 1) {
            System.out.println("Usage: java FindAllMatches.java <test_file_name>");
            return;
        }

         */

        BufferedReader in = new BufferedReader(new FileReader(args[0]));
        String[] head = in.readLine().split(" ");
        int N = Integer.parseInt(head[0]);
        int M = Integer.parseInt(head[1]);
        System.out.println(N + " " + M);
        // needle -> pattern
        byte[] needle = new byte[M];
        byte[] haystack = new byte[N];
        for (int i = 0; i < N; i++) {
            int n = Integer.parseInt(in.readLine());
            haystack[i] = (byte) n;
           // System.out.println("given pattern: " + needle[i]);
        }
        for (int i = 0; i < M; i++) {
            int m = Integer.parseInt(in.readLine());
            needle[i] = (byte) m;
          //  System.out.println("given string: " + haystack[i]);

        }
        in.close();

        // YOUR CODE
        S s = new S(N, M, needle, haystack);
        /*
        var matches = s.solve();
        for (var match : matches) System.out.printf("%d ", match);
         */
    }


}

