package stream;

import compression.CompressionStrategy;
import compression.DeflaterCompressionStrategy;
import model.ByteSignature;
import model.FileEntry;
import util.RWUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class implements an output stream for compressing file parallelly.
 * Thw whole process is composed of three process, chunking, compressing and writing. They
 * are all run in parallelly in different thread. The compression process itself also run in
 * parallel using multi thread to speed up the compression.
 */
public class ParallelCompressOutputStream extends OutputStream {

	private static class CompressedData {
		private long seqNum;
		private byte[] data;

		public CompressedData(long seqNum, byte[] data) {
			this.seqNum = seqNum;
			this.data = data;
		}

		public long getSeqNum() {
			return seqNum;
		}

		public byte[] getData() {
			return data;
		}
	}

	/**
	 * A task that compress the given data and put it into queue.
	 */
	private static class CompressTask implements Callable<Boolean> {
		private long seqNumber;
		private byte[] chunk;
		private CompressionStrategy compressionStrategy;
		private BlockingQueue<CompressedData> compressedDataQueue;

		public CompressTask(long num, byte[] chunk, CompressionStrategy compressionStrategy, BlockingQueue<CompressedData> comprssedDataQueue) {
			this.seqNumber = num;
			this.chunk = chunk;
			this.compressionStrategy = compressionStrategy;
			this.compressedDataQueue = comprssedDataQueue;
		}

