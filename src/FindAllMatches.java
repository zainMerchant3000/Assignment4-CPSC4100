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
            var fr = new int[101];
            var rank = new byte[101];
            compress(needle, fr, rank);
            Arrays.fill(fr, 0);
            Arrays.fill(rank, (byte) 0);
            compress(haystack, fr, rank);
            int h = 0;
            for (var b : needle) h = (h * RAD + b) % MOD;
            hash = h;
        }

        private void compress(byte[] xs, int[] fr, byte[] rank) {
            byte x = 0;
            for (var n : xs) fr[n]++;
            for (int i = 1; i < 101; i++) {
                rank[i] = x;
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
        if (args.length != 1) {
            System.out.println("Usage: java FindAllMatches.java <test_file_name>");
            return;
        }

        BufferedReader in = new BufferedReader(new FileReader(args[0]));
        String[] head = in.readLine().split(" ");
        int N = Integer.parseInt(head[0]);
        int M = Integer.parseInt(head[1]);

        byte[] needle = new byte[M];
        byte[] haystack = new byte[M];
        for (int i = 0; i < N; i++) {
            int n = Integer.parseInt(in.readLine());
            needle[i] = (byte) n;
        }
        for (int i = 0; i < M; i++) {
            int m = Integer.parseInt(in.readLine());
            haystack[i] = (byte) m;
        }
        in.close();

        // YOUR CODE
        S s = new S(N, M, needle, haystack);
        var matches = s.solve();
        for (var match : matches) System.out.printf("%d ", match);
    }
}

