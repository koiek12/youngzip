package decompression;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.zip.DataFormatException;

public interface DecompressionStrategy {

	/**
	 * Decompresses a compressed block from inputStream. Return the decompressed block.
	 */
	byte[] decompress(PushbackInputStream inputStream) throws IOException, DataFormatException;
}
