package notyetnamed.example.currency_demo.services;

import notyetnamed.example.currency_demo.components.XmlHandler;
import notyetnamed.example.currency_demo.dto.ConversionDto;
import notyetnamed.example.currency_demo.dto.HistoryEntry;
import notyetnamed.example.currency_demo.dto.HistoryFilter;
import notyetnamed.example.currency_demo.entities.Conversion;
import notyetnamed.example.currency_demo.entities.Currency;
import notyetnamed.example.currency_demo.entities.CurrencyValue;
import notyetnamed.example.currency_demo.mappers.ConversionDtoMapper;
import notyetnamed.example.currency_demo.mappers.ConversionMapper;
import notyetnamed.example.currency_demo.repositories.ConversionRepository;
import notyetnamed.example.currency_demo.repositories.CurrencyRepository;
import notyetnamed.example.currency_demo.repositories.CurrencyValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CurrencyService {
    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private CurrencyValueRepository currencyValueRepository;

    @Autowired
    private ConversionRepository conversionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private XmlHandler xmlHandler;

    private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
    private SimpleDateFormat historyFilterFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Value("${currency.url}")
    private String url;

    public void getCourses(String dateString) throws IOException, ParserConfigurationException, SAXException {
        URL url = new URL(this.url + dateString);
        InputStream xmlStream = url.openStream();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser parser = factory.newSAXParser();
        parser.parse(xmlStream, xmlHandler);
    }

    public List<Currency> getCurrenciesList() {
        return currencyRepository.findAll().stream()
                .sorted(Comparator.comparing(Currency::getName)).collect(Collectors.toList());
    }

    public ConversionDto convert(ConversionDto conversionDto) throws ParserConfigurationException, SAXException, IOException {
        BigDecimal amount = conversionDto.getAmount();

        Currency currencyFrom = currencyRepository.findByNumCode(conversionDto.getCurrencyFromCode()).get();
        Currency currencyTo = currencyRepository.findByNumCode(conversionDto.getCurrencyToCode()).get();

        java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
        Optional<CurrencyValue> currencyFromOptional = currencyValueRepository.findByCurrencyIdAndDate(currencyFrom.getId(), date);
        Optional<CurrencyValue> currencyToOptional = currencyValueRepository.findByCurrencyIdAndDate(currencyTo.getId(), date);

        if (currencyFromOptional.isPresent() && currencyToOptional.isPresent()) {
            BigDecimal fromValue = currencyFrom.getNumCode().equals("643") ? new BigDecimal("1") : currencyFromOptional.get().getRubValue();
            BigDecimal toValue = currencyTo.getNumCode().equals("643") ? new BigDecimal("1") : currencyToOptional.get().getRubValue();

            BigDecimal result = amount.multiply(fromValue).multiply(BigDecimal.valueOf(currencyFrom.getNominal()))
                                .divide(toValue, 4, RoundingMode.HALF_UP)
                                .divide(BigDecimal.valueOf(currencyTo.getNominal()), 4, RoundingMode.HALF_UP).setScale(4);

            Conversion conversion = new Conversion();
            conversion.setCurrencyFrom(currencyFrom);
            conversion.setCurrencyTo(currencyTo);
            conversion.setAmount(amount);
            conversion.setResult(result);
            conversion.setDate(date);
            conversion.setUser(accountService.getCurrentUser());

            return ConversionDtoMapper.getMapping(conversionRepository.saveAndFlush(conversion));
        }

        getCourses(format.format(new Date()));

        currencyFromOptional = currencyValueRepository.findByCurrencyIdAndDate(currencyFrom.getId(), date);
        currencyToOptional = currencyValueRepository.findByCurrencyIdAndDate(currencyTo.getId(), date);

        BigDecimal fromValue = currencyFrom.getNumCode().equals("643") ? new BigDecimal("1") : currencyFromOptional.get().getRubValue();
        BigDecimal toValue = currencyTo.getNumCode().equals("643") ? new BigDecimal("1") : currencyToOptional.get().getRubValue();

        BigDecimal result = amount.multiply(fromValue).multiply(BigDecimal.valueOf(currencyFrom.getNominal()))
                                .divide(toValue, 4, RoundingMode.HALF_UP)
                                .divide(BigDecimal.valueOf(currencyTo.getNominal()), 4, RoundingMode.HALF_UP).setScale(4);

        Conversion conversion = new Conversion();
        conversion.setCurrencyFrom(currencyFrom);
        conversion.setCurrencyTo(currencyTo);
        conversion.setAmount(amount);
        conversion.setResult(result);
        conversion.setDate(date);
        conversion.setUser(accountService.getCurrentUser());

        return ConversionDtoMapper.getMapping(conversionRepository.saveAndFlush(conversion));
    }

    public List<HistoryEntry> getHistory() {
        return conversionRepository.findByUserId(accountService.getCurrentUser().getId())
                .stream()
                .sorted(Comparator.comparing(Conversion::getId).reversed())
                .map(ConversionMapper::getMapping).collect(Collectors.toList());
    }

    public List<HistoryEntry> getHistory(HistoryFilter filter) throws ParseException {
        int currentUserId = accountService.getCurrentUser().getId();
        String currencyFromCode = filter.getCurrencyFromCode();
        String currencyToCode = filter.getCurrencyToCode();

        if (filter.getDate().length() != 0) {
            java.sql.Date parsedDate = new java.sql.Date(historyFilterFormat.parse(filter.getDate()).getTime());
            if (currencyFromCode.length() == 3 && currencyToCode.length() == 3) {
                Currency currencyFrom = currencyRepository.findByNumCode(currencyFromCode).get();
                Currency currencyTo = currencyRepository.findByNumCode(currencyToCode).get();

                return conversionRepository.findByCurrencyFromAndCurrencyToAndDateAndUserId(currencyFrom,
                        currencyTo, parsedDate, currentUserId)
                        .stream()
                        .sorted(Comparator.comparing(Conversion::getId).reversed())
                        .map(ConversionMapper::getMapping).collect(Collectors.toList());
            }
            if (currencyFromCode.length() == 0 && currencyToCode.length() == 3) {
                Currency currencyTo = currencyRepository.findByNumCode(currencyToCode).get();

                return conversionRepository.findByCurrencyToAndDateAndUserId(currencyTo, parsedDate, currentUserId)
                        .stream()
                        .sorted(Comparator.comparing(Conversion::getId).reversed())
                        .map(ConversionMapper::getMapping).collect(Collectors.toList());
            }
            if (currencyFromCode.length() == 3 && currencyToCode.length() == 0) {
                Currency currencyFrom = currencyRepository.findByNumCode(currencyFromCode).get();

                return conversionRepository.findByCurrencyFromAndDateAndUserId(currencyFrom, parsedDate, currentUserId)
                        .stream()
                        .sorted(Comparator.comparing(Conversion::getId).reversed())
                        .map(ConversionMapper::getMapping).collect(Collectors.toList());
            }
            return conversionRepository.findByDateAndUserId(parsedDate, currentUserId).stream()
                    .sorted(Comparator.comparing(Conversion::getId).reversed())
                    .map(ConversionMapper::getMapping).collect(Collectors.toList());
        }
        if (currencyFromCode.length() == 3 && currencyToCode.length() == 3) {
            Currency currencyFrom = currencyRepository.findByNumCode(currencyFromCode).get();
            Currency currencyTo = currencyRepository.findByNumCode(currencyToCode).get();

            return conversionRepository.findByCurrencyFromAndCurrencyToAndUserId(currencyFrom, currencyTo, currentUserId)
                    .stream()
                    .sorted(Comparator.comparing(Conversion::getId).reversed())
                    .map(ConversionMapper::getMapping).collect(Collectors.toList());
        }
        if (currencyFromCode.length() == 0 && currencyToCode.length() == 3) {
            Currency currencyTo = currencyRepository.findByNumCode(currencyToCode).get();

            return conversionRepository.findByCurrencyToAndUserId(currencyTo, currentUserId)
                    .stream()
                    .sorted(Comparator.comparing(Conversion::getId).reversed())
                    .map(ConversionMapper::getMapping).collect(Collectors.toList());
        }
        if (currencyFromCode.length() == 3 && currencyToCode.length() == 0) {
            Currency currencyFrom = currencyRepository.findByNumCode(currencyFromCode).get();

            return conversionRepository.findByCurrencyFromAndUserId(currencyFrom, currentUserId)
                    .stream()
                    .sorted(Comparator.comparing(Conversion::getId).reversed())
                    .map(ConversionMapper::getMapping).collect(Collectors.toList());
        }
        return getHistory();
    }
}
