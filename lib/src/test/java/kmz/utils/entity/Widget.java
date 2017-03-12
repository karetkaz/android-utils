package kmz.utils.entity;

public class Widget {

    public final String debug = null;
    public final Window window = null;
    public final Image image = null;
    public final Text text = null;

    public static class Window {
        public final String title = null;
        public final String name = null;
        public final Integer width = null;
        public final Integer height = null;
    }

    public static class Image {
        public final String src = null;
        public final String name = null;
        public final int hOffset = 0;
        public final int vOffset = 0;
        public final String alignment = null;
    }

    public static class Text {
        public final String data = null;
        public final int size = 0;
        public final String style = null;
        public final String name = null;
        public final int hOffset = 0;
        public final int vOffset = 0;
        public final String alignment = null;
        public final String onMouseUp = null;
    }

    public static class Entity {
        public final Widget widget = null;
    }
}
