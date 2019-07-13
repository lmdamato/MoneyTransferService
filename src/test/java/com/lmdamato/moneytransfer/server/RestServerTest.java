package com.lmdamato.moneytransfer.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmdamato.moneytransfer.handler.MoneyTransferHandler;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.vavr.control.Option;
import lombok.NonNull;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestServerTest {
    private static final HttpHandler ROOT = new RoutingHandler()
        .put("/create/{userId}", MoneyTransferHandler::createUserHandler)
        .get("/balance/{userId}", MoneyTransferHandler::getBalanceHandler)
        .post("/deposit/{userId}/{amount}", MoneyTransferHandler::depositHandler)
        .post("/withdraw/{userId}/{amount}", MoneyTransferHandler::withdrawHandler)
        .post("/transfer/{from}/{to}/{amount}", MoneyTransferHandler::transferHandler)
        .setFallbackHandler(ResponseCodeHandler.HANDLE_404);

    private static final int PORT = 8080;
    private static final String ENDPOINT = "http://localhost:" + PORT;

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String AMOUNT = "amount";

    private Undertow server;
    private CloseableHttpClient client;

    @Before
    public void setup() {
        server = Undertow
            .builder()
            .addHttpListener(PORT, "0.0.0.0", ROOT)
            .build();

        server.start();

        client = HttpClientBuilder.create().build();
    }

    @After
    public void dispose() throws IOException {
        client.close();
        server.stop();
    }

    @Test
    public void givenAUserDoesNotExist_WhenAUserIsCreated_then201IsReturned() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();
        final HttpUriRequest createUserRequest = new HttpPut(ENDPOINT + "/create/" + id);

        // When
        try (final CloseableHttpResponse httpResponse = client.execute(createUserRequest)) {
            // Then
            assertEquals(httpResponse.getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);
        }
    }

    @Test
    public void givenAUserExists_WhenTheSameUserIsCreated_then204IsReturned() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();
        final HttpUriRequest createUserRequest = new HttpPut(ENDPOINT + "/create/" + id);

        client.execute(createUserRequest).close();

        // When
        try (final CloseableHttpResponse secondCreate = client.execute(createUserRequest)) {
            // Then
            assertEquals(secondCreate.getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT);
        }
    }

    @Test
    public void givenAUserExists_WhenBalanceIsRetrieved_thenTheRequestSucceeds() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();
        createUser(id);

        // When

        // Then
        checkBalance(id, 0.0);
    }

    @Test
    public void givenAUserDoesNotExist_WhenBalanceIsRetrieved_then404IsReturned() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();

        // When
        final HttpUriRequest getBalanceRequest = new HttpGet(ENDPOINT + "/balance/" + id);

        try (final CloseableHttpResponse response = client.execute(getBalanceRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test
    public void givenAUserExists_WhenValueIsDeposited_thenTheRequestSucceeds() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();
        createUser(id);

        // When
        final double amount = 12.34;
        final HttpUriRequest depositRequest = new HttpPost(ENDPOINT + "/deposit/" + id + "/" + amount);

        try (final CloseableHttpResponse response = client.execute(depositRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT);
        }

        checkBalance(id, amount);
    }

    @Test
    public void givenAUserDoesNotExist_WhenValueIsDeposited_then404IsReturned() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();

        // When
        final String amount = "12.34";
        final HttpUriRequest depositRequest = new HttpPost(ENDPOINT + "/deposit/" + id + "/" + amount);

        try (final CloseableHttpResponse response = client.execute(depositRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test
    public void givenAUserExists_WhenNegativeValueIsDeposited_then400IsReturned() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();
        createUser(id);

        // When
        final String amount = "-12.34";
        final HttpUriRequest depositRequest = new HttpPost(ENDPOINT + "/deposit/" + id + "/" + amount);

        try (final CloseableHttpResponse response = client.execute(depositRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test
    public void givenAUserWithEnoughFundsExists_WhenValueIsWithdrawn_then204IsReturned() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();
        createUser(id);

        final String amountToDeposit = "12.34";
        deposit(id, amountToDeposit);

        // When
        final String amountToWithdraw = "2.01";
        final HttpUriRequest withdrawRequest = new HttpPost(ENDPOINT + "/withdraw/" + id + "/" + amountToWithdraw);

        try (final CloseableHttpResponse response = client.execute(withdrawRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT);
        }

        checkBalance(id, 10.33);
    }

    @Test
    public void givenAUserWithNotEnoughFundsExists_WhenValueIsWithdrawn_then403IsReturned() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();
        createUser(id);

        // When
        final String amountToWithdraw = "2.01";
        final HttpUriRequest withdrawRequest = new HttpPost(ENDPOINT + "/withdraw/" + id + "/" + amountToWithdraw);

        try (final CloseableHttpResponse response = client.execute(withdrawRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_FORBIDDEN);
        }
    }

    @Test
    public void givenAUserDoesNotExist_WhenValueIsWithdrawn_then404IsReturned() throws IOException {
        // Given
        final String id = UUID.randomUUID().toString();

        // When
        final String amountToWithdraw = "2.01";
        final HttpUriRequest withdrawRequest = new HttpPost(ENDPOINT + "/withdraw/" + id + "/" + amountToWithdraw);

        try (final CloseableHttpResponse response = client.execute(withdrawRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test
    public void givenTwoUsersWithEnoughFundsExist_WhenValueIsTransferred_then204IsReturned() throws IOException {
        // Given
        final String id1 = UUID.randomUUID().toString();
        final String id2 = UUID.randomUUID().toString();

        createUser(id1);
        createUser(id2);

        final String amountToDeposit = "12.34";
        deposit(id1, amountToDeposit);

        // When
        final String amountToTransfer = "2.35";
        final HttpUriRequest transferRequest = new HttpPost(
            ENDPOINT + "/transfer/" + id1 + "/" + id2 + "/" + amountToTransfer
        );

        try (final CloseableHttpResponse response = client.execute(transferRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT);
        }

        checkBalance(id1, 9.99);
        checkBalance(id2, 2.35);
    }

    @Test
    public void givenOnlyOneUserExists_WhenValueIsTransferred_then404IsReturned() throws IOException {
        // Given
        final String id1 = UUID.randomUUID().toString();
        final String id2 = UUID.randomUUID().toString();

        createUser(id1);

        final String amountToDeposit = "12.34";
        deposit(id1, amountToDeposit);

        // When
        final String amountToTransfer = "2.35";
        final HttpUriRequest transferRequest1 = new HttpPost(
            ENDPOINT + "/transfer/" + id1 + "/" + id2 + "/" + amountToTransfer
        );

        try (final CloseableHttpResponse response = client.execute(transferRequest1)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND);
        }

        final HttpUriRequest transferRequest2 = new HttpPost(
            ENDPOINT + "/transfer/" + id2 + "/" + id1 + "/" + amountToTransfer
        );

        try (final CloseableHttpResponse response = client.execute(transferRequest2)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND);
        }

        checkBalance(id1, 12.34);
    }

    @Test
    public void givenTwoUsersExist_WhenNegativeValueIsTransferred_then400IsReturned() throws IOException {
        // Given
        final String id1 = UUID.randomUUID().toString();
        final String id2 = UUID.randomUUID().toString();

        createUser(id1);
        createUser(id2);

        final String amountToDeposit = "12.34";
        deposit(id1, amountToDeposit);

        // When
        final String amountToTransfer = "-2.35";
        final HttpUriRequest transferRequest1 = new HttpPost(
            ENDPOINT + "/transfer/" + id1 + "/" + id2 + "/" + amountToTransfer
        );

        try (final CloseableHttpResponse response = client.execute(transferRequest1)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_BAD_REQUEST);
        }

        checkBalance(id1, 12.34);
        checkBalance(id2, 0.0);
    }

    @Test
    public void givenOneUserHasNotEnoughFunds_WhenValueIsTransferred_then403IsReturned() throws IOException {
        // Given
        final String id1 = UUID.randomUUID().toString();
        final String id2 = UUID.randomUUID().toString();

        createUser(id1);
        createUser(id2);

        // When
        final String amountToTransfer = "2.35";
        final HttpUriRequest transferRequest = new HttpPost(
            ENDPOINT + "/transfer/" + id1 + "/" + id2 + "/" + amountToTransfer
        );

        try (final CloseableHttpResponse response = client.execute(transferRequest)) {
            // Then
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_FORBIDDEN);
        }
    }

    @SuppressWarnings("unchecked")
    private Option<Double> getAmountFromJsonString(@NonNull final String jsonString) throws IOException {
        final Map<String, Double> map = mapper.readValue(jsonString, Map.class);

        return Option.of(map.get(AMOUNT));
    }

    private String inputStreamToString(@NonNull final InputStream in) throws IOException {
        final StringBuilder out = new StringBuilder();

        try (final Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            int c;
            while ((c = reader.read()) != -1) {
                out.append((char) c);
            }
        }

        return out.toString();
    }

    private void createUser(@NonNull final String userId) throws IOException {
        final HttpUriRequest createUserRequest = new HttpPut(ENDPOINT + "/create/" + userId);

        client.execute(createUserRequest).close();
    }

    private void deposit(@NonNull final String userId, @NonNull final String amount) throws IOException {
        final HttpUriRequest createUserRequest = new HttpPost(ENDPOINT + "/deposit/" + userId + "/" + amount);

        client.execute(createUserRequest).close();
    }

    private void checkBalance(@NonNull final String userId, @NonNull final double expected) throws IOException {
        final HttpUriRequest getBalanceRequest = new HttpGet(ENDPOINT + "/balance/" + userId);
        try (final CloseableHttpResponse response = client.execute(getBalanceRequest)) {
            final String responseBody = inputStreamToString(response.getEntity().getContent());
            final Option<Double> returnedAmount = getAmountFromJsonString(responseBody);

            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
            assertTrue(returnedAmount.isDefined());
            assertEquals(expected, returnedAmount.get(), 0);
        }
    }
}
