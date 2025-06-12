package kmz.utils.entity;

import kmz.utils.XmlParser;
import kmz.utils.mapper.DateTimeMapper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

/**
 * Created by zoltan on 3/12/17.
 */
public class Person {
	public static class Address {
		public String type;
		public String street;
		public String phone;
	}

	public int id;
	public String name;
	public String sex;

	@XmlElement(name = "birth_date")
	@XmlJavaTypeAdapter(value = DateTimeMapper.class)
	@XmlParser.Name(name = "birth_date", mapper = DateTimeMapper.class)
	public Date birthDate;

	public Address[] home_address;
	public Address[] work_address;
}
