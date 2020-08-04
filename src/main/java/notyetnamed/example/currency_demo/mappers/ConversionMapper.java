package notyetnamed.example.currency_demo.mappers;

import notyetnamed.example.currency_demo.entities.Conversion;
import notyetnamed.example.currency_demo.entities.Currency;
import notyetnamed.example.currency_demo.dto.HistoryEntry;

public class ConversionMapper {
    private ConversionMapper() {}

    public static HistoryEntry getMapping(Conversion conversion) {
        HistoryEntry result = new HistoryEntry();

        Currency currencyFrom = conversion.getCurrencyFrom();
        Currency currencyTo = conversion.getCurrencyTo();

        result.setCurrencyFrom("(" + currencyFrom.getNumCode() + ") " + currencyFrom.getName());
        result.setCurrencyTo("(" + currencyTo.getNumCode() + ") " + currencyTo.getName());
        result.setAmount(conversion.getAmount());
        result.setResult(conversion.getResult());
        result.setDate(conversion.getDate());

        return result;
    }
}
