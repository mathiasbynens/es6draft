/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.ErrorConstructor;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 *
 */
public abstract class ShellGlobalObject extends GlobalObject {
    protected final ShellConsole console;
    protected final Realm realm;
    protected final Path baseDir;
    protected final Path script;
    protected final ScriptCache scriptCache;

    public ShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm);
        this.realm = realm;
        this.console = console;
        this.baseDir = baseDir;
        this.script = script;
        this.scriptCache = scriptCache;
    }

    @Override
    public void initialise(ExecutionContext cx) {
        super.initialise(cx);
        createProperties(this, cx, ShellGlobalObject.class);
    }

    /**
     * Compiles the script {@code name} from the 'scripts' directory
     */
    public static Script compileScript(ScriptCache scriptCache, String name) throws IOException,
            ParserException, CompilationException {
        String sourceName = "/scripts/" + name;
        try (InputStream stream = ShellGlobalObject.class.getResourceAsStream(sourceName)) {
            return scriptCache.script(sourceName, 1, stream);
        }
    }

    protected Path absolutePath(Path file) {
        return baseDir.resolve(file);
    }

    protected Path relativePath(Path file) {
        return baseDir.resolve(script.getParent().resolve(file));
    }

    /**
     * Parses, compiles and executes the javascript file
     */
    public void eval(Path fileName, Path file) throws IOException, ParserException,
            CompilationException {
        Script script = scriptCache.script(fileName.toString(), 1, file);
        ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    /**
     * Executes the given script
     */
    public void eval(Script script) {
        ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    /**
     * Parses, compiles and executes the javascript file (uses {@link #scriptCache})
     */
    public void include(Path file) throws IOException, ParserException, CompilationException {
        Script script = scriptCache.get(absolutePath(file));
        ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    protected static ScriptException throwError(Realm realm, String message) {
        ErrorConstructor ctor = (ErrorConstructor) realm.getIntrinsic(Intrinsics.Error);
        Object error = ctor.construct(realm.defaultContext(), Objects.toString(message, ""));
        return _throw(error);
    }

    protected String read(Path path) {
        if (!Files.exists(path)) {
            _throw(String.format("can't open '%s'", path.toString()));
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw throwError(realm, e.getMessage());
        }
    }

    protected Object load(Path fileName, Path path) {
        if (!Files.exists(path)) {
            _throw(String.format("can't open '%s'", path.toString()));
        }
        try {
            eval(fileName, path);
            return UNDEFINED;
        } catch (IOException e) {
            throw throwError(realm, e.getMessage());
        } catch (ParserException | CompilationException e) {
            throw e.toScriptException(realm.defaultContext());
        }
    }

    /**
     * Returns the well-known symbol {@code name} or undefined if there is no such symbol
     */
    @Function(name = "getSym", arity = 1)
    public Object getSym(String name) {
        try {
            if (name.startsWith("@@")) {
                return BuiltinSymbol.valueOf(name.substring(2)).get();
            }
        } catch (IllegalArgumentException e) {
        }
        return UNDEFINED;
    }

    /**
     * Creates a new Symbol object
     */
    @Function(name = "newSym", arity = 2)
    public Object newSym(String name, boolean _private) {
        return new Symbol(name, _private);
    }
}
