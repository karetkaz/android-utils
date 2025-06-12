package kmz.utils.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import kmz.utils.XmlParser;

import javax.xml.bind.annotation.XmlValue;
import java.util.List;

public class FileSystem {
	public Directory directory;

	public static class Directory {
		public String name;
		public List<Directory> directory;
		public List<File> file;
	}

	public static class File {
		public String name;
		public String type;

		@XmlValue
		@JacksonXmlCData
		@XmlParser.Name(name = XmlParser.VALUE)
		public String text;
	}
}
