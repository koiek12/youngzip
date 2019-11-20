package decompression;

import java.io.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class InflaterDecompressionStrategy implements DecompressionStrategy  {

	private ByteArrayOutputStream baos;
	private byte[] inputBuf;
	private byte[] inflated;
	private Inflater inflater;

	public InflaterDecompressionStrategy() {
		baos = new ByteArrayOutputStream();
		inputBuf = new byte[512];
		inflated = new byte[512];
		inflater = new Inflater(true);
	}

	/**
	 * Decompresses a compressed block from inputStream. Return the decompressed block.
	 * If some bytes still remain in inflater, push the data back to inputStream.
	 */
	@Override public byte[] decompress(PushbackInputStream in) throws IOException, DataFormatException {
		inflater.reset();
		baos.reset();
		int n;
		while (!inflater.finished()) {
			n = inflater.inflate(inflated, 0, inflated.length);
			if(n > 0) {
				baos.write(inflated, 0, n);
			} else if (inflater.needsInput()) {
				int len = in.read(inputBuf, 0, inputBuf.length);
				if (len == -1) {
					throw new EOFException("Unexpected end of ZLIB input stream");
				}
				inflater.setInput(inputBuf, 0, len);
			}
		}
		if(inflater.getRemaining() > 0) {
			in.unread(inputBuf, inputBuf.length - inflater.getRemaining(), inflater.getRemaining());
			inflater.reset();
		}
		return baos.toByteArray();
	}
}
