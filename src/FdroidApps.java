import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class FdroidApps {

	ArrayList<String> apps = new ArrayList<String>();
	String FDROID_URL = "https://f-droid.org/repository/browse/";
	String GET_PARAMETER_FD_PAGE = "fdpage";
	String GET_PARAMETER_FD_CATEGORY = "fdcategory";
	String ID_XPATH = "//*[@id=\"content\"]/div[1]/div/div[2]/a";
	String URL_DELIMETER = "=|&";
	ArrayList<String> appCategories = new ArrayList<String>();
	ArrayList<String> CATEGORIES = new ArrayList<String>();
	{
		CATEGORIES.add("Office");
		CATEGORIES.add("Navigation");
		CATEGORIES.add("Multimedia");
		CATEGORIES.add("Internet");
		CATEGORIES.add("System");
		CATEGORIES.add("Games");
		CATEGORIES.add("Science & Education");
		CATEGORIES.add("Development");
		CATEGORIES.add("Phone & SMS");
		CATEGORIES.add("Wallpaper");
		CATEGORIES.add("Reading");
		CATEGORIES.add("None");
		CATEGORIES.add("Mathematics");
	}
	WebDriver driver;
	List<WebElement> idElements;

	public FdroidApps() {
		System.setProperty("webdriver.chrome.driver",
				"./driver/chromedriver.exe");
		driver = new ChromeDriver();
		crawlURL();
		driver.close();
	}

	protected List<String> getApps() {
		return apps;
	}

	protected List<String> getAppCategories() {
		return appCategories;
	}

	protected void crawlURL() {

		//for (int j = 0; j < CATEGORIES.size(); j++) {
		for (int j = 0; j < 1; j++) {
			String category = CATEGORIES.get(j);
			for (int k = 1;; k++) {
				String url = getURLWithParameters(
						Integer.valueOf(k).toString(), category);
				driver.get(url);
				idElements = driver.findElements(By.xpath(ID_XPATH));
				if (idElements.size() == 0)
					break;
				for (int i = 0; i < idElements.size(); i++) {
					WebElement idElement = idElements.get(i);
					String id = extractIdFromURL(idElement.getAttribute("href"));
					System.out.println("id=" + id);
					apps.add(id);
					appCategories.add(category);
				}
			}
		}
	}

	private String extractIdFromURL(String url) {
		Scanner sc = new Scanner(url);
		sc.useDelimiter(URL_DELIMETER);
		sc.next();
		sc.next();
		sc.next();
		String id = sc.next();
		if (id.equals("fdid"))
			id = sc.next();
		sc.close();
		return id;
	}

	private String getURLWithParameters(String pageId, String fdCategory) {
		ArrayList<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
		parameters.add(new BasicNameValuePair(GET_PARAMETER_FD_PAGE, pageId));
		parameters.add(new BasicNameValuePair(GET_PARAMETER_FD_CATEGORY,
				fdCategory));
		String paramsStr = URLEncodedUtils.format(parameters, "utf-8");
		return FDROID_URL + "?" + paramsStr;
	}
}
