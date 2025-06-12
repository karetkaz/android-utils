package kmz.utils.entity;

import java.util.List;

public class FoodMenu {
	public List<Food> food;

	public static class Food {
		public String name;
		public String price;
		public String description;
		public int calories;
	}
}
