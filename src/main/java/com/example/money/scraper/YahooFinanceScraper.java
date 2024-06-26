package com.example.money.scraper;

import com.example.money.model.Company;
import com.example.money.model.Dividend;
import com.example.money.model.ScrapedResult;
import com.example.money.model.constants.Month;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class YahooFinanceScraper implements Scraper {

    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history?period1=%d&period2=%d&interval=1mo";
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";
    private static final long START_TIME = 86400; // 60 * 60 * 24
    @Override
    public ScrapedResult scrap(Company company) {
        var scrapResult = new ScrapedResult();
        scrapResult.setCompany(company);
        try {
            long end = System.currentTimeMillis() / 1000;
            String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, end);
            Connection connection = Jsoup.connect(url);
            Document document = connection.get();

            Elements parsingDivs = document.getElementsByAttributeValue("data-test", "historical-prices");
            Element tableEle = parsingDivs.get(0);  // 테이블 전체

            Element tbody = null;
            if (tableEle.children().size() > 1) {
                tbody = tableEle.children().get(1);
            }

            List<Dividend> dividends = new ArrayList<>();
            if (tbody != null) {
                for(Element e : tbody.children()) {
                    String txt = e.text();
                    if(!txt.endsWith("Dividend")) {
                        continue;
                    }
                    String[] splits = txt.split(" ");
                    int month = Month.strToNumber(splits[0]);
                    int day = Integer.parseInt(splits[1].replace(",", ""));
                    int year = Integer.parseInt(splits[2]);
                    String dividend = splits[3];
                    if(month < 0) {
                        throw new RuntimeException("Unexpected Month enum value -> " + splits[0]);
                    }

                    dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));
    //                        Dividend.builder()
    //                        .date(LocalDateTime.of(year, month, day, 0, 0))
    //                        .dividend(dividend)
    //                        .build());
                }
            }
            scrapResult.setDividends(dividends);
        } catch (IOException e) {
            // TODO
            throw new RuntimeException(e);
        }
        return scrapResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            Document document = Jsoup.connect(url).get();
            Element titleEle = document.getElementsByTag("h1").get(0);
            String title = titleEle.text();
            int index = title.indexOf(" - ");
            if (index != -1) {
                title = titleEle.text().split(" - ")[1].trim();
            } else {
                title = titleEle.text().trim();
            }
//            String title = titleEle.text().split(" - ")[1].trim();
            return new Company(ticker,title);
//            return Company.builder()
//                    .ticker(ticker)
//                    .name(title)
//                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
