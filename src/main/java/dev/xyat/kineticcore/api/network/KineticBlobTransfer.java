package dev.xyat.kineticcore.api.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sends payloads larger than one custom payload packet (1 MiB by default) as numbered parts and puts them back
 * together on the receiving side. The sender gives every transfer an id and sends each part with its index and the
 * part count; the receiver feeds the parts to an {@link Assembler}, which also reports how much has arrived so a
 * screen can show a percentage.
 */
public final class KineticBlobTransfer {
    /** Part size that leaves room for the rest of a packet under the default payload limit. */
    public static final int DEFAULT_PART_BYTES = 512 * 1024;

    private KineticBlobTransfer() {
    }

    /**
     * Splits data into parts of at most {@code partBytes}; empty data gives one empty part.
     *
     * @throws IllegalArgumentException if {@code partBytes} is below 1
     */
    public static List<byte[]> split(byte[] data, int partBytes) {
        Objects.requireNonNull(data, "data");
        if (partBytes < 1) throw new IllegalArgumentException("partBytes must be positive");
        List<byte[]> parts = new ArrayList<>(Math.max(1, (data.length + partBytes - 1) / partBytes));
        if (data.length == 0) parts.add(new byte[0]);
        for (int start = 0; start < data.length; start += partBytes) {
            int length = Math.min(partBytes, data.length - start);
            byte[] part = new byte[length];
            System.arraycopy(data, start, part, 0, length);
            parts.add(part);
        }
        return parts;
    }

    /**
     * Collects the parts of several transfers. Transfers that stop arriving are dropped after a timeout, and a
     * transfer that would exceed the size limit is rejected. Thread-safe.
     */
    public static final class Assembler {
        private final int maxTotalBytes;
        private final long timeoutMillis;
        private final Map<Long, Pending> pending = new HashMap<>();

        /**
         * @param maxTotalBytes largest payload one transfer may add up to
         * @param timeoutMillis how long a transfer may go without a new part before it is dropped
         */
        public Assembler(int maxTotalBytes, long timeoutMillis) {
            if (maxTotalBytes < 1) throw new IllegalArgumentException("maxTotalBytes must be positive");
            this.maxTotalBytes = maxTotalBytes;
            this.timeoutMillis = Math.max(1000L, timeoutMillis);
        }

        /**
         * Adds one part. Returns the whole payload once every part of the transfer has arrived, otherwise
         * {@code null}. Parts with a bad index or count, and transfers over the size limit, are discarded.
         */
        public synchronized byte[] accept(long transferId, int index, int total, byte[] part) {
            expire();
            if (part == null || total < 1 || total > 1 << 16 || index < 0 || index >= total) return null;
            Pending transfer = pending.computeIfAbsent(transferId, id -> new Pending(total));
            if (transfer.parts.length != total) {
                pending.remove(transferId);
                return null;
            }
            if (transfer.parts[index] == null) {
                transfer.bytes += part.length;
                transfer.received++;
            }
            transfer.parts[index] = part;
            transfer.touched = System.currentTimeMillis();
            if (transfer.bytes > maxTotalBytes) {
                pending.remove(transferId);
                return null;
            }
            if (transfer.received < total) return null;
            pending.remove(transferId);
            byte[] whole = new byte[(int) transfer.bytes];
            int at = 0;
            for (byte[] piece : transfer.parts) {
                System.arraycopy(piece, 0, whole, at, piece.length);
                at += piece.length;
            }
            return whole;
        }

        /** Share of the transfer's parts that have arrived, from 0 to 1; 0 for an unknown transfer. */
        public synchronized float progress(long transferId) {
            Pending transfer = pending.get(transferId);
            return transfer == null ? 0F : transfer.received / (float) transfer.parts.length;
        }

        /** Forgets a transfer, for example when the screen that waited for it closes. */
        public synchronized void cancel(long transferId) {
            pending.remove(transferId);
        }

        private void expire() {
            long now = System.currentTimeMillis();
            for (Iterator<Pending> it = pending.values().iterator(); it.hasNext(); ) {
                if (now - it.next().touched > timeoutMillis) it.remove();
            }
        }

        private static final class Pending {
            final byte[][] parts;
            int received;
            long bytes;
            long touched = System.currentTimeMillis();

            Pending(int total) {
                parts = new byte[total][];
            }
        }
    }
}
