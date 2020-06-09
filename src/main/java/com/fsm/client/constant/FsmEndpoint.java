package com.fsm.client.constant;

public enum FsmEndpoint {
    CREATE("/rest/v1/fsm/create"),
    DESTROY("/rest/v1/fsm/{transaction_id}"),
    SET_DATA("/rest/v1/fsm/{transaction_id}/current/data"),
    MOVE_NEXT_STATE("/rest/v1/fsm/{transaction_id}/next/{eventName}"),
    FAIL("/rest/v1/fsm/{transactionId}/state/fail");

    private String context;

    FsmEndpoint(String context) {
        this.context = context;
    }

    public String getContext() {
        return context;
    }
}
