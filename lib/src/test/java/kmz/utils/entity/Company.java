package kmz.utils.entity;

import kmz.utils.XmlParser;

import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

public class Company {
	@XmlElementWrapper(name = "employees")
	@XmlParser.Name(name = "employee", wrapper = "employees")
	public List<Employee> employees;

	public static class Employee {
		public int id;
		public String name;
		public String position;
		public String department;
	}
}
