
package com.sun.sgs.impl.kernel.schedule;

import com.sun.sgs.app.TaskRejectedException;

import com.sun.sgs.kernel.KernelAppContext;
import com.sun.sgs.kernel.RecurringTaskHandle;
import com.sun.sgs.kernel.TaskReservation;

import java.util.Properties;


/**
 * This interface is used to define top-level schedulers which are used
 * directly by the <code>MasterScheduler</code>. All implementations must
 * implement a constructor of the form <code>(java.util.Properties)</code>.
 *
 * @since 1.0
 * @author Seth Proctor
 */
interface SystemScheduler {

    /**
     * Tells the scheduler that a new application will be submitting tasks
     * to be scheduled.
     *
     * @param context the application's context
     * @param properties the <code>Properties</code> for the application
     *
     * @throws IllegalArgumentException if the context was already registered
     */
    public void registerApplication(KernelAppContext context,
                                    Properties properties);

    /**
     * Returns the next task to run. This call blocks until a task is
     * available to run.
     *
     * @return the next <code>ScheduledTask</code>
     *
     * @throws InterruptedException if the thread is interrupted while
     *                              waiting for a task
     */
    public ScheduledTask getNextTask() throws InterruptedException;

    /**
     * Reserves a space for a task.
     *
     * @return a <code>TaskReservation</code> for the task
     *
     * @throws TaskRejectedException if a reservation cannot be made, if
     *                               the task is recurring, or if the
     *                               associated context has not been registered
     */
    public TaskReservation reserveTask(ScheduledTask task);

    /**
     * Adds a task to the scheduler. This task is ready to execute as soon
     * as there are available resources and its starting time arrives. Note
     * that while recurring tasks are initially added to the scheduler
     * via <code>addRecurringTask</code>, each recurrence of that task
     * (including the first one) is added through this method, though a
     * unique instance of <code>ScheduledTask</code> must be provided each
     * time.
     *
     * @param task the <code>ScheduledTask</code> that is ready to run
     *
     * @throws TaskRejectedException if the task cannot be added, or if the
     *                               associated context has not been registered
     */
    public void addTask(ScheduledTask task);

    /**
     * Adds a task to the scheduler. This task is a recurring task that is
     * scheduled to start at some point in the future. The task will not
     * actually start executing until <code>start</code> is called on the
     * returned handle.
     *
     * @param task the <code>ScheduledTask</code> to run recurringly
     *
     * @return a <code>RecurringTaskHandle</code> that manages the task
     *
     * @throws TaskRejectedException if the associated context has not been
     *                               registered
     */
    public RecurringTaskHandle addRecurringTask(ScheduledTask task);

}
