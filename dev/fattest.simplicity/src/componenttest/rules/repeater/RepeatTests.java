/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.RepeatTestFilter;

/**
 * Used as a JUnit <code>@ClassRule</code> to repeat all tests with the specified actions.
 * <br>
 * <b>NOTE: Order is significant when building the object! Tests will be run in the order of the builder methods.</b>
 * <br>
 * For example, if a FAT was written with Java EE 7 features initially, and we want to repeat
 * the tests with Java EE 8 equivalent features, do the following:
 *
 * <pre>
 * <code>@ClassRule
 * public static RepeatTests r = new RepeatTests().withoutModification()
 *                                                .andWith(FeatureReplacementAction.EE8_FEATURES());
 * </code>
 * </pre>
 */
public class RepeatTests extends ExternalResource {

    /**
     * Adds an iteration of test execution without making any modifications
     */
    public static RepeatTests withoutModification() {
        return new RepeatTests().andWithoutModification();
    }

    /**
     * Adds an iteration of test execution, where the action.setup() is called before repeating the tests.
     */
    public static RepeatTests with(RepeatTestAction action) {
        return new RepeatTests().andWith(action);
    }

    private static final RepeatTestAction NO_MODIFICATION_ACTION = new EmptyAction();

    private final List<RepeatTestAction> actions = new ArrayList<>();

    private RepeatTests() {
        // private ctor, require static entry point
    }

    /**
     * Adds an iteration of test execution without making any modifications
     */
    public RepeatTests andWithoutModification() {
        actions.add(NO_MODIFICATION_ACTION);
        return this;
    }

    /**
     * Adds an iteration of test execution, where the action.setup() is called before repeating the tests.
     */
    public RepeatTests andWith(RepeatTestAction action) {
        actions.add(action);
        return this;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new CompositeRepeatTestActionStatement(actions, statement);
    }

    private static class CompositeRepeatTestActionStatement extends Statement {
        private static Class<?> c = CompositeRepeatTestActionStatement.class;

        private final Statement statement;
        private final List<RepeatTestAction> actions;

        private CompositeRepeatTestActionStatement(List<RepeatTestAction> actions, Statement statement) {
            this.statement = statement;
            this.actions = actions;
        }

        @Override
        public void evaluate() throws Throwable {
            final String m = "evaluate";
            Log.info(c, m, "All tests will run " + actions.size() + " times:");
            for (int i = 0; i < actions.size(); i++)
                Log.info(c, m, "  [" + i + "] " + actions.get(i));
            for (RepeatTestAction action : actions) {
                RepeatTestFilter.CURRENT_REPEAT_ACTION = action.getID();
                if (action.isEnabled()) {
                    Log.info(c, m, "===================================");
                    Log.info(c, m, "");
                    Log.info(c, m, "Running tests with action: " + action);
                    Log.info(c, m, "");
                    Log.info(c, m, "===================================");
                    action.setup();
                    statement.evaluate();
                }
            }
        }
    }
}