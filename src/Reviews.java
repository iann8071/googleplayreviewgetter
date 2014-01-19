import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class Reviews {

	String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details";
	String GET_PARAMETER_ID = "id";
	String GET_PARAMETER_HL = "hl";
	String LOGIN_EMAIL = "iann8071@gmail.com";
	String LOGIN_PASS = "iann2357";

	HashMap<String, Integer> rateMap = new HashMap<String, Integer>();
	{
		rateMap.put("0", 0);
		rateMap.put("20", 1);
		rateMap.put("40", 2);
		rateMap.put("60", 3);
		rateMap.put("80", 4);
		rateMap.put("100", 5);
	}
	ArrayList<String> hls = new ArrayList<String>();
	{
		hls.add("en");
		hls.add("ja");
	}
	ArrayList<String> devices = new ArrayList<String>();
	WebDriver driver;
	private static final String FOLDER_PATH = "C:\\Users\\Kiichi\\googleplayreviewviewer\\results";
	private final ArrayList<String> searchedApps = new ArrayList<String>();
	int count = 0;

	public static void main(String[] args) throws InterruptedException {
		Reviews r = new Reviews();
	}

	public Reviews() throws InterruptedException {
		System.setProperty("webdriver.chrome.driver",
				"./driver/chromedriver.exe");
		login();
		crawlReviews();
		driver.close();
	}

	private void crawlReviews() throws InterruptedException {
		FdroidApps fa = new FdroidApps();
		List<String> apps = fa.getApps();
		List<String> appCategories = fa.getAppCategories();
		for (String appCategory : appCategories) {
			File searchedAppsArray = new File(FOLDER_PATH + "/" + appCategory);
			if (searchedAppsArray.listFiles() != null)
				for (File searchedApp : searchedAppsArray.listFiles()) {
					searchedApps.add(searchedApp.getName());
				}
		}

		apps = new ArrayList<String>();
		apps.add("com.bottleworks.dailymoney");
		apps.add("com.android.keepass");
		apps.add("org.dsandler.apps.markers");
		apps.add("com.evancharlton.mileage");

		for (int j = 0; j < apps.size(); j++) {
			String app = apps.get(j);
			if (searchedApps.contains(app.replaceAll("\\.", "_") + ".csv")) {
				// continue;
			}
			try {
				boolean crached = false;
				//String appCategory = appCategories.get(j);
				String appCategory = "Office";
				for (String hl : hls) {
					driver.get(getURLWithParameters(app, hl));

					try {
						waitIfLoading();
						driver.findElement(
								By.xpath("//*[@id=\"body-content\"]/div[2]/div[2]/div[1]/div[2]/div[2]"))
								.click();
						waitIfLoading();
					} catch (ElementNotVisibleException e) {
						continue;
					} catch (NoSuchElementException e) {
						continue;
					} catch (WebDriverException e) {
						j--;
						driver.close();
						login();
						break;
					}

					WebElement dropdownMenuContainer = driver.findElement(By
							.className("id-review-device-filter"));
					List<WebElement> dropdownMenuChildren = dropdownMenuContainer
							.findElement(By.className("dropdown-menu-children"))
							.findElements(By.xpath("div"));

					for (WebElement dropdownMenuChild : dropdownMenuChildren) {

						deviceMenuContainerClick();
						if (dropdownMenuChild.getText().equals("All Devices")) {
							deviceMenuContainerClick();
							continue;
						}
						String deviceName = dropdownMenuChild.getText();
						dropdownMenuChildClick(dropdownMenuChild);
						waitIfLoading();
						try {
							if (driver
									.findElement(
											By.className("id-no-reviews-container"))
									.getText()
									.equals("There are no reviews matching these criteria."))
								continue;
						} catch (NoSuchElementException e) {

						}

						for (int i = 1;; i++) {
							waitIfLoading();
							try {
								if (i != 1) {

									driver.findElement(
											By.xpath("//*[@id=\"body-content\"]/div[2]/div[2]/div[1]/div[2]/div[2]"))
											.click();

								}
							} catch (ElementNotVisibleException e) {

							} catch (NoSuchElementException e) {
								break;
							} catch (WebDriverException e) {
								j--;
								driver.close();
								login();
								break;
							}
							waitIfLoading();
							List<WebElement> expandPages = driver
									.findElements(By
											.xpath("//*[@id=\"body-content\"]/div[2]/div[2]/div[1]/div[2]/div[1]/div/div"));
							if (i >= expandPages.size()) {
								break;
							}
							WebElement expandPage = expandPages.get(i);
							List<WebElement> columns = expandPage
									.findElements(By.xpath("div"));
							for (WebElement column : columns) {
								List<WebElement> reviews = column
										.findElements(By.xpath("div"));
								for (WebElement review : reviews) {
									WebElement reviewBodyElement;
									WebElement rateElement;
									reviewBodyElement = review.findElement(By
											.className("review-body"));
									rateElement = review.findElement(By
											.className("current-rating"));
									String reviewBody = reviewBodyElement
											.getText();

									if (reviewBody.isEmpty()) {
										remove(app, appCategory);
										restart();
										crached = true;
										j--;
										break;
									}
									int rate = extractRate(rateElement
											.getAttribute("style"));
									System.out.println("Category:"
											+ appCategory);
									System.out.println("Rate:" + rate);
									System.out.println("Review Body:");
									System.out.println(reviewBody);
									writeResult(appCategory, app, rate,
											deviceName, reviewBody);
								}

								if (crached) {
									break;
								}
							}
							if (crached)
								break;
						}
						if (crached)
							break;
					}
					if (crached)
						break;
				}
			} catch (WebDriverException e) {
				j--;
				driver.close();
				login();
				break;
			}
		}
	}

	private void restart() throws InterruptedException {
		driver.close();
		Thread.sleep(60000);
		login();
	}

	private void remove(String app, String category) {
		File file = new File(FOLDER_PATH + "\\" + category + "\\"
				+ app.replaceAll("\\.", "_") + ".csv");
		if (file.isFile())
			file.delete();
	}

	private void waitIfLoading() {
		WebElement loading = driver.findElement(By.className("reviews"))
				.findElement(By.className("expand-loading"));
		int waitLevel = 1;
		while (!loading.getAttribute("style").contains("none")) {
			try {
				Thread.sleep(5000 * waitLevel);
				waitLevel++;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void deviceMenuContainerClick() throws InterruptedException {
		while (true) {
			try {
				driver.findElement(By.className("id-review-device-filter"))
						.click();
				break;
			} catch (ElementNotVisibleException e) {
				Thread.sleep(1000);
			}
		}
	}

	private void dropdownMenuChildClick(WebElement child)
			throws InterruptedException {
		while (true) {
			try {
				child.click();
				break;
			} catch (ElementNotVisibleException e) {
				Thread.sleep(1000);
			}
		}
	}

	private int extractRate(String style) {
		Scanner sc = new Scanner(style);
		sc.useDelimiter("(width: )|%;|\\.");
		String width = sc.next();
		sc.close();
		return rateMap.get(width);
	}

	private void writeResult(String category, String name, int rate,
			String deviceName, String review) {
		try {
			File folder = new File(FOLDER_PATH + "\\" + category);
			folder.mkdirs();
			File csv = new File(FOLDER_PATH + "\\" + category + "\\"
					+ name.replaceAll("\\.", "_") + ".csv");
			PrintWriter pw = new PrintWriter(new FileWriter(csv, true));
			pw.println(rate + "," + deviceName + ","
					+ review.replaceAll(",", ""));
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void login() {
		driver = new ChromeDriver();
		driver.get("https://accounts.google.com/ServiceLogin?");
		driver.findElement(By.id("Email")).sendKeys(LOGIN_EMAIL);
		driver.findElement(By.id("Passwd")).sendKeys(LOGIN_PASS);
		driver.findElement(By.id("signIn")).click();
	}

	private String getURLWithParameters(String id, String hl) {
		ArrayList<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
		parameters.add(new BasicNameValuePair(GET_PARAMETER_ID, id));
		parameters.add(new BasicNameValuePair(GET_PARAMETER_HL, hl));
		String paramsStr = URLEncodedUtils.format(parameters, "utf-8");
		return GOOGLE_PLAY_URL + "?" + paramsStr;
	}
}
