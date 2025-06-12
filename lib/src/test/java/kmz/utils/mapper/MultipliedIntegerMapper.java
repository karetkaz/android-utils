package kmz.utils.mapper;

import kmz.utils.XmlParser;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MultipliedIntegerMapper extends XmlAdapter<String, Integer> implements XmlParser.Mapper<Integer> {

	private static final int multiplier = 2;

	@Override
	public Integer valueOf(String value) {
		return Integer.parseInt(value) * multiplier;
	}

	@Override
	public Integer unmarshal(String value) {
		return Integer.parseInt(value) * multiplier;
	}

	@Override
	public String marshal(Integer value) {
		return String.valueOf(value);
	}
}
