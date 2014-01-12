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
	WebDriver driver;
	private static final String FOLDER_PATH = "C:\\Users\\Kiichi\\googleplayreviewviewer\\results";

	public static void main(String[] args) {
		Reviews r = new Reviews();
	}

	public Reviews() {
		System.setProperty("webdriver.chrome.driver",
				"./driver/chromedriver.exe");
		driver = new ChromeDriver();
		crawlReviews();
		driver.close();
	}

	private void crawlReviews() {
		FdroidApps fa = new FdroidApps();
		List<String> apps = fa.getApps();
		List<String> appCategories = fa.getAppCategories();
		for (int j = 0; j < apps.size(); j++) {
			String app = apps.get(j);
			String appCategory = appCategories.get(j);
			for (String hl : hls) {
				driver.get(getURLWithParameters(app, hl));
				boolean isFirst = true;
				for (int i = 1;; i++) {
					try {
						waitIfLoading();
						driver.findElement(
								By.xpath("//*[@id=\"body-content\"]/div[2]/div[2]/div[1]/div[2]/div[2]"))
								.click();
					} catch (ElementNotVisibleException e) {
						break;
					} catch (NoSuchElementException e) {
						break;
					} catch (WebDriverException e) {
						j--;
						driver.close();
						driver = new ChromeDriver();
						break;
					}
					List<WebElement> expandPages = driver
							.findElements(By
									.xpath("//*[@id=\"body-content\"]/div[2]/div[2]/div[1]/div[2]/div[1]/div/div"));
					if (i >= expandPages.size())
						break;
					WebElement expandPage = expandPages.get(i);
					List<WebElement> columns = expandPage.findElements(By
							.xpath("div"));
					for (WebElement column : columns) {
						List<WebElement> reviews = column.findElements(By
								.xpath("div"));
						for (WebElement review : reviews) {
							WebElement reviewBodyElement;
							WebElement rateElement;
							if (isFirst) {
								reviewBodyElement = review.findElement(By
										.xpath("div[2]/div[2]"));
								rateElement = review
										.findElement(By
												.xpath("div[2]/div[1]/div[1]/div[2]/div/div"));
							} else {
								reviewBodyElement = review.findElement(By
										.xpath("div/div[2]"));
								try {
									rateElement = review
											.findElement(By
													.xpath("div/div[1]/div[1]/div[2]/div/div"));
								} catch (NoSuchElementException e) {
									rateElement = review
											.findElement(By
													.xpath("div[1]/div[1]/div[2]/div/div"));
								}
								// *[@id="body-content"]/div[2]/div[2]/div[1]/div[2]/div[1]/div/div[4]/div[2]/div[3]/div/div[1]/div[1]/div[2]/div/div
								// *[@id="body-content"]/div[2]/div[2]/div[1]/div[2]/div[1]/div/div[5]/div[2]/div[1]/div[1]/div[1]/div[2]/div/div
							}
							String reviewBody = reviewBodyElement.getText();
							int rate = extractRate(rateElement
									.getAttribute("style"));
							System.out.println("Category:" + appCategory);
							System.out.println("Rate:" + rate);
							System.out.println("Review Body:");
							System.out.println(reviewBody);
							writeResult(appCategory, app, rate, reviewBody);
							isFirst = false;
						}
					}
				}
			}
		}
	}

	private void waitIfLoading() {
		// *[@id="body-content"]/div[2]/div[2]/div[1]/div[2]/div[5]
		WebElement loading = driver
				.findElement(By
						.xpath("//*[@id=\"body-content\"]/div[2]/div[2]/div[1]/div[2]/div[5]"));
		while (!loading.getAttribute("style").contains("none")) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			String review) {
		try {
			File folder = new File(FOLDER_PATH + "\\" + category);
			folder.mkdirs();
			File csv = new File(FOLDER_PATH + "\\" + category + "\\"
					+ name.replaceAll("\\.", "_") + ".csv");
			PrintWriter pw = new PrintWriter(new FileWriter(csv, true));
			pw.println(rate + "," + review);
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isNextEnded() {
		List<WebElement> next;
		try {
			next = driver
					.findElements(By
							.xpath("//*[@id=\"body-content\"]/div[2]/div[2]/div[1]/div[2]/div[2]"));
			return false;
		} catch (ElementNotVisibleException e) {
			return true;
		}
	}

	private String getURLWithParameters(String id, String hl) {
		ArrayList<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
		parameters.add(new BasicNameValuePair(GET_PARAMETER_ID, id));
		parameters.add(new BasicNameValuePair(GET_PARAMETER_HL, hl));
		String paramsStr = URLEncodedUtils.format(parameters, "utf-8");
		return GOOGLE_PLAY_URL + "?" + paramsStr;
	}
}
