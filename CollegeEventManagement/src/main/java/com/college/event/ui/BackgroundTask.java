package com.college.event.ui;

import com.college.event.service.ServiceException;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Runs slow work (database calls) on a background thread, then hands the result
 * back to the Swing thread. This keeps the window from freezing.
 *
 * Swing rule: only the Event Dispatch Thread may touch components. SwingWorker's
 * doInBackground() runs on another thread, and done() runs on the Swing thread.
 *
 * The three parameters are functional interfaces, so callers pass lambdas:
 *   Callable  - the work to do, returns a result (may throw an exception)
 *   Consumer  - what to do with the result
 *   Runnable  - something to do at the end, success or failure
 */
public final class BackgroundTask {

    private BackgroundTask() {
    }

    public static <T> void run(Component parent, Callable<T> work, Consumer<T> onSuccess) {
        run(parent, work, onSuccess, () -> { });
    }

    public static <T> void run(Component parent, Callable<T> work,
                               Consumer<T> onSuccess, Runnable onFinish) {

        final Window window = (parent instanceof Window)
                ? (Window) parent
                : (parent == null ? null : SwingUtilities.getWindowAncestor(parent));
        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                if (window != null) {
                    window.setCursor(Cursor.getDefaultCursor());
                }
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    String message = (cause instanceof ServiceException)
                            ? cause.getMessage()
                            : "Unexpected error: " + cause;
                    UITheme.showError(parent, message);
                } finally {
                    onFinish.run();
                }
            }
        };
        worker.execute();
    }
}
