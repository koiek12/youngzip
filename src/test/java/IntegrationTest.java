import org.junit.Assert;
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
	@Test
	public void test_compress_and_decompress_text() throws InterruptedException, NoSuchAlgorithmException, IOException, DataFormatException {
		String inputDirectory = "src/test/resources/text";
		String zipDirectory = "src/test/resources/zip";
		String outputDirectory = "src/test/resources/dest";
		test_compress_and_decompress(inputDirectory, zipDirectory, outputDirectory);
	}

	@Test
	public void test_compress_and_decompress_pdf() throws InterruptedException, NoSuchAlgorithmException, IOException, DataFormatException {
		String inputDirectory = "src/test/resources/pdf";
		String zipDirectory = "src/test/resources/zip";
		String outputDirectory = "src/test/resources/dest";
		test_compress_and_decompress(inputDirectory, zipDirectory, outputDirectory);
	}

	public void test_compress_and_decompress(String inputDirectory, String zipDirectory, String outputDirectory) throws InterruptedException, NoSuchAlgorithmException, IOException, DataFormatException {
		long start = System.currentTimeMillis();
		System.out.println("compressing...");
		YoungZip.compress(inputDirectory, zipDirectory);
		System.out.println("compression time : " + ( System.currentTimeMillis() - start )/1000.0 );

		System.out.println("decompressing...");
		start = System.currentTimeMillis();
		YoungZip.decompress(zipDirectory, outputDirectory);
		System.out.println("decompression time : " + ( System.currentTimeMillis() - start )/1000.0 );

		Path inputDirPath = Paths.get(inputDirectory);
		List<Path> inputFiles = Files.walk(inputDirPath)
			.sorted(Comparator.reverseOrder())
			.collect(Collectors.toList());
		Path outputDirPath = Paths.get(outputDirectory);
		List<Path> outputFiles = Files.walk(outputDirPath)
			.sorted(Comparator.reverseOrder())
			.collect(Collectors.toList());

		Assert.assertEquals(inputFiles.size(), outputFiles.size());
		for(int i=0;i<inputFiles.size();++i) {
			Path input = inputFiles.get(i);
			Path output = outputFiles.get(i);
			if(!Files.isDirectory(input))
				Assert.assertArrayEquals(Files.readAllBytes(input), Files.readAllBytes(output));
		}

		for(int i=0;i<outputFiles.size();i++) {
			if(!Files.isDirectory(outputFiles.get(i)))
				Files.delete(outputFiles.get(i));
		}

		Files.walk(Paths.get(zipDirectory))
			.sorted(Comparator.reverseOrder())
			.filter(path -> !Files.isDirectory(path))
			.forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
	}


}
