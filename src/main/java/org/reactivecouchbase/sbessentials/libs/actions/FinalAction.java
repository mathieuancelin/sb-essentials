package org.reactivecouchbase.sbessentials.libs.actions;

import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.concurrent.Future;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class FinalAction {

    private final Action action;
    private final RequestContext rc;
    private final Function<RequestContext, Future<Result>> block;

    FinalAction(Action action, RequestContext rc, Function<RequestContext, Future<Result>> block) {
        this.action = action;
        this.rc = rc;
        this.block = block;
    }

    Future<Result> run(ExecutorService ec) {
        return Future.async(() -> action.innerInvoke(rc, block), ec).flatMap(e -> e, ec).recoverWith(t ->
                Future.successful(Actions.transformError(t, rc))
        , ec);
    }

    public static FinalAction sync(Function<RequestContext, Result> block) {
        return Actions.EMPTY.sync(block);
    }

    public static FinalAction async(Function<RequestContext, Future<Result>> block) {
        return Actions.EMPTY.async(block);
    }
}
