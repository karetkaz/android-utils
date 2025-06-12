package kmz.utils.entity;

import kmz.utils.XmlParser;
import kmz.utils.mapper.SafeStringMapper;

import java.util.List;

public class JsonTest {
	@XmlParser.Name(name = "fred", mapper = SafeStringMapper.class)
	public Object fred;
	public jim jim;
	public String bert;
	public harry harry;
	public array array;
	public object object;

	public static class jim {
		public String attribute;
	}

	public static class harry {
		@XmlParser.Name(name = "alf", mapper = SafeStringMapper.class)
		public Object alf;
	}

	public static class array {
		public List<Double> x;
	}

	public static class strings {
		public List<String> s;
	}

	public static class object {
		public String faith;
		public String hope;
		public String charity;
		public String badfaith;
		public strings strings;
		public subobjects subobjects;
		public badarray badarray;
	}

	public static class subobjects {
		public List<object> object;
	}

	public static class badarray {
		public List<object> object;
	}
}
