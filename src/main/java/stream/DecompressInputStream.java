package stream;

import decompression.DecompressionStrategy;
import decompression.InflaterDecompressionStrategy;
import model.ByteSignature;
import model.FileEntry;
import util.RWUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.DataFormatException;

/**
 * This class implements an input stream for decompressing file generated by ParallelCompressOutputStream.
 */
public class DecompressInputStream extends InputStream {

	private PushbackInputStream in;
	private DecompressionStrategy decompressionStrategy;
	private boolean done;
	private FileEntry processingFile;
	private FileEntry currentFile;
	private byte[] currentBlock;
	private int currentRead;

	public DecompressInputStream(InputStream in) throws IOException, DataFormatException {
		this(in, new InflaterDecompressionStrategy());
	}
	/**
	 * Creates a new stream. Decompress the first block and check its signature.
	 */
	public DecompressInputStream(InputStream in, DecompressionStrategy decompressionStrategy) throws IOException, DataFormatException {
		if(in == null) {
			throw new IllegalArgumentException("Inputstream is null.");
		}
		if(decompressionStrategy == null) {
			throw new IllegalArgumentException("DecompressionStrategy is null.");
		}
		this.in = new PushbackInputStream(in, 1024*12);
		this.decompressionStrategy = decompressionStrategy;
		this.processingFile = null;
		this.currentFile = null;
		this.done = false;

		this.currentRead = 0;
		this.currentBlock = decompressionStrategy.decompress(this.in);
		ByteSignature byteSignature = readHeader();
		if(byteSignature != ByteSignature.FILE)
			throw new IOException("invalid file format");
	}

	/**
	 * Get next file to process.
	 */
	public FileEntry getNextEntry() {
		if(done) {
			return null;
		}
		return processingFile = currentFile;
	}

	public void closeEntry() throws IOException {
		if(currentFile == null) {
			throw new IllegalStateException("Put file entry into the stream before close.");
		}
		if(done) {
			return;
		}
		read();
		processingFile = null;
	}

	@Override public int read() throws IOException {
		return read(new byte[1]);
	}

	@Override public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * Reads from the current block into an array of bytes.
	 * if remaining byte in current block is bigger than length,
	 * read next block. If next block starts with FILE signature,
	 * stop processing current file and calling this function afterward
	 * will return -1 until you set next file entry by getNextEntry function.
	 */
	@Override public int read(byte[] b, int off, int length) throws IOException {
		if(done || processingFile == null)
			return -1;

		int returnLen = 0;
		int remain = currentBlock.length - currentRead;
		if(remain < length) {
			System.arraycopy(currentBlock, currentRead, b, off, remain);
			currentRead += remain;
			returnLen += remain;
			try {
				currentRead = 0;
				currentBlock = decompressionStrategy.decompress(this.in);
				ByteSignature byteSignature = readHeader();
				if(byteSignature == ByteSignature.FILE) {
					processingFile = null;
				} else if(byteSignature == ByteSignature.END) {
					done = true;
					processingFile = null;
				} else if(byteSignature != ByteSignature.CHUNK){
					throw new IOException("invalid file format");
				}
			} catch (DataFormatException e) {
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}
		} else {
			System.arraycopy(currentBlock, currentRead, b, off, length);
			currentRead += length;
			returnLen += length;
		}
		return returnLen;
	}

	public void close() throws IOException {
		in.close();
	}

	/*
	 * Read header information from currentBlock. Return header type.
	 * If header type is FILE, assign current FileEntry with header information.
	 */
	private ByteSignature readHeader() {
		long signature = RWUtil.get64(currentBlock, 0);
		if(signature == ByteSignature.END.getValue()) {
			currentRead += 8;
			return ByteSignature.END;
		} else if(signature == ByteSignature.CHUNK.getValue()) {
			currentRead += 8;
			return ByteSignature.CHUNK;
		}
		int nameLen = (int)RWUtil.get32(currentBlock, 8);
		currentFile = new FileEntry(
			new String(currentBlock, 12, nameLen),
			FileEntry.FileType.fromInteger(RWUtil.get16(currentBlock, nameLen+12)),
			RWUtil.get64(currentBlock, nameLen+14)
		);
		currentRead += 8 + 4 + nameLen + 2 + 8;
		return ByteSignature.FILE;
	}
}