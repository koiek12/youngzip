package compression;

public interface CompressionStrategy {

	/**
	 * Compresses the input data. Return the compressed data.
	 */
	byte[] compress(byte[] data);
}
