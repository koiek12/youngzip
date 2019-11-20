package compression;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

public class DeflaterCompressionStrategy implements CompressionStrategy {

	/*
	 * ThreadLocal to avoid reallocating memory of deflater and buffer, resulting in increased performance.
	 */
	private static final ThreadLocal<Deflater> threadLocalDeflator = ThreadLocal.withInitial(() -> new Deflater(5, true));
	private static final ThreadLocal<byte[]> threadLocalDeflateBuffer = ThreadLocal.withInitial(() -> new byte[1024]);
	private static final ThreadLocal<ByteArrayOutputStream> threadLocalByteArrayOutputStream = ThreadLocal.withInitial(() -> new ByteArrayOutputStream(1024*9));

	/**
	 * Compresses the input data. Return the compressed data. Reuse buffers and deflater for performance.
	 */
	@Override public byte[] compress(byte[] data) {
		if(data.length == 0) return new byte[0];

		byte[] deflated = threadLocalDeflateBuffer.get();
		Deflater deflater = threadLocalDeflator.get();
		deflater.reset();
		ByteArrayOutputStream baos = threadLocalByteArrayOutputStream.get();
		baos.reset();

		deflater.setInput(data);
		deflater.finish();
		int len = -1;
		while(!deflater.finished()) {
			len = deflater.deflate(deflated, 0, deflated.length, Deflater.SYNC_FLUSH);
			if(len > 0) baos.write(deflated, 0, len);
		}
		return baos.toByteArray();
	}
}
