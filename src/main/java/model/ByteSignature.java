package model;

public enum ByteSignature {
	FILE(  0x020124b50L),
	CHUNK( 0x080742b50L),
	END(   0x108084b50L),
	PART(  0x132132123L);

	private long value;

	ByteSignature(long value) {
		this.value = value;
	}

	public long getValue() {
		return this.value;
	}
}
