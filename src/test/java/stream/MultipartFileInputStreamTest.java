package stream;

import org.junit.Test;

import java.io.IOException;

public class MultipartFileInputStreamTest {

	@Test(expected = IllegalArgumentException.class)
	public void init_null_file_path() throws IOException {
		new MultipartFileInputStream(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void init_empty_file_path() throws IOException {
		new MultipartFileInputStream("");
	}

	@Test(expected = IOException.class)
	public void init_invalid_file_type() throws IOException {
		new MultipartFileInputStream("abc");
	}
}
