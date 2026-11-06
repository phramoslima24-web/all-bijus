package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.AllBijuuMod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

/** Agendador simples de tarefas do servidor (thread do servidor apenas). */
public final class ServerTasks {
    private static final class Task {
        final long at;
        final Runnable run;

        Task(long at, Runnable run) {
            this.at = at;
            this.run = run;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now;

    private ServerTasks() {
    }

    public static void after(int ticks, Runnable r) {
        TASKS.add(new Task(now + Math.max(0, ticks), r));
    }

    public static void repeat(int delay, int interval, int times, IntConsumer body) {
        for (int i = 0; i < times; i++) {
            final int idx = i;
            after(delay + interval * i, () -> body.accept(idx));
        }
    }

    public static void tick() {
        now++;
        if (TASKS.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.at <= now) {
                due.add(t.run);
                it.remove();
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (Throwable t) {
                AllBijuuMod.LOGGER.error("Erro em tarefa agendada", t);
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }
}
