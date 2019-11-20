package model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FileEntry {
	private String name;
	private FileType type;
	private long size;


	public FileEntry(String name, FileType type, long size) {
		this.name = name;
		this.type = type;
		this.size = size;
	}

	public String getName() {
		return name;
	}
	public FileType getType() {
		return type;
	}
	public long getSize() {
		return size;
	}

	public enum FileType {
		FILE(0),
		DIRECTORY(1),
		SYMLINK(2);

		private int value;
		FileType(int value) {
			this.value = value;
		}
		public int getValue() {
			return this.value;
		}

		public static FileType fromInteger(int value) {
			for(FileType fileType : FileType.values()) {
				if(fileType.value == value)
					return fileType;
			}
			return null;
		}

		public static FileType fromPath(Path path) {
			if(Files.isSymbolicLink(path)) return FileType.SYMLINK;
			else if(Files.isDirectory(path)) return FileType.DIRECTORY;
			return FileType.FILE;
		}
	}
}
