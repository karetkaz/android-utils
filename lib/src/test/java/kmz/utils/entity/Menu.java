package kmz.utils.entity;

public class Menu {

    public final String id = null;
    public final String value = null;
    public final Popup popup = null;

    public static class Popup {
        //@XmlDeserializer.SerializedName(value = "menuitems", group = "menuitems")
        public final MenuItem[] menuitem = null;
    }

    public static class MenuItem {
        public final String value = null;
        public final String onclick = null;
    }

    // Root Entity ?
    public static class Entity {
        public final Menu menu = null;
    }

}
