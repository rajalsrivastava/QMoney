
package com.crio.warmup.stock;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  public static final String TOKEN = "b2aa9f126dcee21d707aadf68430de853d958c4b";
  
  public static RestTemplate restTemplate = new RestTemplate();
  public static PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate); 

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  // for the stocks provided in the Json.
  // Use the function you just wrote #calculateAnnualizedReturns.
  // Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // TODO:
  // Ensure all tests are passing using below command
  // ./gradlew test --tests ModuleThreeRefactorTest

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException, DateTimeParseException {

    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    LocalDate endLocalDate = LocalDate.parse(args[1]);

    File trades = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();

    PortfolioTrade[] tradeJsons = objectMapper.readValue(trades, PortfolioTrade[].class);

    for (int i = 0; i < tradeJsons.length; i++) {
      annualizedReturns.add(getAnnualizedReturn(tradeJsons[i], endLocalDate));
    }
    // Sort in Descending order

    Comparator<AnnualizedReturn> SortByAnnReturn =
        Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    Collections.sort(annualizedReturns, SortByAnnReturn);

    for(AnnualizedReturn d:annualizedReturns)
    {
      System.out.println(d.getAnnualizedReturn());
    }
    return annualizedReturns;
  }

  
  
  public static AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade, LocalDate endLocalDate) {

    String ticker = trade.getSymbol();
    LocalDate startLocalDate = trade.getPurchaseDate();

    if (startLocalDate.compareTo(endLocalDate) >= 0) {
      throw new RuntimeException();
    }

    // create a url object for the ap1 call
    // TOKEN is a class variable

    String token=TOKEN;
    String url =prepareUrl(trade, endLocalDate,token);
       
   
    RestTemplate restTemplate = new RestTemplate();

    // api returns a list of results for each day's closing details

    TiingoCandle[] stocksStartToEndDate = restTemplate.getForObject(url, TiingoCandle[].class);

    // Extract stocks for startDate & endDate

    if (stocksStartToEndDate != null) {
      TiingoCandle stockStartDate = stocksStartToEndDate[0];
      TiingoCandle stockLatest = stocksStartToEndDate[stocksStartToEndDate.length - 1];

      Double buyPrice = stockStartDate.getOpen();
      Double sellPrice = stockLatest.getClose();

      AnnualizedReturn annualizedReturn =
          calculateAnnualizedReturns(endLocalDate, trade, buyPrice, sellPrice);

      return annualizedReturn;

    }

    else {

      return new AnnualizedReturn(ticker, Double.NaN, Double.NaN);
    }
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Return the populated list of AnnualizedReturn for all stocks.
  // Annualized returns should be calculated in two steps:
  // 1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  // 1.1 Store the same as totalReturns
  // 2. Calculate extrapolated annualized returns by scaling the same in years span.
  // The formula is:
  // annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  // 2.1 Store the same as annualized_returns
  // Test the same using below specified command. The build should be successful.
  // ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {

    double totalReturn = (sellPrice - buyPrice) / buyPrice;
   

    Double numYears = (double) ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.24;
    

    Double annualizedReturns = Math.pow((1 + totalReturn), (1 / numYears)) - 1;
    

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturn);

  }



  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    
    return candles.get(0).getOpen();
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size()-1).getClose();
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
      RestTemplate rt = new RestTemplate();
      String sym = trade.getSymbol();
      String Url = prepareUrl(trade, endDate, TOKEN);
      TiingoCandle[] tc = rt.getForObject(Url, TiingoCandle[].class);
      
      return Arrays.asList(tc);
  }


  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.
  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    PortfolioTrade[] trades = getPortfolioTradesFromFile(args);
    RestTemplate rt = new RestTemplate();
    List<TotalReturnsDto> ls = new ArrayList<TotalReturnsDto>();
   
    for (PortfolioTrade pf : trades) {
      // LocalDate start = pf.getPurchaseDate();
      String sym = pf.getSymbol();
      LocalDate localDate = LocalDate.parse(args[1]);

      String Url = prepareUrl(pf, localDate, TOKEN);
      TiingoCandle[] tc = rt.getForObject(Url, TiingoCandle[].class);
      if (tc == null) {
        continue;
      } // candle helper object to sort symbols according to their current prices ->
      TotalReturnsDto temp = new TotalReturnsDto(sym, tc[tc.length - 1].getClose());
      ls.add(temp);
    }
    Collections.sort(ls, new Comparator<TotalReturnsDto>() {
      @Override
      public int compare(TotalReturnsDto p1, TotalReturnsDto p2) {
        return (int) (p1.getClosingPrice().compareTo(p2.getClosingPrice()));
      }
    });
    List<String> ans = new ArrayList<>();
    for (int i = 0; i < ls.size(); i++) {
      ans.add(ls.get(i).getSymbol());
    }
    return ans;
  }

  private static PortfolioTrade[] getPortfolioTradesFromFile(String[] args) throws URISyntaxException, StreamReadException, DatabindException, IOException {
    File f = resolveFileFromResources(args[0]);
    ObjectMapper om = getObjectMapper();
    PortfolioTrade[] trades = om.readValue(f, PortfolioTrade[].class);
    return trades;
  }

  // TODO:
  // After refactor, make sure that the tests pass by using these two commands
  // ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  // ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  public static List<PortfolioTrade> readTradesFromJson(String filename)
      throws IOException, URISyntaxException {
    File file = resolveFileFromResources(filename);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] trades = objectMapper.readValue(file, PortfolioTrade[].class);
    return Arrays.asList(trades);
  }



  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {

    File file = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] trades = objectMapper.readValue(file, PortfolioTrade[].class);
    List<String> symbols = new ArrayList<String>();
    for (PortfolioTrade t : trades)
    {
      symbols.add(t.getSymbol());
    }
     return symbols;
  }





  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI())
        .toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }



  // TODO:
  // Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {

    String url =
        "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices?" + "startDate="
            + trade.getPurchaseDate().toString() + "&endDate=" + endDate + "&token=" + token;
    return url;
  }


  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       //String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();
       PortfolioTrade[] portfolioTrades = objectMapper.readValue(file, PortfolioTrade[].class);
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }






















  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadFile(args));


    printJsonObject(mainReadQuotes(args));

    printJsonObject(mainCalculateSingleReturn(args));

    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }



  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 =
        "/home/crio-user/workspace/rajal10srivastava-ME_QMONEY_V2/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@5542c4ed";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplication.mainReadFile()";
    String lineNumberFromTestFileInStackTrace = "29:1";


    return Arrays.asList(
        new String[] {valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
            functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace});



  }

  public static String getToken() {
    return TOKEN;



    



  }
}

