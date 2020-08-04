package notyetnamed.example.currency_demo.mappers;

import notyetnamed.example.currency_demo.dto.ConversionDto;
import notyetnamed.example.currency_demo.entities.Conversion;

public class ConversionDtoMapper {
    private ConversionDtoMapper() {}

    public static ConversionDto getMapping(Conversion conversion) {
        ConversionDto result = new ConversionDto();

        result.setCurrencyFromCode(conversion.getCurrencyFrom().getNumCode());
        result.setCurrencyToCode(conversion.getCurrencyTo().getNumCode());
        result.setAmount(conversion.getAmount());
        result.setResult(conversion.getResult());
        result.setDate(conversion.getDate());

        return result;
    }
}
