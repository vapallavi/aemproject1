package com.adobe.aem.guides.demo.core.servlets;

import java.io.IOException;
import java.security.SecureRandom;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

/**
 * Path-based servlet that returns six random characters.
 *
 * <pre>
 * GET /bin/demo/random?type=Number   -&gt; 6 random digits
 * GET /bin/demo/random?type=Letters  -&gt; 6 random letters
 * GET /bin/demo/random?type=Random   -&gt; 3 digits + 3 letters (e.g. 345IJH)
 * </pre>
 *
 * The same modes are accepted as parameter names:
 * {@code ?Number}, {@code ?Letters}, {@code ?Random}.
 */
@Component(service = { Servlet.class })
@SlingServletPaths(value = "/bin/demo/random")
@ServiceDescription("Path based random number and letter servlet")
public class RandomGeneratorServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    static final String PATH = "/bin/demo/random";
    static final String PARAM_TYPE = "type";
    static final String MODE_NUMBER = "Number";
    static final String MODE_LETTERS = "Letters";
    static final String MODE_RANDOM = "Random";

    private static final String DIGITS = "0123456789";
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int TOKEN_LENGTH = 6;
    private static final int MIXED_HALF = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain");

        String mode = resolveMode(request);
        if (mode == null) {
            response.setStatus(400);
            response.getWriter().write(
                    "Pass type=Number, type=Letters, or type=Random (example: "
                            + PATH + "?type=Number)");
            return;
        }

        response.setHeader("Allow", HttpConstants.METHOD_GET);
        response.getWriter().write(generate(mode));
    }

    String generate(String mode) {
        if (MODE_NUMBER.equalsIgnoreCase(mode)) {
            return randomChars(DIGITS, TOKEN_LENGTH);
        }
        if (MODE_LETTERS.equalsIgnoreCase(mode)) {
            return randomChars(LETTERS, TOKEN_LENGTH);
        }
        return randomChars(DIGITS, MIXED_HALF) + randomChars(LETTERS, MIXED_HALF);
    }

    String resolveMode(SlingHttpServletRequest request) {
        String type = request.getParameter(PARAM_TYPE);
        if (isBlank(type)) {
            type = request.getParameter("Type");
        }
        if (!isBlank(type)) {
            return normalizeMode(type);
        }
        if (request.getParameter(MODE_NUMBER) != null) {
            return MODE_NUMBER;
        }
        if (request.getParameter(MODE_LETTERS) != null) {
            return MODE_LETTERS;
        }
        if (request.getParameter(MODE_RANDOM) != null) {
            return MODE_RANDOM;
        }
        return null;
    }

    private String normalizeMode(String value) {
        if (MODE_NUMBER.equalsIgnoreCase(value)) {
            return MODE_NUMBER;
        }
        if (MODE_LETTERS.equalsIgnoreCase(value)) {
            return MODE_LETTERS;
        }
        if (MODE_RANDOM.equalsIgnoreCase(value)) {
            return MODE_RANDOM;
        }
        return null;
    }

    private String randomChars(String alphabet, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
