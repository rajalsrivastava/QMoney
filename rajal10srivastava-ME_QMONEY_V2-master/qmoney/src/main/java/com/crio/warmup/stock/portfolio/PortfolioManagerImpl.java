
package com.crio.warmup.stock.portfolio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  private StockQuotesService stockQuotesService;

  PortfolioManagerImpl(StockQuotesService stockQuotesService){
    this.stockQuotesService = stockQuotesService;
  }



  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF
  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
  LocalDate endDate){

    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> annualizedReturns=new ArrayList<AnnualizedReturn>();

    // loop through each of the Portfolio trade objects
    for(int i=0;i<portfolioTrades.size();i++)
    {
      // get annualized return objects for each

      annualizedReturn = getAnnualizedReturn(portfolioTrades.get(i), endDate);

      // add those as list
      annualizedReturns.add(annualizedReturn);
    }
      
    Comparator<AnnualizedReturn> SortByAnnReturn = getComparator();
    
    Collections.sort(annualizedReturns,SortByAnnReturn);

    return annualizedReturns;
  }

  public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade,LocalDate endLocalDate){
    
    AnnualizedReturn annualizedReturn;
    String symbol=trade.getSymbol();
    LocalDate startLocalDate = trade.getPurchaseDate();

    try{
      // Fetch data
      List<Candle> stocksStartToEndDate;

      stocksStartToEndDate=getStockQuote(symbol, startLocalDate, endLocalDate);

      // Extract stocks for startDate and endDate
      Candle stockStartDate = stocksStartToEndDate.get(0);
      Candle stockLatest = stocksStartToEndDate.get(stocksStartToEndDate.size()-1);

      Double buyPrice = stockStartDate.getOpen();
      Double sellPrie = stockLatest.getClose();

      // calculate total returns
      Double totalReturn = (sellPrie - buyPrice) / buyPrice;

      //calculate years
      Double numYears = (double) ChronoUnit.DAYS.between(startLocalDate,endLocalDate) / 365;

      // calculate annualized returns using formula
      Double annualizedReturns = Math.pow((1+totalReturn),(1/numYears))-1;

      annualizedReturn = new AnnualizedReturn(symbol, annualizedReturns, totalReturn);
      
    }catch(JsonProcessingException e){
      annualizedReturn = new AnnualizedReturn(symbol, Double.NaN, Double.NaN);
    }
    
    return annualizedReturn;

  }






  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate startLocalDate, LocalDate endLocalDate)
      throws JsonProcessingException {
      // get stocks available from the start date to the end date
      // startDate - purchase date
      // throw error if start date not before end date

      // if(from.compareTo(to) >= 0){
      //  throw new RuntimeException();
      // }

      // // create a url object for the api call
      // String url = buildUri(symbol, from, to);

      // // api returns a list of results for each day's stock data
      // TiingoCandle[] stocksStartToEndDate = restTemplate.getForObject(url,TiingoCandle[].class);

      // if (stocksStartToEndDate == null) {
      //   return new ArrayList<Candle>();
      // } 
      // else {
      //   List<Candle> stock = Arrays.asList(stocksStartToEndDate);
      //   return stock;
      // }

      return stockQuotesService.getStockQuote(symbol , startLocalDate, endLocalDate);
      
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {

    String token="6ca592aa23678f618d24d7fd977c56c3a7d62d87";


       String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
            + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

      String url = uriTemplate.replace("$APIKEY", token).replace("$SYMBOL", symbol)
      .replace("STARTDATE", startDate.toString())
      .replace("ENDDATE", endDate.toString());

      return url;
  
   }




  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.

}
