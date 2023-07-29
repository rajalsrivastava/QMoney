
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  public static final String TOKEN="b2aa9f126dcee21d707aadf68430de853d958c4b";
  private RestTemplate restTemplate;
  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException{

        List<Candle> stocksStartToEndDate;

        //throw eror if start date not before end date
        if(from.compareTo(to) >= 0){
          throw new RuntimeException();
         }
         
         //create a url object for the api call
         String url = buildUri(symbol, from, to);
         
         String stocks=restTemplate.getForObject(url, String.class);
         ObjectMapper objectMapper=getObjectMapper();
   
         TiingoCandle[] stocksStartToEndDateArray = objectMapper.readValue(stocks,TiingoCandle[].class);
   
         if (stocksStartToEndDateArray != null) {
           stocksStartToEndDate = Arrays.asList(stocksStartToEndDateArray);
         } 
         else {
           stocksStartToEndDate = Arrays.asList(new TiingoCandle[0]);
           
         }

         return stocksStartToEndDate;
      }
  

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.
  
  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {

       String uriTemplate = String.format("https://api.tiingo.com/tiingo/daily/%s/prices?"
       + "startDate=%s&endDate=%s&token=%s", symbol, startDate, endDate, TOKEN);

     
      return uriTemplate;
  
   }

}
