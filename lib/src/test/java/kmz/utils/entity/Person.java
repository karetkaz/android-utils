package kmz.utils.entity;

/**
 * Created by zoltan on 3/12/17.
 */
public class Person {
 	public static class Address {
 		public String _type;
 		public String street;
 		public String phone;
 	}

    public int id;
    public String name;
    public String _sex;
    public Address home_address[];
    public Address work_address[];
}
