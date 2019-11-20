package util;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RWUtil {
	/**
	 * Fetches unsigned 16-bit value from byte array at specified offset.
	 * The bytes are assumed to be in Intel (little-endian) byte order.
	 */
	public static final int get16(byte b[], int off) {
		return Byte.toUnsignedInt(b[off]) | (Byte.toUnsignedInt(b[off+1]) << 8);
	}

	/**
	 * Fetches unsigned 32-bit value from byte array at specified offset.
	 * The bytes are assumed to be in Intel (little-endian) byte order.
	 */
	public static final long get32(byte b[], int off) {
		return (get16(b, off) | ((long)get16(b, off+2) << 16)) & 0xffffffffL;
	}

	/**
	 * Fetches signed 64-bit value from byte array at specified offset.
	 * The bytes are assumed to be in Intel (little-endian) byte order.
	 */
	public static final long get64(byte b[], int off) {
		return get32(b, off) | (get32(b, off+4) << 32);
	}

	/**
	 * Writes a 8-bit byte to the output stream.
	 */
	public static void write8(OutputStream out, int v) throws IOException {
		out.write(v & 0xff);
	}

	/**
	 * Writes a 16-bit short to the output stream in little-endian byte order.
	 */
	public static void write16(OutputStream out, int v) throws IOException {
		out.write((v >>> 0) & 0xff);
		out.write((v >>> 8) & 0xff);
	}

	/**
	 * Writes a 32-bit int to the output stream in little-endian byte order.
	 */
	public static void write32(OutputStream out, long v) throws IOException {
		out.write((int)((v >>>  0) & 0xff));
		out.write((int)((v >>>  8) & 0xff));
		out.write((int)((v >>> 16) & 0xff));
		out.write((int)((v >>> 24) & 0xff));
	}

	/**
	 * Writes a 64-bit int to the output stream in little-endian byte order.
	 */
	public static void write64(OutputStream out, long v) throws IOException {
		out.write((int)((v >>>  0) & 0xff));
		out.write((int)((v >>>  8) & 0xff));
		out.write((int)((v >>> 16) & 0xff));
		out.write((int)((v >>> 24) & 0xff));
		out.write((int)((v >>> 32) & 0xff));
		out.write((int)((v >>> 40) & 0xff));
		out.write((int)((v >>> 48) & 0xff));
		out.write((int)((v >>> 56) & 0xff));
	}

	public static void writePadding(OutputStream out, int size) throws NoSuchAlgorithmException, IOException {
		byte[] bytes = new byte[size];
		SecureRandom.getInstanceStrong().nextBytes(bytes);
		out.write(bytes);
	}
}
