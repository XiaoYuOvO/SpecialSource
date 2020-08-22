package net.md_5.specialsource.util;

public class StringUtil {
    public static String toBase26(int n) {
        StringBuilder s = new StringBuilder();
        while (n > 0){
            n--;
            int m = n % 26;
            s.insert(0, (char) (m + 'a'));
            n = (n - m) / 26;
        }
        return s.toString();
    }
}