		@Override public Boolean call() {
			try {
				compressedDataQueue.put(new CompressedData(seqNumber, compressionStrategy.compress(chunk)));
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}

	/**
	 * A task that write compressed data to output file. Consumes compressed data
	 * from PriorityBlockingQueue in order of sequnce number. This task should be run
	 * only in single thread.
	 */
	private static class writeTask implements Callable<Boolean> {

		private OutputStream out;
		private AtomicBoolean dataSubmitFinished;
		private long seqNumber;
		private PriorityBlockingQueue<CompressedData> compressedDataQueue;

		public writeTask(OutputStream out, AtomicBoolean dataSubmitFinished, PriorityBlockingQueue<CompressedData> compressedDataQueue) {
			this.dataSubmitFinished = dataSubmitFinished;
			this.seqNumber = 0;
			this.out = out;
			this.compressedDataQueue = compressedDataQueue;
		}

		@Override public Boolean call() {
			while(!dataSubmitFinished.get() || !compressedDataQueue.isEmpty()) {
				try {
					CompressedData compressedData = compressedDataQueue.peek();
					if(compressedData != null && compressedData.getSeqNum() == seqNumber) {
						compressedDataQueue.take();
						out.write(compressedData.getData());
						seqNumber++;
					} else {
						Thread.sleep(500);
					}
				} catch (IOException ex) {
					ex.printStackTrace();
					return false;
				} catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}
	}

	private OutputStream out;
	private int chunkSize;
	private CompressionStrategy compressionStrategy;
	private ExecutorService compressTaskExecutor;
	private ExecutorService writeTaskExecutor;
	private AtomicBoolean dataSubmitFinished;
	private PriorityBlockingQueue<CompressedData> compressedDataQueue;
	private long seqNumber;

	private FileEntry currentFile;
	private ByteArrayOutputStream chunkBuffer;

	public ParallelCompressOutputStream(OutputStream out) {
		this(out, 1024*128, new DeflaterCompressionStrategy());
	}

	public ParallelCompressOutputStream(OutputStream out, CompressionStrategy compressionStrategy) {
		this(out, 1024*128, compressionStrategy);
	}

	/**
	 * Creates a new stream. Start writeTask thread.
	 */
	public ParallelCompressOutputStream(OutputStream out, int chunkSize, CompressionStrategy compressionStrategy) {
		if(out == null || compressionStrategy == null) {
			throw new IllegalArgumentException();
		}
		if(chunkSize <= 1024) {
			throw new IllegalArgumentException("chunk size is too small");
		}
		this.out = out;
		this.chunkSize = chunkSize;
		this.compressionStrategy = compressionStrategy;

		this.compressTaskExecutor = new ThreadPoolExecutor(
			4, 4, 30,
			TimeUnit.SECONDS, new ArrayBlockingQueue<>(100),
			new ThreadPoolExecutor.CallerRunsPolicy());
		this.writeTaskExecutor = Executors.newSingleThreadExecutor();
		this.dataSubmitFinished = new AtomicBoolean();
		this.compressedDataQueue = new PriorityBlockingQueue<>(100, Comparator.comparingLong(CompressedData::getSeqNum));
		this.seqNumber = 0;

		this.currentFile = null;
		this.chunkBuffer = new ByteArrayOutputStream(chunkSize + 1024);

		writeTaskExecutor.submit(
			new writeTask(out, dataSubmitFinished, compressedDataQueue)
		);
	}

	/**
	 * Begins writing a new compressed file entry. Write file header information to chunk buffer.
	 */
	public void putNextEntry(FileEntry fileEntry) throws IOException {
		if(fileEntry == null) {
			throw new IllegalArgumentException("fileEntry is empty");
		}
		writeFileHeader(fileEntry);
		currentFile = fileEntry;
	}

	/**
	 * Stops compressing and writing current file. If file is directory or some data still remain
	 * in chunk buffer, compress and write the data. Reset the chunk buffer for additional file.
	 */
	public void closeEntry() {
		if(currentFile == null) {
			throw new IllegalStateException("Put file entry into the stream before close.");
		}
		if(currentFile.getType() == FileEntry.FileType.DIRECTORY) {
			compressTaskExecutor.submit(
				new CompressTask(seqNumber++, chunkBuffer.toByteArray(), compressionStrategy, compressedDataQueue)
			);
		} else if(chunkBuffer.size() > 8) {
			compressTaskExecutor.submit(
				new CompressTask(seqNumber++, chunkBuffer.toByteArray(), compressionStrategy, compressedDataQueue)
			);
		}
		chunkBuffer.reset();
		currentFile = null;
	}

	private void halt() {
		compressTaskExecutor.shutdownNow();
		writeTaskExecutor.shutdownNow();
	}
	/**
	 * Finalize compressing. Compress and write the final trailer and wait for compression and write
	 * thread to terminate. Must be called after writing all content.
	 */
	public void finish() throws InterruptedException, IOException, NoSuchAlgorithmException {
		if(currentFile != null) {
			throw new IllegalStateException("current file entry is not closed.");
		}
		chunkBuffer.reset();
		writeFinalTrailer();
		compressTaskExecutor.submit(
			new CompressTask(seqNumber++, chunkBuffer.toByteArray(), compressionStrategy, compressedDataQueue)
		);
		compressTaskExecutor.shutdown();
		if(!compressTaskExecutor.awaitTermination(1, TimeUnit.HOURS)) {
			halt();
			throw new IOException();
		}
		dataSubmitFinished.set(true);
		writeTaskExecutor.shutdown();
		if(!writeTaskExecutor.awaitTermination(1, TimeUnit.HOURS)) {
			halt();
			throw new IOException();
		}
	}

	public void close() throws IOException {
		out.close();
		chunkBuffer.close();
	}

	@Override public void write(int b) throws IOException {
		write(new byte[]{(byte)b});
	}

	@Override public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte[] b, int offset, int length) throws IOException {
		if(currentFile == null) {
			throw new IllegalStateException("no file entry to write");
		}
		if(currentFile.getType() == FileEntry.FileType.DIRECTORY) {
			throw new IllegalStateException("can't write to directory");
		}

		chunkBuffer.write(b, offset, length);
		if(chunkBuffer.size() >= chunkSize) {
			compressTaskExecutor.submit(
				new CompressTask(seqNumber++, chunkBuffer.toByteArray(), compressionStrategy, compressedDataQueue)
			);
			chunkBuffer.reset();
			writeChunkHeader();
		}
	}

	/*
	 * Write file header to chunk buffer. File header indicates that current
	 * compressed block is beginning of new file.
	 */
	private void writeFileHeader(FileEntry fileEntry) throws IOException {
		byte[] name = fileEntry.getName().getBytes("utf-8");
		RWUtil.write64(chunkBuffer, ByteSignature.FILE.getValue());
		RWUtil.write32(chunkBuffer, name.length);
		chunkBuffer.write(name);
		if(fileEntry.getType() == FileEntry.FileType.DIRECTORY) {
			RWUtil.write16(chunkBuffer, FileEntry.FileType.DIRECTORY.getValue());
		} else {
			RWUtil.write16(chunkBuffer, FileEntry.FileType.FILE.getValue());
		}
		RWUtil.write64(chunkBuffer, fileEntry.getSize());
	}

	/*
	 * Write chunk header to chunk buffer. Chunk header indicates that current
	 * compressed block is part of current file and should be added to current file.
	 */
	private void writeChunkHeader() throws IOException {
		RWUtil.write64(chunkBuffer, ByteSignature.CHUNK.getValue());
	}

	/*
	 * Write file header to chunk buffer. End header indicates end of compressed file.
	 */
	private void writeFinalTrailer() throws IOException, NoSuchAlgorithmException {
		RWUtil.write64(chunkBuffer, ByteSignature.END.getValue());
		RWUtil.writePadding(chunkBuffer, 1024);
	}
}
