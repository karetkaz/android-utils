package kmz.utils;

import com.google.gson.Gson;
import junit.framework.Assert;
import kmz.utils.entity.Menu;
import kmz.utils.entity.Person;
import kmz.utils.entity.Widget;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.InputStream;
import java.io.InputStreamReader;

import static junit.framework.Assert.assertEquals;

/**
 * Unit test, which will parse xml files and deserialize them into entities.
 *
 * test data was taken from: http://json.org/example.html
 */
@RunWith(RobolectricTestRunner.class)
public class XmlDeserializeTest {

    @Test
    public void testParesMenu() throws Exception {
        Menu xml1 = deserializeXml("xml/json.org-menu.xml", Menu.class);
        Menu xml2 = deserializeXml("xml/json.org-menu.xml", Menu.Entity.class).menu;
        Menu json = deserializeJson("json/json.org-menu.json", Menu.Entity.class).menu;

        Assert.assertNotNull(xml1);
        Assert.assertNotNull(xml2);
        Assert.assertNotNull(json);
        Assert.assertEquals(json.id, xml1.id);
        Assert.assertEquals(json.id, xml2.id);
        Assert.assertEquals(json.value, xml1.value);
        Assert.assertEquals(json.value, xml2.value);
        Assert.assertTrue(json.popup.menuitem.length > 2);
        Assert.assertEquals(json.popup.menuitem.length, xml1.popup.menuitem.length);
        Assert.assertEquals(json.popup.menuitem.length, xml2.popup.menuitem.length);
        Assert.assertEquals(json.popup.menuitem[0].onclick, xml1.popup.menuitem[0].onclick);
        Assert.assertEquals(json.popup.menuitem[0].onclick, xml2.popup.menuitem[0].onclick);
        Assert.assertEquals(json.popup.menuitem[0].value, xml1.popup.menuitem[0].value);
        Assert.assertEquals(json.popup.menuitem[0].value, xml2.popup.menuitem[0].value);
        Assert.assertEquals(json.popup.menuitem[1].onclick, xml1.popup.menuitem[1].onclick);
        Assert.assertEquals(json.popup.menuitem[1].onclick, xml2.popup.menuitem[1].onclick);
        Assert.assertEquals(json.popup.menuitem[1].value, xml1.popup.menuitem[1].value);
        Assert.assertEquals(json.popup.menuitem[1].value, xml2.popup.menuitem[1].value);
        Assert.assertEquals(json.popup.menuitem[2].onclick, xml1.popup.menuitem[2].onclick);
        Assert.assertEquals(json.popup.menuitem[2].onclick, xml2.popup.menuitem[2].onclick);
        Assert.assertEquals(json.popup.menuitem[2].value, xml1.popup.menuitem[2].value);
        Assert.assertEquals(json.popup.menuitem[2].value, xml2.popup.menuitem[2].value);
    }

    @Test
    public void testParesWidget() throws Exception {
        Widget xml1 = deserializeXml("xml/json.org-widget.xml", Widget.class);
        Widget xml2 = deserializeXml("xml/json.org-widget.xml", Widget.Entity.class).widget;
        Widget json = deserializeJson("json/json.org-widget.json", Widget.Entity.class).widget;

        Assert.assertNotNull(xml1);
        Assert.assertNotNull(xml2);
        Assert.assertNotNull(json);
        Assert.assertEquals(json.debug, xml1.debug);
        Assert.assertEquals(json.debug, xml2.debug);

        Assert.assertEquals(json.image.alignment, xml1.image.alignment);
        Assert.assertEquals(json.image.alignment, xml2.image.alignment);
        Assert.assertEquals(json.image.hOffset, xml1.image.hOffset);
        Assert.assertEquals(json.image.hOffset, xml2.image.hOffset);
        Assert.assertEquals(json.image.vOffset, xml1.image.vOffset);
        Assert.assertEquals(json.image.vOffset, xml2.image.vOffset);
        Assert.assertEquals(json.image.name, xml1.image.name);
        Assert.assertEquals(json.image.name, xml2.image.name);
        Assert.assertEquals(json.image.src, xml1.image.src);
        Assert.assertEquals(json.image.src, xml2.image.src);

        Assert.assertEquals(json.text.alignment, xml1.text.alignment);
        Assert.assertEquals(json.text.alignment, xml2.text.alignment);
        Assert.assertEquals(json.text.data, xml1.text.data);
        Assert.assertEquals(json.text.data, xml2.text.data);
        Assert.assertEquals(json.text.hOffset, xml1.text.hOffset);
        Assert.assertEquals(json.text.hOffset, xml2.text.hOffset);
        Assert.assertEquals(json.text.vOffset, xml1.text.vOffset);
        Assert.assertEquals(json.text.vOffset, xml2.text.vOffset);
        Assert.assertEquals(json.text.name, xml1.text.name);
        Assert.assertEquals(json.text.name, xml2.text.name);
        Assert.assertEquals(json.text.onMouseUp, xml1.text.onMouseUp.trim());
        Assert.assertEquals(json.text.onMouseUp, xml2.text.onMouseUp.trim());
        Assert.assertEquals(json.text.size, xml1.text.size);
        Assert.assertEquals(json.text.size, xml2.text.size);
        Assert.assertEquals(json.text.style, xml1.text.style);
        Assert.assertEquals(json.text.style, xml2.text.style);

        Assert.assertEquals(json.window.name, xml1.window.name);
        Assert.assertEquals(json.window.name, xml2.window.name);
        Assert.assertEquals(json.window.title, xml1.window.title);
        Assert.assertEquals(json.window.title, xml2.window.title);
        Assert.assertEquals(json.window.width, xml1.window.width);
        Assert.assertEquals(json.window.width, xml2.window.width);
        Assert.assertEquals(json.window.height, xml1.window.height);
        Assert.assertEquals(json.window.height, xml2.window.height);
    }

    @Test
    public void testParesPerson() throws Exception {
        Person person;
        try (InputStream stream = XmlDeserializeTest.class.getClassLoader().getResourceAsStream("xml/person.xml")) {
            person = XmlDeserializer.deserialize(new InputStreamReader(stream), Person.class, "_");
        }

        Assert.assertNotNull(person);
        Assert.assertEquals(1234, person.id);
        Assert.assertEquals("other", person._sex);
        Assert.assertEquals("Joe Doe", person.name);
        Assert.assertEquals(2, person.home_address.length);
        Assert.assertEquals(1, person.work_address.length);
    }


    private static<Entity> Entity deserializeXml(String path, Class<Entity> entityClass) throws Exception {
        try (InputStream f = XmlDeserializeTest.class.getClassLoader().getResourceAsStream(path)) {
            return XmlDeserializer.deserialize(new InputStreamReader(f), entityClass, "");
        }
    }
    private static<Entity> Entity deserializeJson(String path, Class<Entity> entityClass) throws Exception {
        try (InputStream f = XmlDeserializeTest.class.getClassLoader().getResourceAsStream(path)) {
            return new Gson().fromJson(new InputStreamReader(f), entityClass);
        }
    }
}