import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.csvreader.CsvWriter;
import com.csvreader.CsvReader;

public class CsvStockDiv
{
	static String inputFile = "C:\\Users\\hyche_000\\Desktop\\上市公司.csv";
	static String outputFile = "C:\\Users\\hyche_000\\Desktop\\Stock\\上市公司殖利率.csv";
	static boolean alreadyExists;
	static CsvWriter csvOutput;
	static CsvReader companyList;
	static String[] cashDiv = new String[5]; //Store value of 5 year cash dividend of a stock
	static String[] stockDiv = new String[5]; //Store value of 5 year stock dividend of a stock
	static String[] bPS = new String[5]; //Store value of 5 year book per share of a stock
	static String price = "0"; //Current price of stock
	static String stockID = ""; //Stock id
	static String stockName = ""; //Stock name
	BigDecimal earning = BigDecimal.ZERO; //Average annual earning of 5 years
	BigDecimal yield = BigDecimal.ZERO; //earning divide by current price
	static String year = "0"; //Latest year of data
	static String earliestAvailYear = "0"; //Earliest year with available data
	static String targetPrice = ""; //calculated by earning divide by target yield
	static String tempStockID = ""; //Store temporary stockID, if requested stockID is same as current stockID, no need to connect to the website again
	
	public static void main(String[] args)
	{
		try
		{
			alreadyExists = new File(outputFile).exists();
			
			csvOutput = new CsvWriter(new FileWriter(outputFile, true), ',');
			
			// if the file didn't already exist then we need to write out the header line
			if (!alreadyExists)
			{
				csvOutput.write("No.");
				csvOutput.write("股號");
				csvOutput.write("名稱");
				csvOutput.write("201405股價");
				csvOutput.write("5年平均獲利");
				csvOutput.write("殖利率");
				csvOutput.endRecord();
			}
			
			CsvReader companyList = new CsvReader(inputFile);
			companyList.readHeaders();
			
			for(int i = 0; i < 78; i++)
				companyList.readRecord();
			
			int no = 0; //No.
			//boolean bbh = false;
			//while(bbh)
			//while(companyList.readRecord() & i < 2)
			int fileIndex = 1;
			for(int i = 78; i < 844; i++)
			{
				csvOutput = new CsvWriter(new FileWriter(outputFile, true), ',');
				/*
				if(i % 20 == 0 & i > 20)
				{
					csvOutput.close(); //Finish editing previous file
					FileUtils.copyFile(new File(outputFiles[fileIndex]), 
							new File(outputFiles[++fileIndex]));
					
					csvOutput = new CsvWriter(new FileWriter(outputFiles[fileIndex], true), ',');
				}*/
				
				companyList.readRecord();
				stockID  = companyList.get("ID");
				System.out.println((i + 1) + ". " + stockID);
				
				retrieveData();
				while(price.equals(""))
				{
					System.out.println("Connection failed, rest for 5 minutes...");
					for(int j = 5; j > 0; j--)
					{
						int random = ((int)(Math.random() * 10) - 5) * 1000;
						Thread.sleep(60000 + random);
						System.out.println("Waiting... " + j);
					}
					retrieveData();
				}
				csvOutput.write(String.valueOf(i + 1));
				csvOutput.write(stockID);
				csvOutput.write(stockName.substring(0, stockName.length() - 6));
				csvOutput.write(price);
				if(checkIfDataComplete())
				{
					csvOutput.write(earning().toString());
					csvOutput.write(yield().toString());
				} else
				{
					csvOutput.write("No data");
					csvOutput.write("No data");
				}

				csvOutput.endRecord();
				csvOutput.close();
				if((i + 1) % 10 == 0)
				{
					for(int j = 5; j > 0; j--)
					{
						Thread.sleep(60000);
						System.out.println("Resting... " + j);
					}
				}
				int random = ((int)(Math.random() * 10) - 5) * 1000;
				Thread.sleep(60000 + random);
			}
			
			companyList.close();
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public static String subtractString(String s, int n)
	{
		int input = Integer.valueOf(s);
		String output = String.valueOf(input - n);
		return output;
	}
	
	public static void clearData()
	{
		stockName = "";
		price = "";
		year = "0";
		for(int i = 0; i < 5; i++)
		{
			cashDiv[i] = "-";
			stockDiv[i] = "-";
			bPS[i] = "-";
		}
	}
	
	public static void retrieveData() throws IOException
	{
		clearData();
		//Data from yahoo finance website, used to retrieve stock name by stock id
		Connection yConnection = Jsoup.connect("https://tw.stock.yahoo.com/q/bc?s=" + stockID)
								.userAgent("Mozilla");
		yConnection.timeout(0);
		Document yFinance = yConnection.get();
		Element yContent = yFinance.head();
		Elements yLinks = yContent.getElementsByTag("title");
		String yTitle = yLinks.toString();
		for(int i = 1; yTitle.charAt(i - 1) != ')'; i++)
		{
			stockName = stockName + yTitle.charAt(i);
			if(yTitle.charAt(i) == '>')
				//Stock name
				stockName = "";
		}
		
		//Data from goodinfo.com, used to retrieve price, year, dividends and bps
		Connection goodConnection = Jsoup.connect("http://www.goodinfo.tw/stockinfo/StockBzPerformance.asp?STOCK_ID=" + stockID + "&YEAR_PERIOD=9999&RPT_CAT=M_YEAR")
									.userAgent("Mozilla"); 
		goodConnection.timeout(0);
		Document doc = goodConnection.get();
		Element content = doc.body();
		Elements stockPrice = new Elements();
		
		//Retrieve price
		//Because there will be different color for the stock price and background when price drop, rise, drop 7%, rise 7% and non
		if(! content.getElementsByAttributeValue("style", "font-weight:bold;color:red").text().equals(""))
			stockPrice = content.getElementsByAttributeValue("style", "font-weight:bold;color:red");
		else if(! content.getElementsByAttributeValue("style", "font-weight:bold;color:green").text().equals(""))
			stockPrice = content.getElementsByAttributeValue("style", "font-weight:bold;color:green");
		else if(! content.getElementsByAttributeValue("style", "font-weight:bold;color:black").text().equals(""))
			stockPrice = content.getElementsByAttributeValue("style", "font-weight:bold;color:black");
		else if(! content.getElementsByAttributeValue("style", "font-weight:bold;color:yellow;background-color:red").text().equals(""))
			stockPrice = content.getElementsByAttributeValue("style", "font-weight:bold;color:yellow;background-color:red");
		else if(! content.getElementsByAttributeValue("style", "font-weight:bold;color:yellow;background-color:green").text().equals(""))
			stockPrice = content.getElementsByAttributeValue("style", "font-weight:bold;color:yellow;background-color:green");
		price = removeComma(stockPrice.text()); //Remove comma in stockPrice higher than one thousand, ex. 1,650.00
	
		//Retrieve year
		Elements links = content.getElementsByTag("nobr");
		for(int i = 0; year.equals("0") & i < links.size() - 1; i++)
		{
			String yearCheck = links.get(i + 1).text();
			//System.out.println(yearCheck);
			if(links.get(i).text().equals("最低") & isInt(yearCheck))
			{
				year = links.get(i + 1).text();
				//If the latest year's data of dividend hasn't been upload, then start counting from the previous year
				if(links.get(i + 18).text().equals("-"))
				{
					year = String.valueOf(Integer.valueOf(year) - 1);
				}
			} else if(links.get(i).text().equals("最低") & yearCheck.length() == 4)
			{
				if(isInt(yearCheck.substring(0, 2)) & yearCheck.charAt(2) == 'Q' & isInt(yearCheck.substring(3)))
				{
					year = links.get(i + 23).text();
					if(links.get(i + 41).text().equals("-"))
					{
						year = String.valueOf(Integer.valueOf(year) - 1);
					}
				}
			}
		}
		//System.out.println("year = " + year);
		
		//Retrieve earliestAvailYear
		for(int i = 0; i < links.size(); i++)
		{
			String str = links.get(i).text();
			if(isInt(str) & str.length() == 4)
				earliestAvailYear = str;
		}
		
		//Retrieve dividends and bps
		for(int i = 0; i < links.size(); i++)
		{
			String text = links.get(i).text();
			if(text.equals(year))
			{
				cashDiv[0] = links.get(i + 17).text();
				stockDiv[0] = links.get(i + 18).text();
				bPS[0] = links.get(i + 16).text();

			} else if(text.equals(subtractString(year, 1)))
			{
				cashDiv[1] = links.get(i + 17).text();
				stockDiv[1] = links.get(i + 18).text();
				bPS[1] = links.get(i + 16).text();
			} else if(text.equals(subtractString(year, 2)))
			{
				cashDiv[2] = links.get(i + 17).text();
				stockDiv[2] = links.get(i + 18).text();
				bPS[2] = links.get(i + 16).text();
			} else if(text.equals(subtractString(year, 3)))
			{
				cashDiv[3] = links.get(i + 17).text();
				stockDiv[3] = links.get(i + 18).text();
				bPS[3] = links.get(i + 16).text();
			}else if(text.equals(subtractString(year, 4)))
			{
				cashDiv[4] = links.get(i + 17).text();
				stockDiv[4] = links.get(i + 18).text();
				bPS[4] = links.get(i + 16).text();
				
			}
		}
		tempStockID = stockID;
	}
	
	public static boolean checkIfDataComplete()
	{
		for(String str : cashDiv)
			if(str.equals("-"))
				return false;
		for(String str : stockDiv)
			if(str.equals("-"))
				return false;
		for(String str : bPS)
			if(str.equals("-"))
				return false;
		return true;
	}
	
	public static String removeComma(String s)
	{
		for(int i = 0; i < s.length(); i++)
		{
			if(s.substring(i, i + 1).equals(","))
			{
				s = s.substring(0, i) + s.substring(i + 1);
			}
		}
		return s;
	}
	
	public static boolean isInt(String s)
	{
		for(char c : s.toCharArray())
			if(! (c == '0' | c == '1' | c == '2' | c == '3' | c == '4' | 
				c == '5' | c == '6' | c == '7' | c == '8' | c == '9'))
				return false;
		return true;
	}
	public static BigDecimal yield()
	{
		return earning().multiply(new BigDecimal(100)).divide(new BigDecimal(price), 2, RoundingMode.HALF_EVEN);
	}
	
	public static BigDecimal earning()
	{
		double d = 0;
		for(int i = 0; i < 5; i++)
			d = d + Double.valueOf(cashDiv[i]);
		double s = 1;
		for(int i = 0; i < 5; i++)
			s = s * (1 + (Double.valueOf(stockDiv[i]) / 10));
		double b5 = Double.valueOf(bPS[0]);
		double b1 = Double.valueOf(bPS[4]);
		BigDecimal output = new BigDecimal((d + (b5 * s) - b1) / 5);
		return output.setScale(2, RoundingMode.HALF_EVEN);
	}

}
