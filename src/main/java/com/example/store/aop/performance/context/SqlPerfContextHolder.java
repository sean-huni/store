package com.example.store.aop.performance.context;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

// Thread-local context holder
@Component
public class SqlPerfContextHolder {

    private final ThreadLocal<Deque<SqlPerfContext>> contextStack = ThreadLocal.withInitial(ArrayDeque::new);

    public void push(SqlPerfContext context) {
        contextStack.get().push(context);
    }

    public SqlPerfContext pop() {
        return contextStack.get().poll();
    }

    public SqlPerfContext peek() {
        return contextStack.get().peek();
    }

    public void clear() {
        contextStack.remove();
    }
}