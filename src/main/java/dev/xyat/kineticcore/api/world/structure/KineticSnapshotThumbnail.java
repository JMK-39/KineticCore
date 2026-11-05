package dev.xyat.kineticcore.api.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Small pictures of block snapshots for lists: the snapshot seen from the 3D preview's default angle, every visible
 * block face filled with the block's map colour and shaded by its direction. Drawn on the CPU without a game window,
 * so a server can make them and keep them as PNG files ({@link #encodePng(int[], int, int)}).
 */
public final class KineticSnapshotThumbnail {
    /** The picture's angle, the same as the 3D preview's default view. */
    private static final double YAW = Math.toRadians(225.0), PITCH = Math.toRadians(30.0);
    /** Larger snapshots are downsampled to at most this many cells per side first. */
    private static final int MAX_SIDE = 128;
    private static final int SUPERSAMPLE = 2;
    private static final int MISSING_COLOR = 0xD03030;
    /** Face brightness by direction (down, up, north, south, west, east), as blocks are shaded in the overworld. */
    private static final float[] SHADE = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};
    private static final int[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    private KineticSnapshotThumbnail() {
    }

    /**
     * Draws a snapshot into a {@code size} × {@code size} picture, returned as ARGB pixels row by row. The background
     * is transparent; blocks without a map colour (glass, air-like blocks) are left out and blocks unknown to this
     * game are red.
     */
    public static int[] render(KineticBlockSnapshot snapshot, int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be positive");
        int longest = Math.max(snapshot.sizeX(), Math.max(snapshot.sizeY(), snapshot.sizeZ()));
        KineticBlockSnapshot cells = snapshot.downsample((longest + MAX_SIDE - 1) / MAX_SIDE);
        int sx = cells.sizeX(), sy = cells.sizeY(), sz = cells.sizeZ();
        int[] colors = paletteColors(cells);

        // Rotation as in the preview: yaw about Y, then pitch about X. Rows give screen x, screen y and depth.
        double cy = Math.cos(YAW), syaw = Math.sin(YAW), cp = Math.cos(PITCH), sp = Math.sin(PITCH);
        double[][] r = {
                {cy, 0, syaw},
                {sp * syaw, cp, -sp * cy},
                {-cp * syaw, sp, cp * cy}
        };
        int n = size * SUPERSAMPLE;
        double halfX = (Math.abs(r[0][0]) * sx + Math.abs(r[0][1]) * sy + Math.abs(r[0][2]) * sz) / 2.0;
        double halfY = (Math.abs(r[1][0]) * sx + Math.abs(r[1][1]) * sy + Math.abs(r[1][2]) * sz) / 2.0;
        double scale = Math.min(n / (2.0 * Math.max(0.5, halfX)), n / (2.0 * Math.max(0.5, halfY))) * 0.92;
        // Screen offsets of one step along each block axis; screen y grows downwards, depth grows towards the viewer.
        double[][] axis = new double[3][3];
        for (int a = 0; a < 3; a++) {
            axis[a][0] = scale * r[0][a];
            axis[a][1] = -scale * r[1][a];
            axis[a][2] = r[2][a];
        }
        double originX = n / 2.0 - (axis[0][0] * sx + axis[1][0] * sy + axis[2][0] * sz) / 2.0;
        double originY = n / 2.0 - (axis[0][1] * sx + axis[1][1] * sy + axis[2][1] * sz) / 2.0;
        double originZ = -(axis[0][2] * sx + axis[1][2] * sy + axis[2][2] * sz) / 2.0;

        int[] image = new int[n * n];
        double[] depth = new double[n * n];
        Arrays.fill(depth, Double.NEGATIVE_INFINITY);
        for (int face = 0; face < 6; face++) {
            int[] normal = NORMALS[face];
            double facing = r[2][0] * normal[0] + r[2][1] * normal[1] + r[2][2] * normal[2];
            if (facing <= 1.0E-6) continue;
            // The face spans the two block axes other than its normal's.
            int along = normal[0] != 0 ? 0 : normal[1] != 0 ? 1 : 2;
            double[] e1 = axis[along == 0 ? 1 : 0], e2 = axis[along == 2 ? 1 : 2];
            double det = e1[0] * e2[1] - e2[0] * e1[1];
            if (Math.abs(det) < 1.0E-9) continue;
            int positive = normal[0] + normal[1] + normal[2] > 0 ? 1 : 0;
            float shade = SHADE[face];
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    for (int x = 0; x < sx; x++) {
                        int color = colors[cells.index(x, y, z)];
                        if (color == 0) continue;
                        int nx = x + normal[0], ny = y + normal[1], nz = z + normal[2];
                        if (nx >= 0 && ny >= 0 && nz >= 0 && nx < sx && ny < sy && nz < sz && colors[cells.index(nx, ny, nz)] != 0) continue;
                        double cx = x + (along == 0 ? positive : 0), cyy = y + (along == 1 ? positive : 0), cz = z + (along == 2 ? positive : 0);
                        double ox = originX + axis[0][0] * cx + axis[1][0] * cyy + axis[2][0] * cz;
                        double oy = originY + axis[0][1] * cx + axis[1][1] * cyy + axis[2][1] * cz;
                        double oz = originZ + axis[0][2] * cx + axis[1][2] * cyy + axis[2][2] * cz;
                        fill(image, depth, n, ox, oy, oz, e1, e2, det, shade(color, shade));
                    }
                }
            }
        }
        return shrink(image, n, size);
    }

    /** Map colour of each palette entry as opaque RGB, 0 for entries that are not drawn. */
    private static int[] paletteColors(KineticBlockSnapshot snapshot) {
        int[] colors = new int[snapshot.paletteSize()];
        for (int i = 1; i < colors.length; i++) {
            if (snapshot.isMissing(i)) {
                colors[i] = MISSING_COLOR;
                continue;
            }
            BlockState state = snapshot.paletteState(i);
            try {
                colors[i] = state.isAir() ? 0 : state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
            } catch (RuntimeException e) {
                // A block whose colour depends on a real level is drawn in a neutral grey.
                colors[i] = 0x808080;
            }
        }
        return colors;
    }

    private static int shade(int rgb, float shade) {
        int red = Math.round(((rgb >> 16) & 0xFF) * shade);
        int green = Math.round(((rgb >> 8) & 0xFF) * shade);
        int blue = Math.round((rgb & 0xFF) * shade);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    /** Fills the parallelogram {@code origin + u·e1 + v·e2} (u, v in [0, 1]) where it is nearer than what is drawn. */
    private static void fill(int[] image, double[] depth, int n, double ox, double oy, double oz, double[] e1,
                             double[] e2, double det, int argb) {
        double minX = Math.min(Math.min(ox, ox + e1[0]), Math.min(ox + e2[0], ox + e1[0] + e2[0]));
        double maxX = Math.max(Math.max(ox, ox + e1[0]), Math.max(ox + e2[0], ox + e1[0] + e2[0]));
        double minY = Math.min(Math.min(oy, oy + e1[1]), Math.min(oy + e2[1], oy + e1[1] + e2[1]));
        double maxY = Math.max(Math.max(oy, oy + e1[1]), Math.max(oy + e2[1], oy + e1[1] + e2[1]));
        int x0 = Math.max(0, (int) Math.floor(minX)), x1 = Math.min(n - 1, (int) Math.ceil(maxX));
        int y0 = Math.max(0, (int) Math.floor(minY)), y1 = Math.min(n - 1, (int) Math.ceil(maxY));
        // A small overlap closes the hairline gaps between neighbouring faces.
        final double lo = -0.02, hi = 1.02;
        for (int py = y0; py <= y1; py++) {
            double dy = py + 0.5 - oy;
            for (int px = x0; px <= x1; px++) {
                double dx = px + 0.5 - ox;
                double u = (dx * e2[1] - e2[0] * dy) / det;
                if (u < lo || u > hi) continue;
                double v = (e1[0] * dy - dx * e1[1]) / det;
                if (v < lo || v > hi) continue;
                double z = oz + u * e1[2] + v * e2[2];
                int pixel = py * n + px;
                if (z <= depth[pixel]) continue;
                depth[pixel] = z;
                image[pixel] = argb;
            }
        }
    }

    /** Averages each supersampled block of pixels, keeping partly covered edges soft. */
    private static int[] shrink(int[] image, int n, int size) {
        int[] out = new int[size * size];
        int samples = SUPERSAMPLE * SUPERSAMPLE;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int alpha = 0, red = 0, green = 0, blue = 0;
                for (int sy = 0; sy < SUPERSAMPLE; sy++) {
                    for (int sx = 0; sx < SUPERSAMPLE; sx++) {
                        int argb = image[(y * SUPERSAMPLE + sy) * n + x * SUPERSAMPLE + sx];
                        if (argb == 0) continue;
                        alpha++;
                        red += (argb >> 16) & 0xFF;
                        green += (argb >> 8) & 0xFF;
                        blue += argb & 0xFF;
                    }
                }
                if (alpha == 0) continue;
                out[y * size + x] = (alpha * 255 / samples) << 24 | (red / alpha) << 16 | (green / alpha) << 8 | blue / alpha;
            }
        }
        return out;
    }

    /** Encodes ARGB pixels, row by row, as a PNG file with transparency. */
    public static byte[] encodePng(int[] argb, int width, int height) {
        if (width <= 0 || height <= 0 || argb.length < width * height) throw new IllegalArgumentException("bad image size");
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflate = new DeflaterOutputStream(raw, new Deflater(Deflater.BEST_COMPRESSION))) {
                byte[] row = new byte[1 + width * 4];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = argb[y * width + x];
                        row[1 + x * 4] = (byte) (pixel >> 16);
                        row[2 + x * 4] = (byte) (pixel >> 8);
                        row[3 + x * 4] = (byte) pixel;
                        row[4 + x * 4] = (byte) (pixel >>> 24);
                    }
                    deflate.write(row);
                }
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(png);
            out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            DataOutputStream headerOut = new DataOutputStream(header);
            headerOut.writeInt(width);
            headerOut.writeInt(height);
            headerOut.write(new byte[]{8, 6, 0, 0, 0});
            chunk(out, "IHDR", header.toByteArray());
            chunk(out, "IDAT", raw.toByteArray());
            chunk(out, "IEND", new byte[0]);
            return png.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot encode a thumbnail", e);
        }
    }

    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] name = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        out.writeInt(data.length);
        out.write(name);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }
}
