package org.reactivecouchbase.sbessentials.libs.actions;

import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.concurrent.Future;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class Action {

    private final ActionStep actionStep;
    private final RequestContext rc;
    private final Function<RequestContext, Future<Result>> block;

    Action(ActionStep actionStep, RequestContext rc, Function<RequestContext, Future<Result>> block) {
        this.actionStep = actionStep;
        this.rc = rc;
        this.block = block;
    }

    Future<Result> run(ExecutorService ec) {
        return Future.async(() -> actionStep.innerInvoke(rc, block), ec).flatMap(e -> e, ec).recoverWith(t ->
                Future.successful(ActionsHelperInternal.transformError(t, rc))
        , ec);
    }

    public static Action sync(Function<RequestContext, Result> block) {
        return ActionsHelperInternal.EMPTY.sync(block);
    }

    public static Action async(Function<RequestContext, Future<Result>> block) {
        return ActionsHelperInternal.EMPTY.async(block);
    }
}
