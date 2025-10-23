package com.example.store.aop.performance.context;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

// Thread-local context holder
@Component
public class SqlPerformanceContextHolder {

    private final ThreadLocal<Deque<SqlPerformanceContext>> contextStack = ThreadLocal.withInitial(ArrayDeque::new);

    public void push(SqlPerformanceContext context) {
        contextStack.get().push(context);
    }

    public SqlPerformanceContext pop() {
        return contextStack.get().poll();
    }

    public SqlPerformanceContext peek() {
        return contextStack.get().peek();
    }

    public void clear() {
        contextStack.remove();
    }
}