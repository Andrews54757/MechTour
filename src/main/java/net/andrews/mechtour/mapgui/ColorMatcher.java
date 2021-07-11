package net.andrews.mechtour.mapgui;

public class ColorMatcher {
    private static int[][] MAP_COLORS;
    static {

        int[] colors = { 0, 8368696, 16247203, 13092807, 16711680, 10526975, 10987431, 31744, 16777215, 10791096,
                9923917, 7368816, 4210943, 9402184, 16776437, 14188339, 11685080, 6724056, 15066419, 8375321, 15892389,
                5000268, 10066329, 5013401, 8339378, 3361970, 6704179, 6717235, 10040115, 1644825, 16445005, 6085589,
                4882687, 55610, 8476209, 7340544, 13742497, 10441252, 9787244, 7367818, 12223780, 6780213, 10505550,
                3746083, 8874850, 5725276, 8014168, 4996700, 4993571, 5001770, 9321518, 2430480, 12398641, 9715553,
                6035741, 1474182, 3837580, 5647422, 1356933 };

        MAP_COLORS = new int[colors.length * 4][3];

        int index = 0;
        for (int color : colors) {
            for (int shade = 0; shade < 4; shade++) {
                int i = 220;
                if (shade == 3) {
                    i = 135;
                }

                if (shade == 2) {
                    i = 255;
                }

                if (shade == 1) {
                    i = 220;
                }

                if (shade == 0) {
                    i = 180;
                }
                int r = (color >> 16 & 255) * i / 255;
                int g = (color >> 8 & 255) * i / 255;
                int b = (color & 255) * i / 255;

                MAP_COLORS[index++] = new int[] {r,g,b};
            }

        }

    }

    // calculate color difference according to the paper "Measuring perceived color
    // difference
    // using YIQ NTSC transmission color space in mobile applications" by Y.
    // Kotsarenko and F. Ramos
    public static double colorDelta(int r1, int g1, int b1, int a1, int r2, int g2, int b2, int a2, boolean yOnly) {

        if (a1 == a2 && r1 == r2 && g1 == g2 && b1 == b2)
            return 0;

        if (a1 < 255) {
            double a1_2 = (double)a1 / 255;
            r1 = blend(r1, a1_2);
            g1 = blend(g1, a1_2);
            b1 = blend(b1, a1_2);
        }

        if (a2 < 255) {
            double a2_2 = (double)a2 / 255;
            r2 = blend(r2, a2_2);
            g2 = blend(g2, a2_2);
            b2 = blend(b2, a2_2);
        }

        double y = rgb2y(r1, g1, b1) - rgb2y(r2, g2, b2);

        if (yOnly)
            return y; // brightness difference only

        double i = rgb2i(r1, g1, b1) - rgb2i(r2, g2, b2);
        double q = rgb2q(r1, g1, b1) - rgb2q(r2, g2, b2);

        return 0.5053 * y * y + 0.299 * i * i + 0.1957 * q * q;
    }

    private static double rgb2y(int r, int g, int b) {
        return r * 0.29889531 + g * 0.58662247 + b * 0.11448223;
    }

    private static double rgb2i(int r, int g, int b) {
        return r * 0.59597799 - g * 0.27417610 - b * 0.32180189;
    }

    private static double rgb2q(int r, int g, int b) {
        return r * 0.21147017 - g * 0.52261711 + b * 0.31114694;
    }

    // blend semi-transparent color with white
    private static int blend(int c, double a) {
        return (int)(255 + (c - 255) * a);
    }

    public static byte getBestColor(int r, int g, int b, int a) {
        int bestColor = 0;
        double bestColorScore = 0;

        for (int i = 4; i < MAP_COLORS.length; i++) {
            int[] color = MAP_COLORS[i];
            double score = colorDelta(r, g, b, a, color[0], color[1], color[2], 255, false);

            if (i == 4 || score < bestColorScore) {
                bestColor = i;
                bestColorScore = score;
            }
        }

        return (byte)bestColor;
    }


}
