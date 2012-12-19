import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

/** Deserialize an object from an xml file.
 *
 *
 */
public class XmlDeserializer {

	private static final String TAG_XML_DESERIALIZER = "xml.obj.deserializer";

	// converts a list to an array of the given type using reflection.
	private static Object toArray(ArrayList<?> list, Class<?> type) {
		int length = list.size();
		Object result = Array.newInstance(type, length);
		for (int i = 0; i < length; ++i) {
			Array.set(result, i, list.get(i));
		}
		return result;
	}

	// converts a string to the given type using reflection.
	private static Object toObject(String value, Class<?> type) {
		// basic types and primitives ...
		Object result = null;
		if (type == Boolean.class || type == boolean.class) {
			result = Boolean.valueOf(value);
		}
		else if (type == Byte.class || type == byte.class) {
			result = Byte.valueOf(value);
		}
		else if (type == Short.class || type == short.class) {
			result = Short.valueOf(value);
		}
		else if (type == Integer.class || type == int.class) {
			result = Integer.valueOf(value);
		}
		else if (type == Long.class || type == long.class) {
			result = Long.valueOf(value);
		}
		else if (type == Float.class || type == float.class) {
			result = Float.valueOf(value);
		}
		else if (type == Double.class || type == double.class) {
			result = Double.valueOf(value);
		}
		else if (type == Character.class || type == char.class) {
			switch (value.length()) {
				case 0:
					result = Character.valueOf('\0');
					break;
				case 1:
					result = Character.valueOf(value.charAt(0));
					break;
				default:
					result = Character.valueOf(value.charAt(0));
					break;
			}
		}
		else if (type == Date.class) {
			try
			{
				result = Date.parse(value);
			} catch (Exception e)
			{
				e = null;
			}
		}
		else if (type == String.class) {
			result = value;
		}
		return result;
	}

	// consumes the current tag of form `<tag>text</tag>` and returns the contained text.
	private static String readStringTag(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.next(); // skip the begin tag
		parser.require(XmlPullParser.TEXT, null, null);
		String result = parser.getText();
		parser.next(); // skip the end tag
		parser.require(XmlPullParser.END_TAG, null, null);
		return result;
	}

