package kmz.utils.mapper;

import kmz.utils.XmlParser;

public class SafeStringMapper implements XmlParser.Mapper<String> {
	@Override
	public String valueOf(String value) {
		if (value == null) {
			return "";
		}
		return value;
	}
}
