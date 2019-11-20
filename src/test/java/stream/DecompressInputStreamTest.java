package stream;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import compression.DeflaterCompressionStrategy;
import decompression.InflaterDecompressionStrategy;
import model.FileEntry;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.zip.DataFormatException;

public class DecompressInputStreamTest {

	@Test(expected = IllegalArgumentException.class)
	public void init_null_inputstream() throws IOException, DataFormatException {
		new DecompressInputStream(
			null,
			new InflaterDecompressionStrategy()
		);
	}

	@Test(expected = IllegalArgumentException.class)
	public void init_null_decomp_strategy() throws IOException, DataFormatException {
		new DecompressInputStream(
			new ByteInputStream(),
			null
		);
	}

	@Test
	public void read_before_getEntry() throws IOException, DataFormatException, NoSuchAlgorithmException, InterruptedException {
		byte[] src = new byte[1024*1024*20];
		SecureRandom.getInstanceStrong().nextBytes(src);

		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		ParallelCompressOutputStream pcos =
			new ParallelCompressOutputStream(compressed, 1024*128, new DeflaterCompressionStrategy());
		pcos.putNextEntry(new FileEntry("src", FileEntry.FileType.FILE, 1024*1024*20));
		for(int i=0;i<1024*20;i++) {
			pcos.write(src, i*1024, 1024);
		}
		pcos.closeEntry();
		pcos.finish();
		pcos.close();

		DecompressInputStream dis = new DecompressInputStream(
			new ByteArrayInputStream(compressed.toByteArray()),
			new InflaterDecompressionStrategy()
		);
		int len = dis.read();
		Assert.assertEquals(len, -1);
	}
}