	/** Deserializes an xml.
	 *
	 * @param parser the parser
	 * @param cls the type of the resulting object.
	 * @param attributePrefix prefix of the class members in witch attributes are deserialized. Using empty string is allowed. Passing null disables attribute parsing.
	 * @return an instance of cls type.
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 *
	 * usage:
	 * 	<person sex="other">
	 * 		<id>1234</id>
	 * 		<name>Joe Doe</name>
	 * 		<home_address type="home">
	 * 			<street>101 Sweet Home</street>
	 * 			<phone>333-3333</phone>
	 * 		</home_address>
	 * 		<home_address type="home2">
	 * 			<street>101 Sweet Home</street>
	 * 			<phone>333-3333</phone>
	 * 		</home_address>
	 * 		<work_address>
	 * 			<street>303 Office Street</street>
	 * 			<phone>444-4444</phone>
	 * 		</work_address>
	 * 	</person>
	 *
	 * classes:
	 * 	public static class Address {
	 * 		public String _type;
	 * 		public String street;
	 * 		public String phone;
	 * 	}
	 *
	 * 	public static class Person {
	 * 		public int id;
	 * 		public String name;
	 * 		public String _sex;
	 * 		public Address home_address[];
	 * 		public Address work_address[];
	 * 	}
	 *
	 * all members must be public to be accessible through reflection.
	 * member names must be the same as in the xml, case sensitive.
	 * the above example uses the "_" prefix to deserialize attributes.
	 *
	 * then deserializing the object should look like:
	 * Person p = (Person)XmlDeserializer.deserialize(xmlParser, Person.class, "_");
	 *
	 */
	public static Object deserialize(XmlPullParser parser, Class<?> cls, String attributePrefix) throws InstantiationException, IllegalAccessException {
		Object result = null;

		try {
			parser.require(XmlPullParser.START_TAG, null, null);

			// basic types and primitives ...
			if (cls.isPrimitive()) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Boolean.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Byte.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Short.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Integer.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Long.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Float.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Double.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Character.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == Date.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else if (cls == String.class) {
				result = toObject(readStringTag(parser), cls);
			}
			else {
				result = cls.newInstance();
				String tag_list = "";
				ArrayList<Object> list = new ArrayList<Object>();

				// deserialize attributes.
				if (attributePrefix != null && parser.getEventType() == XmlPullParser.START_TAG) {
					for (int i = 0; i < parser.getAttributeCount(); i += 1) {
						String attrName = parser.getAttributeName(i);
						try {
							Field f = cls.getField(attributePrefix + attrName);
							Object v = toObject(parser.getAttributeValue(i), f.getType());
							f.set(result, v);
						}
						catch (NoSuchFieldException e) {
							Log.i(TAG_XML_DESERIALIZER, String.format("class does not contain field: %s", attrName));
						}
					}
				}

				boolean noFieldSetted = true;
				while (parser.next() != XmlPullParser.END_TAG) {
					if (parser.getEventType() != XmlPullParser.START_TAG) {
						Log.v(TAG_XML_DESERIALIZER, String.format("Skipping: name(`%s`) text(`%s`)", parser.getName(), parser.getText().replace("\n", "\\n")));
						continue;
					}

					String tag_name = parser.getName();

					try {
						Field f = cls.getField(tag_name);
						Class<?> t = f.getType();

						// if this is an array
						if (t.isArray()) {

							// flush the old list
							if (!tag_list.equals(tag_name)) {

								// flush the list
								if (list.size() > 0) {
									try {
										Field f2 = cls.getField(tag_list);
										f2.set(result, toArray(list, f2.getType().getComponentType()));
									}
									catch (NoSuchFieldException e) {
										Log.i(TAG_XML_DESERIALIZER, String.format("class does not contain field: %s", tag_list));
									}
								}

								// re populate the list
								list.clear();
								Object old = f.get(result);
								if (old != null) {
									int length = Array.getLength(old);
									for (int i = 0; i < length; ++i) {
										list.add(Array.get(old, i));
									}
									f.set(result, null);
								}
							}

							// insert the new item into the list
							list.add(deserialize(parser, t.getComponentType(), attributePrefix));
							tag_list = tag_name;
						}

						// this is an object
						else {
							f.set(result, deserialize(parser, f.getType(), attributePrefix));
							noFieldSetted = false;
						}
					}
					catch (NoSuchFieldException e) {
						Log.i(TAG_XML_DESERIALIZER, String.format("class does not contain field: %s", tag_name));
						int level = 1;
						while (level > 0) {
							switch (parser.next()) {
								case XmlPullParser.START_TAG:
									level++;
									break;
								case XmlPullParser.END_TAG:
									level--;
									break;
							}
						}
					}
				}

				// flush the list
				if (list.size() > 0) {
					try {
						Field f = cls.getField(tag_list);
						f.set(result, toArray(list, f.getType().getComponentType()));
						noFieldSetted = false;
					}
					catch (NoSuchFieldException e) {
						Log.i(TAG_XML_DESERIALIZER, String.format("class does not contain field: %s", tag_list));
					}
				}
				if (noFieldSetted) {
					Log.w(TAG_XML_DESERIALIZER, String.format("there were no fiels setted in class: %s", cls.getName()));
				}
			}
		}
		catch (XmlPullParserException e) {
			Log.e(TAG_XML_DESERIALIZER, e.getMessage());
		}
		catch (IOException e) {
			Log.e(TAG_XML_DESERIALIZER, e.getMessage());
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T deserialize(Reader in, Class<T> cls, String attributePrefix) throws InstantiationException, IllegalAccessException, IOException {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in);
			parser.next();
			return (T)deserialize(parser, cls, attributePrefix);
		}
		catch (XmlPullParserException e) {
			e = null;
		}
		return null;
	}

	public static <T> T deserialize(Reader reader, Class<T> cls) throws InstantiationException, IllegalAccessException, IOException {
		return (T)deserialize(reader, cls, null);
	}

	public static void test() {

		class Address {
			public String _type;
			public String street;
			public String phone;
		}

		class Person {
			public int id;
			public String name;
			public String _sex;
			public Address home_address[];
			public Address work_address[];
		}

		String xml = "<person sex=\"other\"> <id>1234</id> <name>Joe Doe</name> <home_address type=\"home\"> <street>101 Sweet Home</street> <phone>333-3333</phone> </home_address> <home_address type=\"home2\"> <street>101 Sweet Home</street> <phone>333-3333</phone> </home_address> <work_address> <street>303 Office Street</street> <phone>444-4444</phone> </work_address> </person>";

		try {
			Person p = (Person)XmlDeserializer.deserialize(new StringReader(xml), Person.class, "_");

			for (Field f : Person.class.getFields()) {
				System.out.println(String.format("%s: %s", f.getName(), f.get(p)));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
}
