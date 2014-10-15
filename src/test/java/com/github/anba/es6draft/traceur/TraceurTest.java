/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.traceur;

import static com.github.anba.es6draft.repl.global.V8ShellGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTestsAsArray;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.es6draft.repl.WindowTimers;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Functional.Function;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 *
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "traceur.test", file = "resource:/test-configuration.properties")
public class TraceurTest {
    private static final Configuration configuration = loadConfiguration(TraceurTest.class);

    @Parameters(name = "{0}")
    public static Iterable<Object[]> suiteValues() throws IOException {
        return loadTestsAsArray(configuration,
                new Function<Path, BiFunction<Path, Iterator<String>, TestInfo>>() {
                    @Override
                    public TestInfos apply(Path basedir) {
                        return new TestInfos(basedir);
                    }
                });
    }

    @ClassRule
    public static TestGlobals<V8ShellGlobalObject, TraceurTestInfo> globals = new TestGlobals<V8ShellGlobalObject, TraceurTestInfo>(
            configuration) {
        @Override
        protected Set<CompatibilityOption> getOptions() {
            EnumSet<CompatibilityOption> options = EnumSet.copyOf(super.getOptions());
            options.add(CompatibilityOption.AsyncFunction);
            options.add(CompatibilityOption.Exponentiation);
            options.add(CompatibilityOption.Comprehension);
            return options;
        }

        @Override
        protected ObjectAllocator<V8ShellGlobalObject> newAllocator(ShellConsole console,
                TraceurTestInfo test, ScriptCache scriptCache) {
            return newGlobalObjectAllocator(console, test.getBaseDir(), test.getScript(),
                    scriptCache);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ScriptExceptionHandler exceptionHandler = ScriptExceptionHandler.none();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public TraceurTestInfo test;

    private static final class TraceurTestInfo extends TestInfo {
        boolean expect = true;
        boolean async = false;

        TraceurTestInfo(Path basedir, Path script) {
            super(basedir, script);
        }
    }

    private V8ShellGlobalObject global;
    private AsyncHelper async;
    private WindowTimers timers;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // filter disabled tests
        assumeTrue(test.isEnabled());

        global = globals.newGlobal(new TraceurConsole(), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
        if (test.async) {
            async = install(new AsyncHelper(), AsyncHelper.class);
            timers = install(new WindowTimers(), WindowTimers.class);
        }
        if (test.expect) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            expected.expect(Matchers.either(StandardErrorHandler.defaultMatcher()).or(
                    ScriptExceptionHandler.defaultMatcher()));
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        if (global != null) {
            global.getScriptLoader().getExecutor().shutdown();
        }
    }

    @Test
    public void runTest() throws Throwable {
        // evaluate actual test-script
        global.eval(test.getScript(), test.toFile());

        // wait for pending tasks to finish
        if (test.async) {
            assertFalse(async.doneCalled);
            global.getRealm().getWorld().runEventLoop(timers);
            assertTrue(async.doneCalled);
        } else {
            global.getRealm().getWorld().runEventLoop();
        }
    }

    private <T> T install(T object, Class<T> clazz) {
        Realm realm = global.getRealm();
        Properties.createProperties(realm.defaultContext(), realm.getGlobalThis(), object, clazz);
        return object;
    }

    public static final class AsyncHelper {
        boolean doneCalled = false;

        @Properties.Function(name = "done", arity = 0)
        public void done() {
            assertFalse(doneCalled);
            doneCalled = true;
        }
    }

    private static final class TestInfos implements BiFunction<Path, Iterator<String>, TestInfo> {
        private static final Pattern FlagsPattern = Pattern.compile("\\s*//\\s*(.*)\\s*");
        private final Path basedir;

        public TestInfos(Path basedir) {
            this.basedir = basedir;
        }

        @Override
        public TestInfo apply(Path file, Iterator<String> lines) {
            TraceurTestInfo test = new TraceurTestInfo(basedir, file);
            Pattern p = FlagsPattern;
            while (lines.hasNext()) {
                String line = lines.next();
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String s = m.group(1);
                    if ("Only in browser.".equals(s) || s.startsWith("Skip.")) {
                        test.setEnabled(false);
                    } else if (s.equals("Async.")) {
                        test.async = true;
                    } else if (s.startsWith("Error:")) {
                        test.expect = false;
                    } else if (s.startsWith("Options:")) {
                        // ignore
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            return test;
        }
    }
}
