import compression.CompressionStrategy;
import compression.DeflaterCompressionStrategy;
import decompression.DecompressionStrategy;
import decompression.InflaterDecompressionStrategy;
import model.FileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.*;

import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

public class YoungZip {

	private static Logger logger = LoggerFactory.getLogger(YoungZip.class);

	public static void compress(String inputDirectory, String outputDirectory) throws IOException, InterruptedException, NoSuchAlgorithmException {
		compress(inputDirectory, outputDirectory, 100*1024*1024, new DeflaterCompressionStrategy());
	}

	public static void compress(String inputDirectory, String outputDirectory, int partSizeLimit, CompressionStrategy compressionStrategy) throws IOException, InterruptedException, NoSuchAlgorithmException {
		Path inputDirPath = Paths.get(inputDirectory);
		Path outputDirPath = Paths.get(outputDirectory);
		if(!(Files.exists(inputDirPath))) {
			logger.error("input directory dose not exist.");
			throw new NoSuchFileException(inputDirectory);
		}
		if (!(Files.exists(outputDirPath))) {
			Files.createDirectories(Paths.get(outputDirectory));
		}
		List<Path> fileList = Files.walk(inputDirPath)
			.filter(path -> !path.toString().equals(inputDirectory))
			.collect(Collectors.toList());

		String zipFile = outputDirPath.toString() + "/" + inputDirPath.getFileName().toString() + ".zip";
		MultipartFileOutputStream mfos = new MultipartFileOutputStream(zipFile, partSizeLimit);
		ParallelCompressOutputStream afos = new ParallelCompressOutputStream(mfos, compressionStrategy);
		byte[] buf = new byte[1024];
		for (Path path : fileList) {
			try {
				String name = inputDirPath.relativize(path).toString();
				FileEntry fileEntry = new FileEntry(name, FileEntry.FileType.fromPath(path), Files.size(path));
				afos.putNextEntry(fileEntry);
				if(fileEntry.getType() == FileEntry.FileType.FILE) {
					InputStream fis = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
					int len = -1;
					while ((len = fis.read(buf)) != -1) {
						afos.write(buf, 0, len);
					}
				}
				afos.closeEntry();
			} catch (NoSuchFileException e) {
				logger.warn(e.getMessage(), e);
				continue;
			}
		}
		afos.finish();
		afos.close();
	}

	public static void decompress(String inputDirectory, String outputDirectory) throws IOException, DataFormatException {
		decompress(inputDirectory, outputDirectory, new InflaterDecompressionStrategy());
	}

	public static void decompress(String inputDirectory, String outputDirectory, DecompressionStrategy decompressionStrategy) throws IOException, DataFormatException {
		Path inputDirPath = Paths.get(inputDirectory);
		Path outputDirPath = Paths.get(outputDirectory);
		if (!(Files.exists(inputDirPath))) {
			logger.error("input directory dose not exist.");
			throw new NoSuchFileException(inputDirectory);
		}
		if (!(Files.exists(outputDirPath))) {
			Files.createDirectories(Paths.get(outputDirectory));
		}
		List<Path> zipFiles = Files.walk(inputDirPath)
			.filter(path -> path.toString().endsWith("zip") && !Files.isDirectory(path))
			.collect(Collectors.toList());
		if(zipFiles.isEmpty()) {
			logger.error("Zip file not found in the directory.");
			throw new FileNotFoundException("Zip file not found in the directory");
		}

		String zipFile = zipFiles.get(0).toString();
		MultipartFileInputStream mfis = new MultipartFileInputStream(zipFile);
		DecompressInputStream afis = new DecompressInputStream(mfis, decompressionStrategy);
		byte[] buffer = new byte[1024];
		FileEntry entry;
		while((entry = afis.getNextEntry())!= null) {
			Path filePath = Paths.get(outputDirectory, entry.getName());
			if(entry.getType() == FileEntry.FileType.DIRECTORY) {
				Files.createDirectories(filePath);
			} else if(entry.getType() == FileEntry.FileType.FILE) {
				Files.createDirectories(filePath.getParent());
				OutputStream fos = new BufferedOutputStream(Files.newOutputStream(filePath));
				int length = -1;
				while ((length = afis.read(buffer)) != -1) {
					fos.write(buffer, 0, length);
				}
				fos.close();
			}
			afis.closeEntry();
		}
		afis.close();
	}

	public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException, DataFormatException {
		String inputDirectory = args[0];
		String outputDirectory = args[1];
		if(args.length >= 3) {
			long originalSize = Files.walk(Paths.get(inputDirectory)).mapToLong( p -> p.toFile().length() ).sum();
			long start = System.currentTimeMillis();
			logger.info("compressing...");
			int compressedSizeLimit = Integer.valueOf(args[2]);
			compress(inputDirectory, outputDirectory, compressedSizeLimit*1024*1024, new DeflaterCompressionStrategy());
			long compressedSize = Files.walk(Paths.get(outputDirectory)).mapToLong( p -> p.toFile().length() ).sum();
			logger.info("compression completed! elapsed time : " + ( System.currentTimeMillis() - start )/1000.0 );
			logger.info("original size : " + originalSize + " => " + " compressed size : " + compressedSize);
		} else if(args.length >= 2) {
			logger.info("decompressing...");
			long start = System.currentTimeMillis();
			decompress(inputDirectory, outputDirectory, new InflaterDecompressionStrategy());
			logger.info("decompression completed! elapsed time : " + ( System.currentTimeMillis() - start )/1000.0 );
		}
	}
}
