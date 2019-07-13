package com.lmdamato.moneytransfer.server;

import com.lmdamato.moneytransfer.handler.MoneyTransferHandler;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.ResponseCodeHandler;

public class RestServer {
    private static final HttpHandler ROOT = new RoutingHandler()
        .put("/create/{userId}", MoneyTransferHandler::createUserHandler)
        .get("/balance/{userId}", MoneyTransferHandler::getBalanceHandler)
        .post("/deposit/{userId}/{amount}", MoneyTransferHandler::depositHandler)
        .post("/withdraw/{userId}/{amount}", MoneyTransferHandler::withdrawHandler)
        .post("/transfer/{from}/{to}/{amount}", MoneyTransferHandler::transferHandler)
        .setFallbackHandler(ResponseCodeHandler.HANDLE_404);

    public static void main(String[] args) {
        final Undertow ut = Undertow
            .builder()
            .addHttpListener(8080, "0.0.0.0", ROOT)
            .build();

        ut.start();
    }
}
