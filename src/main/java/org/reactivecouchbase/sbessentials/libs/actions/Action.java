package org.reactivecouchbase.sbessentials.libs.actions;

import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.result.Result;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class Action {

    final ActionStep actionStep;
    final RequestContext rc;
    final Function<RequestContext, Future<Result>> block;
    final ExecutorService ec;

    Action(ActionStep actionStep, RequestContext rc, Function<RequestContext, Future<Result>> block, ExecutorService ec) {
        this.actionStep = actionStep;
        this.rc = rc;
        this.block = block;
        this.ec = ec;
    }

    Future<Result> run() {
        try {
            //return Future.async(() -> actionStep.innerInvoke(rc, block), ec).flatMap(e -> e, ec).recoverWith(t ->
            //Future.successful(ActionsHelperInternal.transformError(t, rc)), ec);
            Future<Result> result = actionStep.innerInvoke(rc, block);
            return result.recoverWith(t -> Future.successful(ActionsHelperInternal.transformError(t, rc)), ec);
        } catch (Exception e) {
            return Future.failed(e);
        }
    }

    public Action withExecutor(ExecutorService ec) {
        return new Action(actionStep, rc, block, ec);
    }

    public static Action sync(Function<RequestContext, Result> block) {
        return ActionsHelperInternal.EMPTY.sync(block);
    }

    public static Action async(Function<RequestContext, Future<Result>> block) {
        return ActionsHelperInternal.EMPTY.async(block);
    }

    public static Action async(ExecutorService ec, Function<RequestContext, Future<Result>> block) {
        return ActionsHelperInternal.EMPTY.async(ec, block);
    }
}
