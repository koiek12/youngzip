package CompressionStrategy;

import compression.DeflaterCompressionStrategy;
import decompression.InflaterDecompressionStrategy;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.zip.DataFormatException;

public class CompressionStrategyTest {
	@Test
	public void deflate_and_inflate_string() throws IOException, DataFormatException {
		String src = "xzczxcxzcasdasdascxzczxcdjoiqjdiopaskdpo3dascxzc23#4xzczxcz";
		byte[] compressed = new DeflaterCompressionStrategy().compress(src.getBytes());
		byte[] decompressed = new InflaterDecompressionStrategy().decompress(new PushbackInputStream(new ByteArrayInputStream(compressed)));
		Assert.assertEquals(src, new String(decompressed));
	}
}
