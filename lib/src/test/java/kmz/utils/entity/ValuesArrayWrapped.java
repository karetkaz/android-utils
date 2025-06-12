package kmz.utils.entity;

import kmz.utils.XmlParser;
import kmz.utils.mapper.MultipliedIntegerMapper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;

public class ValuesArrayWrapped {

	@XmlElement(name = "value")
	@XmlElementWrapper(name = "values")
	@XmlJavaTypeAdapter(value = MultipliedIntegerMapper.class)
	@XmlParser.Name(name = "value", wrapper = "values")// todo: XmlJavaTypeAdapter is ignored: , mapper = MultipliedIntegerMapper.class)
	int[] values;

	@Override
	public String toString() {
		return "Result{" +
				"values=" + Arrays.toString(values) +
				'}';
	}
}
