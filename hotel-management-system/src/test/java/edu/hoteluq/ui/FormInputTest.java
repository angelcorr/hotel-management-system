package edu.hoteluq.ui;
import edu.hoteluq.exceptions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class FormInputTest {
 @ParameterizedTest @ValueSource(strings={""," ","1,50","1.000,00","NaN","Infinity","1e3"}) void invalidMonetaryText(String value){assertThrows(HotelException.class,()->FormInput.decimal(value,"COP"));}
 @Test void exactDecimalParsing(){assertEquals(new BigDecimal("100.50"),FormInput.decimal("100.50","COP"));}
 @ParameterizedTest @ValueSource(strings={"1.5","abc","2147483648"}) void invalidInteger(String value){assertThrows(HotelException.class,()->FormInput.integer(value,"número"));}
 @Test void timeAndBogotaInstant(){assertEquals(LocalTime.of(15,30),FormInput.time("15:30","hora"));assertEquals(Instant.parse("2027-01-01T05:00:00Z"),FormInput.instant("2027-01-01T00:00"));assertNull(FormInput.instant(""));}
 @Test void invalidTimeAndIdentifiers(){assertThrows(HotelException.class,()->FormInput.time("25:00","hora"));assertThrows(HotelException.class,()->FormInput.instant("mañana"));assertThrows(HotelException.class,()->FormInput.ids("not-a-uuid"));}
 @Test void exceptionMessagesAreCentralizedAndDoNotExposeUnexpectedInternals(){assertTrue(UiText.error(new AuthenticationException("secret")).contains("Sesión"));assertTrue(UiText.error(new AuthorizationException()).contains("permiso"));assertFalse(UiText.error(new IllegalStateException("secret")).contains("secret"));}
 @Test void moneyAndTimePresentationAreExplicit(){assertTrue(UiText.cop(new BigDecimal("100.50")).contains("100,5"));assertTrue(UiText.cop(BigDecimal.ONE).endsWith("COP"));assertTrue(UiText.at(Instant.parse("2027-01-01T05:00:00Z")).contains("01/01/2027 00:00:00"));}
}
