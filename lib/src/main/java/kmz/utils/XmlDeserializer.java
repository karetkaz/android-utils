package kmz.utils;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

/**
 * Simple Gson like Xml deserializer.
 *
 * Member names of the entity must be equal with xml tags, case sensitive.
 * To parse attributes, use attributePrefix with a non null value.
 *
 * To deserialize an xml into an entity object call deserialize like:
 * Person person = XmlDeserializer.deserialize(new InputStreamReader(stream), Person.class, "_");
 *
 * @ see XmlDeserializeTest for more
 */

public class XmlDeserializer {

	private static final  DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final String TAG_XML_DESERIALIZER = "xml.obj.deserializer";

	@SuppressWarnings("unchecked")
	public static <T> T deserialize(Reader in, Class<T> cls, String attributePrefix)
			throws InstantiationException, IllegalAccessException, IOException, XmlPullParserException {
		XmlPullParser parser = Xml.newPullParser();
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		parser.setInput(in);
		parser.next();
		String rootTag = parser.getName();
		Field[] fields = getFields(cls);
		if (fields.length == 1 && fields[0].getName().equals(rootTag)) {
			Object result = cls.newInstance();
			Field field = fields[0];
			field.setAccessible(true);
			field.set(result, readObject(parser, field.getType(), attributePrefix));
			return (T) result;
		}
		return (T) readObject(parser, cls, attributePrefix);
	}

	private static Object readObject(XmlPullParser parser, Class<?> cls, String attributePrefix)
			throws InstantiationException, IllegalAccessException {
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
				ArrayList<Object> list = new ArrayList<>();

				// deserialize attributes.
				if (attributePrefix != null && parser.getEventType() == XmlPullParser.START_TAG) {
					for (int i = 0; i < parser.getAttributeCount(); i += 1) {
						String attrName = parser.getAttributeName(i);
						try {
							Field f = getField(cls, attributePrefix + attrName);
							Object v = toObject(parser.getAttributeValue(i), f.getType());
							f.set(result, v);
						}
						catch (NoSuchFieldException e) {
							Log.i(TAG_XML_DESERIALIZER, String.format("class does not contain field: %s", attrName));
						}
					}
				}

				boolean noFieldsSet = true;
				while (parser.next() != XmlPullParser.END_TAG) {
					if (parser.getEventType() != XmlPullParser.START_TAG) {
						Log.v(TAG_XML_DESERIALIZER, String.format("Skipping: name(`%s`) text(`%s`)", parser.getName(), parser.getText().replace("\n", "\\n")));
						continue;
					}

					String tag_name = parser.getName();

					try {
						Field f = getField(cls, tag_name);
						Class<?> t = f.getType();

						// if this is an array
						if (t.isArray()) {

							// flush the old list
							if (!tag_list.equals(tag_name)) {

								// flush the list
								if (list.size() > 0) {
									try {
										Field f2 = getField(cls, tag_list);
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
							list.add(readObject(parser, t.getComponentType(), attributePrefix));
							tag_list = tag_name;
						}

						// this is an object
						else {
							f.set(result, readObject(parser, f.getType(), attributePrefix));
							noFieldsSet = false;
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
						Field f = getField(cls, tag_list);
						f.set(result, toArray(list, f.getType().getComponentType()));
						noFieldsSet = false;
					}
					catch (NoSuchFieldException e) {
						Log.i(TAG_XML_DESERIALIZER, String.format("class does not contain field: %s", tag_list));
					}
				}
				if (noFieldsSet) {
					Log.w(TAG_XML_DESERIALIZER, String.format("no fields were set in class: %s", cls.getName()));
				}
			}
		}
		catch (XmlPullParserException | IOException e) {
			Log.e(TAG_XML_DESERIALIZER, e.getMessage());
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
				default:
					Log.w(TAG_XML_DESERIALIZER, String.format("failed to convert text to character: %s", value));
					break;

				case 1:
					result = value.charAt(0);
					break;
			}
		}
		else if (type == Date.class) {
			try {
				result = formatter.parse(value);
			}
			catch (Exception e) {
				Log.w(TAG_XML_DESERIALIZER, String.format("failed to parse date: %s", value));
			}
		}
		else if (type == String.class) {
			result = value;
		}
		return result;
	}

	// get the field by name in a class, including inherited classes as well
	private static Field getField(Class cls, String fieldName) throws NoSuchFieldException {
		while (cls != null) {
			if (cls == Object.class) {
				break;
			}
			try {
				Field result = cls.getDeclaredField(fieldName);
				result.setAccessible(true);
				return result;
			} catch (NoSuchFieldException e) {
				// if not found, try to lookup in the super class
				cls = cls.getDeclaringClass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}

	private static Field[] getFields(Class cls) {
		ArrayList<Field> fields = new ArrayList<>();
		for (Class base = cls; base != null; base = base.getSuperclass()) {
			// todo: filter accessors (like: "shadow$_klass_" (containing: '$'))
			if (base == Object.class) {
				break;
			}
			fields.addAll(0, Arrays.asList(base.getDeclaredFields()));
		}
		return fields.toArray(new Field[fields.size()]);
	}

}
