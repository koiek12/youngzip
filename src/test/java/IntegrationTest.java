import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

public class IntegrationTest {

	@Before
	public void clean() throws IOException {
		Path rootPath = Paths.get("../comptest/zip");
		List<Path> pathsToDelete = Files.walk(rootPath)
			.sorted(Comparator.reverseOrder())
			.collect(Collectors.toList());
		for(Path path : pathsToDelete) {
			if(!path.equals(rootPath))
				Files.deleteIfExists(path);
		}
		rootPath = Paths.get("../comptest/dest");
		pathsToDelete = Files.walk(rootPath)
			.sorted(Comparator.reverseOrder())
			.collect(Collectors.toList());
		for(Path path : pathsToDelete) {
			if(!path.equals(rootPath))
				Files.deleteIfExists(path);
		}
	}

	@Test
	public void test_compress_and_decompress_text() throws InterruptedException, NoSuchAlgorithmException, IOException, DataFormatException {
		String inputDirectory = "src/test/resources/text";
		String zipDirectory = "../comptest/zip";
		String outputDirectory = "../comptest/dest";
		long start = System.currentTimeMillis();
		System.out.println("compressing...");
		YoungZip.compress(inputDirectory, zipDirectory);
		System.out.println("compression time : " + ( System.currentTimeMillis() - start )/1000.0 );

		System.out.println("decompressing...");
		start = System.currentTimeMillis();
		YoungZip.decompress(zipDirectory, outputDirectory);
		System.out.println("decompression time : " + ( System.currentTimeMillis() - start )/1000.0 );
	}

	@Test
	public void test_compress_and_decompress1() throws InterruptedException, NoSuchAlgorithmException, IOException, DataFormatException {
		String inputDirectory = "../linguist2";
		String zipDirectory = "../comptest/zip";
		String outputDirectory = "../comptest/dest";
		long start = System.currentTimeMillis();
		System.out.println("compressing...");
		YoungZip.compress(inputDirectory, zipDirectory);
		System.out.println("compression time : " + ( System.currentTimeMillis() - start )/1000.0 );

		System.out.println("decompressing...");
		start = System.currentTimeMillis();
		YoungZip.decompress(zipDirectory, outputDirectory);
		System.out.println("decompression time : " + ( System.currentTimeMillis() - start )/1000.0 );
	}

	@Test
	public void test_compress_and_decompress2() throws InterruptedException, NoSuchAlgorithmException, IOException, DataFormatException {
		String inputDirectory = "../statvideo";
		String zipDirectory = "../comptest/zip";
		String outputDirectory = "../comptest/dest";
		long start = System.currentTimeMillis();
		System.out.println("compressing...");
		YoungZip.compress(inputDirectory, zipDirectory);
		System.out.println("compression time : " + ( System.currentTimeMillis() - start )/1000.0 );

		System.out.println("decompressing...");
		start = System.currentTimeMillis();
		YoungZip.decompress(zipDirectory, outputDirectory);
		System.out.println("decompression time : " + ( System.currentTimeMillis() - start )/1000.0 );

	}

	@Test
	public void test_compress_and_decompress3() throws InterruptedException, NoSuchAlgorithmException, IOException, DataFormatException {
		String inputDirectory = "../edtest";
		String zipDirectory = "../comptest/zip";
		String outputDirectory = "../comptest/dest";
		long start = System.currentTimeMillis();
		System.out.println("compressing...");
		YoungZip.compress(inputDirectory, zipDirectory);
		System.out.println("compression time : " + ( System.currentTimeMillis() - start )/1000.0 );

		System.out.println("decompressing...");
		start = System.currentTimeMillis();
		YoungZip.decompress(zipDirectory, outputDirectory);
		System.out.println("decompression time : " + ( System.currentTimeMillis() - start )/1000.0 );

	}

	@Test
	public void test_compress_and_decompress4() throws InterruptedException, NoSuchAlgorithmException, IOException, DataFormatException {
		String inputDirectory = "../stat";
		String zipDirectory = "../comptest/zip";
		String outputDirectory = "../comptest/dest";
		long start = System.currentTimeMillis();
		System.out.println("compressing...");
		YoungZip.compress(inputDirectory, zipDirectory);
		System.out.println("compression time : " + ( System.currentTimeMillis() - start )/1000.0 );

		System.out.println("decompressing...");
		start = System.currentTimeMillis();
		YoungZip.decompress(zipDirectory, outputDirectory);
		System.out.println("decompression time : " + ( System.currentTimeMillis() - start )/1000.0 );
	}

}
