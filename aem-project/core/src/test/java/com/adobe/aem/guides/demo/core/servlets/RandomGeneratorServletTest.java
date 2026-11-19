package com.adobe.aem.guides.demo.core.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class RandomGeneratorServletTest {

    private final RandomGeneratorServlet fixture = new RandomGeneratorServlet();

    @Test
    void numberReturnsSixDigits(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("type", "Number");

        fixture.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertEquals(200, context.response().getStatus());
        assertEquals(6, output.length());
        assertTrue(output.chars().allMatch(Character::isDigit), output);
    }

    @Test
    void lettersReturnsSixLetters(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("type", "Letters");

        fixture.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertEquals(200, context.response().getStatus());
        assertEquals(6, output.length());
        assertTrue(output.chars().allMatch(Character::isLetter), output);
    }

    @Test
    void randomReturnsThreeDigitsThenThreeLetters(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("type", "Random");

        fixture.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertEquals(200, context.response().getStatus());
        assertEquals(6, output.length());
        assertTrue(output.substring(0, 3).chars().allMatch(Character::isDigit), output);
        assertTrue(output.substring(3).chars().allMatch(Character::isLetter), output);
    }

    @Test
    void namedNumberParameterIsAccepted(AemContext context) throws ServletException, IOException {
        context.request().addRequestParameter("Number", "");

        fixture.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertEquals(6, output.length());
        assertTrue(output.chars().allMatch(Character::isDigit), output);
    }

    @Test
    void missingParameterReturnsBadRequest(AemContext context) throws ServletException, IOException {
        fixture.doGet(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("type=Number"));
    }
}
