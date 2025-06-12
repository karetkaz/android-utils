package kmz.utils.entity;

import kmz.utils.XmlParser;
import kmz.utils.mapper.MultipliedIntegerMapper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;

public class ValuesArray {

	@XmlElement(name = "value")
	@XmlJavaTypeAdapter(value = MultipliedIntegerMapper.class)
	@XmlParser.Name(name = "value")// todo: XmlJavaTypeAdapter is ignored: , mapper = MultipliedIntegerMapper.class)
	int[] values;

	@Override
	public String toString() {
		return "Result{" +
				"values=" + Arrays.toString(values) +
				'}';
	}
}
