package kmz.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kmz.utils.entity.Bookstore;
import kmz.utils.entity.Company;
import kmz.utils.entity.Embedded;
import kmz.utils.entity.Escapes;
import kmz.utils.entity.FileSystem;
import kmz.utils.entity.FoodMenu;
import kmz.utils.entity.JsonTest;
import kmz.utils.entity.Menu;
import kmz.utils.entity.Person;
import kmz.utils.entity.ValuesArray;
import kmz.utils.entity.ValuesArrayWrapped;
import kmz.utils.entity.ValuesList;
import kmz.utils.entity.ValuesListWrapped;
import kmz.utils.entity.Widget;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Unit test, which will parse xml files and deserialize them into entities.
 * <p>
 * test data was taken from: <a href="http://json.org/example.html">json.org/example.html</a>
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLog.class}, manifest = Config.NONE)
public class XmlParserTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File XML_PERSON = new File("person.xml");
    private static final File XML_ARRAY_VALUES = new File("array.xml");
    private static final File XML_ARRAY_WRAPPED = new File("array_wrapped.xml");

    // test files from json.org
    private static final File XML_MENU = new File("json.org/menu.xml");
    private static final File JSON_MENU = new File("json.org/menu.json");
    private static final File XML_WIDGET = new File("json.org/widget.xml");
    private static final File JSON_WIDGET = new File("json.org/widget.json");

    // test files from: https://www.codeproject.com/Articles/5384262/Mini-XML-A-Powerful-XML-Parser
    private static final File XML_BOOKS = new File("xml/books.xml");
    private static final File XML_EMPLOYEES = new File("xml/Employees.xml");
    private static final File XML_ESCAPES = new File("xml/escapes.xml");
    private static final File XML_FOOD_MENU = new File("xml/foodmenu.xml");
    private static final File XML_EMBEDDED = new File("xml/embedded.xml");
    private static final File XML_JSON_TEST = new File("xml/jsontest.xml");
    private static final File XML_FILE_SYSTEM = new File("xml/testdir.xml");

    @Before
    public void before() {
        //System.out.println("Setting it up!");
        ShadowLog.stream = System.out;
    }

    @Test
    public void testMenu() throws Exception {
        Menu xml1 = deserializeXmlParser(Menu.class, XML_MENU);
        Menu xml2 = deserializeXmlJackson(Menu.class, XML_MENU);
        Menu json = deserializeJson(Menu.Entity.class, JSON_MENU).menu;

        assertEqualsNotNull(xml2, xml1);
        assertEqualsNotNull(json, xml1);
        assertEqualsNotNull(json, xml2);
    }

    @Test
    public void testWidget() throws Exception {
        Widget xml1 = deserializeXmlParser(Widget.class, XML_WIDGET);
        Widget xml2 = deserializeXmlJackson(Widget.class, XML_WIDGET);
        Widget json = deserializeJson(Widget.Entity.class, JSON_WIDGET).widget;

        assertEqualsNotNull(xml2, xml1);
        assertEqualsNotNull(json, xml1);
        assertEqualsNotNull(json, xml2);
    }

    @Test
    public void testPerson() throws Exception {
        Person xml1 = deserializeXmlParser(Person.class, XML_PERSON);
        Person xml2 = deserializeXmlJackson(Person.class, XML_PERSON);

        assertEqualsNotNull(xml1, xml2);

        Assert.assertEquals(1234, xml1.id);
        Assert.assertEquals(1234, xml2.id);
        Assert.assertEquals("other", xml1.sex);
        Assert.assertEquals("other", xml2.sex);
        Assert.assertEquals("Joe Doe", xml1.name);
        Assert.assertEquals("Joe Doe", xml2.name);
        Assert.assertEquals(2, xml1.home_address.length);
        Assert.assertEquals(2, xml2.home_address.length);
        Assert.assertEquals(1, xml1.work_address.length);
        Assert.assertEquals(1, xml2.work_address.length);
    }

    @Test
    public void testArray() throws Exception {
        testParseFile(byte[].class, XML_ARRAY_VALUES);
        testParseFile(Byte[].class, XML_ARRAY_VALUES);

        testParseFile(short[].class, XML_ARRAY_VALUES);
        testParseFile(Short[].class, XML_ARRAY_VALUES);

        testParseFile(int[].class, XML_ARRAY_VALUES);
        testParseFile(Integer[].class, XML_ARRAY_VALUES);

        testParseFile(long[].class, XML_ARRAY_VALUES);
        testParseFile(Long[].class, XML_ARRAY_VALUES);

        testParseFile(float[].class, XML_ARRAY_VALUES);
        testParseFile(Float[].class, XML_ARRAY_VALUES);

        testParseFile(double[].class, XML_ARRAY_VALUES);
        testParseFile(Double[].class, XML_ARRAY_VALUES);

        testParseFile(ValuesList.class, XML_ARRAY_VALUES);
        testParseFile(ValuesArray.class, XML_ARRAY_VALUES);

        testParseFile(ValuesListWrapped.class, XML_ARRAY_WRAPPED);
        testParseFile(ValuesArrayWrapped.class, XML_ARRAY_WRAPPED);
    }

    @Test
    public void testAssets() throws Exception {
        testParseFile(Bookstore.class, XML_BOOKS);
        testParseFile(Company.class, XML_EMPLOYEES);
        testParseFile(Escapes.class, XML_ESCAPES);
        testParseFile(FoodMenu.class, XML_FOOD_MENU);
    }

    @Test(expected = Exception.class)
    // embedded text cannot be parsed
    public void testEmbedded() throws Exception {
        testParseFile(Embedded.class, XML_EMBEDDED);
    }

    @Test(expected = ComparisonFailure.class)
    // Jakson parser does not allow merging arrays
    public void testFileSystem() throws Exception {
        testParseFile(FileSystem.class, XML_FILE_SYSTEM);
    }

    @Test//(expected = ComparisonFailure.class)
    // Jakson parser returns empty string instead of null (fixed with SafeStringMapper)
    public void testJsonTest() throws Exception {
        testParseFile(JsonTest.class, XML_JSON_TEST);
    }

    public static void testParseFile(Class<?> type, File file) throws Exception {
        Object resultJackson = deserializeXmlJackson(type, file);
        Object resultXmlParser = deserializeXmlParser(type, file);
        assertEqualsNotNull(resultJackson, resultXmlParser);
        System.out.println(GSON.toJson(resultXmlParser));
    }

    private static <Entity> Entity deserializeXmlJackson(Class<Entity> type, File file) throws Exception {
        try (InputStream f = XmlParserTest.class.getClassLoader().getResourceAsStream(file.getPath())) {
            if (f == null) throw new FileNotFoundException(file.getPath());
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.setDefaultUseWrapper(false);
            xmlMapper.registerModule(new JaxbAnnotationModule());
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return xmlMapper.readValue(new InputStreamReader(f), type);
        }
    }

    private static <Entity> Entity deserializeXmlParser(Class<Entity> type, File file) throws Exception {
        try (InputStream f = XmlParserTest.class.getClassLoader().getResourceAsStream(file.getPath())) {
            if (f == null) throw new FileNotFoundException(file.getPath());
            return new XmlParser().readValue(new InputStreamReader(f), type);
        }
    }

    private static <Entity> Entity deserializeJson(Class<Entity> type, File file) throws Exception {
        try (InputStream f = XmlParserTest.class.getClassLoader().getResourceAsStream(file.getPath())) {
            if (f == null) throw new FileNotFoundException(file.getPath());
            return new Gson().fromJson(new InputStreamReader(f), type);
        }
    }

    private static void assertEqualsNotNull(Object expected, Object actual) throws Exception {
        Assert.assertNotNull(expected);
        DeepCompare.equals(expected, actual, Assert::assertEquals);
    }

    interface DeepCompare {
        boolean COMPARE_TRIM_STRINGS = true;

        void assertEqual(String message, Object expected, Object actual);

        /**
         * compare objects recursively
         * if there are back-references aka cycles in the objects, StackOverflowError shall break the infinite compare
         */
        static boolean equals(Object expected, Object actual, DeepCompare asserter) throws Exception {
            if (expected == null || actual == null) {
                if (expected != actual) {
                    if (asserter != null) asserter.assertEqual("comparing null to something", expected, actual);
                    return false;
                }
                return true;
            }

            Class<?> expectedType = expected.getClass();
            Class<?> actualType = actual.getClass();

            if (!expectedType.isAssignableFrom(actualType) && !actualType.isAssignableFrom(expectedType)) {
                if (asserter != null) asserter.assertEqual("Incompatible types", expected, actual);
                return false;
            }

            if (Enum.class.isAssignableFrom(expectedType) || Enum.class.isAssignableFrom(actualType)) {
                if (Enum.class.isAssignableFrom(expectedType) != Enum.class.isAssignableFrom(actualType)) {
                    if (asserter != null) asserter.assertEqual("comparing Enum to something", expected, actual);
                    return false;
                }
                return expected == actual;
            }

            if (String.class.isAssignableFrom(expectedType) || String.class.isAssignableFrom(actualType)) {
                if (String.class.isAssignableFrom(expectedType) != String.class.isAssignableFrom(actualType)) {
                    if (asserter != null) asserter.assertEqual("comparing String to something", expected, actual);
                    return false;
                }
                String expectedStr = (String) expected;
                String actualStr = (String) actual;

                if (COMPARE_TRIM_STRINGS) {
                    expectedStr = expectedStr.replace('\t', ' ').trim();
                    actualStr = actualStr.replace('\t', ' ').trim();
                }
                if (!expectedStr.equals(actualStr)) {
                    if (asserter != null) asserter.assertEqual("comparing String to something", expected, actual);
                    return false;
                }
                return true;
            }

            if (Collection.class.isAssignableFrom(expectedType) || Collection.class.isAssignableFrom(actualType)) {
                if (Collection.class.isAssignableFrom(expectedType) != Collection.class.isAssignableFrom(actualType)) {
                    if (asserter != null) asserter.assertEqual("comparing Collection to something", expected, actual);
                    return false;
                }

                Iterator<?> aIt = ((Collection<?>) expected).iterator();
                Iterator<?> bIt = ((Collection<?>) actual).iterator();
                while (aIt.hasNext() && bIt.hasNext()) {
                    if (!equals(aIt.next(), bIt.next(), asserter)) {
                        if (asserter != null) asserter.assertEqual("not equals", expected, actual);
                        return false;
                    }
                }
                if (aIt.hasNext() || bIt.hasNext()) {
                    if (asserter != null) asserter.assertEqual("Collection not iterated to the end", expected, actual);
                    return false;
                }
                return true;
            }

            if (expectedType.isArray() || actualType.isArray()) {
                if (expectedType.isArray() != actualType.isArray()) {
                    if (asserter != null) asserter.assertEqual("comparing Array to something", expected, actual);
                    return false;
                }

                if (expectedType.getComponentType().isPrimitive() || actualType.getComponentType().isPrimitive()) {
                    return Arrays.deepEquals(new Object[]{expected}, new Object[]{actual});
                }

                Object[] aArr = (Object[]) expected;
                Object[] bArr = (Object[]) actual;
                if (aArr.length != bArr.length) {
                    if (asserter != null) asserter.assertEqual("different Array lengths", expected, actual);
                    return false;
                }
                for (int i = 0; i < aArr.length; i++) {
                    if (!equals(aArr[i], bArr[i], asserter)) {
                        if (asserter != null) asserter.assertEqual("not equals", aArr[i], bArr[i]); // fixme: remove
                        return false;
                    }
                }
                return true;
            }

            try {
                Method equals = expectedType.getDeclaredMethod("equals", Object.class);
                if (equals != Object.class.getDeclaredMethod("equals", Object.class)) {
                    if (!(boolean) equals.invoke(expected, actual)) {
                        if (asserter != null) asserter.assertEqual("not equals", expected, actual);
                        return false;
                    }
                    return true;
                }
            } catch (NoSuchMethodException ignore) {}
            try {
                Method equals = actualType.getDeclaredMethod("equals", Object.class);
                if (equals != Object.class.getDeclaredMethod("equals", Object.class)) {
                    if (!(boolean) equals.invoke(actual, expected)) {
                        if (asserter != null) asserter.assertEqual("not equals", expected, actual);
                        return false;
                    }
                    return true;
                }
            } catch (NoSuchMethodException ignore) {}

            if (expectedType != actualType) {
                if (asserter != null) asserter.assertEqual("not equals", expectedType, actualType);
                return false;
            }

            for (Field field : expectedType.getDeclaredFields()) {
                field.setAccessible(true);
                if (!equals(field.get(expected), field.get(actual), asserter)) {
                    if (asserter != null) asserter.assertEqual("not equals", expected, actual);
                    return false;
                }
            }

            return true;
        }
    }
}
