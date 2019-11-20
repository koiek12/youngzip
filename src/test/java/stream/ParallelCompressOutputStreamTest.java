package stream;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;

public class ParallelCompressOutputStreamTest {

	@Test(expected = IllegalArgumentException.class)
	public void init_null_output_stream() throws IOException {
		new ParallelCompressOutputStream(null, 1024, new DeflaterCompressionStrategy());
	}

	@Test(expected = IllegalArgumentException.class)
	public void init_null_strategy() throws IOException {
		new ParallelCompressOutputStream(new ByteArrayOutputStream(), 1024, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void init_invalid_chunk() throws IOException {
		new ParallelCompressOutputStream(new ByteArrayOutputStream(), 23, null);
	}

	@Test(expected = IllegalStateException.class)
	public void write_without_entry() throws IOException {
		new ParallelCompressOutputStream(new ByteArrayOutputStream(), 1025, new DeflaterCompressionStrategy())
			.write(new byte[1], 0, 1);
	}

	@Test(expected = IllegalStateException.class)
	public void write_with_directory() throws IOException {
		ParallelCompressOutputStream pcos =
			new ParallelCompressOutputStream(new ByteArrayOutputStream(), 1025, new DeflaterCompressionStrategy());
		pcos.putNextEntry(new FileEntry("dir", FileEntry.FileType.DIRECTORY, 20));
		pcos.write(new byte[1], 0, 1);
	}

	@Test(expected = IllegalStateException.class)
	public void finish_without_closeEntry() throws IOException, NoSuchAlgorithmException, InterruptedException {
		ParallelCompressOutputStream pcos =
			new ParallelCompressOutputStream(new ByteArrayOutputStream(), 1025, new DeflaterCompressionStrategy());
		pcos.putNextEntry(new FileEntry("dir", FileEntry.FileType.DIRECTORY, 20));
		pcos.finish();
	}

	@Test(expected = IllegalStateException.class)
	public void closeEntry_before_putNextEntry() throws IOException, NoSuchAlgorithmException, InterruptedException {
		ParallelCompressOutputStream pcos =
			new ParallelCompressOutputStream(new ByteArrayOutputStream(), 1025, new DeflaterCompressionStrategy());
		pcos.closeEntry();
	}
	@Test
	public void compress_and_decompress() throws IOException, NoSuchAlgorithmException, DataFormatException, InterruptedException {
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
		FileEntry fileEntry = dis.getNextEntry();
		Assert.assertEquals(fileEntry.getName(), "src");
		Assert.assertEquals(fileEntry.getType(), FileEntry.FileType.FILE);
		Assert.assertEquals(fileEntry.getSize(), 1024*1024*20);

		ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len = -1;
		while((len = dis.read(buf)) != -1) {
			decompressed.write(buf, 0, len);
		}
		dis.closeEntry();
		Assert.assertArrayEquals(src, decompressed.toByteArray());
	}

	@Test
	public void compress_only_directory() throws IOException, NoSuchAlgorithmException, DataFormatException, InterruptedException {
		List<FileEntry> fileEntryList = new ArrayList<>();
		fileEntryList.add(new FileEntry("a", FileEntry.FileType.DIRECTORY, 0));
		fileEntryList.add(new FileEntry("b", FileEntry.FileType.DIRECTORY, 0));
		fileEntryList.add(new FileEntry("c", FileEntry.FileType.DIRECTORY, 0));
		fileEntryList.add(new FileEntry("d", FileEntry.FileType.DIRECTORY, 0));

		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		ParallelCompressOutputStream pcos =
			new ParallelCompressOutputStream(compressed, 1024*128, new DeflaterCompressionStrategy());
		for(FileEntry f : fileEntryList) {
			pcos.putNextEntry(f);
			pcos.closeEntry();
		}
		pcos.finish();
		pcos.close();

		DecompressInputStream dis = new DecompressInputStream(
			new ByteArrayInputStream(compressed.toByteArray()),
			new InflaterDecompressionStrategy()
		);
		for(FileEntry fileEntry : fileEntryList) {
			FileEntry decompressedFileEntry = dis.getNextEntry();
			Assert.assertEquals(fileEntry.getName(), decompressedFileEntry.getName());
			Assert.assertEquals(fileEntry.getType(), decompressedFileEntry.getType());
			Assert.assertEquals(fileEntry.getSize(), decompressedFileEntry.getSize());
			dis.closeEntry();
		}
	}
}
