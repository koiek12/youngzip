package stream;

import model.ByteSignature;
import util.RWUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class implements an output stream for distributing data to fixed-size files.
 */
public class MultipartFileOutputStream extends OutputStream {
	private String filePath;
	private Path currentFile;
	private OutputStream currentOutPutStream;
	private int partSizeLimit;
	private int currentPartSize;
	private int writtenByteSize;

	/**
	 * Creates a new output stream with the specified compression partSizeLimit
	 */
	public MultipartFileOutputStream(String filePath, int partSizeLimit) throws IOException {
		if(filePath == null || filePath.isEmpty()) {
			throw new IllegalArgumentException("Invalid file path.");
		}
		if(partSizeLimit <= 0) {
			throw new IllegalArgumentException("Invalid partition size limit." + partSizeLimit);
		}
		if(!filePath.endsWith("zip")) {
			throw new IOException("Invalid file type");
		}
		this.filePath = filePath;
		this.partSizeLimit = partSizeLimit;
		this.currentPartSize = 1;
		this.writtenByteSize = 0;
		openNextPart();
	}

	/**
	 * Write header of multipart File. Header consisit of signature and partNumber.
	 */
	private void writePartHeader() throws IOException {
		RWUtil.write64(currentOutPutStream, ByteSignature.PART.getValue());
		RWUtil.write32(currentOutPutStream, currentPartSize -1);
	}

	/**
	 * Open next part file and write part header.
	 */
	private void openNextPart() throws IOException {
		currentFile = Paths.get(filePath);
		currentOutPutStream = Files.newOutputStream(currentFile);
		writePartHeader();
	}

	private byte[] singleByteBuffer = new byte[1];
	@Override public void write(int b) throws IOException {
		singleByteBuffer[0] = (byte)b;
		write(singleByteBuffer);
	}
	@Override public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	/**
	 * Writes an array of bytes to the current part file.
	 * If byte length plus byte written to current part is bigger than partSizeLimit,
	 * change to new part file and start writing to the new part file.
	 */
	@Override public void write(byte[] b, int offset, int length) throws IOException {
		if(writtenByteSize + length > partSizeLimit) {
			currentOutPutStream.write(b, offset, partSizeLimit - writtenByteSize);
			currentOutPutStream.close();
			Files.move(currentFile, Paths.get(filePath.substring(0,filePath.length()-2) + (currentPartSize -1)));
			currentPartSize++;
			openNextPart();
			currentOutPutStream.write(b, offset + (partSizeLimit - writtenByteSize), length - (partSizeLimit - writtenByteSize));
			writtenByteSize = length - (partSizeLimit - writtenByteSize);
		} else {
			currentOutPutStream.write(b, offset, length);
			writtenByteSize += length;
		}
	}

	@Override public void close() throws IOException {
		currentOutPutStream.close();
	}
}
