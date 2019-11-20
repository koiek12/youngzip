package stream;

import org.junit.Test;

import java.io.IOException;

public class MultipartFileOutputStreamTest {

	@Test(expected = IllegalArgumentException.class)
	public void init_null_file_path() throws IOException {
		new MultipartFileOutputStream(null, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void init_empty_file_path() throws IOException {
		new MultipartFileOutputStream("", 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void init_invalid_part_size_limit() throws IOException {
		new MultipartFileOutputStream("abc.zip", -1);
	}

	@Test(expected = IOException.class)
	public void init_invalid_file_type() throws IOException {
		new MultipartFileOutputStream("abc", 10);
	}
}
